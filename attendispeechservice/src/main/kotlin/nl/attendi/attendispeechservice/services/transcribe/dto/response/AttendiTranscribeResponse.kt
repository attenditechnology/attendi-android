package nl.attendi.attendispeechservice.services.transcribe.dto.response

import kotlinx.serialization.Serializable

@Serializable
internal data class AttendiTranscribeResponse(val transcript: String)