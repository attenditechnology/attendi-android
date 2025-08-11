package nl.attendi.attendispeechservice.services.asynctranscribe

/**
 * A generic contract for establishing and interacting with a streaming or real-time connection.
 *
 * Consumers can implement this interface to use their own backend services or protocols.
 * This allows flexibility in how messages are sent or received (e.g., using a custom WebSocket, HTTP/2, etc).
 */
interface AsyncTranscribeService {

    /**
     * Initiates the connection and sets a listener for lifecycle and message events.
     *
     * @param listener An implementation of [AsyncTranscribeServiceListener] to handle callbacks.
     */
    suspend fun connect(listener: AsyncTranscribeServiceListener)

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
 * Used in conjunction with [AsyncTranscribeService] to respond to connection lifecycle changes
 * and receive real-time data.
 */
interface AsyncTranscribeServiceListener {

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
    fun onError(error: AsyncTranscribeServiceError)

    /** Called when the connection is closed or terminated. */
    fun onClose()
}

/**
 * Describes the various types of connection-related errors that may occur.
 *
 * These errors are passed through [AsyncTranscribeServiceListener.onError] to help diagnose
 * and respond to failures.
 */
sealed class AsyncTranscribeServiceError(message: String?) : Exception(message) {

    /** Indicates a failure to establish the connection. */
    data class FailedToConnect(override val message: String?) : AsyncTranscribeServiceError(message)

    /** Indicates the connection closed unexpectedly or with an abnormal code. */
    data class ClosedAbnormally(override val message: String?) :
        AsyncTranscribeServiceError(message)

    /** Connection attempt exceeded the allowed timeout. */
    data object ConnectTimeout : AsyncTranscribeServiceError("Connection attempt timed out") {
        // Serializable object must implement 'readResolve'. This is because when serializing/deserializing
        // the system doesn't understand that this class is meant to be a singleton and thus it needs
        // to implement readResolve to return always the same instance.
        @Suppress("unused")
        private fun readResolve(): Any = ConnectTimeout
    }

    /** Disconnection attempt exceeded the allowed timeout. */
    data object DisconnectTimeout : AsyncTranscribeServiceError("Disconnection attempt timed out") {
        // Serializable object must implement 'readResolve'. This is because when serializing/deserializing
        // the system doesn't understand that this class is meant to be a singleton and thus it needs
        // to implement readResolve to return always the same instance.
        @Suppress("unused")
        private fun readResolve(): Any = DisconnectTimeout
    }

    /** An unknown or unclassified connection error. */
    data class Unknown(override val message: String?) : AsyncTranscribeServiceError(message)
}
