package nl.attendi.attendispeechservice.data.connection.websocket

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nl.attendi.attendispeechservice.data.service.transcribeasyncservice.dto.request.TranscribeAsyncAppSettingsRequest
import nl.attendi.attendispeechservice.data.service.transcribeasyncservice.dto.request.TranscribeAsyncClientConfigurationMessageRequest
import nl.attendi.attendispeechservice.data.service.transcribeasyncservice.dto.request.TranscribeAsyncVoiceEditingAppSettingsRequest
import java.util.UUID

/**
 * Utility factory for generating standardized WebSocket messages used by [AttendiWebSocketConnection].
 *
 * This object produces JSON payloads for:
 * - Initial configuration (`"type": "configuration"`)
 * - Graceful session termination (`"type": "endOfAudioStream"`)
 *
 * These messages conform to Attendi's real-time transcription API format.
 *
 * Consumers can use this if they are implementing a custom connection
 * or want full control over the config message content.
 */
internal object AttendiWebSocketConnectionFactory {

    /**
     * Builds a JSON configuration message to initialize a transcription session.
     *
     * This message is sent automatically by [AttendiWebSocketConnection] after a successful connection,
     * but SDK consumers may call it directly to customize the configuration or use it in their own implementations.
     *
     * @param type The message type (default: `"configuration"`).
     * @param model The transcription model to use (e.g., `"ResidentialCare"`).
     * @param reportId Optional report identifier. A random UUID is used by default.
     * @param sessionId Optional session identifier. A random UUID is used by default.
     * @param isVoiceEditingEnabled Whether voice editing features should be enabled.
     * @param shouldUseAttendiEntityRecognitionModel Whether to enable Attendi-specific entity recognition.
     *
     * @return A JSON string representing the configuration message.
     */
    fun makeConfigMessage(
        type: String = "configuration",
        model: String = "ResidentialCare",
        reportId: String = UUID.randomUUID().toString(),
        sessionId: String = UUID.randomUUID().toString(),
        isVoiceEditingEnabled: Boolean = false,
        shouldUseAttendiEntityRecognitionModel: Boolean = false
    ) : String {
        return Json.encodeToString(
            TranscribeAsyncClientConfigurationMessageRequest(
                type = type,
                model = model,
                reportId = reportId,
                sessionId = sessionId,
                features = TranscribeAsyncAppSettingsRequest(
                    voiceEditing = TranscribeAsyncVoiceEditingAppSettingsRequest(
                        isEnabled = isVoiceEditingEnabled,
                        useAttendiEntityRecognitionModel = shouldUseAttendiEntityRecognitionModel
                    )
                )
            )
        )
    }

    /**
     * Generates a simple message signaling the end of the audio stream.
     *
     * This is used to instruct the Attendi backend to gracefully terminate the session.
     * Called by [AttendiWebSocketConnection] during `disconnect()`.
     *
     * @return A JSON string representing the end-of-stream message.
     */
    fun makeCloseMessage() : String {
        return "{\"type\": \"endOfAudioStream\"}"
    }
}