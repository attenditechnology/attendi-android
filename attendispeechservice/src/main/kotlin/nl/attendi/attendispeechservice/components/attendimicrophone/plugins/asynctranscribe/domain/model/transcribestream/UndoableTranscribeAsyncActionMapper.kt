package nl.attendi.attendispeechservice.components.attendimicrophone.plugins.asynctranscribe.domain.model.transcribestream

import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.asynctranscribe.domain.model.transcribeasync.TranscribeAsyncAction
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.asynctranscribe.domain.model.transcribeasync.TranscribeAsyncActionData
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.asynctranscribe.domain.model.transcribeasync.TranscribeAsyncRemoveAnnotationParameters
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.asynctranscribe.domain.model.transcribeasync.TranscribeAsyncReplaceTextMapper
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.asynctranscribe.domain.model.transcribeasync.TranscribeAsyncReplaceTextParameters

object UndoableTranscribeAsyncActionMapper {

    fun map(
        currentState: AttendiStreamState,
        actions: List<TranscribeAsyncAction>
    ): List<UndoableTranscribeAction> {
        var updatedText = currentState.text
        val annotations = currentState.annotations.toMutableList()
        return actions.map { action ->
            val undoable = when (action) {
                is TranscribeAsyncAction.ReplaceText -> {
                    val undoableReplaceText = makeUndoableReplaceTextAction(updatedText, action)
                    // Update text progressively to match the original order
                    updatedText = TranscribeAsyncReplaceTextMapper.map(updatedText, action)
                    undoableReplaceText
                }

                is TranscribeAsyncAction.AddAnnotation -> {
                    // Insert AddAnnotations progressively to allow remove/update operations find
                    // the added annotation in the same action list
                    annotations.add(action)
                    makeUndoableAddAnnotation(action)
                }

                is TranscribeAsyncAction.RemoveAnnotation -> makeUndoableRemoveAnnotation(
                    action,
                    annotations
                )

                is TranscribeAsyncAction.UpdateAnnotation -> makeUndoableUpdateAnnotation(
                    action,
                    annotations
                )
            }
            undoable
        }
    }

    private fun makeUndoableAddAnnotation(action: TranscribeAsyncAction.AddAnnotation): UndoableTranscribeAction {
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

    private fun makeUndoableRemoveAnnotation(
        action: TranscribeAsyncAction.RemoveAnnotation,
        annotations: List<TranscribeAsyncAction>
    ): UndoableTranscribeAction {
        val removed =
            annotations.find { (it as? TranscribeAsyncAction.AddAnnotation)?.parameters?.id == action.parameters.id }
                ?: throw IllegalStateException("Annotation to remove not found for action: $action in $annotations")
        return UndoableTranscribeAction(action, listOf(removed))
    }

    private fun makeUndoableUpdateAnnotation(
        action: TranscribeAsyncAction.UpdateAnnotation,
        annotations: List<TranscribeAsyncAction>
    ): UndoableTranscribeAction {
        val updated = annotations.find { (it as? TranscribeAsyncAction.AddAnnotation)?.parameters?.id == action.parameters.id }
                ?: throw IllegalStateException("Annotation to update not found for action: $action in $annotations")
        val removedAnnotation = TranscribeAsyncAction.RemoveAnnotation(actionData = action.actionData, parameters = TranscribeAsyncRemoveAnnotationParameters(id = action.actionData.id))
        return UndoableTranscribeAction(action, listOf(removedAnnotation, updated))
    }

    private fun makeUndoableReplaceTextAction(
        currentText: String,
        action: TranscribeAsyncAction.ReplaceText
    ): UndoableTranscribeAction {
        val params = action.parameters

        val replacedText =
            currentText.substring(params.startCharacterIndex, params.endCharacterIndex)

        val inverseAction = TranscribeAsyncAction.ReplaceText(
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