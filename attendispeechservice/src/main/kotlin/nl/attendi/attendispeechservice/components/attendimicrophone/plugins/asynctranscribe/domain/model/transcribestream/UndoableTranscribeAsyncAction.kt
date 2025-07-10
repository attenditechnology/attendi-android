package nl.attendi.attendispeechservice.components.attendimicrophone.plugins.asynctranscribe.domain.model.transcribestream

import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.asynctranscribe.domain.model.transcribeasync.TranscribeAsyncAction

/**
 * Represents a transcription action paired with its inverse, enabling undo and redo functionality.
 *
 * This structure allows the system to efficiently revert previous operations by storing both
 * the original action and its corresponding inverse. The inverse is precomputed to avoid
 * recalculating how to undo the operation at runtime.
 *
 * @property original The original [TranscribeAsyncAction] that was applied to the transcription stream.
 * @property inverse A list of actions that, when applied, revert the effects of the original action.
 * For example, undoing an [TranscribeAsyncAction.UpdateAnnotation] action may require both
 * a remove and an add action to restore the original state.
 */
data class UndoableTranscribeAction(
    val original: TranscribeAsyncAction,
    val inverse: List<TranscribeAsyncAction>
)