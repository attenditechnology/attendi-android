package nl.attendi.attendispeechservice.components.attendirecorder.plugins.asynctranscribeplugin.mapper

import nl.attendi.attendispeechservice.components.attendirecorder.plugins.asynctranscribeplugin.model.AttendiStreamState
import nl.attendi.attendispeechservice.components.attendirecorder.plugins.asynctranscribeplugin.model.UndoableTranscribeAction
import nl.attendi.attendispeechservice.services.asynctranscribe.model.TranscribeAsyncAction
import nl.attendi.attendispeechservice.services.asynctranscribe.model.TranscribeAsyncActionData
import nl.attendi.attendispeechservice.services.asynctranscribe.model.TranscribeAsyncRemoveAnnotationParameters
import nl.attendi.attendispeechservice.services.asynctranscribe.model.TranscribeAsyncReplaceTextMapper
import nl.attendi.attendispeechservice.services.asynctranscribe.model.TranscribeAsyncReplaceTextParameters

internal object UndoableTranscribeAsyncActionMapper {

    fun map(
        currentState: AttendiStreamState,
        actions: List<TranscribeAsyncAction>
    ): List<UndoableTranscribeAction> {
        var updatedText = currentState.text
        val annotations = currentState.annotations.toMutableList()
        return actions.map { action ->
            val undoable = when (action) {
                is TranscribeAsyncAction.ReplaceText -> {
                    val undoableReplaceText = createUndoableReplaceTextAction(updatedText, action)
                    // Update text progressively to match the original order
                    updatedText = TranscribeAsyncReplaceTextMapper.map(updatedText, action)
                    undoableReplaceText
                }

                is TranscribeAsyncAction.AddAnnotation -> {
                    // Insert AddAnnotations progressively to allow remove/update operations find
                    // the added annotation in the same action list
                    annotations.add(action)
                    createUndoableAddAnnotation(action)
                }

                is TranscribeAsyncAction.RemoveAnnotation -> createUndoableRemoveAnnotation(
                    action,
                    annotations
                )

                is TranscribeAsyncAction.UpdateAnnotation -> createUndoableUpdateAnnotation(
                    action,
                    annotations
                )
            }
            undoable
        }
    }

    private fun createUndoableAddAnnotation(action: TranscribeAsyncAction.AddAnnotation): UndoableTranscribeAction {
        return UndoableTranscribeAction(
            action,
            listOf(
                TranscribeAsyncAction.RemoveAnnotation(
                    TranscribeAsyncActionData(
                        id = action.actionData.id,
                        index = action.actionData.index
                    ),
                    parameters = TranscribeAsyncRemoveAnnotationParameters(
                        id = action.parameters.id
                    )
                )
            )
        )
    }

    private fun createUndoableRemoveAnnotation(
        action: TranscribeAsyncAction.RemoveAnnotation,
        annotations: List<TranscribeAsyncAction>
    ): UndoableTranscribeAction {
        val removed =
            annotations.find { (it as? TranscribeAsyncAction.AddAnnotation)?.parameters?.id == action.parameters.id }
                ?: throw IllegalStateException("Annotation to remove not found for action: $action in $annotations")
        return UndoableTranscribeAction(action, listOf(removed))
    }

    private fun createUndoableUpdateAnnotation(
        action: TranscribeAsyncAction.UpdateAnnotation,
        annotations: List<TranscribeAsyncAction>
    ): UndoableTranscribeAction {
        val updated =
            annotations.find { (it as? TranscribeAsyncAction.AddAnnotation)?.parameters?.id == action.parameters.id }
                ?: throw IllegalStateException("Annotation to update not found for action: $action in $annotations")
        val removedAnnotation =
            TranscribeAsyncAction.RemoveAnnotation(
                actionData = action.actionData,
                parameters = TranscribeAsyncRemoveAnnotationParameters(id = action.actionData.id)
            )
        return UndoableTranscribeAction(action, listOf(removedAnnotation, updated))
    }

    private fun createUndoableReplaceTextAction(
        currentText: String,
        action: TranscribeAsyncAction.ReplaceText
    ): UndoableTranscribeAction {
        val params = action.parameters

        val replacedText =
            currentText.substring(params.startCharacterIndex, params.endCharacterIndex)

        val inverseAction =
            TranscribeAsyncAction.ReplaceText(
                actionData = TranscribeAsyncActionData(
                    id = action.actionData.id,
                    index = action.actionData.index
                ),
                parameters = TranscribeAsyncReplaceTextParameters(
                    startCharacterIndex = params.startCharacterIndex,
                    endCharacterIndex = params.startCharacterIndex + params.text.length,
                    text = replacedText
                )
            )

        return UndoableTranscribeAction(action, listOf(inverseAction))
    }
}