package nl.attendi.attendispeechservice.components.attendimicrophone.plugins

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import nl.attendi.attendispeechservice.domain.connection.AttendiConnection
import nl.attendi.attendispeechservice.domain.connection.AttendiConnectionError
import nl.attendi.attendispeechservice.domain.connection.AttendiConnectionListener
import nl.attendi.attendispeechservice.components.attendimicrophone.AttendiMicrophoneState
import nl.attendi.attendispeechservice.data.decoder.AttendiDefaultMessageDecoder
import nl.attendi.attendispeechservice.domain.decoder.AttendiMessageDecoder
import nl.attendi.attendispeechservice.domain.model.transcribestream.AttendiStreamState
import nl.attendi.attendispeechservice.domain.model.transcribestream.AttendiTranscribeStream
import nl.attendi.attendispeechservice.components.attendimicrophone.AttendiMicrophone
import nl.attendi.attendispeechservice.data.connection.websocket.AttendiWebSocketConnection

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
 * A plugin for real-time, asynchronous speech transcription using [AttendiConnection] and [AttendiMessageDecoder].
 *
 * This plugin connects to a WebSocket transcription service, listens to audio frames from the microphone,
 * sends them in chunks, and updates the transcription stream based on incoming messages.
 *
 * Can be registered into an [AttendiMicrophone] pipeline using the [AttendiMicrophonePlugin] interface.
 *
 * @param connection The connection implementation (e.g., [AttendiWebSocketConnection]) used to send audio and receive messages.
 * @param messageDecoder The decoder used to interpret JSON messages from the backend. Defaults to [AttendiDefaultMessageDecoder].
 * @param onStreamUpdated Callback invoked whenever the transcribe stream is updated with new actions.
 * @param onStreamCompleted Callback invoked when the session ends normally or with an error.
 * @param onError Callback invoked when a connection or decoding error occurs.
 */
class AttendiAsyncTranscribePlugin(
    private val connection: AttendiConnection,
    private val messageDecoder: AttendiMessageDecoder = AttendiDefaultMessageDecoder,
    private val onStreamUpdated: (AttendiTranscribeStream) -> Unit,
    private val onStreamCompleted: (AttendiTranscribeStream, withError: Boolean) -> Unit = { _, _ -> },
    private val onError: (AttendiAsyncTranscribePluginError, AttendiTranscribeStream) -> Unit = { _, _ -> }
) : AttendiMicrophonePlugin {

    private var streamingBuffer = mutableListOf<Short>()

    companion object {
        const val N_SAMPLES_PER_MESSAGE = 4224 // around 264 ms of audio at 16 kHz
    }

    private var transcribeStream = AttendiTranscribeStream(
        state = AttendiStreamState(text = "", annotations = emptyList()),
        operationHistory = emptyList(),
        undoneOperations = emptyList()
    )

    private var removeAudioFramesListener: (() -> Unit)? = null
    private var removeStopRecordingListener: (() -> Unit)? = null
    private var streamFinishedWithError = false
    private var isClosingConnection = false

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
            connection.connect(listener = object : AttendiConnectionListener {

                override fun onOpen() {
                    clearTranscribeStream()
                    isClosingConnection = false
                    streamFinishedWithError = false
                }

                override fun onMessage(message: String) {
                    try {
                        val transcribeActions = messageDecoder.decode(message)
                        transcribeStream = transcribeStream.receiveActions(transcribeActions)
                        onStreamUpdated(transcribeStream)
                    } catch (e: Exception) {
                        streamFinishedWithError = true
                        onError(AttendiAsyncTranscribePluginError.Decode(e), transcribeStream)
                        state.coroutineScope.launch {
                            state.stop(delayMilliseconds = 0)
                            for (errorCallback in state.errorCallbacks) {
                                errorCallback(Exception("Async Transcribe Decode error"))
                            }
                        }
                        closeConnection()
                    }
                }

                override fun onError(error: AttendiConnectionError) {
                    onError(AttendiAsyncTranscribePluginError.Connection(error), transcribeStream)

                    state.coroutineScope.launch {
                        state.stop(delayMilliseconds = 0)
                        for (errorCallback in state.errorCallbacks) {
                            errorCallback(Exception("Async Transcribe Connection error"))
                        }
                    }

                    onStreamCompleted(transcribeStream, true)
                }

                override fun onClose() {
                    onStreamCompleted(transcribeStream, streamFinishedWithError)
                }
            })

            audioJob = state.coroutineScope.launch {
                removeAudioFramesListener = state.onAudioFrames { audioFrames ->
                    processAudioFrames(audioFrames)
                }

                removeStopRecordingListener = state.onStopRecording {
                    sendRemainingRecording()
                    closeConnection()
                }
            }
        }
    }

    private suspend fun sendRemainingRecording() {
        if (connection.isConnected()) {
            connection.send(streamingBuffer.toShortArray())
        }
    }

    private fun closeConnection() {
        if (isClosingConnection) { return }
        isClosingConnection = true

        audioJob?.cancel()

        removeAudioFramesListener?.invoke()
        removeAudioFramesListener = null

        removeStopRecordingListener?.invoke()
        removeStopRecordingListener = null

        connection.disconnect()
        streamingBuffer.clear()
    }

    private fun clearTranscribeStream() {
        transcribeStream = AttendiTranscribeStream(
            state = AttendiStreamState(text = "", annotations = emptyList()),
            operationHistory = emptyList(),
            undoneOperations = emptyList()
        )
    }

    /**
     * Deactivates the plugin, closes the WebSocket connection, and clears any pending audio buffers or listeners.
     */
    override fun deactivate(state: AttendiMicrophoneState) {
        closeConnection()
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
        if (!connection.isConnected()) {
            return
        }

        streamingBuffer.addAll(audioFrames)

        // Wait until we have enough audio frames to send
        if (streamingBuffer.size < N_SAMPLES_PER_MESSAGE) return

        // Get the first N_SAMPLES_PER_MESSAGE frames and remove them from the buffer
        val frames = streamingBuffer.subList(0, N_SAMPLES_PER_MESSAGE).toMutableList()
        if (connection.send(frames.toShortArray())) {
            streamingBuffer = streamingBuffer.drop(N_SAMPLES_PER_MESSAGE).toMutableList()
        }
    }
}