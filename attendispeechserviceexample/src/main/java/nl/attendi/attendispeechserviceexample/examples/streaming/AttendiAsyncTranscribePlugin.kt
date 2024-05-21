/// Copyright 2023 Attendi Technology B.V.
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.

package nl.attendi.attendispeechserviceexample.examples.streaming

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNames
import nl.attendi.attendispeechservice.client.AttendiClient
import nl.attendi.attendispeechservice.client.TranscribeAPIConfig
import nl.attendi.attendispeechservice.components.attendimicrophone.AttendiMicrophoneState
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.AttendiMicrophonePlugin

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

const val WEBSOCKET_NORMAL_CLOSURE_CODE = 1000

/**
 * This message is sent to the transcription server when the microphone starts recording.
 */
@Serializable
data class ClientConfigurationMessage(
    /**
     * Is always "ClientConfiguration" for this message type. However, the serialization somehow
     * doesn't include the messageType in the JSON when we use a default value for the field.
     */
    val messageType: String,
    /**
     * The model to use for transcription. If not specified, the backend uses a default model
     * specified for the customer.
     */
    val model: String?,
    // TODO: the sample rate will be removed as we currently only support 16 kHz.
    val sampleRate: Int,
    /**
     * Allows for associating multiple transcriptions into `sessions` and `reports`.
     */
    val reportUuid: String?,
    val sessionUuid: String?
)

/**
 * Data model for messages from the transcription websocket server.
 */
@Serializable
data class IncomingTranscriptionMessage @OptIn(ExperimentalSerializationApi::class) constructor(
    // TODO: should we rename the field to `messageType` for consistency?
    @JsonNames("type") val messageType: IncomingTranscriptionMessageType, val text: String
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
enum class IncomingTranscriptionMessageType {
    // Anticipating changing the names of the message types (current ones are `UnprocessedSegment`,
    // `ProcessedSegment`, `ProcessedStream`)
    /**
     * The intermediate segments that can still change.
     */
    @JsonNames("UnprocessedSegment", "TentativeSegment")
    TentativeSegment,

    /**
     * Intermediate segments that are final. Some more postprocessing is usually done on these segments.
     */
    @JsonNames("ProcessedSegment", "FinalSegment")
    FinalSegment,

    /**
     * The final transcription of the audio stream. This can be used to replace the text in the UI
     * in the end.
     */
    @JsonNames("ProcessedStream", "CompletedStream")
    CompletedStream
}

/**
 * The time to wait for the server to close the socket after the end of the audio stream is sent.
 */
const val waitForServerToCloseSocketAfterEndOfAudioStreamTimeoutMilliseconds = 5000L

/**
 * When the microphone starts recording, set up a websocket connection with the streaming transcription
 * server, and start sending audio frames and receiving transcription results.
 */
class AttendiAsyncTranscribePlugin(
    private val apiConfig: TranscribeAPIConfig,
    /**
     * Called when the socket is closing. The code is the status code sent by the server, and the
     * reason is a human-readable string explaining why the server closed the connection.
     *
     * The `AttendiMicrophoneState` is passed to the callback to allow calling some plugin
     * functionality at the plugin's callsite.
     */
    private val onSocketClosing: (webSocket: WebSocket, code: Int, reason: String, state: AttendiMicrophoneState) -> Unit = { _, _, _, _ -> },
    /**
     * Called when the socket fails. The throwable is the exception that caused the failure, or
     * null if the failure was a timeout.
     */
    private val onSocketFailure: (webSocket: WebSocket, t: Throwable, response: Response?, state: AttendiMicrophoneState) -> Unit = { _, _, _, _ -> },
    /**
     * Called when a message is received from the server.
     */
    private val onIncomingMessage: (IncomingTranscriptionMessage, state: AttendiMicrophoneState) -> Unit,
) : AttendiMicrophonePlugin {
    private val client = AttendiClient(apiConfig)

    // TODO: check: is it necessary to refresh this token in this plugin?
    private var authenticationToken: String? = null

    private var socket: WebSocket? = null

    private val N_SAMPLES_PER_MESSAGE = 4224 // around 264 ms of audio at 16 kHz
    private var streamingBuffer = mutableListOf<Short>()

    private val reportUUID: UUID = UUID.randomUUID()
    private val sessionUUID: UUID = UUID.randomUUID()

    override fun activate(state: AttendiMicrophoneState) {
        state.onBeforeStartRecording {
            if (authenticationToken == null) authenticationToken = client.authenticate(apiConfig)

            assert(socket == null) { "Socket should not be set when starting to record, was null" }

            val socketBaseUrl =
                client.getApiURL()?.replace("http", "ws") ?: throw Exception("No API URL provided")
            val socketUrl = "$socketBaseUrl/v1/speech/transcribe/stream"

            val request = Request.Builder().url(socketUrl)
                .addHeader("Authorization", "Bearer $authenticationToken").build()

            socket = OkHttpClient().newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    val configMessage = Json.encodeToString(
                        ClientConfigurationMessage(
                            // TODO: Use a default for the messageType. But somehow the serialization then doesn't
                            //  include the messageType in the JSON. So for now we set it explicitly to "ClientConfiguration".
                            messageType = "ClientConfiguration",
                            // TODO: I don't think it's always necessary to include the model
                            model = "ResidentialCare",
                            // TODO: I don't think we need to include the sample rate
                            sampleRate = 16000,
                            reportUuid = reportUUID.toString(),
                            sessionUuid = sessionUUID.toString()
                        )
                    )

                    webSocket.send(configMessage)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    val deserialized = Json.decodeFromString<IncomingTranscriptionMessage>(text)
                    onIncomingMessage(deserialized, state)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    onSocketClosing(webSocket, code, reason, state)
                    socket = null

                    if (code == WEBSOCKET_NORMAL_CLOSURE_CODE) return

                    // When the connection is closed abnormally, stop the microphone and show an error.
                    state.coroutineScope.launch {
                        state.stop(delayMilliseconds = 0)

                        for (errorCallback in state.errorCallbacks) {
                            errorCallback(Exception("WebSocket failure: $reason"))
                        }
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    onSocketFailure(webSocket, t, response, state)
                    socket = null

                    state.coroutineScope.launch {
                        state.stop(delayMilliseconds = 0)

                        for (errorCallback in state.errorCallbacks) {
                            errorCallback(Exception("WebSocket failure: $t"))
                        }
                    }
                }
            })
        }

        // When receiving audio frames from the microphone, we collect the samples in a buffer. When
        // we have enough samples in the buffer, we send them to the server.
        state.onAudioFrames { audioFrames ->
            if (socket == null) return@onAudioFrames

            streamingBuffer.addAll(audioFrames)

            // Wait until we have enough audio frames to send
            if (streamingBuffer.size < N_SAMPLES_PER_MESSAGE) return@onAudioFrames

            // Get the first N_SAMPLES_PER_MESSAGE frames and remove them from the buffer
            val frames = streamingBuffer.subList(0, N_SAMPLES_PER_MESSAGE).toMutableList()
            streamingBuffer = streamingBuffer.drop(N_SAMPLES_PER_MESSAGE).toMutableList()

            socket?.send(frames.toByteString())
        }

        state.onStopRecording {
            // Send the remaining audio frames
            socket?.send(streamingBuffer.toByteString())
            // Clear the streaming buffer
            streamingBuffer = mutableListOf()

            // TODO: probably nicer not to use a raw string here
            socket?.send("{\"messageType\": \"endOfAudioStream\"}")

            // Set a timeout for the server to close the connection
            val reachedTimeoutWithoutClosing =
                withTimeoutOrNull(waitForServerToCloseSocketAfterEndOfAudioStreamTimeoutMilliseconds) {
                    // Wait for the server to close the connection.
                    // This assumes that we set the socket to null in the onClosing and onFailure callbacks.
                    while (socket != null) {
                        delay(50)
                    }

                    // False since we didn't reach the timeout, since the socket is null and was closed.
                    false
                } ?: true

            // Close the socket if still open after the timeout
            if (reachedTimeoutWithoutClosing) {
                // TODO: find proper code and reason to close it with
                socket?.close(1001, "Timeout reached after end of audio stream message sent")
                // TODO: I don't think we have to set the socket to null here, since it's done in the callbacks
                socket = null
            }

        }
    }
}

private fun List<Short>.toByteString(): ByteString {
    val byteBuffer = ByteBuffer.allocate(this.size * 2).order(ByteOrder.LITTLE_ENDIAN)
    for (short in this) {
        byteBuffer.putShort(short)
    }

    // We need to reset the position to 0 before converting to a ByteString.
    // The buffer position is at the end of the buffer after the loop, which
    // causes `.toByteString()` to return an empty ByteString.
    byteBuffer.position(0)
    return byteBuffer.toByteString()
}
