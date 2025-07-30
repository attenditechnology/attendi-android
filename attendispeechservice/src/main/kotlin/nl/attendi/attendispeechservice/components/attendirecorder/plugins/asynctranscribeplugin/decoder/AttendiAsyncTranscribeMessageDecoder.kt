package nl.attendi.attendispeechservice.components.attendirecorder.plugins.asynctranscribeplugin.decoder

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import nl.attendi.attendispeechservice.services.asynctranscribe.model.TranscribeAsyncAction
import nl.attendi.attendispeechservice.components.attendirecorder.plugins.asynctranscribeplugin.mapper.TranscribeAsyncActionMapper
import nl.attendi.attendispeechservice.services.asynctranscribe.dto.response.TranscribeAsyncResponse

/**
 * Default implementation of [AsyncTranscribeMessageDecoder] provided by the Attendi SDK.
 *
 * This decoder is responsible for transforming raw JSON responses received from Attendi’s
 * WebSocket-based asynchronous transcription API into a list of high-level [TranscribeAsyncAction]s.
 *
 * It delegates the decoding logic to [AsyncTranscribeMessageDecoder], which first
 * deserializes the JSON payload into an internal DTO model and then maps it to domain-level
 * actions using [TranscribeAsyncActionMapper].
 *
 * This class is used internally by the Attendi SDK, but consumers may provide a custom
 * implementation of [AsyncTranscribeMessageDecoder] to support different protocols,
 * formats, or business rules.
 */
internal object AttendiAsyncTranscribeMessageDecoder : AsyncTranscribeMessageDecoder {

    // A lenient JSON parser that safely ignores unknown fields in the payload
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Decodes a raw JSON response into a list of [TranscribeAsyncAction]s.
     *
     * @param response A raw JSON string received from the WebSocket.
     * @return A list of parsed [TranscribeAsyncAction]s. May be empty if the response contains no actionable data.
     * @throws SerializationException If the JSON is malformed or does not match the expected schema.
     */
    override fun decode(response: String): List<TranscribeAsyncAction> {
        val attendiResponse = json.decodeFromString<TranscribeAsyncResponse>(response)
        return TranscribeAsyncActionMapper.map(attendiResponse)
    }
}
