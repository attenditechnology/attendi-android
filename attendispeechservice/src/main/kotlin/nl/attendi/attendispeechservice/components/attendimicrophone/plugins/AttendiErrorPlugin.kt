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

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import nl.attendi.attendispeechservice.R
import nl.attendi.attendispeechservice.components.attendimicrophone.AttendiMicrophoneState

/**
 * Adds basic error handling functionality by using the microphone's
 * [AttendiMicrophoneState.onError] API.
 *
 * Currently does the following when an error occurs:
 * - show a dialog with an error message
 * - vibrate the device
 */
class AttendiErrorPlugin : AttendiMicrophonePlugin {
    override fun activate(state: AttendiMicrophoneState) {
        state.onError {
            state.showDialog {
                AlertDialog(onDismissRequest = {
                    state.isDialogOpen = false
                },
                    title = { Text(state.context.getString(R.string.error_title)) },
                    text = { Text(state.context.getString(R.string.error_body, it.message)) },
                    confirmButton = {
                        Button(onClick = { state.isDialogOpen = false }) {
                            Text(state.context.getString(R.string.ok))
                        }
                    })
            }

            state.vibrate()
        }
    }
}
