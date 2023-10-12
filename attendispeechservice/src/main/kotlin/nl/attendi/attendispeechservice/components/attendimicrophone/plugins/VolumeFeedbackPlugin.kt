/// Copyright 2023 Attendi Technology B.V.
/// 
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
/// 
///     http://www.apache.org/licenses/LICENSE-2.0
/// 
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.

package nl.attendi.attendispeechservice.components.attendimicrophone.plugins

import nl.attendi.attendispeechservice.components.attendimicrophone.AttendiMicrophoneState
import nl.attendi.attendispeechservice.components.attendimicrophone.MicrophoneUIState
import kotlin.math.pow

/**
 * To give extra feedback to the user that the microphone is recording, we display the volume
 * of the audio signal, which fills the inside of the microphone's cone in tandem with the
 * volume level.
 */
class VolumeFeedbackPlugin : AttendiMicrophonePlugin {
    private var volume: Double = 0.0

    override fun activate(state: AttendiMicrophoneState) {
        state.onSignalEnergy {
            val alpha = getMovingAverageAlpha(it)
            volume = (1 - alpha) * volume + alpha * it

            val normalizedVolume = normalizeVolume(volume)

            // We want to always scale the volume feedback by at least this factor of the maximum size
            // This means that the volume feedback will always be visible,
            // even when the volume is very low.
            val minimumVolumeFactor = 0.2
            val newMicrophoneFillLevel =
                minimumVolumeFactor + (1 - minimumVolumeFactor) * normalizedVolume;

            val currentFillLevel = state.animatedMicrophoneFillLevel

            if (currentFillLevel == newMicrophoneFillLevel) {
                return@onSignalEnergy
            }

            state.animatedMicrophoneFillLevel = newMicrophoneFillLevel;
        }

        state.onUIState { uiState ->
            if (uiState == MicrophoneUIState.NotStartedRecording) {
                volume = 0.0
            }
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
fun getMovingAverageAlpha(currentVolume: Double): Double {
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
fun normalizeVolume(audioLevel: Double) = ((audioLevel - 800) / 2400).coerceIn(0.0, 1.0).pow(0.25)
