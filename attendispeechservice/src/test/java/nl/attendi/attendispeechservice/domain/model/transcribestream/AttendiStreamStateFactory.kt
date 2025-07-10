package nl.attendi.attendispeechservice.domain.model.transcribestream

import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.asynctranscribe.domain.model.transcribestream.AttendiStreamState
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.asynctranscribe.domain.model.transcribeasync.TranscribeAsyncAction
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.asynctranscribe.domain.model.transcribeasync.TranscribeAsyncActionData
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.asynctranscribe.domain.model.transcribeasync.TranscribeAsyncAnnotationEntityType
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.asynctranscribe.domain.model.transcribeasync.TranscribeAsyncAnnotationIntentStatus
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.asynctranscribe.domain.model.transcribeasync.TranscribeAsyncAnnotationParameters
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.asynctranscribe.domain.model.transcribeasync.TranscribeAsyncAnnotationType

object AttendiStreamStateFactory {

    fun makeSample(): AttendiStreamState {
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