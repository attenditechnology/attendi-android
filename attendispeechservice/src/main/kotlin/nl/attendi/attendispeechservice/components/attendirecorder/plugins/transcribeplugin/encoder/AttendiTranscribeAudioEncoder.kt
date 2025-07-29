package nl.attendi.attendispeechservice.components.attendirecorder.plugins.transcribeplugin.encoder

import nl.attendi.attendispeechservice.audio.AudioEncoder
import java.util.Base64

/**
 * Default implementation of [TranscribeAudioEncoder] provided by the Attendi SDK.
 *
 * This encoder transforms raw PCM audio data into a base64-encoded string,
 * which is the expected format for Attendi's transcription API.
 *
 * Consumers may use this implementation as-is, or replace it with a custom encoder
 * if a different format or preprocessing step is required.
 */
internal object AttendiTranscribeAudioEncoder: TranscribeAudioEncoder {

    override suspend fun encode(audioSamples: List<Short>): String {
        val byteArray = AudioEncoder.shortsToByteArray(audioSamples)
        val audioEncoded = Base64.getEncoder().encodeToString(byteArray)
        return audioEncoded
    }
}