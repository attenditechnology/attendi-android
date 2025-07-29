package nl.attendi.attendispeechservice.components.attendimicrophone.plugins

import nl.attendi.attendispeechservice.components.attendimicrophone.microphone.AttendiMicrophoneModel
import nl.attendi.attendispeechservice.components.attendimicrophone.microphone.AttendiMicrophonePlugin
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderModel
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderState
import kotlin.math.pow

/**
 * To give extra feedback to the user that the microphone is recording, we display the volume
 * of the audio signal, which fills the inside of the microphone's cone in tandem with the
 * volume level.
 */
class AttendiVolumeFeedbackPlugin : AttendiMicrophonePlugin {
    private var volume: Double = 0.0

    override suspend fun activate(recorderModel: AttendiRecorderModel, microphoneModel: AttendiMicrophoneModel) {
        recorderModel.onAudio { audioFrame ->
            val rmsLevel = audioFrame.getVolume()
            val alpha = getMovingAverageAlpha(rmsLevel)
            volume = (1 - alpha) * volume + alpha * rmsLevel

            val normalizedVolume = normalizeVolume(volume)

            // We want to always scale the volume feedback by at least this factor of the maximum size
            // This means that the volume feedback will always be visible,
            // even when the volume is very low.
            val minimumVolumeFactor = 0.2
            val newMicrophoneFillLevel = minimumVolumeFactor + (1 - minimumVolumeFactor) * normalizedVolume

            val currentFillLevel = microphoneModel.uiState.value.animatedMicrophoneFillLevel

            if (currentFillLevel == newMicrophoneFillLevel) {
                return@onAudio
            }
            microphoneModel.updateAnimatedMicrophoneFillLevel(newMicrophoneFillLevel)
        }

        recorderModel.onStateUpdate { state ->
            if (state == AttendiRecorderState.NotStartedRecording) {
                volume = 0.0
                microphoneModel.updateAnimatedMicrophoneFillLevel(volume)
            }
        }
    }

    /**
     * We bias the volume to stay high if it was high recently.
     * When the volume is high, the alpha is reduced, so that the
     * volume stays high.
     * This makes the volume feedback a bit smoother as it doesn't
     * come down as quickly.
     */
    private fun getMovingAverageAlpha(currentVolume: Double): Double {
        val alpha = 0.7
        val volumeBiasThreshold = 0.7
        val volumeBiasAmount = 0.12

        val normalizedVolume = normalizeVolume(currentVolume)

        val highVolumeBias = if (normalizedVolume > volumeBiasThreshold) volumeBiasAmount else 0.0
        return alpha - highVolumeBias
    }

    /**
     * Hand tuned to give good values for the microphone volume, such that the value is 0 when
     * not talking, and 1 when talking at a normal volume.
     *
     * During testing, observed values were around 700 when no speaking occurred, with speaking volume up
     * to 3000-5000.
     */
    private fun normalizeVolume(audioLevel: Double) =
        ((audioLevel - 300) / 2400).coerceIn(0.0, 1.0).pow(0.25)
}
