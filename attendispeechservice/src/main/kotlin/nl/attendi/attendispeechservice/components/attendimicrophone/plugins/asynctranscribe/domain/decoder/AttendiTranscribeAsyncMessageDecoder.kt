package nl.attendi.attendispeechservice.components.attendimicrophone.plugins.asynctranscribe.domain.decoder

import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.asynctranscribe.domain.model.transcribeasync.TranscribeAsyncAction

/**
 * A contract for decoding raw text-based messages (typically JSON) into domain-level actions.
 *
 * This interface allows SDK consumers to customize how messages received from a real-time
 * connection (e.g. WebSocket) are interpreted and transformed into [TranscribeAsyncAction] objects.
 *
 * Use this if you want to plug in your own decoder logic — for example, if your backend service
 * emits a different message format than Attendi's.
 */
interface AttendiTranscribeAsyncMessageDecoder {

    /**
     * Decodes a raw message string into a list of [TranscribeAsyncAction] objects.
     *
     * @param response The raw string message, typically received from a connection listener.
     * @return A list of parsed [TranscribeAsyncAction]s. May be empty if the message doesn't map to any action.
     * @throws Exception if decoding fails. Implementations should clearly document failure modes.
     */
    fun decode(response: String): List<TranscribeAsyncAction>
}