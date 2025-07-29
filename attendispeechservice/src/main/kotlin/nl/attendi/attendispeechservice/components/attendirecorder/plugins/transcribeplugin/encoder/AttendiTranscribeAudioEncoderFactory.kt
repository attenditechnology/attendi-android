package nl.attendi.attendispeechservice.components.attendirecorder.plugins.transcribeplugin.encoder

/**
 * Factory object for creating instances of [AttendiTranscribeAudioEncoder].
 */
object AttendiTranscribeAudioEncoderFactory {

    /**
     * Returns an instance of [TranscribeAudioEncoder].
     *
     * Currently returns the singleton implementation [AttendiTranscribeAudioEncoder].
     * This can be extended in the future to support dynamic configuration or multiple encoder strategies.
     */
    fun create(): TranscribeAudioEncoder {
        return AttendiTranscribeAudioEncoder
    }
}