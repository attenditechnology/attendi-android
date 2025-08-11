package nl.attendi.attendispeechservice.components.attendirecorder.plugins.transcribeplugin

import nl.attendi.attendispeechservice.components.attendirecorder.plugins.transcribeplugin.encoder.AttendiTranscribeAudioEncoderFactory
import nl.attendi.attendispeechservice.components.attendirecorder.plugins.transcribeplugin.encoder.TranscribeAudioEncoder
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderModel
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderPlugin
import nl.attendi.attendispeechservice.services.transcribe.TranscribeService
import nl.attendi.attendispeechservice.utils.invokeAll

/**
 * A plugin that sends recorded audio to a backend transcription API for speech-to-text processing.
 *
 * [AttendiSyncTranscribePlugin] captures raw audio frames during a recording session, encodes them,
 * and sends the encoded audio to a transcription service once recording stops. It is designed for use
 * with synchronous (blocking) transcription services where the entire audio must be captured
 * before transcription can begin.
 *
 * Use this plugin when you want to automatically transcribe user speech after they finish speaking,
 * such as in short-form interactions (e.g. form inputs, commands, or voice notes).
 *
 * Hooks into the [AttendiRecorderModel] lifecycle:
 * - Clears audio buffer before recording starts.
 * - Collects audio frames while recording.
 * - Triggers transcription upon stopping.
 *
 * Notifies the caller using the provided callbacks:
 * - [onStartRecording] — called when recording begins.
 * - [onTranscribeStarted] — called when transcription starts (after recording ends).
 * - [onTranscribeCompleted] — called with the resulting transcript or an error upon completion.
 *
 * Note: This plugin stores audio frames in memory for the duration of a recording session.
 * It is best suited for short to moderate-length recordings.
 *
 * @param service A service implementation that communicates with a transcription backend.
 * @param audioEncoder Responsible for encoding raw PCM audio into the format required by the API.
 * @param onStartRecording Callback invoked when the recording task is started.
 * @param onTranscribeStarted Callback invoked when the transcription task is started.
 * @param onTranscribeCompleted Callback invoked when the transcription completes. Provides either
 * the transcribed text or an error if the transcription failed.
 */
class AttendiSyncTranscribePlugin(
    private val service: TranscribeService,
    private val audioEncoder: TranscribeAudioEncoder = AttendiTranscribeAudioEncoderFactory.create(),
    private val onStartRecording: () -> Unit = { },
    private val onTranscribeStarted: () -> Unit = { },
    private val onTranscribeCompleted: (String?, error: Exception?) -> Unit
) : AttendiRecorderPlugin {

    private var audioFrames = mutableListOf<Short>()

    override suspend fun activate(model: AttendiRecorderModel) {
        model.onBeforeStartRecording {
            audioFrames.clear()
        }

        model.onStartRecording {
            onStartRecording()
        }

        model.onAudio { audioFrame ->
            audioFrames.addAll(audioFrame.samples)
        }

        model.onStopRecording {
            onTranscribeStarted()
            try {
                val encodedAudio = audioEncoder.encode(audioFrames.toList())
                val transcript = service.transcribe(encodedAudio)
                onTranscribeCompleted(transcript, null)
            } catch (e: Exception) {
                onTranscribeCompleted(null, e)
                model.callbacks.onError.invokeAll(Exception(e.message))
            } finally {
                audioFrames.clear()
            }
        }
    }
}
