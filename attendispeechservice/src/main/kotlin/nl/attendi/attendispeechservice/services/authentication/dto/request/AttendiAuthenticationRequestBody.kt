package nl.attendi.attendispeechservice.services.authentication.dto.request

import kotlinx.serialization.Serializable

@Serializable
internal data class AttendiAuthenticationRequestBody(
    val userId: String,
    val unitId: String,
    val userAgent: String?
)
