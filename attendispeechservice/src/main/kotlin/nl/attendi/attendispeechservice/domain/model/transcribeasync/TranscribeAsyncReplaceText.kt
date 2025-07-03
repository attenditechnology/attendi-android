package nl.attendi.attendispeechservice.domain.model.transcribeasync

data class TranscribeAsyncReplaceTextParameters(
    val startCharacterIndex: Int,
    val endCharacterIndex: Int,
    val text: String
)