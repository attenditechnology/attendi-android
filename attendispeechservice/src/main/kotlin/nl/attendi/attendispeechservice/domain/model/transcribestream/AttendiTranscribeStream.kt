package nl.attendi.attendispeechservice.domain.model.transcribestream

import nl.attendi.attendispeechservice.domain.model.transcribeasync.TranscribeAsyncAction

/**
 * Represents the current state and history of a real-time transcription stream.
 *
 * This model is updated incrementally as transcription events (i.e., [TranscribeAsyncAction]s)
 * are received over time via WebSocket.
 *
 * @property state The current transcribed text and annotation state.
 * @property operationHistory The chronological list of all transcription actions applied to this stream.
 */
data class AttendiTranscribeStream(
    val state: AttendiStreamState,
    val operationHistory: List<TranscribeAsyncAction>
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
        val newHistory = operationHistory + actions
        return copy(state = newState, operationHistory = newHistory)
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
     * - [AddAnnotation]: Appends a new annotation.
     * - [RemoveAnnotation]: Removes annotation(s) by ID.
     * - [ReplaceText]: Replaces a portion of the text using character indices.
     * - [UpdateAnnotation]: Updates properties of an existing annotation.
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
                    currentText = replaceText(currentText, action)
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
                action = oldAnnotation.action.copy(
                    id = oldAnnotation.action.id,
                    index = oldAnnotation.action.index
                ),
                parameters = oldAnnotation.parameters.copy(
                    startCharacterIndex = oldAnnotation.parameters.startCharacterIndex,
                    endCharacterIndex = oldAnnotation.parameters.endCharacterIndex
                )
            )
            annotations[index] = updatedAnnotation
        }
    }

    /**
     * Replaces a portion of the text between the given indices with new content.
     *
     * @throws IllegalArgumentException if the indices are out of bounds.
     */
    private fun replaceText(
        original: String,
        action: TranscribeAsyncAction.ReplaceText
    ): String {
        val params = action.parameters
        require(params.startCharacterIndex in 0..original.length) { "startCharacterIndex out of bounds" }
        require(params.endCharacterIndex in params.startCharacterIndex..original.length) { "endCharacterIndex out of bounds" }

        return original.substring(0, params.startCharacterIndex) +
                params.text +
                original.substring(params.endCharacterIndex)
    }
}