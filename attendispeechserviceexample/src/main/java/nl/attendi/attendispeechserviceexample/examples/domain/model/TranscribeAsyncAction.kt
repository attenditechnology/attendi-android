package nl.attendi.attendispeechserviceexample.examples.domain.model

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