package nl.attendi.attendispeechserviceexample.examples.screens.nonstreamingscreen

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import nl.attendi.attendispeechservice.components.attendimicrophone.microphone.AttendiMicrophone
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.AttendiVolumeFeedbackPlugin
import nl.attendi.attendispeechservice.components.attendirecorder.plugins.AttendiErrorPlugin
import nl.attendi.attendispeechservice.components.attendirecorder.plugins.transcribeplugin.AttendiTranscribePlugin
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderFactory
import nl.attendi.attendispeechservice.services.transcribe.AttendiTranscribeServiceFactory
import nl.attendi.attendispeechserviceexample.examples.plugins.ExampleErrorLoggerPlugin
import nl.attendi.attendispeechserviceexample.examples.plugins.ExampleWavTranscribePlugin
import nl.attendi.attendispeechserviceexample.examples.services.ExampleAttendiTranscribeAPI
import nl.attendi.attendispeechserviceexample.ui.theme.AttendiSpeechServiceExampleTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwoMicrophonesNonStreamingScreenView() {
    val context = LocalContext.current
    var shortText by remember { mutableStateOf("") }
    var largeText by remember { mutableStateOf("") }

    Column {
        CenterAlignedTopAppBar(
            title = {
                Text("Non-Streaming")
            }
        )
        Column(
            modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .border(1.dp, Color(206, 206, 206), RoundedCornerShape(8.dp))
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextField(
                    value = shortText,
                    onValueChange = { shortText = it },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.width(16.dp))

                val recorder = AttendiRecorderFactory.create(
                    plugins = listOf(
                        AttendiErrorPlugin(context = context),
                        ExampleWavTranscribePlugin(context = context),
                        AttendiTranscribePlugin(
                            service = AttendiTranscribeServiceFactory.create(
                                ExampleAttendiTranscribeAPI.transcribeAPIConfig
                            ),
                            // Ignoring the returned error for brevity
                            onTranscribeCompleted = { transcript, _ ->
                                shortText = transcript ?: ""
                            }
                        ),
                        ExampleErrorLoggerPlugin()
                    )
                )

                AttendiMicrophone(
                    recorder = recorder,
                    plugins = listOf(
                        AttendiVolumeFeedbackPlugin()
                    )
                )
            }

            Column(
                modifier = Modifier
                    .border(1.dp, Color(206, 206, 206), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                TextField(
                    value = largeText,
                    onValueChange = { largeText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(0.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                val recorder = AttendiRecorderFactory.create(
                    plugins = listOf(
                        AttendiErrorPlugin(context = context),
                        ExampleWavTranscribePlugin(context = context),
                        AttendiTranscribePlugin(
                            service = AttendiTranscribeServiceFactory.create(
                                ExampleAttendiTranscribeAPI.transcribeAPIConfig
                            ),
                            // Ignoring the returned error for brevity
                            onTranscribeCompleted = { transcript, _ ->
                                largeText = transcript ?: ""
                            }
                        ),
                        ExampleErrorLoggerPlugin()
                    )
                )

                AttendiMicrophone(
                    recorder = recorder,
                    plugins = listOf(
                        AttendiVolumeFeedbackPlugin()
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AttendiSpeechServiceExampleTheme {
        TwoMicrophonesNonStreamingScreenView()
    }
}