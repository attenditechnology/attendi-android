package nl.attendi.attendispeechservice.components.attendimicrophone.plugins.asynctranscribe.domain.model.transcribeasync

/**
 * Represents a sealed hierarchy of asynchronous transcription actions.
 *
 * Designed as a sealed class with concrete data classes instead of abstract properties
 * to ensure compatibility with Swift.
 *
 */
sealed class TranscribeAsyncAction {
    data class AddAnnotation(val actionData: TranscribeAsyncActionData, val parameters: TranscribeAsyncAnnotationParameters) : TranscribeAsyncAction()
    data class RemoveAnnotation(val actionData: TranscribeAsyncActionData, val parameters: TranscribeAsyncRemoveAnnotationParameters) : TranscribeAsyncAction()
    data class UpdateAnnotation(val actionData: TranscribeAsyncActionData, val parameters: TranscribeAsyncAnnotationParameters) : TranscribeAsyncAction()
    data class ReplaceText(val actionData: TranscribeAsyncActionData, val parameters: TranscribeAsyncReplaceTextParameters) : TranscribeAsyncAction()
}

data class TranscribeAsyncActionData(
    val id: String,
    val index: Int
)