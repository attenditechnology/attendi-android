package nl.attendi.attendispeechservice.asynctranscribeplugin.model.transcribestream

import nl.attendi.attendispeechservice.services.asynctranscribe.model.TranscribeAsyncAction
import nl.attendi.attendispeechservice.services.asynctranscribe.model.TranscribeAsyncAnnotationEntityType
import nl.attendi.attendispeechservice.services.asynctranscribe.model.TranscribeAsyncAnnotationIntentStatus
import nl.attendi.attendispeechservice.services.asynctranscribe.model.TranscribeAsyncAnnotationType
import nl.attendi.attendispeechservice.components.attendirecorder.plugins.asynctranscribeplugin.model.AttendiStreamState
import nl.attendi.attendispeechservice.components.attendirecorder.plugins.asynctranscribeplugin.model.AttendiTranscribeStream
import org.junit.Assert
import org.junit.Test

class AttendiTranscribeStreamTests {

    @Test
    fun receiveActions_whenActionsAreEmpty_returnsSameStream() {
        val attendiStreamState = AttendiStreamState(
            text = "Attendi",
            annotations = emptyList()
        )
        val actions = emptyList<TranscribeAsyncAction>()
        var sut = AttendiTranscribeStream(
            state = attendiStreamState,
            operationHistory = emptyList(),
            undoneOperations = emptyList()
        )

        sut = sut.receiveActions(actions)

        Assert.assertEquals("Attendi", sut.state.text)
        Assert.assertEquals(0, sut.state.annotations.size)
        Assert.assertEquals(0, sut.operationHistory.size)
        Assert.assertEquals(0, sut.undoneOperations.size)
    }

    @Test
    fun receiveActions_whenActionsAreNotEmpty_returnsTransformedStream() {
        val attendiStreamState = AttendiStreamState(
            text = "",
            annotations = emptyList()
        )
        val initialStream = AttendiTranscribeStream(
            state = attendiStreamState,
            operationHistory = emptyList(),
            undoneOperations = emptyList()
        )
        val sampleAttendiStreamState = AttendiStreamStateFactory.createSample()

        val sut = initialStream.receiveActions(TranscribeAsyncActionFactory.createSample())

        Assert.assertEquals("Attendi", sut.state.text)
        Assert.assertEquals(4, sut.state.annotations.size)
        // First State AddAnnotation
        val annotation0 = sampleAttendiStreamState.annotations[0]
        Assert.assertEquals("1", annotation0.actionData.id)
        Assert.assertEquals(1, annotation0.actionData.index)
        Assert.assertEquals("1A", annotation0.parameters.id)
        Assert.assertEquals(0, annotation0.parameters.startCharacterIndex)
        Assert.assertEquals(0, annotation0.parameters.endCharacterIndex)
        Assert.assertEquals(
            TranscribeAsyncAnnotationType.TranscriptionTentative,
            annotation0.parameters.type
        )
        // Second State AddAnnotation
        val annotation1 = sampleAttendiStreamState.annotations[1]
        Assert.assertEquals("2", annotation1.actionData.id)
        Assert.assertEquals(2, annotation1.actionData.index)
        Assert.assertEquals("2A", annotation1.parameters.id)
        Assert.assertEquals(0, annotation1.parameters.startCharacterIndex)
        Assert.assertEquals(0, annotation1.parameters.endCharacterIndex)
        Assert.assertEquals(
            TranscribeAsyncAnnotationType.Entity(
                type = TranscribeAsyncAnnotationEntityType.NAME,
                text = "Entity"
            ), annotation1.parameters.type
        )
        // Third State AddAnnotation
        val annotation2 = sampleAttendiStreamState.annotations[2]
        Assert.assertEquals("5", annotation2.actionData.id)
        Assert.assertEquals(5, annotation2.actionData.index)
        Assert.assertEquals("5A", annotation2.parameters.id)
        Assert.assertEquals(1, annotation2.parameters.startCharacterIndex)
        Assert.assertEquals(5, annotation2.parameters.endCharacterIndex)
        Assert.assertEquals(
            TranscribeAsyncAnnotationType.Intent(status = TranscribeAsyncAnnotationIntentStatus.PENDING),
            annotation2.parameters.type
        )
        // Fourth State AddAnnotation
        val annotation3 = sampleAttendiStreamState.annotations[3]
        Assert.assertEquals("7", annotation3.actionData.id)
        Assert.assertEquals(7, annotation3.actionData.index)
        Assert.assertEquals("7A", annotation3.parameters.id)
        Assert.assertEquals(1, annotation3.parameters.startCharacterIndex)
        Assert.assertEquals(3, annotation3.parameters.endCharacterIndex)
        Assert.assertEquals(
            TranscribeAsyncAnnotationType.TranscriptionTentative,
            annotation3.parameters.type
        )
        // Operation History
        Assert.assertEquals(8, sut.operationHistory.size)
        // ReplaceText Operation with inverse ReplaceText
        val undoableTranscribeActionOriginal0 = sut.operationHistory[0].original as TranscribeAsyncAction.ReplaceText
        val undoableTranscribeActionInverse0 = sut.operationHistory[0].inverse[0] as TranscribeAsyncAction.ReplaceText
        Assert.assertEquals("0", undoableTranscribeActionOriginal0.actionData.id)
        Assert.assertEquals(0, undoableTranscribeActionOriginal0.actionData.index)
        Assert.assertEquals(0, undoableTranscribeActionOriginal0.parameters.startCharacterIndex)
        Assert.assertEquals(0, undoableTranscribeActionOriginal0.parameters.endCharacterIndex)
        Assert.assertEquals("Attendi", undoableTranscribeActionOriginal0.parameters.text)
        Assert.assertEquals("0", undoableTranscribeActionInverse0.actionData.id)
        Assert.assertEquals(0, undoableTranscribeActionInverse0.actionData.index)
        Assert.assertEquals(0, undoableTranscribeActionInverse0.parameters.startCharacterIndex)
        Assert.assertEquals(7, undoableTranscribeActionInverse0.parameters.endCharacterIndex)
        Assert.assertEquals("", undoableTranscribeActionInverse0.parameters.text)
        // AddAnnotation Operation with inverse RemoveAnnotation
        val undoableTranscribeActionOriginal2 = sut.operationHistory[2].original as TranscribeAsyncAction.AddAnnotation
        val undoableTranscribeActionInverse2 = sut.operationHistory[2].inverse[0] as TranscribeAsyncAction.RemoveAnnotation
        Assert.assertEquals("2", undoableTranscribeActionOriginal2.actionData.id)
        Assert.assertEquals(2, undoableTranscribeActionOriginal2.actionData.index)
        Assert.assertEquals("2A", undoableTranscribeActionOriginal2.parameters.id)
        Assert.assertEquals(0, undoableTranscribeActionOriginal2.parameters.startCharacterIndex)
        Assert.assertEquals(0, undoableTranscribeActionOriginal2.parameters.endCharacterIndex)
        Assert.assertEquals(TranscribeAsyncAnnotationEntityType.NAME, (undoableTranscribeActionOriginal2.parameters.type as TranscribeAsyncAnnotationType.Entity).type)
        Assert.assertEquals("Entity", (undoableTranscribeActionOriginal2.parameters.type as TranscribeAsyncAnnotationType.Entity).text)
        Assert.assertEquals("2", undoableTranscribeActionInverse2.actionData.id)
        Assert.assertEquals(2, undoableTranscribeActionInverse2.actionData.index)
        Assert.assertEquals("2A", undoableTranscribeActionInverse2.parameters.id)
        // RemoveAnnotation Operation with inverse AddAnnotation
        val undoableTranscribeActionOriginal4 = sut.operationHistory[4].original as TranscribeAsyncAction.RemoveAnnotation
        val undoableTranscribeActionInverse4 = sut.operationHistory[4].inverse[0] as TranscribeAsyncAction.AddAnnotation
        Assert.assertEquals("4", undoableTranscribeActionOriginal4.actionData.id)
        Assert.assertEquals(4, undoableTranscribeActionOriginal4.actionData.index)
        Assert.assertEquals("3A", undoableTranscribeActionOriginal4.parameters.id)
        Assert.assertEquals("3", undoableTranscribeActionInverse4.actionData.id)
        Assert.assertEquals(3, undoableTranscribeActionInverse4.actionData.index)
        Assert.assertEquals("3A", undoableTranscribeActionInverse4.parameters.id)
        Assert.assertEquals(0, undoableTranscribeActionInverse4.parameters.startCharacterIndex)
        Assert.assertEquals(0, undoableTranscribeActionInverse4.parameters.endCharacterIndex)
        Assert.assertEquals(TranscribeAsyncAnnotationType.TranscriptionTentative, undoableTranscribeActionInverse4.parameters.type as TranscribeAsyncAnnotationType.TranscriptionTentative)
        // UpdateAnnotation Operation with inverse AddAnnotation
        val undoableTranscribeActionOriginal7 = sut.operationHistory[7].original as TranscribeAsyncAction.UpdateAnnotation
        val undoableTranscribeActionInverseRemove7 = sut.operationHistory[7].inverse[0] as TranscribeAsyncAction.RemoveAnnotation
        val undoableTranscribeActionInverseAdd7 = sut.operationHistory[7].inverse[1] as TranscribeAsyncAction.AddAnnotation
        Assert.assertEquals("7", undoableTranscribeActionOriginal7.actionData.id)
        Assert.assertEquals(7, undoableTranscribeActionOriginal7.actionData.index)
        Assert.assertEquals("6A", undoableTranscribeActionOriginal7.parameters.id)
        Assert.assertEquals(1, undoableTranscribeActionOriginal7.parameters.startCharacterIndex)
        Assert.assertEquals(3, undoableTranscribeActionOriginal7.parameters.endCharacterIndex)
        Assert.assertEquals(TranscribeAsyncAnnotationType.TranscriptionTentative, undoableTranscribeActionOriginal7.parameters.type as TranscribeAsyncAnnotationType.TranscriptionTentative)
        Assert.assertEquals("7", undoableTranscribeActionInverseRemove7.actionData.id)
        Assert.assertEquals(7, undoableTranscribeActionInverseRemove7.actionData.index)
        Assert.assertEquals("7", undoableTranscribeActionInverseRemove7.parameters.id)
        Assert.assertEquals("6", undoableTranscribeActionInverseAdd7.actionData.id)
        Assert.assertEquals(6, undoableTranscribeActionInverseAdd7.actionData.index)
        Assert.assertEquals("6A", undoableTranscribeActionInverseAdd7.parameters.id)
        Assert.assertEquals(1, undoableTranscribeActionInverseAdd7.parameters.startCharacterIndex)
        Assert.assertEquals(5, undoableTranscribeActionInverseAdd7.parameters.endCharacterIndex)
        Assert.assertEquals(TranscribeAsyncAnnotationIntentStatus.PENDING, (undoableTranscribeActionInverseAdd7.parameters.type as TranscribeAsyncAnnotationType.Intent).status)
        // Empty UndoneOperations history
        Assert.assertEquals(0, sut.undoneOperations.size)
    }

    @Test
    fun undoOperations_whenIndexIsWithinOperationHistoryBounds_returnsPreviousTranscribeState() {
        val attendiStreamState = AttendiStreamState(
            text = "",
            annotations = emptyList()
        )
        var initialStream = AttendiTranscribeStream(
            state = attendiStreamState,
            operationHistory = emptyList(),
            undoneOperations = emptyList()
        )
        initialStream = initialStream.receiveActions(TranscribeAsyncActionFactory.createSample())

        var sut = initialStream.undoOperations(4)

        val state = sut.state
        Assert.assertEquals("Attendi", state.text)
        Assert.assertEquals(3, state.annotations.size)
        // First state Add Annotation
        val annotation1 = state.annotations[0]
        Assert.assertEquals("1", annotation1.actionData.id)
        Assert.assertEquals(1, annotation1.actionData.index)
        Assert.assertEquals("1A", annotation1.parameters.id)
        Assert.assertEquals(0, annotation1.parameters.startCharacterIndex)
        Assert.assertEquals(0, annotation1.parameters.endCharacterIndex)
        Assert.assertEquals(
            TranscribeAsyncAnnotationType.TranscriptionTentative,
            annotation1.parameters.type
        )
        // Second state Add Annotation
        val annotation2 = state.annotations[1]
        Assert.assertEquals("2", annotation2.actionData.id)
        Assert.assertEquals(2, annotation2.actionData.index)
        Assert.assertEquals("2A", annotation2.parameters.id)
        Assert.assertEquals(0, annotation2.parameters.startCharacterIndex)
        Assert.assertEquals(0, annotation2.parameters.endCharacterIndex)
        Assert.assertEquals(
            TranscribeAsyncAnnotationType.Entity(
                type = TranscribeAsyncAnnotationEntityType.NAME,
                text = "Entity"
            ), annotation2.parameters.type
        )
        // Third state Add Annotation
        val annotation3 = state.annotations[2]
        Assert.assertEquals("3", annotation3.actionData.id)
        Assert.assertEquals(3, annotation3.actionData.index)
        Assert.assertEquals("3A", annotation3.parameters.id)
        Assert.assertEquals(0, annotation3.parameters.startCharacterIndex)
        Assert.assertEquals(0, annotation3.parameters.endCharacterIndex)
        Assert.assertEquals(
            TranscribeAsyncAnnotationType.TranscriptionTentative,
            annotation3.parameters.type
        )
        Assert.assertEquals(4, sut.operationHistory.size)
        Assert.assertEquals(4, sut.undoneOperations.size)

        // Roll back 2 times more
        sut = sut.undoOperations(2)

        Assert.assertEquals("Attendi", sut.state.text)
        Assert.assertEquals(1, sut.state.annotations.size)
        Assert.assertEquals(2, sut.operationHistory.size)
        Assert.assertEquals(6, sut.undoneOperations.size)

        // Roll back 1 more time to remove all add annotations
        sut = sut.undoOperations(1)

        Assert.assertEquals("Attendi", sut.state.text)
        Assert.assertEquals(0, sut.state.annotations.size)
        Assert.assertEquals(1, sut.operationHistory.size)
        Assert.assertEquals(7, sut.undoneOperations.size)

        // Roll back 1 more time to remove the replace text annotation
        sut = sut.undoOperations(1)

        Assert.assertEquals("", sut.state.text)
        Assert.assertEquals(0, sut.state.annotations.size)
        Assert.assertEquals(0, sut.operationHistory.size)
        Assert.assertEquals(8, sut.undoneOperations.size)
    }

    @Test
    fun undoOperations_whenIndexIsBeyondOperationHistoryBounds_returnsEmptyTranscribeState() {
        val attendiStreamState = AttendiStreamState(
            text = "",
            annotations = emptyList()
        )
        var initialStream = AttendiTranscribeStream(
            state = attendiStreamState,
            operationHistory = emptyList(),
            undoneOperations = emptyList()
        )
        initialStream = initialStream.receiveActions(TranscribeAsyncActionFactory.createSample())

        val sut = initialStream.undoOperations(20)

        Assert.assertEquals("", sut.state.text)
        Assert.assertEquals(0, sut.state.annotations.size)
        Assert.assertEquals(0, sut.operationHistory.size)
        Assert.assertEquals(8, sut.undoneOperations.size)
    }

    @Test
    fun redoOperations_whenIndexIsWithinUndoneOperationsBounds_returnsNextTranscribeState() {
        val attendiStreamState = AttendiStreamState(
            text = "",
            annotations = emptyList()
        )
        var initialStream = AttendiTranscribeStream(
            state = attendiStreamState,
            operationHistory = emptyList(),
            undoneOperations = emptyList()
        )
        initialStream = initialStream.receiveActions(TranscribeAsyncActionFactory.createSample())

        var sut = initialStream.undoOperations(4).redoOperations(1)

        val state = sut.state
        Assert.assertEquals("Attendi", state.text)
        Assert.assertEquals(2, state.annotations.size)
        // First state Add Annotation
        val annotation1 = state.annotations[0]
        Assert.assertEquals("1", annotation1.actionData.id)
        Assert.assertEquals(1, annotation1.actionData.index)
        Assert.assertEquals("1A", annotation1.parameters.id)
        Assert.assertEquals(0, annotation1.parameters.startCharacterIndex)
        Assert.assertEquals(0, annotation1.parameters.endCharacterIndex)
        Assert.assertEquals(
            TranscribeAsyncAnnotationType.TranscriptionTentative,
            annotation1.parameters.type
        )
        // Second state Add Annotation
        val annotation2 = state.annotations[1]
        Assert.assertEquals("2", annotation2.actionData.id)
        Assert.assertEquals(2, annotation2.actionData.index)
        Assert.assertEquals("2A", annotation2.parameters.id)
        Assert.assertEquals(0, annotation2.parameters.startCharacterIndex)
        Assert.assertEquals(0, annotation2.parameters.endCharacterIndex)
        Assert.assertEquals(
            TranscribeAsyncAnnotationType.Entity(
                type = TranscribeAsyncAnnotationEntityType.NAME,
                text = "Entity"
            ), annotation2.parameters.type
        )
        Assert.assertEquals(5, sut.operationHistory.size)
        Assert.assertEquals(3, sut.undoneOperations.size)

        // Redo 2 times more
        sut = sut.redoOperations(2)

        Assert.assertEquals("Attendi", sut.state.text)
        Assert.assertEquals(4, sut.state.annotations.size)
        Assert.assertEquals(7, sut.operationHistory.size)
        Assert.assertEquals(1, sut.undoneOperations.size)

        // Redo 1 time more, back to original
        sut = sut.redoOperations(1)

        Assert.assertEquals(4, sut.state.annotations.size)
        Assert.assertEquals(8, sut.operationHistory.size)
        Assert.assertEquals(0, sut.undoneOperations.size)
        Assert.assertEquals(initialStream.state, sut.state)
        Assert.assertEquals(initialStream.operationHistory, sut.operationHistory)
        Assert.assertEquals(initialStream.undoneOperations, sut.undoneOperations)
    }

    @Test
    fun redoOperations_whenIndexIsBeyondUndoneOperationsBounds_returnsOriginalTranscribeState() {
        val attendiStreamState = AttendiStreamState(
            text = "",
            annotations = emptyList()
        )
        var initialStream = AttendiTranscribeStream(
            state = attendiStreamState,
            operationHistory = emptyList(),
            undoneOperations = emptyList()
        )
        initialStream = initialStream.receiveActions(TranscribeAsyncActionFactory.createSample())

        val sut = initialStream.undoOperations(4).redoOperations(20)

        Assert.assertEquals(4, sut.state.annotations.size)
        Assert.assertEquals(8, sut.operationHistory.size)
        Assert.assertEquals(0, sut.undoneOperations.size)
        Assert.assertEquals(initialStream.state, sut.state)
        Assert.assertEquals(initialStream.operationHistory, sut.operationHistory)
        Assert.assertEquals(initialStream.undoneOperations, sut.undoneOperations)
    }
}