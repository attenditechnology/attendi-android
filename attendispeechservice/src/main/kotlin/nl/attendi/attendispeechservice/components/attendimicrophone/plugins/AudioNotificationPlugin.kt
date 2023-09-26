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

import android.media.MediaPlayer
import nl.attendi.attendispeechservice.R
import nl.attendi.attendispeechservice.components.attendimicrophone.AttendiMicrophoneState
import nl.attendi.attendispeechservice.components.attendimicrophone.MicrophoneUIState

/**
 * Play sounds at certain points of the microphone component's lifecycle to give more feedback
 * to the user. Specifically, play a start sound when the UI state is set to
 * [MicrophoneUIState.Recording] and play a stop sound just before recording is stopped.
 */
class AudioNotificationPlugin : AttendiMicrophonePlugin {
    private var startNotificationSound: MediaPlayer? = null
    private var stopNotificationSound: MediaPlayer? = null
    private var errorNotificationSound: MediaPlayer? = null

    override fun activate(state: AttendiMicrophoneState) {
        if (state.silent) return

        state.onFirstClick {
            startNotificationSound = MediaPlayer.create(state.context, R.raw.start_notification)
            stopNotificationSound = MediaPlayer.create(state.context, R.raw.stop_notification)
            errorNotificationSound = MediaPlayer.create(state.context, R.raw.error_notification)
        }

        state.onUIState { uiState ->
            if (uiState == MicrophoneUIState.Recording) {
                startNotificationSound?.start()
            }
        }

        state.onBeforeStopRecording {
            stopNotificationSound?.start()
        }

        // TODO: move this to the error plugin
        //  To do that, we first need to create the `addCommand` and `executeCommand` plugin APIs.
        state.onError { _ ->
            errorNotificationSound?.start()
        }
    }

    override fun deactivate(state: AttendiMicrophoneState) {
        startNotificationSound?.release()
        stopNotificationSound?.release()
        errorNotificationSound?.release()
    }
}
