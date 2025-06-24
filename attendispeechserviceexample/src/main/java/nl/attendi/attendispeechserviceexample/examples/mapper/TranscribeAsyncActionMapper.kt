package nl.attendi.attendispeechserviceexample.examples.mapper

import nl.attendi.attendispeechserviceexample.examples.data.transcribeasyncservice.dto.response.TranscribeAsyncActionTypeResponse
import nl.attendi.attendispeechserviceexample.examples.data.transcribeasyncservice.dto.response.TranscribeAsyncAnnotationEntityTypeResponse
import nl.attendi.attendispeechserviceexample.examples.data.transcribeasyncservice.dto.response.TranscribeAsyncAnnotationIntentStatusResponse
import nl.attendi.attendispeechserviceexample.examples.data.transcribeasyncservice.dto.response.TranscribeAsyncAnnotationParameterTypeResponse
import nl.attendi.attendispeechserviceexample.examples.data.transcribeasyncservice.dto.response.TranscribeAsyncAnnotationResponse
import nl.attendi.attendispeechserviceexample.examples.data.transcribeasyncservice.dto.response.TranscribeAsyncSchemaResponse
import nl.attendi.attendispeechserviceexample.examples.domain.model.TranscribeAsyncAction
import nl.attendi.attendispeechserviceexample.examples.domain.model.TranscribeAsyncActionData
import nl.attendi.attendispeechserviceexample.examples.domain.model.TranscribeAsyncAddAnnotationEntityType
import nl.attendi.attendispeechserviceexample.examples.domain.model.TranscribeAsyncAddAnnotationIntentStatus
import nl.attendi.attendispeechserviceexample.examples.domain.model.TranscribeAsyncAddAnnotationParameters
import nl.attendi.attendispeechserviceexample.examples.domain.model.TranscribeAsyncAddAnnotationType
import nl.attendi.attendispeechserviceexample.examples.domain.model.TranscribeAsyncRemoveAnnotationParameters
import nl.attendi.attendispeechserviceexample.examples.domain.model.TranscribeAsyncReplaceTextParameters
import nl.attendi.attendispeechserviceexample.examples.domain.model.TranscribeAsyncUpdateAnnotationEntityType
import nl.attendi.attendispeechserviceexample.examples.domain.model.TranscribeAsyncUpdateAnnotationParameters
import nl.attendi.attendispeechserviceexample.examples.domain.model.TranscribeAsyncUpdateAnnotationType

object TranscribeAsyncActionMapper {

    fun map(response: TranscribeAsyncSchemaResponse): List<TranscribeAsyncAction> {
        return response.actions.map { action ->
            when (action.type) {
                TranscribeAsyncActionTypeResponse.ADD_ANNOTATION -> {
                    mapAddAnnotation(action)
                }

                TranscribeAsyncActionTypeResponse.UPDATE_ANNOTATION -> {
                    mapUpdateAnnotation(action)
                }

                TranscribeAsyncActionTypeResponse.REMOVE_ANNOTATION -> {
                    mapRemoveAnnotation(action)
                }

                TranscribeAsyncActionTypeResponse.REPLACE_TEXT -> {
                    mapReplaceTextAnnotation(action)
                }
            }
        }
    }

    private fun mapAddAnnotation(action: TranscribeAsyncAnnotationResponse): TranscribeAsyncAction {
        val parameters = action.parameters ?: error("Missing required field: 'parameters' in AddAnnotation")
        return when (parameters.type) {
            TranscribeAsyncAnnotationParameterTypeResponse.TRANSCRIPTION_TENTATIVE -> {
                TranscribeAsyncAction.AddAnnotation(
                    action = TranscribeAsyncActionData(
                        id = action.id,
                        index = action.index
                    ),
                    parameters = TranscribeAsyncAddAnnotationParameters(
                        id = parameters.id
                            ?: error("Missing required field: 'id' in AddAnnotation TRANSCRIPTION_TENTATIVE Parameters"),
                        startCharacterIndex = parameters.startCharacterIndex
                            ?: error("Missing required field: 'startCharacterIndex' in AddAnnotation TRANSCRIPTION_TENTATIVE Parameters"),
                        endCharacterIndex = parameters.endCharacterIndex
                            ?: error("Missing required field: 'endCharacterIndex' in AddAnnotation TRANSCRIPTION_TENTATIVE Parameters"),
                        type = TranscribeAsyncAddAnnotationType.TranscriptionTentative
                    )
                )
            }

            TranscribeAsyncAnnotationParameterTypeResponse.INTENT -> {
                val statusResponse = parameters.parameters?.status
                    ?: error("Missing required field: 'status' in AddAnnotation INTENT Parameters")
                val status = when (statusResponse) {
                    TranscribeAsyncAnnotationIntentStatusResponse.PENDING -> TranscribeAsyncAddAnnotationIntentStatus.PENDING
                    TranscribeAsyncAnnotationIntentStatusResponse.RECOGNIZED -> TranscribeAsyncAddAnnotationIntentStatus.RECOGNIZED
                }
                TranscribeAsyncAction.AddAnnotation(
                    action = TranscribeAsyncActionData(
                        id = action.id,
                        index = action.index
                    ),
                    parameters = TranscribeAsyncAddAnnotationParameters(
                        id = parameters.id
                            ?: error("Missing required field: 'id' in AddAnnotation INTENT Parameters"),
                        startCharacterIndex = parameters.startCharacterIndex
                            ?: error("Missing required field: 'startCharacterIndex' in AddAnnotation INTENT Parameters"),
                        endCharacterIndex = parameters.endCharacterIndex
                            ?: error("Missing required field: 'endCharacterIndex' in AddAnnotation INTENT Parameters"),
                        type = TranscribeAsyncAddAnnotationType.Intent(status = status)
                    )
                )
            }

            TranscribeAsyncAnnotationParameterTypeResponse.ENTITY -> {
                val typeResponse = parameters.parameters?.type
                    ?: error("Missing required field: 'type' in AddAnnotation ENTITY Parameters")
                val type = when (typeResponse) {
                    TranscribeAsyncAnnotationEntityTypeResponse.NAME -> TranscribeAsyncAddAnnotationEntityType.NAME
                }

                TranscribeAsyncAction.AddAnnotation(
                    action = TranscribeAsyncActionData(
                        id = action.id,
                        index = action.index
                    ),
                    parameters = TranscribeAsyncAddAnnotationParameters(
                        id = parameters.id
                            ?: error("Missing required field: 'id' in AddAnnotation ENTITY Parameters"),
                        startCharacterIndex = parameters.startCharacterIndex
                            ?: error("Missing required field: 'startCharacterIndex' in AddAnnotation ENTITY Parameters"),
                        endCharacterIndex = parameters.endCharacterIndex
                            ?: error("Missing required field: 'endCharacterIndex' in AddAnnotation ENTITY Parameters"),
                        type = TranscribeAsyncAddAnnotationType.Entity(
                            type = type,
                            text = parameters.parameters.text
                                ?: error("Missing required field: 'text' in AddAnnotation ENTITY Parameters")
                        )
                    )
                )
            }

            null -> error("Missing required field: 'type' in AddAnnotation Parameters")
        }
    }

    private fun mapUpdateAnnotation(action: TranscribeAsyncAnnotationResponse): TranscribeAsyncAction {
        val parameters = action.parameters ?: error("Missing required field: 'parameters' in UpdateAnnotation")
        return when (parameters.type) {
            TranscribeAsyncAnnotationParameterTypeResponse.TRANSCRIPTION_TENTATIVE -> {
                TranscribeAsyncAction.UpdateAnnotation(
                    action = TranscribeAsyncActionData(
                        id = action.id,
                        index = action.index
                    ),
                    parameters = TranscribeAsyncUpdateAnnotationParameters(
                        id = parameters.id
                            ?: error("Missing required field: 'id' in UpdateAnnotation TRANSCRIPTION_TENTATIVE Parameters"),
                        startCharacterIndex = parameters.startCharacterIndex
                            ?: error("Missing required field: 'startCharacterIndex' in UpdateAnnotation TRANSCRIPTION_TENTATIVE Parameters"),
                        endCharacterIndex = parameters.endCharacterIndex
                            ?: error("Missing required field: 'endCharacterIndex' in UpdateAnnotation TRANSCRIPTION_TENTATIVE Parameters"),
                        type = TranscribeAsyncUpdateAnnotationType.TranscriptionTentative
                    )
                )
            }

            TranscribeAsyncAnnotationParameterTypeResponse.ENTITY -> {
                val typeResponse = parameters.parameters?.type
                    ?: error("Missing required field: 'type' in UpdateAnnotation ENTITY Parameters")
                val type = when (typeResponse) {
                    TranscribeAsyncAnnotationEntityTypeResponse.NAME -> TranscribeAsyncUpdateAnnotationEntityType.NAME
                }
                TranscribeAsyncAction.UpdateAnnotation(
                    action = TranscribeAsyncActionData(
                        id = action.id,
                        index = action.index
                    ),
                    parameters = TranscribeAsyncUpdateAnnotationParameters(
                        id = parameters.id
                            ?: error("Missing required field: 'id' in UpdateAnnotation ENTITY Parameters"),
                        startCharacterIndex = parameters.startCharacterIndex
                            ?: error("Missing required field: 'startCharacterIndex' in UpdateAnnotation ENTITY Parameters"),
                        endCharacterIndex = parameters.endCharacterIndex
                            ?: error("Missing required field: 'endCharacterIndex' in UpdateAnnotation ENTITY Parameters"),
                        type = TranscribeAsyncUpdateAnnotationType.Entity(
                            type = type,
                            text = parameters.parameters.text
                                ?: error("Missing required field: 'text' in UpdateAnnotation ENTITY Parameters")
                        )
                    )
                )
            }

            TranscribeAsyncAnnotationParameterTypeResponse.INTENT -> {
                error("UpdateAnnotation INTENT is not available")
            }

            null -> error("Missing required field: 'type' in UpdateAnnotation Parameters")
        }
    }

    private fun mapRemoveAnnotation(action: TranscribeAsyncAnnotationResponse): TranscribeAsyncAction {
        val parameters = action.parameters ?: error("Missing required field: 'parameters' in RemoveAnnotation")
        return TranscribeAsyncAction.RemoveAnnotation(
            action = TranscribeAsyncActionData(
                id = action.id,
                index = action.index
            ),
            parameters = TranscribeAsyncRemoveAnnotationParameters(
                id = parameters.id
                    ?: error("Missing required field: 'id' in REMOVE_ANNOTATION")
            )
        )
    }

    private fun mapReplaceTextAnnotation(action: TranscribeAsyncAnnotationResponse): TranscribeAsyncAction {
        val parameters = action.parameters ?: error("Missing required field: 'parameters' in ReplaceTextAnnotation")
        return TranscribeAsyncAction.ReplaceText(
            action = TranscribeAsyncActionData(
                id = action.id,
                index = action.index
            ),
            parameters = TranscribeAsyncReplaceTextParameters(
                startCharacterIndex = parameters.startCharacterIndex
                    ?: error("Missing required field: 'startCharacterIndex' in REPLACE_TEXT Parameters"),
                endCharacterIndex = parameters.endCharacterIndex
                    ?: error("Missing required field: 'endCharacterIndex' in REPLACE_TEXT Parameters"),
                text = parameters.text
                    ?: error("Missing required field: 'text' in REPLACE_TEXT Parameters"),
            )
        )
    }
}