package nl.attendi.attendispeechservice.components.attendirecorder.plugins.asynctranscribeplugin

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import nl.attendi.attendispeechservice.audio.AudioEncoder
import nl.attendi.attendispeechservice.audio.AudioFrame
import nl.attendi.attendispeechservice.components.attendirecorder.plugins.asynctranscribeplugin.decoder.AsyncTranscribeMessageDecoder
import nl.attendi.attendispeechservice.components.attendirecorder.plugins.asynctranscribeplugin.decoder.AttendiAsyncTranscribeMessageDecoderFactory
import nl.attendi.attendispeechservice.services.asynctranscribe.AttendiAsyncTranscribeServiceImpl
import nl.attendi.attendispeechservice.components.attendirecorder.plugins.asynctranscribeplugin.decoder.AttendiAsyncTranscribeMessageDecoder
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderModel
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderPlugin
import nl.attendi.attendispeechservice.components.attendirecorder.plugins.asynctranscribeplugin.model.AttendiTranscribeStream
import nl.attendi.attendispeechservice.services.asynctranscribe.AsyncTranscribeService
import nl.attendi.attendispeechservice.services.asynctranscribe.AsyncTranscribeServiceError
import nl.attendi.attendispeechservice.services.asynctranscribe.AsyncTranscribeServiceListener
import nl.attendi.attendispeechservice.utils.invokeAll

/**
 * A plugin for real-time, asynchronous speech transcription using [AsyncTranscribeService] and [AsyncTranscribeMessageDecoder].
 * Designed to be extensible and customizable by allowing custom implementations of [AsyncTranscribeService] and [AsyncTranscribeMessageDecoder] to be passed in.
 * Two key components — AttendiConnection and AttendiMessageDecoder — are injected via constructor parameters,
 * allowing users to swap or extend behavior without modifying the core plugin.
 *
 * By using interfaces (AttendiConnection, AttendiMessageDecoder), we decouple the plugin logic from specific implementations:
 * AsyncTranscribeService: Defines how and where audio is sent and responses are received.
 * TranscribeAsyncMessageDecoder: Defines how incoming messages are interpreted into List<TranscribeAsyncAction> models.
 *
 * For typical use cases with the Attendi WebSocket service, the plugin provides:
 * [AttendiAsyncTranscribeServiceImpl]: A default WebSocket-based connection that handles sending/receiving messages to Attendi’s servers.
 * [AttendiAsyncTranscribeMessageDecoder]: A decoder that interprets Attendi-formatted JSON messages into actionable TranscribeAsyncAction objects.
 * These cover most use cases out of the box.
 *
 * If you want to integrate Attendi’s plugin with your own transcription server, or use a different message format,
 * you can do so by providing custom implementations of the AttendiConnection and AttendiMessageDecoder interfaces.
 *
 * @param service The transcribe async service (e.g., [AttendiAsyncTranscribeServiceImpl]) used to send audio and receive messages.
 * @param serviceMessageDecoder The decoder used to interpret JSON messages from the backend. Defaults to [AttendiAsyncTranscribeMessageDecoder].
 * @param onStreamConnecting Callback invoked when the plugin is preparing and attempting to establish a connection.
 * @param onStreamStarted Callback invoked once the stream has been successfully established and is ready to receive audio.
 * @param onStreamUpdated Callback invoked whenever the transcribe stream is updated with new actions.
 * @param onStreamCompleted Callback invoked when the session ends normally or with an exception.
 */
class AttendiAsyncTranscribePlugin(
    private val service: AsyncTranscribeService,
    private val serviceMessageDecoder: AsyncTranscribeMessageDecoder = AttendiAsyncTranscribeMessageDecoderFactory.create(),
    private val onStreamConnecting: () -> Unit = { },
    private val onStreamStarted: () -> Unit = { },
    private val onStreamUpdated: (AttendiTranscribeStream) -> Unit,
    private val onStreamCompleted: (AttendiTranscribeStream, error: Exception?) -> Unit = { _, _ -> }
) : AttendiRecorderPlugin {

    private var transcribeStream = AttendiTranscribeStream()
    private var streamingBuffer = mutableListOf<Short>()
    // We only send audio data on an open connection.
    private var isConnectionOpen = false
    // Avoid closing twice
    private var isClosingConnection = false
    // We store the pluginError in an instance variable so we can pass it to the `onStreamCompleted` callback, even if the error was generated in a different method.
    private var pluginError: Exception? = null
    private var audioJob: Job? = null
    private var scope = CoroutineScope(Dispatchers.IO)

    /**
     * Activates the plugin and sets up listeners for:
     * - WebSocket lifecycle (open, message, error, close)
     * - Audio frame streaming from the microphone
     * - Stop recording events
     *
     * Starts sending audio when enough buffered samples are collected.
     */
    override suspend fun activate(model: AttendiRecorderModel) {
        model.onBeforeStartRecording {
            resetPluginState()
            onStreamConnecting()
            try {
                service.connect(listener = object :
                    AsyncTranscribeServiceListener {
                    override fun onOpen() {
                        isConnectionOpen = true
                        onStreamStarted()
                    }

                    override fun onMessage(message: String) {
                        try {
                            val transcribeActions = serviceMessageDecoder.decode(message)
                            transcribeStream = transcribeStream.receiveActions(transcribeActions)
                            onStreamUpdated(transcribeStream)
                        } catch (exception: Exception) {
                            forceStopRecording(model, exception)
                            scope.launch {
                                closeConnection()
                            }
                        }
                    }

                    override fun onError(error: AsyncTranscribeServiceError) {
                        forceStopRecording(model, error)
                    }

                    override fun onClose() {
                        isConnectionOpen = false
                        onStreamCompleted(transcribeStream, pluginError)
                    }
                })
            } catch (exception: Exception) {
                forceStopRecording(model, exception)
            }
        }

        model.onAudio { audioFrame ->
            processAudioFrame(audioFrame)
        }

        model.onBeforeStopRecording {
            closeConnection()
        }
    }

    private fun resetPluginState() {
        transcribeStream = AttendiTranscribeStream()
        isConnectionOpen = false
        isClosingConnection = false
        pluginError = null
    }

    private fun forceStopRecording(model: AttendiRecorderModel, exception: Exception) {
        pluginError = exception
        scope.launch {
            model.stop()
            model.callbacks.onError.invokeAll(exception)
        }
    }

    private suspend fun closeConnection() {
        if (isClosingConnection) {
            return
        }
        isClosingConnection = true
        pluginError = null

        audioJob?.cancel()

        service.disconnect()
        streamingBuffer.clear()
    }

    /**
     * Deactivates the plugin, closes the WebSocket connection, and clears any pending audio buffers or listeners.
     */
    override suspend fun deactivate(model: AttendiRecorderModel) {
        scope.launch {
            closeConnection()
        }
    }

    private suspend fun processAudioFrame(audioFrame: AudioFrame) {
        // Wait for the socket connection to be open prior on adding frames to the buffer
        if (!isConnectionOpen) {
            return
        }
        val byteArray = AudioEncoder.shortsToByteArray(audioFrame.samples)
        service.send(byteArray)
    }
}