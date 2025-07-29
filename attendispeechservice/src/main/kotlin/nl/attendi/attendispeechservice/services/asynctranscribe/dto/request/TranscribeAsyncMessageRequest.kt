package nl.attendi.attendispeechservice.services.asynctranscribe.dto.request

import kotlinx.serialization.Serializable

/**
 * This message is sent to the transcription server when the microphone starts recording.
 */
@Serializable
internal data class TranscribeAsyncMessageRequest(
    /**
     * Is always "ClientConfiguration" for this message type. However, the serialization somehow
     * doesn't include the messageType in the JSON when we use a default value for the field.
     */
    val type: String,
    /**
     * The model to use for transcription. If not specified, the backend uses a default model
     * specified for the customer.
     */
    val model: String?,
    /**
     * Allows for associating multiple transcriptions into `sessions` and `reports`.
     */
    val reportId: String?,
    val features: TranscribeAsyncAppSettingsRequest? = null
)

/**
 * These are configuration features sent as part of the client configuration message which allows
 * us to send feature information, such as whether we can use voice editing or not.
 */
@Serializable
internal data class TranscribeAsyncAppSettingsRequest(
    val voiceEditing: TranscribeAsyncVoiceEditingAppSettingsRequest
)

@Serializable
internal data class TranscribeAsyncVoiceEditingAppSettingsRequest(
    /**
     * When enabled it allows voice editing, otherwise voice editing is disabled.
     */
    val isEnabled: Boolean
)