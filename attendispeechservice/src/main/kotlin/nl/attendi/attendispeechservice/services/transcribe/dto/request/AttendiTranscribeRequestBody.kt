package nl.attendi.attendispeechservice.services.transcribe.dto.request

import kotlinx.serialization.Serializable

/**
 * Requests to Attendi's transcribe-like APIs usually require the same base request body.
 */
@Serializable
internal data class AttendiTranscribeRequestBody(
    val audio: String,
    val userId: String,
    val unitId: String,
    val metadata: AttendiTranscribeRequestMetadata,
    val config: AttendiTranscribeRequestConfig,
)

@Serializable
internal data class AttendiTranscribeRequestMetadata(
    val userAgent: String,
    val reportId: String
)

@Serializable
internal data class AttendiTranscribeRequestConfig(
    val model: String
)