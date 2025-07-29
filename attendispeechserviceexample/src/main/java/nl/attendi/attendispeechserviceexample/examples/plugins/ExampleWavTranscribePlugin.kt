package nl.attendi.attendispeechserviceexample.examples.plugins

import android.content.Context
import android.util.Log
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderPlugin
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderModel
import nl.attendi.attendispeechservice.audio.AudioEncoder
import java.io.File
import java.io.FileOutputStream

/**
 * An example implementation of [AttendiRecorderPlugin] that collects audio frames during recording,
 * converts them to a WAV file upon stopping, and saves the file to external storage.
 *
 * This plugin listens to audio frames emitted by the recorder, accumulates them,
 * and on recording stop, encodes the collected PCM samples into a WAV format file
 * with a sample rate of 16 kHz. The output file is saved under the app's external files directory.
 *
 * @param context Android context used for file storage and logging.
 */
class ExampleWavTranscribePlugin(
    private val context: Context
) : AttendiRecorderPlugin {

    private var audioFrames = mutableListOf<Short>()

    override suspend fun activate(model: AttendiRecorderModel) {
        model.onAudio { audioFrame ->
            audioFrames.addAll(audioFrame.samples)
        }

        model.onStopRecording {
            try {
                val outputFile = File(context.getExternalFilesDir(null), "output.wav")
                val wav = AudioEncoder.pcmToWav(audioFrames, 16000)
                FileOutputStream(outputFile).use { fos ->
                    fos.write(wav)
                }
                Log.d("ExampleWavTranscribePlugin", "Saved to: ${outputFile.absolutePath}")
            } catch (e: Exception) {
                Log.d("ExampleWavTranscribePlugin", "Error: $e")
            }
            audioFrames.clear()
        }
    }
}
