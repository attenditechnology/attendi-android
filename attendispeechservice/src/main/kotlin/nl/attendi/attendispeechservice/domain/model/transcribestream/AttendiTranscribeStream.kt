package nl.attendi.attendispeechservice.domain.model.transcribestream

import nl.attendi.attendispeechservice.domain.model.transcribeasync.TranscribeAsyncAction
import nl.attendi.attendispeechservice.domain.model.transcribeasync.TranscribeAsyncReplaceTextMapper

/**
 * Represents the current state and history of a real-time transcription stream,
 * with support for undo and redo operations.
 *
 * This model is updated incrementally as transcription events (i.e., [TranscribeAsyncAction]s)
 * are received over time via WebSocket. It maintains an operation history and supports undoing or
 * redoing previously applied actions using their inverse representations.
 *
 * The main entry point for updating this model is the [receiveActions] method,
 * which applies new transcription actions, updates the internal state accordingly,
 * and maintains undo/redo history.
 *
 * @property state The current transcribed text and annotation state.
 * @property operationHistory The chronological list of applied transcription actions, each paired with its inverse.
 * @property undoneOperations A stack of recently undone actions, enabling redo functionality.
 */
data class AttendiTranscribeStream(
    val state: AttendiStreamState = AttendiStreamState("", emptyList()),
    val operationHistory: List<UndoableTranscribeAction> = emptyList(),
    val undoneOperations: List<UndoableTranscribeAction> = emptyList()
) {
    /**
     * Updates the stream with a new list of [TranscribeAsyncAction]s.
     *
     * Applies the actions to the current [state] and appends them to the [operationHistory].
     *
     * @param actions The list of actions to apply to this stream.
     * @return A new updated [AttendiTranscribeStream] with modified state and extended history.
     */
    fun receiveActions(actions: List<TranscribeAsyncAction>): AttendiTranscribeStream {
        val newState = state.apply(actions = actions)
        val newHistory = operationHistory + UndoableTranscribeAsyncActionMapper.map(state, actions)
        return copy(
            state = newState,
            operationHistory = newHistory,
            undoneOperations = emptyList() // new user action invalidates redo
        )
    }

    /**
     * Undo the last [count] operations, returning a new [AttendiTranscribeStream].
     */
    fun undoOperations(count: Int): AttendiTranscribeStream {
        require(count >= 0) { "Undo count must be non-negative" }
        val toUndo = operationHistory.takeLast(count)
        val remainingHistory = operationHistory.dropLast(count)
        val inverseActions = toUndo.map { it.inverse.reversed() }.flatten()
        val reversedActions = inverseActions.reversed()
        val newState = state.apply(reversedActions)

        return copy(
            state = newState,
            operationHistory = remainingHistory,
            undoneOperations = toUndo + undoneOperations
        )
    }

    /**
     * Redo the last [count] operations, returning a new [AttendiTranscribeStream].
     */
    fun redoOperations(count: Int): AttendiTranscribeStream {
        require(count >= 0) { "Redo count must be non-negative" }

        if (undoneOperations.isEmpty()) return this

        val toRedo = undoneOperations.take(count)
        val remainingUndone = undoneOperations.drop(count)
        val redoActions = toRedo.map { it.original }

        val newState = state.apply(redoActions)
        val newHistory = operationHistory + toRedo

        return copy(
            state = newState,
            operationHistory = newHistory,
            undoneOperations = remainingUndone
        )
    }
}

/**
 * Represents the current state of the transcript, including its text content and annotations.
 *
 * This state evolves over time as actions such as text replacements, additions, or annotation updates occur.
 *
 * @property text The current transcript text.
 * @property annotations A list of current annotations applied to the text.
 */
data class AttendiStreamState(
    val text: String,
    val annotations: List<TranscribeAsyncAction.AddAnnotation>
) {
    /**
     * Applies a series of [TranscribeAsyncAction]s to this state and returns a new updated state.
     *
     * The following actions are supported:
     * - [TranscribeAsyncAction.AddAnnotation]: Appends a new annotation.
     * - [TranscribeAsyncAction.RemoveAnnotation]: Removes annotation(s) by ID.
     * - [TranscribeAsyncAction.ReplaceText]: Replaces a portion of the text using character indices.
     * - [TranscribeAsyncAction.UpdateAnnotation]: Updates properties of an existing annotation.
     *
     * @param actions The actions to apply in sequence.
     * @return A new [AttendiStreamState] after all actions have been applied.
     */
    fun apply(
        actions: List<TranscribeAsyncAction>
    ): AttendiStreamState {
        var currentText = text
        val currentAnnotations = annotations.toMutableList()
        actions.forEach { action ->
            when (action) {
                is TranscribeAsyncAction.AddAnnotation -> {
                    currentAnnotations.add(action)
                }

                is TranscribeAsyncAction.RemoveAnnotation -> {
                    currentAnnotations.removeIf { action.parameters.id == it.parameters.id }
                }

                is TranscribeAsyncAction.ReplaceText -> {
                    currentText = TranscribeAsyncReplaceTextMapper.map(currentText, action)
                }

                is TranscribeAsyncAction.UpdateAnnotation -> {
                    updateAnnotation(currentAnnotations, action)
                }
            }
        }
        return AttendiStreamState(text = currentText, annotations = currentAnnotations)
    }

    private fun updateAnnotation(
        annotations: MutableList<TranscribeAsyncAction.AddAnnotation>,
        action: TranscribeAsyncAction.UpdateAnnotation
    ) {
        val index =
            annotations.indexOfFirst { action.parameters.id == it.parameters.id }
        if (index != -1) {
            val oldAnnotation = annotations[index]
            val updatedAnnotation = oldAnnotation.copy(
                actionData = action.actionData,
                parameters = action.parameters
            )
            annotations[index] = updatedAnnotation
        }
    }
}