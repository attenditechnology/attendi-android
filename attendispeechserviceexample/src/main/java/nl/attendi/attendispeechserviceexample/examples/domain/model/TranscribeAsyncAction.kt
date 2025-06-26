package nl.attendi.attendispeechserviceexample.examples.domain.model

/**
 * Represents a sealed hierarchy of asynchronous transcription actions.
 *
 * Designed as a sealed class with concrete data classes instead of abstract properties
 * to ensure compatibility with Swift.
 *
 */
sealed class TranscribeAsyncAction {
    data class AddAnnotation(val action: TranscribeAsyncActionData, val parameters: TranscribeAsyncAddAnnotationParameters) : TranscribeAsyncAction()
    data class RemoveAnnotation(val action: TranscribeAsyncActionData, val parameters: TranscribeAsyncRemoveAnnotationParameters) : TranscribeAsyncAction()
    data class UpdateAnnotation(val action: TranscribeAsyncActionData, val parameters: TranscribeAsyncUpdateAnnotationParameters) : TranscribeAsyncAction()
    data class ReplaceText(val action: TranscribeAsyncActionData, val parameters: TranscribeAsyncReplaceTextParameters) : TranscribeAsyncAction()
}

data class TranscribeAsyncActionData(
    val id: String,
    val index: Int
)