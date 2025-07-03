package nl.attendi.attendispeechservice.data.connection.websocket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import nl.attendi.attendispeechservice.data.client.AttendiClient
import nl.attendi.attendispeechservice.data.client.TranscribeAPIConfig
import nl.attendi.attendispeechservice.domain.connection.AttendiConnection
import nl.attendi.attendispeechservice.domain.connection.AttendiConnectionError
import nl.attendi.attendispeechservice.domain.connection.AttendiConnectionListener
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.toByteString
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * A concrete implementation of [AttendiConnection] using a WebSocket-based streaming protocol.
 *
 * This class is part of the Attendi SDK and manages the real-time communication channel to
 * the Attendi transcription backend. It handles connection lifecycle, message dispatching,
 * and error reporting according to the [AttendiConnection] contract.
 *
 * Consumers may use this implementation as-is or provide their own by implementing [AttendiConnection].
 *
 * @param accessToken Optional pre-acquired access token for authentication. If null, the SDK
 * will attempt to authenticate using [TranscribeAPIConfig].
 * @param apiConfig Configuration options for accessing the Attendi API.
 */
class AttendiWebSocketConnection(
    private val accessToken: String? = null,
    private val apiConfig: TranscribeAPIConfig
) : AttendiConnection {

    companion object {
        /** Maximum duration to wait when establishing a connection. */
        const val CONNECTION_TIMEOUT_MILLIS = 20_000L

        /** Timeout to wait for the server to close the connection after sending the end-of-stream message. */
        const val SERVER_CLOSE_SOCKET_TIMEOUT_MILLIS = 5_000L

        /** Polling interval for checking if the server closed the socket. */
        const val SERVER_CLOSE_SOCKET_INTERVAL_CHECK_MILLIS = 50L

        /** WebSocket closure code indicating a normal, expected shutdown. */
        const val WEBSOCKET_NORMAL_CLOSURE_CODE = 1000

        /** WebSocket closure code indicating a timeout, forced shutdown. */
        const val WEBSOCKET_TIMEOUT_CLOSURE_CODE = 1001
    }

    @Volatile private var socket: WebSocket? = null
    @Volatile private var isSocketConnected = false
    private var listener: AttendiConnectionListener? = null

    private val client = AttendiClient(apiConfig)
    private val okHttpClient = OkHttpClient()

    private var socketJob = SupervisorJob()
    private var socketScope = CoroutineScope(Dispatchers.IO + socketJob)

    /**
     * Initiates a WebSocket connection to the Attendi streaming API.
     *
     * This method runs asynchronously and will emit events to the provided [listener]
     * for success, errors, and incoming messages.
     *
     * @param listener An implementation of [AttendiConnectionListener] to observe connection state.
     */
    override fun connect(listener: AttendiConnectionListener) {
        this.listener = listener
        resetScope()

        socketScope.launch {
            val token: String = accessToken
                ?: try {
                    client.authenticate(apiConfig)
                } catch (e: Exception) {
                    listener.onError(AttendiConnectionError.FailedToConnect(message = e.message))
                    return@launch
                }

            val socketUrl = makeSocketUrl()
            if (socketUrl == null) {
                listener.onError(AttendiConnectionError.FailedToConnect(message = "No API URL provided"))
                return@launch
            }

            val request = Request.Builder()
                .url(socketUrl)
                .addHeader("Authorization", "Bearer $token")
                .build()

            try {
                connectSocket(request)
            } catch (e: TimeoutCancellationException) {
                listener.onError(AttendiConnectionError.ConnectTimeout)
            } catch (e: Exception) {
                listener.onError(AttendiConnectionError.Unknown(e.message, e))
            }
        }
    }

    private fun resetScope() {
        socketJob.cancel()
        socketJob = SupervisorJob()
        socketScope = CoroutineScope(Dispatchers.IO + socketJob)
    }

    private suspend fun connectSocket(request: Request) {
        withTimeout(CONNECTION_TIMEOUT_MILLIS) {
            suspendCancellableCoroutine { continuation ->
                socket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        isSocketConnected = true
                        val configMessage = AttendiWebSocketConnectionFactory.makeConfigMessage()
                        webSocket.send(configMessage)
                        listener?.onOpen()
                        continuation.resume(Unit)
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        listener?.onMessage(text)
                    }

                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                        handleSocketClosed()
                        if (code == WEBSOCKET_NORMAL_CLOSURE_CODE) {
                            listener?.onClose()
                            socketScope.cancel()
                        } else {
                            listener?.onError(AttendiConnectionError.ClosedAbnormally(reason))
                        }
                    }

                    override fun onFailure(
                        webSocket: WebSocket,
                        t: Throwable,
                        response: Response?
                    ) {
                        handleSocketClosed()
                        listener?.onError(
                            AttendiConnectionError.Unknown(
                                message = response?.message,
                                cause = t
                            )
                        )
                        if (continuation.isActive) continuation.resumeWithException(t)
                    }
                })
            }
        }
    }

    private fun makeSocketUrl() : String? {
        val socketBaseUrl = client.getApiURL()?.replace("http", "ws")
        return socketBaseUrl?.let {
            "$it/v1/speech/transcribe/stream"
        }
    }

    private fun handleSocketClosed() {
        socket = null
        isSocketConnected = false
    }

    /**
     * Closes the active WebSocket connection.
     *
     * Sends a termination message to the server and attempts to gracefully wait
     * for the server to close the socket. If the server doesn't respond in time,
     * the connection is forcibly terminated.
     */
    override fun disconnect() {
        sendCloseMessageToServer()

        socketScope.launch {
            val socketClosedByServer = waitForServerToCloseSocket()

            if (!socketClosedByServer) {
                forceCloseSocket()
            }
        }
    }

    private fun sendCloseMessageToServer() {
        val closeMessage = AttendiWebSocketConnectionFactory.makeCloseMessage()
        socket?.send(closeMessage)
    }

    private suspend fun waitForServerToCloseSocket(): Boolean {
        return withTimeoutOrNull(SERVER_CLOSE_SOCKET_TIMEOUT_MILLIS) {
            while (socket != null) {
                delay(SERVER_CLOSE_SOCKET_INTERVAL_CHECK_MILLIS)
            }
            true // Socket was closed by server within timeout
        } ?: false // Timeout reached
    }

    private fun forceCloseSocket() {
        socket?.close(WEBSOCKET_TIMEOUT_CLOSURE_CODE, "Timeout reached after end of audio stream message sent")
        socket = null
        listener?.onError(AttendiConnectionError.DisconnectTimeout)
    }

    /**
     * Returns whether the WebSocket is currently open and ready to send/receive messages.
     */
    override fun isConnected(): Boolean = isSocketConnected

    /**
     * Sends a binary message (typically audio) to the Attendi server.
     *
     * @param message A byte array containing audio or binary data.
     * @return `true` if the message was sent successfully, `false` otherwise.
     */
    override suspend fun send(message: ByteArray): Boolean {
        return sendIfConnected { it.send(message.toByteString()) }
    }

    /**
     * Sends a text-based message to the Attendi server.
     *
     * @param message A UTF-8 string message (e.g., control or metadata).
     * @return `true` if the message was sent successfully, `false` otherwise.
     */
    override suspend fun send(message: String): Boolean {
        return sendIfConnected { it.send(message) }
    }

    private fun sendIfConnected(action: (WebSocket) -> Boolean): Boolean {
        return socket?.takeIf { isSocketConnected }?.let { action(it) } ?: false
    }
}