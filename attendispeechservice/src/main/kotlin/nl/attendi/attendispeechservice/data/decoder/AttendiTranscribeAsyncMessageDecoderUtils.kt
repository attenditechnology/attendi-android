package nl.attendi.attendispeechservice.data.decoder

import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import nl.attendi.attendispeechservice.data.service.transcribeasyncservice.dto.response.TranscribeAsyncResponse
import nl.attendi.attendispeechservice.domain.decoder.AttendiTranscribeAsyncMessageDecoder
import nl.attendi.attendispeechservice.domain.model.transcribeasync.TranscribeAsyncAction
import nl.attendi.attendispeechservice.mapper.TranscribeAsyncActionMapper

/**
 * A utility object providing reusable functions for decoding and mapping
 * TranscribeAsync responses in the Attendi WebSocket message flow.
 *
 * This allows both internal and external implementations of [AttendiTranscribeAsyncMessageDecoder]
 * to easily decode JSON responses into [TranscribeAsyncResponse] and map them
 * into a list of [TranscribeAsyncAction]s.
 *
 * The internal JSON parser is configured to ignore unknown fields, which provides
 * safe parsing for forward-compatible or extended message formats.
 */
object AttendiTranscribeAsyncMessageDecoderUtils {

    // A lenient JSON parser that safely ignores unknown fields in the payload
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Decodes a JSON string into a [TranscribeAsyncResponse] object.
     *
     * @param response A JSON string containing an `actions` array.
     * @return A parsed [TranscribeAsyncResponse] object.
     * @throws SerializationException If the input does not conform to the expected schema.
     */
    fun decodeAttendiResponse(response: String): TranscribeAsyncResponse {
        return json.decodeFromString<TranscribeAsyncResponse>(response)
    }

    /**
     * Maps a [TranscribeAsyncResponse] into a list of [TranscribeAsyncAction]s.
     *
     * @param response A decoded [TranscribeAsyncResponse].
     * @return A list of domain-specific [TranscribeAsyncAction]s derived from the response.
     */
    fun mapToActions(response: TranscribeAsyncResponse): List<TranscribeAsyncAction> {
        return TranscribeAsyncActionMapper.map(response)
    }
}