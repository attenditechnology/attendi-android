package nl.attendi.attendispeechservice.domain.model.transcribestream

import nl.attendi.attendispeechservice.domain.model.transcribeasync.TranscribeAsyncAction
import nl.attendi.attendispeechservice.domain.model.transcribeasync.TranscribeAsyncActionData
import nl.attendi.attendispeechservice.domain.model.transcribeasync.TranscribeAsyncAnnotationEntityType
import nl.attendi.attendispeechservice.domain.model.transcribeasync.TranscribeAsyncAnnotationIntentStatus
import nl.attendi.attendispeechservice.domain.model.transcribeasync.TranscribeAsyncAnnotationParameters
import nl.attendi.attendispeechservice.domain.model.transcribeasync.TranscribeAsyncAnnotationType
import nl.attendi.attendispeechservice.domain.model.transcribeasync.TranscribeAsyncRemoveAnnotationParameters
import nl.attendi.attendispeechservice.domain.model.transcribeasync.TranscribeAsyncReplaceTextParameters

object TranscribeAsyncActionFactory {

    fun makeSample(): List<TranscribeAsyncAction> {
        return listOf(
            TranscribeAsyncAction.ReplaceText(
                actionData = TranscribeAsyncActionData(
                    id = "0",
                    index = 0
                ),
                parameters = TranscribeAsyncReplaceTextParameters(
                    text = "Attendi",
                    startCharacterIndex = 0,
                    endCharacterIndex = 0
                )
            ),
            TranscribeAsyncAction.AddAnnotation(
                actionData = TranscribeAsyncActionData(
                    id = "1",
                    index = 1
                ),
                parameters = TranscribeAsyncAnnotationParameters(
                    id = "1A",
                    startCharacterIndex = 0,
                    endCharacterIndex = 0,
                    type = TranscribeAsyncAnnotationType.TranscriptionTentative
                )
            ),
            TranscribeAsyncAction.AddAnnotation(
                actionData = TranscribeAsyncActionData(
                    id = "2",
                    index = 2
                ),
                parameters = TranscribeAsyncAnnotationParameters(
                    id = "2A",
                    startCharacterIndex = 0,
                    endCharacterIndex = 0,
                    type = TranscribeAsyncAnnotationType.Entity(
                        type = TranscribeAsyncAnnotationEntityType.NAME,
                        text = "Entity"
                    )
                )
            ),
            TranscribeAsyncAction.AddAnnotation(
                actionData = TranscribeAsyncActionData(
                    id = "3",
                    index = 3
                ),
                parameters = TranscribeAsyncAnnotationParameters(
                    id = "3A",
                    startCharacterIndex = 0,
                    endCharacterIndex = 0,
                    type = TranscribeAsyncAnnotationType.TranscriptionTentative
                )
            ),
            TranscribeAsyncAction.RemoveAnnotation(
                actionData = TranscribeAsyncActionData(
                    id = "4",
                    index = 4
                ),
                parameters = TranscribeAsyncRemoveAnnotationParameters(
                    id = "3A"
                )
            ),
            TranscribeAsyncAction.AddAnnotation(
                actionData = TranscribeAsyncActionData(
                    id = "5",
                    index = 5
                ),
                parameters = TranscribeAsyncAnnotationParameters(
                    id = "5A",
                    startCharacterIndex = 1,
                    endCharacterIndex = 5,
                    type = TranscribeAsyncAnnotationType.Intent(status = TranscribeAsyncAnnotationIntentStatus.PENDING)
                )
            ),
            TranscribeAsyncAction.AddAnnotation(
                actionData = TranscribeAsyncActionData(
                    id = "6",
                    index = 6
                ),
                parameters = TranscribeAsyncAnnotationParameters(
                    id = "6A",
                    startCharacterIndex = 1,
                    endCharacterIndex = 5,
                    type = TranscribeAsyncAnnotationType.Intent(status = TranscribeAsyncAnnotationIntentStatus.PENDING)
                )
            ),
            TranscribeAsyncAction.UpdateAnnotation(
                actionData = TranscribeAsyncActionData(
                    id = "7",
                    index = 7
                ),
                parameters = TranscribeAsyncAnnotationParameters(
                    id = "6A",
                    startCharacterIndex = 1,
                    endCharacterIndex = 3,
                    type = TranscribeAsyncAnnotationType.TranscriptionTentative
                )
            )
        )
    }
}