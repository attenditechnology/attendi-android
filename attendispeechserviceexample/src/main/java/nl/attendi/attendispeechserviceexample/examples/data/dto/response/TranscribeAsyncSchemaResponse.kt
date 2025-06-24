package nl.attendi.attendispeechserviceexample.examples.data.dto.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A response that is sent to the end user from Attendi's transcription API
 */
@Serializable
data class TranscribeAsyncSchemaResponse(
    val actions: List<TranscribeAsyncAnnotationResponse>
)

@Serializable
data class TranscribeAsyncAnnotationResponse(
    val type: TranscribeAsyncActionTypeResponse,
    val id: String,
    val parameters: TranscribeAsyncAnnotationParametersResponse,
    val index: Int? = null
)

@Serializable
data class TranscribeAsyncAnnotationParametersResponse(
    val type: TranscribeAsyncAnnotationParameterTypeResponse? = null,
    val id: String? = null,
    val text: String? = null,
    val parameters: TranscribeAsyncAnnotationExtraParametersResponse? = null,
    val startCharacterIndex: Int? = null,
    val endCharacterIndex: Int? = null
)

@Serializable
data class TranscribeAsyncAnnotationExtraParametersResponse(
    val status: TranscribeAsyncAnnotationIntentStatusResponse? = null,
    val type: TranscribeAsyncAnnotationEntityTypeResponse? = null
)

@Serializable
enum class TranscribeAsyncActionTypeResponse {
    @SerialName("add_annotation")
    ADD_ANNOTATION,

    @SerialName("update_annotation")
    UPDATE_ANNOTATION,

    @SerialName("remove_annotation")
    REMOVE_ANNOTATION,

    @SerialName("replace_text")
    REPLACE_TEXT
}

@Serializable
enum class TranscribeAsyncAnnotationParameterTypeResponse {
    @SerialName("transcription_tentative")
    TRANSCRIPTION_TENTATIVE,

    @SerialName("intent")
    INTENT,

    @SerialName("entity")
    ENTITY
}

@Serializable
enum class TranscribeAsyncAnnotationIntentStatusResponse {
    @SerialName("pending")
    PENDING,

    @SerialName("recognized")
    RECOGNIZED
}

@Serializable
enum class TranscribeAsyncAnnotationEntityTypeResponse {
    @SerialName("name")
    NAME
}