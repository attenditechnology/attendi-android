package nl.attendi.attendispeechservice.domain.connection

/**
 * A generic contract for establishing and interacting with a streaming or real-time connection.
 *
 * Consumers can implement this interface to use their own backend services or protocols.
 * This allows flexibility in how messages are sent or received (e.g., using a custom WebSocket, HTTP/2, etc).
 */
interface AttendiConnection {

    /**
     * Initiates the connection and sets a listener for lifecycle and message events.
     *
     * @param listener An implementation of [AttendiConnectionListener] to handle callbacks.
     */
    suspend fun connect(listener: AttendiConnectionListener)

    /**
     * Closes the connection if it is currently active.
     */
    suspend fun disconnect()

    /**
     * Sends a textual message over the connection.
     *
     * @param message The message string to send.
     * @return `true` if the message was successfully dispatched; otherwise, `false`.
     */
    suspend fun send(message: String): Boolean

    /**
     * Sends binary data over the connection.
     *
     * @param message The binary payload to send.
     * @return `true` if the message was successfully dispatched; otherwise, `false`.
     */
    suspend fun send(message: ByteArray): Boolean
}

/**
 * Listener for observing connection events and handling incoming messages.
 *
 * Used in conjunction with [AttendiConnection] to respond to connection lifecycle changes
 * and receive real-time data.
 */
interface AttendiConnectionListener {

    /** Called when the connection has been successfully opened. */
    fun onOpen()

    /**
     * Called when a message is received from the connection.
     *
     * @param message A UTF-8 string message from the server or backend.
     */
    fun onMessage(message: String)

    /**
     * Called when an error occurs during connection or message handling.
     *
     * @param error The specific error that occurred.
     */
    fun onError(error: AttendiConnectionError)

    /** Called when the connection is closed or terminated. */
    fun onClose()
}


/**
 * Describes the various types of connection-related errors that may occur.
 *
 * These errors are passed through [AttendiConnectionListener.onError] to help diagnose
 * and respond to failures.
 */
sealed class AttendiConnectionError {

    /** Indicates a failure to establish the connection. */
    data class FailedToConnect(val message: String? = null, val cause: Throwable? = null) :
        AttendiConnectionError()

    /** Indicates the connection closed unexpectedly or with an abnormal code. */
    data class ClosedAbnormally(val message: String? = null, val cause: Throwable? = null) :
        AttendiConnectionError()

    /** Connection attempt exceeded the allowed timeout. */
    data object ConnectTimeout : AttendiConnectionError()

    /** Disconnection attempt exceeded the allowed timeout. */
    data object DisconnectTimeout : AttendiConnectionError()

    /** An unknown or unclassified connection error. */
    data class Unknown(val message: String? = null, val cause: Throwable? = null) :
        AttendiConnectionError()
}
