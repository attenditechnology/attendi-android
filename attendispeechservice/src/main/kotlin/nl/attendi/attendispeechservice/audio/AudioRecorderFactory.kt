package nl.attendi.attendispeechservice.audio

/**
 * Factory object for creating instances of [AudioRecorderImpl].
 */
object AudioRecorderFactory {

    /**
     * Creates the default implementation of [AudioRecorderImpl].
     *
     * This recorder provides basic audio capture functionality using the
     * underlying platform-specific implementation. It is suitable for use
     * in most environments without requiring additional configuration.
     *
     * @return A default instance of [AudioRecorder].
     */
    fun create(): AudioRecorder {
        return AudioRecorderImpl
    }
}