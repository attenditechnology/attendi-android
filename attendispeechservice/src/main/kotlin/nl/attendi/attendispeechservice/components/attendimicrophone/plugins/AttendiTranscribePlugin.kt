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

import nl.attendi.attendispeechservice.client.AttendiClient
import nl.attendi.attendispeechservice.client.TranscribeAPIConfig
import nl.attendi.attendispeechservice.components.attendimicrophone.AttendiMicrophoneState
import java.util.Base64

/**
 * Send recorded audio to Attendi's backend transcription API. Signal the result to the client
 * when a response is received.
 *
 * Registers an audio task called `attendi-transcribe` and sets it as the active audio task.
 */
class AttendiTranscribePlugin(private val apiConfig: TranscribeAPIConfig) :
    AttendiMicrophonePlugin {
    private val client = AttendiClient(apiConfig)

    override fun activate(state: AttendiMicrophoneState) {
        state.registerAudioTask("attendi-transcribe") { wav ->
            val audioEncoded = Base64.getEncoder().encodeToString(wav)

            val transcript = client.transcribe(audioEncoded, apiConfigOverride = apiConfig)

            transcript?.let {
                state.onEvent("attendi-transcribe", it)
                state.onResult(it)
            }
        }

        state.setActiveAudioTask("attendi-transcribe")
    }
}
