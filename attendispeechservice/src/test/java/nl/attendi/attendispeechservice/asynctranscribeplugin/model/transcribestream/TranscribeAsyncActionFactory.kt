package nl.attendi.attendispeechservice.asynctranscribeplugin.model.transcribestream

import nl.attendi.attendispeechservice.services.asynctranscribe.model.TranscribeAsyncAction
import nl.attendi.attendispeechservice.services.asynctranscribe.model.TranscribeAsyncActionData
import nl.attendi.attendispeechservice.services.asynctranscribe.model.TranscribeAsyncAnnotationEntityType
import nl.attendi.attendispeechservice.services.asynctranscribe.model.TranscribeAsyncAnnotationIntentStatus
import nl.attendi.attendispeechservice.services.asynctranscribe.model.TranscribeAsyncAnnotationParameters
import nl.attendi.attendispeechservice.services.asynctranscribe.model.TranscribeAsyncAnnotationType
import nl.attendi.attendispeechservice.services.asynctranscribe.model.TranscribeAsyncRemoveAnnotationParameters
import nl.attendi.attendispeechservice.services.asynctranscribe.model.TranscribeAsyncReplaceTextParameters

object TranscribeAsyncActionFactory {

    fun createSample(): List<TranscribeAsyncAction> {
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