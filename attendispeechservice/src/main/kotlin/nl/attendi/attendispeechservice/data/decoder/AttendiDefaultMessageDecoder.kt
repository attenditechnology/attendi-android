package nl.attendi.attendispeechservice.data.decoder

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import nl.attendi.attendispeechservice.domain.decoder.AttendiMessageDecoder
import nl.attendi.attendispeechservice.data.service.transcribeasyncservice.dto.response.TranscribeAsyncResponse
import nl.attendi.attendispeechservice.domain.model.transcribeasync.TranscribeAsyncAction
import nl.attendi.attendispeechservice.mapper.TranscribeAsyncActionMapper

/**
 * Default implementation of [AttendiMessageDecoder] provided by the Attendi SDK.
 *
 * This decoder transforms raw JSON responses from the Attendi transcription WebSocket into
 * a list of [TranscribeAsyncAction] domain objects using the [TranscribeAsyncActionMapper].
 *
 * It uses a lenient JSON parser that ignores unknown fields, allowing the SDK to remain
 * forward-compatible with new fields added by the server.
 *
 * Consumers can override this implementation by providing their own [AttendiMessageDecoder]
 * if they use a different message format or want more control over the decoding process.
 */
object AttendiDefaultMessageDecoder : AttendiMessageDecoder {

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
        val jsonResponse = json.decodeFromString<TranscribeAsyncResponse>(response)
        return TranscribeAsyncActionMapper.map(jsonResponse)
    }
}
