package nl.attendi.attendispeechservice.components.attendirecorder.plugins.asynctranscribeplugin.decoder

/**
 * Factory object for providing an instance of [AsyncTranscribeMessageDecoder] tailored to the Attendi-specific implementation.
 *
 * This utility serves as a centralized access point to obtain the default decoder used for parsing
 * and mapping transcribe messages in the Attendi domain. It encapsulates the construction logic
 * and ensures that consumers do not need to know the concrete implementation details.
 */
object AttendiAsyncTranscribeMessageDecoderFactory {

    /**
     * Returns an instance of [AsyncTranscribeMessageDecoder].
     *
     * Currently provides the singleton implementation [AttendiAsyncTranscribeMessageDecoder].
     */
    fun create(): AsyncTranscribeMessageDecoder {
        return AttendiAsyncTranscribeMessageDecoder
    }
}