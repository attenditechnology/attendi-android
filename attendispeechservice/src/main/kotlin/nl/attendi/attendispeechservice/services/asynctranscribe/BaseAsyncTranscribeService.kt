package nl.attendi.attendispeechservice.services.asynctranscribe

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.toByteString
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Base implementation of the [AsyncTranscribeService] interface, providing core WebSocket connection
 * management, message handling, and lifecycle support for transcription services.
 *
 * This class handles:
 * - Connecting to a WebSocket endpoint with automatic retries.
 * - Sending initial configuration and closing messages, if provided.
 * - Managing socket lifecycle events (open, message, close, failure).
 * - Enforcing mutual exclusion for connection attempts via a [Mutex].
 * - Abstracting WebSocket [Request] creation and customization.
 *
 * Subclasses must implement [createWebSocketRequest] and may override optional hooks such as:
 * - [onRetryAttempt]: to customize retry behavior (e.g., refresh token or credentials).
 * - [getOpenMessage]: to send a configuration payload upon connection.
 * - [getCloseMessage] and [getCloseCode]: to customize socket shutdown behavior.
 *
 * This base class is designed to be extended by service implementations targeting specific
 * WebSocket-based transcription backends, while abstracting away connection boilerplate.
 */
abstract class BaseAsyncTranscribeService : AsyncTranscribeService {

    private companion object {
        /** Maximum duration to wait when establishing a connection. */
        const val CONNECTION_TIMEOUT_MILLISECONDS = 20_000L

        /** Timeout to wait for the server to close the connection after sending the end-of-stream message. */
        const val SERVER_CLOSE_SOCKET_TIMEOUT_MILLISECONDS = 5_000L

        /** Polling interval for checking if the server closed the socket. */
        const val SERVER_CLOSE_SOCKET_INTERVAL_CHECK_MILLISECONDS = 50L

        /** WebSocket closure code indicating a normal, expected shutdown. */
        const val WEBSOCKET_NORMAL_CLOSURE_CODE = 1000

        /** WebSocket closure code indicating a timeout, forced shutdown. */
        const val WEBSOCKET_TIMEOUT_CLOSURE_CODE = 4000
    }

    /**
     * A mutex to ensure that only one connect() operation can run at a time.
     * This prevents race conditions or concurrent connection attempts,
     * especially if connect() is called repeatedly or from multiple coroutines.
     */
    private val connectMutex = Mutex()
    private var socket: WebSocket? = null
    private var listener: AsyncTranscribeServiceListener? = null
    private val webSocketClient = OkHttpClient()

    /**
     * Creates the WebSocket [Request] object used to initiate the connection.
     *
     * @return A configured [Request] object ready for use with OkHttp.
     */
    protected abstract suspend fun createWebSocketRequest(): Request

    /**
     * Called on retry attempts to allow the consumer to modify the request.
     * Default implementation calls [createWebSocketRequest] again.
     *
     * @param retryAttempt The retry number (starting from 1).
     * @param previousRequest The request used in the failed attempt.
     * @param exception The exception that caused the failure (optional).
     * @return A new [Request] object to retry with.
     */
    protected open suspend fun onRetryAttempt(
        retryAttempt: Int,
        previousRequest: Request?,
        exception: Exception?
    ): Request {
        return createWebSocketRequest()
    }

    /**
     * (Optional) Returns the initial message to be sent immediately after the WebSocket opens.
     *
     * This message is typically used to initialize the transcription session, set language, model,
     * or other backend-specific options. If `null` is returned, no message will be sent.
     *
     * @return A properly formatted open message as a [String] or `null` if not needed.
     */
    protected open fun getOpenMessage(): String? = null

    /**
     * (Optional) Returns the close message that should be sent to the server when ending the session.
     *
     * This message typically signals the end-of-stream or end-of-transcription event,
     * and varies depending on the backend's protocol expectations. If `null` is returned, no message will be sent.
     *
     * @return A properly formatted close message as a [String] or `null` if not needed.
     */
    protected open fun getCloseMessage(): String? = null

    /**
     * Returns the WebSocket close code to be sent when terminating the connection.
     *
     * The default implementation returns `1000`, which indicates a normal closure as per
     * [RFC 6455](https://datatracker.ietf.org/doc/html/rfc6455#section-7.4.1).
     *
     * Subclasses may override this method to provide a custom application-defined close code
     * (typically in the 4000–4999 range) if a different reason for closing should be conveyed.
     *
     * @return An [Int] close code to send with the WebSocket close frame.
     */
    protected open fun getCloseCode(): Int = 1000

    /**
     * Initiates a WebSocket connection to the Attendi streaming API.
     *
     * This method runs asynchronously and will emit events to the provided [listener]
     * for success, errors, and incoming messages.
     *
     * @param listener An implementation of [AsyncTranscribeServiceListener] to observe connection state.
     */
    override suspend fun connect(listener: AsyncTranscribeServiceListener) {
        connectMutex.withLock {
            this@BaseAsyncTranscribeService.listener = listener

            connectSocket(listener)
        }
    }

    /**
     * Attempts to establish a WebSocket connection using a [Request] object. Supports automatic retries
     * if the initial connection attempt fails.
     *
     * This method delegates request construction to either [createWebSocketRequest] (on the first attempt)
     * or [onRetryAttempt] (on subsequent retries). The consumer can override [onRetryAttempt] to dynamically
     * modify the request between retry attempts — for example, by refreshing authentication tokens or cookies.
     *
     * If a timeout occurs, [AsyncTranscribeServiceError.ConnectTimeout] is reported. For other exceptions,
     * the method retries until [retryCount] is exhausted. When all attempts fail,
     * [AsyncTranscribeServiceError.Unknown] or [AsyncTranscribeServiceError.FailedToConnect] is reported.
     *
     * @param listener A listener for receiving error callbacks.
     * @param retryCount The number of retry attempts to perform after the initial failure. Defaults to 1.
     * @param currentRetry The current retry attempt number, starting from 0.
     * @param previousRequest The [Request] used in the previous attempt. `null` for the initial attempt.
     * @param previousException The [Exception] that caused the previous failure, if any.
     */
    private suspend fun connectSocket(
        listener: AsyncTranscribeServiceListener,
        retryCount: Int = 1,
        currentRetry: Int = 0,
        previousRequest: Request? = null,
        previousException: Exception? = null
    ) {
        val request = try {
            if (currentRetry == 0) {
                createWebSocketRequest()
            } else {
                onRetryAttempt(currentRetry, previousRequest, previousException)
            }
        } catch (e: Exception) {
            listener.onError(AsyncTranscribeServiceError.FailedToConnect(e.message))
            return
        }

        try {
            connectSocket(request)
        } catch (e: TimeoutCancellationException) {
            listener.onError(AsyncTranscribeServiceError.ConnectTimeout)
        } catch (e: Exception) {
            if (retryCount == 0) {
                listener.onError(AsyncTranscribeServiceError.Unknown(e.message))
            } else {
                connectSocket(
                    listener,
                    retryCount = retryCount - 1,
                    currentRetry = currentRetry + 1,
                    previousRequest = request,
                    previousException = e
                )
            }
        }
    }

    private suspend fun connectSocket(request: Request) {
        withTimeout(CONNECTION_TIMEOUT_MILLISECONDS) {
            suspendCancellableCoroutine { continuation ->
                socket = webSocketClient.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        getOpenMessage()?.let { configMessage ->
                            webSocket.send(configMessage)
                        }
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
                        } else {
                            listener?.onError(
                                AsyncTranscribeServiceError.ClosedAbnormally(reason)
                            )
                        }
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        handleSocketClosed()
                        listener?.onError(
                            AsyncTranscribeServiceError.Unknown(message = response?.message)
                        )
                        if (continuation.isActive) continuation.resumeWithException(t)
                    }
                })
            }
        }
    }

    private fun handleSocketClosed() {
        socket = null
        listener = null
    }

    /**
     * Closes the active WebSocket connection.
     *
     * Sends a termination message to the server and attempts to gracefully wait
     * for the server to close the socket. If the server doesn't respond in time,
     * the connection is forcibly terminated.
     */
    override suspend fun disconnect() {
        getCloseMessage()?.let { closeMessage ->
            socket?.send(closeMessage)

            val socketClosedByServer = waitForServerToCloseSocket()
            if (!socketClosedByServer) {
                socket?.close(
                    WEBSOCKET_TIMEOUT_CLOSURE_CODE,
                    "Timeout reached after end of audio stream message sent"
                )
                socket = null
                listener?.onError(AsyncTranscribeServiceError.DisconnectTimeout)
            }
        } ?: run {
            socket?.close(getCloseCode(), null)
            socket = null
        }
    }

    private suspend fun waitForServerToCloseSocket(): Boolean {
        return withTimeoutOrNull(SERVER_CLOSE_SOCKET_TIMEOUT_MILLISECONDS) {
            while (socket != null) {
                delay(SERVER_CLOSE_SOCKET_INTERVAL_CHECK_MILLISECONDS)
            }
            true // Socket was closed by server within timeout
        } ?: false // Timeout reached
    }

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
        return socket?.let { action(it) } ?: false
    }
}