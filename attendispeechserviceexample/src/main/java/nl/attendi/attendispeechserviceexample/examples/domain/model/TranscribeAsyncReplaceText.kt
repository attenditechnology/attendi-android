package nl.attendi.attendispeechserviceexample.examples.domain.model

data class TranscribeAsyncReplaceTextParameters(
    val startCharacterIndex: Int,
    val endCharacterIndex: Int,
    val text: String
)