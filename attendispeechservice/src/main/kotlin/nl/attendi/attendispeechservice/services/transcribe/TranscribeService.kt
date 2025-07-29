package nl.attendi.attendispeechservice.services.transcribe

/**
 * Interface for communicating with a synchronous transcription API.
 *
 * Implementations of this interface handle the process of sending encoded audio data
 * to the backend and returning the resulting transcription text.
 *
 * This service is typically used for single-shot transcriptions where the entire
 * audio input is available up front, as opposed to real-time streaming.
 */
interface TranscribeService {

    /**
     * Sends the provided encoded audio data to a transcription API and
     * returns the transcribed text, if available.
     *
     * @param audioEncoded Base64-encoded audio data, typically in a supported PCM format.
     * @return The transcription result as a string, or null if transcription failed or no text was returned.
     */
    suspend fun transcribe(audioEncoded: String): String?
}