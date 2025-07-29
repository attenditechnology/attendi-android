package nl.attendi.attendispeechservice.components.attendirecorder.plugins.transcribeplugin.encoder

/**
 * Interface defining the contract for encoding raw audio data before it is sent
 * to a transcription backend.
 *
 * The encoder transforms a list of raw PCM audio samples (as [Short] values)
 * into a [String] representation—typically base64 or another transport-safe format—
 * suitable for transmission over the network.
 *
 * This abstraction allows SDK consumers to provide custom encoding strategies
 * (e.g., compression or encryption) by supplying their own implementation.
 */
interface TranscribeAudioEncoder {

    /**
     * Encodes a list of PCM audio samples into a string format suitable for API transmission.
     *
     * @param audioSamples The raw audio samples (e.g., from the microphone) to be encoded.
     * @return A [String] containing the encoded audio payload.
     * @throws Exception If encoding fails or the input is invalid.
     */
    suspend fun encode(audioSamples: List<Short>): String
}