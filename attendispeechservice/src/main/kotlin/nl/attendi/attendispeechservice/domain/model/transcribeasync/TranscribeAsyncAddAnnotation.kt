package nl.attendi.attendispeechservice.domain.model.transcribeasync

data class TranscribeAsyncAddAnnotationParameters(
    val id: String,
    val startCharacterIndex: Int,
    val endCharacterIndex: Int,
    val type: TranscribeAsyncAddAnnotationType
)

sealed class TranscribeAsyncAddAnnotationType {
    data object TranscriptionTentative : TranscribeAsyncAddAnnotationType()
    data class Intent(val status: TranscribeAsyncAddAnnotationIntentStatus) : TranscribeAsyncAddAnnotationType()
    data class Entity(val type: TranscribeAsyncAddAnnotationEntityType, val text: String) : TranscribeAsyncAddAnnotationType()
}

enum class TranscribeAsyncAddAnnotationIntentStatus {
    PENDING, RECOGNIZED
}

enum class TranscribeAsyncAddAnnotationEntityType {
    NAME
}