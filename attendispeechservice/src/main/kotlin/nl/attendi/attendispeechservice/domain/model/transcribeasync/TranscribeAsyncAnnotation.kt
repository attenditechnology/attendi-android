package nl.attendi.attendispeechservice.domain.model.transcribeasync

data class TranscribeAsyncAnnotationParameters(
    val id: String,
    val startCharacterIndex: Int,
    val endCharacterIndex: Int,
    val type: TranscribeAsyncAnnotationType
)

sealed class TranscribeAsyncAnnotationType {
    data object TranscriptionTentative : TranscribeAsyncAnnotationType()
    data class Intent(val status: TranscribeAsyncAnnotationIntentStatus) : TranscribeAsyncAnnotationType()
    data class Entity(val type: TranscribeAsyncAnnotationEntityType, val text: String) : TranscribeAsyncAnnotationType()
}

enum class TranscribeAsyncAnnotationIntentStatus {
    PENDING, RECOGNIZED
}

enum class TranscribeAsyncAnnotationEntityType {
    NAME
}