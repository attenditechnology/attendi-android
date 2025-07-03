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

package nl.attendi.attendispeechserviceexample.examples.streaming.twomicrophonesscreen

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import nl.attendi.attendispeechservice.components.attendimicrophone.AttendiMicrophone
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.AttendiErrorPlugin
import nl.attendi.attendispeechserviceexample.exampleAPIConfig
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.AttendiAsyncTranscribePlugin
import nl.attendi.attendispeechserviceexample.examples.connection.custom.CustomConnection
import nl.attendi.attendispeechserviceexample.examples.connection.custom.CustomMessageDecoder
import nl.attendi.attendispeechservice.data.connection.websocket.AttendiWebSocketConnection
import nl.attendi.attendispeechserviceexample.examples.plugins.StopTranscriptionOnPausePlugin

/**
 * This screen and the async transcribe plugin implementation below serves as an example how the streaming
 * API can be configured for your use case by defining what happens
 * when a websocket message is received, when the socket is closing, and when the socket fails.
 *
 */
@Composable
fun TwoMicrophonesScreenStreamingView(
    model: TwoMicrophonesStreamingScreenModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier
                .border(1.dp, Color(206, 206, 206), RoundedCornerShape(8.dp))
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = model.shortTextFieldModel.text,
                onValueChange = { model.shortTextFieldModel.onTextChange(it) },
                singleLine = true,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(0.dp)
                    .onFocusChanged { model.shortTextFieldModel.onFocusChange?.invoke(it.isFocused) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.width(16.dp))

            AttendiMicrophone(
                plugins = listOf(
                    AttendiErrorPlugin(),
                    StopTranscriptionOnPausePlugin,
                    AttendiAsyncTranscribePlugin(
                        connection = CustomConnection,
                        messageDecoder = CustomMessageDecoder,
                        onStreamUpdated = { stream ->
                            model.shortTextFieldModel.onStreamTextChange(stream.state.text)
                        },
                        onStreamCompleted = { stream, _ ->
                            model.shortTextFieldModel.onStreamTextFinished(stream.state.text)
                        }
                    )
                )
            )
        }

        Column(
            modifier = Modifier
                .border(1.dp, Color(206, 206, 206), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            TextField(
                value = model.longTextFieldModel.text,
                onValueChange = { model.longTextFieldModel.onTextChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(0.dp)
                    .onFocusChanged { model.longTextFieldModel.onFocusChange?.invoke(it.isFocused) }
                    .testTag("TwoMicrophonesScreenStreamingLargeTextField"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
            )
            AttendiMicrophone(
                plugins = listOf(
                    AttendiErrorPlugin(),
                    StopTranscriptionOnPausePlugin,
                    AttendiAsyncTranscribePlugin(
                        connection = AttendiWebSocketConnection(
                            apiConfig = exampleAPIConfig
                        ),
                        onStreamUpdated = { stream ->
                            model.longTextFieldModel.onStreamTextChange(stream.state.text)
                        },
                        onStreamCompleted = { stream, _ ->
                            model.longTextFieldModel.onStreamTextFinished(stream.state.text)
                        }
                    )
                ),
                modifier = Modifier.testTag("TwoMicrophonesScreenStreamingLargeTextMicrophone")
            )
        }
    }
}