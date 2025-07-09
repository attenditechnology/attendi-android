package nl.attendi.attendispeechservice.components.attendimicrophone.plugins.asynctranscribe

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import nl.attendi.attendispeechservice.domain.connection.AttendiConnection
import nl.attendi.attendispeechservice.domain.connection.AttendiConnectionError
import nl.attendi.attendispeechservice.domain.connection.AttendiConnectionListener
import nl.attendi.attendispeechservice.components.attendimicrophone.AttendiMicrophoneState
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.AttendiMicrophonePlugin
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.asynctranscribe.data.decoder.AttendiTranscribeAsyncDefaultMessageDecoder
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.asynctranscribe.domain.decoder.AttendiTranscribeAsyncMessageDecoder
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.asynctranscribe.domain.model.transcribestream.AttendiTranscribeStream
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.asynctranscribe.data.connection.websocket.AttendiWebSocketConnection

/**
 * Represents possible errors that can occur during async transcription via [AttendiAsyncTranscribePlugin].
 */
sealed class AttendiAsyncTranscribePluginError {

    /**
     * A connection-related error occurred (e.g., failed to connect, disconnect timeout).
     *
     * Wraps an instance of [AttendiConnectionError].
     */
    data class Connection(val error: AttendiConnectionError) : AttendiAsyncTranscribePluginError()

    /**
     * A decoding failure occurred while parsing a WebSocket message into transcribe actions.
     *
     * Wraps the original [Throwable] to aid debugging.
     */
    data class Decode(val throwable: Throwable) : AttendiAsyncTranscribePluginError()
}

/**
 * A plugin for real-time, asynchronous speech transcription using [AttendiConnection] and [AttendiTranscribeAsyncMessageDecoder].
 * It is designed around the Strategy Design Pattern to promote extensibility and customizability.
 * Two key components — AttendiConnection and AttendiMessageDecoder — are injected via constructor parameters,
 * allowing users to swap or extend behavior without modifying the core plugin.
 *
 * By using interfaces (AttendiConnection, AttendiMessageDecoder), we decouple the plugin logic from specific implementations:
 * AttendiConnection: Defines how audio is sent and responses are received.
 * AttendiMessageDecoder: Defines how incoming messages are interpreted into List<TranscribeAsyncAction> models.
 *
 * For typical use cases with the Attendi WebSocket service, the plugin provides:
 * AttendiWebSocketConnection: A default WebSocket-based connection that handles sending/receiving messages to Attendi’s servers.
 * AttendiDefaultMessageDecoder: A decoder that interprets Attendi-formatted JSON messages into actionable TranscribeAsyncAction objects.
 * These cover most use cases out of the box.
 *
 * If you want to integrate Attendi’s plugin with your own transcription server, or use a different message format,
 * you can do so by providing custom implementations of the AttendiConnection and AttendiMessageDecoder interfaces.
 *
 * @param connection The connection implementation (e.g., [AttendiWebSocketConnection]) used to send audio and receive messages.
 * @param messageDecoder The decoder used to interpret JSON messages from the backend. Defaults to [AttendiTranscribeAsyncDefaultMessageDecoder].
 * @param onStreamUpdated Callback invoked whenever the transcribe stream is updated with new actions.
 * @param onStreamCompleted Callback invoked when the session ends normally or with an error [AttendiAsyncTranscribePluginError].
 */
class AttendiAsyncTranscribePlugin(
    private val connection: AttendiConnection,
    private val messageDecoder: AttendiTranscribeAsyncMessageDecoder = AttendiTranscribeAsyncDefaultMessageDecoder,
    private val onStreamConnecting: () -> Unit = { },
    private val onStreamStarted: () -> Unit = { },
    private val onStreamUpdated: (AttendiTranscribeStream) -> Unit,
    private val onStreamCompleted: (AttendiTranscribeStream, error: AttendiAsyncTranscribePluginError?) -> Unit = { _, _ -> }
) : AttendiMicrophonePlugin {

    private companion object {
        const val N_SAMPLES_PER_MESSAGE = 4224 // around 264 ms of audio at 16 kHz
    }

    private var transcribeStream = AttendiTranscribeStream()
    private var streamingBuffer = mutableListOf<Short>()
    private var removeAudioFramesListener: (() -> Unit)? = null
    private var removeStopRecordingListener: (() -> Unit)? = null
    private var isConnectionOpen = false
    private var isClosingConnection = false
    private var pluginError: AttendiAsyncTranscribePluginError? = null
    private var audioJob: Job? = null

    /**
     * Activates the plugin and sets up listeners for:
     * - WebSocket lifecycle (open, message, error, close)
     * - Audio frame streaming from the microphone
     * - Stop recording events
     *
     * Starts sending audio when enough buffered samples are collected.
     */
    override fun activate(state: AttendiMicrophoneState) {
        state.onBeforeStartRecording {
            resetPluginState()
            onStreamConnecting()
            try {
                connection.connect(listener = object : AttendiConnectionListener {
                    override fun onOpen() {
                        isConnectionOpen = true
                        onStreamStarted()
                    }

                    override fun onMessage(message: String) {
                        try {
                            val transcribeActions = messageDecoder.decode(message)
                            transcribeStream = transcribeStream.receiveActions(transcribeActions)
                            onStreamUpdated(transcribeStream)
                        } catch (e: Exception) {
                            pluginError = AttendiAsyncTranscribePluginError.Decode(e)
                            forceStopMicrophone(state, "Async Transcribe Decode error")
                            state.coroutineScope.launch {
                                closeConnection()
                            }
                        }
                    }

                    override fun onError(error: AttendiConnectionError) {
                        forceStopMicrophone(state, "Async Transcribe Connection error")
                        onStreamCompleted(transcribeStream,
                            AttendiAsyncTranscribePluginError.Connection(error)
                        )
                    }

                    override fun onClose() {
                        isConnectionOpen = false
                        onStreamCompleted(transcribeStream, pluginError)
                    }
                })

                audioJob = state.coroutineScope.launch {
                    removeAudioFramesListener = state.onAudioFrames { audioFrames ->
                        processAudioFrames(audioFrames)
                    }

                    removeStopRecordingListener = state.onStopRecording {
                        sendRemainingAudio()
                        closeConnection()
                    }
                }
            } catch (e: Exception) {
                pluginError =
                    AttendiAsyncTranscribePluginError.Connection(AttendiConnectionError.Unknown("Connection Failed"))
                forceStopMicrophone(state, "Connection failed: ${e.message}")
                onStreamCompleted(transcribeStream, pluginError)
            }
        }
    }

    private fun resetPluginState() {
        transcribeStream = AttendiTranscribeStream()
        isConnectionOpen = false
        isClosingConnection = false
        pluginError = null
    }

    private fun forceStopMicrophone(state: AttendiMicrophoneState, message: String) {
        state.coroutineScope.launch {
            state.stop(delayMilliseconds = 0)
            for (errorCallback in state.errorCallbacks.toList()) {
                errorCallback(Exception(message))
            }
        }
    }

    private suspend fun sendRemainingAudio() {
        connection.send(streamingBuffer.toShortArray())
    }

    private suspend fun closeConnection() {
        if (isClosingConnection) { return }
        isClosingConnection = true
        pluginError = null

        audioJob?.cancel()

        removeAudioFramesListener?.invoke()
        removeAudioFramesListener = null

        removeStopRecordingListener?.invoke()
        removeStopRecordingListener = null

        connection.disconnect()
        streamingBuffer.clear()
    }

    /**
     * Deactivates the plugin, closes the WebSocket connection, and clears any pending audio buffers or listeners.
     */
    override fun deactivate(state: AttendiMicrophoneState) {
        state.coroutineScope.launch {
            closeConnection()
        }
    }

    // Helper extension to convert List<Short> to ByteArray for sending
    private fun List<Short>.toShortArray(): ByteArray {
        val byteArray = ByteArray(this.size * 2)
        this.forEachIndexed { i, value ->
            byteArray[i * 2] = (value.toInt() and 0xFF).toByte()
            byteArray[i * 2 + 1] = ((value.toInt() shr 8) and 0xFF).toByte()
        }
        return byteArray
    }

    private suspend fun processAudioFrames(audioFrames: List<Short>) {
        // Wait for the socket connection to be open prior on adding frames to the buffer
        if (!isConnectionOpen) {
            return
        }

        streamingBuffer.addAll(audioFrames)

        // Wait until we have enough audio frames to send
        if (streamingBuffer.size < N_SAMPLES_PER_MESSAGE) return

        // Get the first N_SAMPLES_PER_MESSAGE frames and remove them from the buffer
        val frames = streamingBuffer.subList(0, N_SAMPLES_PER_MESSAGE
        ).toMutableList()
        if (connection.send(frames.toShortArray())) {
            streamingBuffer = streamingBuffer.drop(N_SAMPLES_PER_MESSAGE).toMutableList()
        }
    }
}