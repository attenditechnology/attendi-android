package nl.attendi.attendispeechservice.audio

/**
 * Wraps the lower-level `AudioRecord` APIs to provide a convenient and suspendable interface
 * for recording audio from the device.
 *
 * This abstraction allows consumers to start and stop audio capture and receive audio frames
 * via a suspendable callback.
 */
interface AudioRecorder {

    /**
     * Indicates whether the recorder is currently capturing audio.
     */
    suspend fun isRecording(): Boolean

    /**
     * Starts recording audio with the given configuration.
     *
     * @param audioRecordingConfig Configuration for the audio source, including sample rate, channel, and encoding.
     * @param onAudio Callback invoked with each [AudioFrame] containing captured audio samples and metadata.
     *
     * This method must only be called when recording is not already in progress. If called again
     * while recording, it will throw [AudioRecorderException.AlreadyRecording] to prevent concurrent usage.
     *
     * This method must only be called with a valid AudioRecordingConfig. If invalid,
     * it will throw [AudioRecorderException.UnsupportedAudioFormat] to prevent starting the recorder.
     *
     * Recording is performed on a background coroutine and audio frames are delivered asynchronously.
     */
    @Throws(AudioRecorderException::class)
    suspend fun startRecording(
        audioRecordingConfig: AudioRecordingConfig,
        onAudio: suspend (AudioFrame) -> Unit
    )

    /**
     * Stops the audio recording if it's currently in progress.
     *
     * If recording is not active, this call has no effect. The recorder will release its resources
     * and cease to invoke further audio callbacks.
     */
    suspend fun stopRecording()
}

sealed class AudioRecorderException(message: String) : Exception(message) {
    data object AlreadyRecording : AudioRecorderException("Recorder is already in use") {
        // Serializable object must implement 'readResolve'. This is because when serializing/deserializing
        // the system doesn't understand that this class is meant to be a singleton and thus it needs
        // to implement readResolve to return always the same instance.
        @Suppress("unused")
        private fun readResolve(): Any = AlreadyRecording
    }
    data object DeniedRecodingPermission : AudioRecorderException("Permission to access the device's microphone is denied") {
        // Serializable object must implement 'readResolve'. This is because when serializing/deserializing
        // the system doesn't understand that this class is meant to be a singleton and thus it needs
        // to implement readResolve to return always the same instance.
        @Suppress("unused")
        private fun readResolve(): Any = AlreadyRecording
    }
    data class UnsupportedAudioFormat(override val message: String) : AudioRecorderException(message)
}