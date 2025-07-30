package nl.attendi.attendispeechservice.services.authentication.dto.response

import kotlinx.serialization.Serializable

@Serializable
internal data class AttendiAuthenticationResponse(val token: String)