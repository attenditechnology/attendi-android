package nl.attendi.attendispeechservice.asynctranscribeplugin.model.transcribestream

import nl.attendi.attendispeechservice.services.asynctranscribe.model.TranscribeAsyncAction
import nl.attendi.attendispeechservice.services.asynctranscribe.model.TranscribeAsyncActionData
import nl.attendi.attendispeechservice.services.asynctranscribe.model.TranscribeAsyncAnnotationEntityType
import nl.attendi.attendispeechservice.services.asynctranscribe.model.TranscribeAsyncAnnotationIntentStatus
import nl.attendi.attendispeechservice.services.asynctranscribe.model.TranscribeAsyncAnnotationParameters
import nl.attendi.attendispeechservice.services.asynctranscribe.model.TranscribeAsyncAnnotationType
import nl.attendi.attendispeechservice.components.attendirecorder.plugins.asynctranscribeplugin.model.AttendiStreamState

object AttendiStreamStateFactory {

    fun createSample(): AttendiStreamState {
        return AttendiStreamState(
            text = "Attendi",
            annotations = listOf(
                TranscribeAsyncAction.AddAnnotation(
                    actionData = TranscribeAsyncActionData("1", 1),
                    parameters = TranscribeAsyncAnnotationParameters(
                        id = "1A",
                        startCharacterIndex = 0,
                        endCharacterIndex = 0,
                        type = TranscribeAsyncAnnotationType.TranscriptionTentative
                    )
                ),
                TranscribeAsyncAction.AddAnnotation(
                    actionData = TranscribeAsyncActionData("2", 2),
                    parameters = TranscribeAsyncAnnotationParameters(
                        id = "2A",
                        startCharacterIndex = 0,
                        endCharacterIndex = 0,
                        type = TranscribeAsyncAnnotationType.Entity(
                            type = TranscribeAsyncAnnotationEntityType.NAME,
                            "Entity"
                        )
                    )
                ),
                TranscribeAsyncAction.AddAnnotation(
                    actionData = TranscribeAsyncActionData("5", 5),
                    parameters = TranscribeAsyncAnnotationParameters(
                        id = "5A",
                        startCharacterIndex = 1,
                        endCharacterIndex = 5,
                        type = TranscribeAsyncAnnotationType.Intent(status = TranscribeAsyncAnnotationIntentStatus.PENDING)
                    )
                ),
                TranscribeAsyncAction.AddAnnotation(
                    actionData = TranscribeAsyncActionData("7", 7),
                    parameters = TranscribeAsyncAnnotationParameters(
                        id = "7A",
                        startCharacterIndex = 1,
                        endCharacterIndex = 3,
                        type = TranscribeAsyncAnnotationType.TranscriptionTentative
                    )
                )
            )
        )
    }
}