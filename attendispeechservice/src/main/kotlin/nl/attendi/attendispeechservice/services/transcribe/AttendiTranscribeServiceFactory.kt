package nl.attendi.attendispeechservice.services.transcribe

import nl.attendi.attendispeechservice.services.AttendiTranscribeAPIConfig
import java.util.UUID

/**
 * Factory for creating instances of [AttendiTranscribeServiceImpl] configured to use the Attendi transcription API.
 *
 * This object provides a convenient entry point for clients to instantiate a fully configured
 * [TranscribeService] without needing to manually construct its dependencies. It encapsulates the
 * creation logic and ensures consistent setup of the underlying [AttendiTranscribeServiceImpl].
 *
 * Typical usage:
 * ```
 * val service = AttendiTranscribeServiceFactory.create(apiConfig)
 * ```
 */
object AttendiTranscribeServiceFactory {

    /**
     * Creates a new instance of [AttendiTranscribeServiceImpl] with the provided configuration.
     *
     * @param apiConfig Configuration for accessing the Attendi transcription API.
     * @param reportId Optional custom report ID. If not provided, a random UUID will be generated.
     * @return A fully configured [TranscribeService] implementation.
     */
    fun create(
        apiConfig: AttendiTranscribeAPIConfig,
        reportId: String = UUID.randomUUID().toString()
    ): TranscribeService {
        return AttendiTranscribeServiceImpl(
            apiConfig = apiConfig,
            reportId = reportId
        )
    }
}