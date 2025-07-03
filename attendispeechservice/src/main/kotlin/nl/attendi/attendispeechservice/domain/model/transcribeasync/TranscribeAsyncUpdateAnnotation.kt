package nl.attendi.attendispeechservice.domain.model.transcribeasync

data class TranscribeAsyncUpdateAnnotationParameters(
    val id: String,
    val startCharacterIndex: Int,
    val endCharacterIndex: Int,
    val type: TranscribeAsyncUpdateAnnotationType
)

sealed class TranscribeAsyncUpdateAnnotationType {
    data object TranscriptionTentative : TranscribeAsyncUpdateAnnotationType()
    data class Entity(val type: TranscribeAsyncUpdateAnnotationEntityType, val text: String) : TranscribeAsyncUpdateAnnotationType()
}

enum class TranscribeAsyncUpdateAnnotationEntityType {
    NAME
}