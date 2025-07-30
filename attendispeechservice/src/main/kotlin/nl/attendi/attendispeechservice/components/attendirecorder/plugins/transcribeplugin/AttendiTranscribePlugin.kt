package nl.attendi.attendispeechservice.components.attendirecorder.plugins.transcribeplugin

import nl.attendi.attendispeechservice.components.attendirecorder.plugins.transcribeplugin.encoder.AttendiTranscribeAudioEncoderFactory
import nl.attendi.attendispeechservice.components.attendirecorder.plugins.transcribeplugin.encoder.TranscribeAudioEncoder
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderModel
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderPlugin
import nl.attendi.attendispeechservice.services.transcribe.TranscribeService
import nl.attendi.attendispeechservice.utils.invokeAll

/**
 * A plugin that sends recorded audio to Attendi's backend transcription API for speech-to-text processing.
 *
 * This plugin registers and activates an audio task named `"attendi-transcribe"`, which handles the audio
 * streaming and transcription. Once the recording session is complete, the plugin listens for a response
 * from the backend and provides the transcription result (or an error, if any) to the client.
 *
 * Use this plugin when you want to automatically transcribe user speech using Attendi's transcription service.
 *
 * @param service A service implementation that communicates with a transcription API such as Attendi's transcription endpoint.
 * @param audioEncoder Responsible for encoding raw PCM audio into the format required by the API.
 * @param onStartRecording Callback invoked when the recording task is started.
 * @param onTranscribeStarted Callback invoked when the transcription task is started.
 * @param onTranscribeCompleted Callback invoked when the transcription completes.
 * Provides either the transcribed text or an error if the transcription failed.
 *
 */
class AttendiTranscribePlugin(
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
