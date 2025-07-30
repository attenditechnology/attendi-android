package nl.attendi.attendispeechservice.components.attendirecorder.plugins.asynctranscribeplugin.decoder

import nl.attendi.attendispeechservice.services.asynctranscribe.model.TranscribeAsyncAction

/**
 * A contract for decoding raw text-based messages (typically JSON) into domain-level actions.
 *
 * This interface allows SDK consumers to customize how messages received from a real-time
 * connection (e.g. WebSocket) are interpreted and transformed into [TranscribeAsyncAction] objects.
 *
 * Use this method when you need to plug in your own decoder logic to handle custom JSON response structures.
 *
 * This is useful when the backend response does not match the expected default shape and you want to
 * deserialize it into your own data classes using kotlinx.serialization.
 *
 * ### Example:
 * Suppose your server returns a JSON payload like:
 *
 * ```json
 * {
 *   "payload": {
 *     "actions": [...]
 *   }
 * }
 * ```
 *
 * You can decode this structure by defining your own serializable data classes that match the nested JSON:
 *
 * ```kotlin
 * @Serializable
 * data class CustomPayloadResponse(
 *   val payload: CustomPayloadWrappedResponse
 * )
 *
 * @Serializable
 * data class CustomPayloadWrappedResponse(
 *   val actions: List<TranscribeAsyncAnnotationResponse>
 * )
 * ```
 *
 * Then, you can use your own deserialization logic (e.g., via `Json.decodeFromString<CustomPayloadResponse>(...)`)
 * to extract the data as needed.
 *
 * This gives you full control over how the response is interpreted and mapped to your models.
 */
interface AsyncTranscribeMessageDecoder {

    /**
     * Decodes a raw message string into a list of [TranscribeAsyncAction] objects.
     *
     * @param response The raw string message, typically received from a connection listener.
     * @return A list of parsed [TranscribeAsyncAction]s. May be empty if the message doesn't map to any action.
     * @throws Exception if decoding fails. Implementations should clearly document failure modes.
     */
    fun decode(response: String): List<TranscribeAsyncAction>
}