package nl.attendi.attendispeechservice.components.attendimicrophone.plugins.asynctranscribe.data.decoder

import kotlinx.serialization.SerializationException
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.asynctranscribe.domain.decoder.AttendiTranscribeAsyncMessageDecoder
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.asynctranscribe.domain.model.transcribeasync.TranscribeAsyncAction
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.asynctranscribe.mapper.TranscribeAsyncActionMapper

/**
 * Default implementation of [AttendiTranscribeAsyncMessageDecoder] provided by the Attendi SDK.
 *
 * This decoder transforms raw JSON responses from the Attendi transcription WebSocket into
 * a list of [TranscribeAsyncAction] domain objects using the [TranscribeAsyncActionMapper].
 *
 * It uses a lenient JSON parser that ignores unknown fields, allowing the SDK to remain
 * forward-compatible with new fields added by the server.
 *
 * Consumers can override this implementation by providing their own [AttendiTranscribeAsyncMessageDecoder]
 * if they use a different message format or want more control over the decoding process.
 */
object AttendiTranscribeAsyncDefaultMessageDecoder : AttendiTranscribeAsyncMessageDecoder {

    /**
     * Decodes a raw JSON response into a list of [TranscribeAsyncAction]s.
     *
     * @param response A raw JSON string received from the WebSocket.
     * @return A list of parsed [TranscribeAsyncAction]s. May be empty if the response contains no actionable data.
     * @throws SerializationException If the JSON is malformed or does not match the expected schema.
     */
    override fun decode(response: String): List<TranscribeAsyncAction> {
        val attendiResponse =
            AttendiTranscribeAsyncMessageDecoderUtils.decodeAttendiResponse(response)
        return AttendiTranscribeAsyncMessageDecoderUtils.mapToActions(attendiResponse)
    }
}
