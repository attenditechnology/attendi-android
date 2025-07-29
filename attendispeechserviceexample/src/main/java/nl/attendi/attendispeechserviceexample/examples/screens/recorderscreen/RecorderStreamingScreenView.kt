package nl.attendi.attendispeechserviceexample.examples.screens.recorderscreen

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.attendi.attendispeechservice.components.attendirecorder.plugins.AttendiAudioNotificationPlugin
import nl.attendi.attendispeechservice.components.attendirecorder.plugins.AttendiErrorPlugin
import nl.attendi.attendispeechservice.components.attendirecorder.plugins.asynctranscribeplugin.AttendiAsyncTranscribePlugin
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderFactory
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderState
import nl.attendi.attendispeechservice.services.asynctranscribe.AttendiAsyncTranscribeServiceFactory
import nl.attendi.attendispeechserviceexample.examples.plugins.ExampleErrorLoggerPlugin
import nl.attendi.attendispeechserviceexample.examples.services.ExampleAttendiTranscribeAPI
import nl.attendi.attendispeechserviceexample.ui.theme.AttendiSpeechServiceExampleTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecorderStreamingScreenView() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    val recorder = remember {
        AttendiRecorderFactory.create(
            plugins = listOf(
                AttendiAudioNotificationPlugin(context = context),
                AttendiErrorPlugin(context = context),
                AttendiAsyncTranscribePlugin(
                    service = AttendiAsyncTranscribeServiceFactory.create(
                        apiConfig = ExampleAttendiTranscribeAPI.transcribeAPIConfig
                    ),
                    onStreamStarted = {
                        text = ""
                    },
                    onStreamUpdated = { stream ->
                        text = stream.state.text
                    },
                    onStreamCompleted = { stream, _ ->
                        text = stream.state.text
                    }
                ),
                ExampleErrorLoggerPlugin()
            )
        )
    }
    val recorderState by recorder.recorderState.collectAsState()

    // The recorder is released manually as it is not attached to an AttendiMicrophone, which
    // handles the release internally.
    DisposableEffect(recorder) {
        onDispose {
            // Launch a coroutine to call the suspend function
            CoroutineScope(Dispatchers.IO).launch {
                recorder.release()
            }
        }
    }

    Column {
        CenterAlignedTopAppBar(
            title = {
                Text("Recorder")
            }
        )
        Column(
            modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        if (recorderState == AttendiRecorderState.NotStartedRecording) {
                            recorder.start()
                        } else if (recorderState == AttendiRecorderState.Recording) {
                            recorder.stop()
                        }
                    }
                }
            ) {
                when (recorderState) {
                    AttendiRecorderState.NotStartedRecording -> Text("Start Recording")
                    AttendiRecorderState.LoadingBeforeRecording -> Text("Loading")
                    AttendiRecorderState.Recording -> Text("Stop Recording")
                    AttendiRecorderState.Processing -> Text("Processing")
                }
            }

            Column(
                modifier = Modifier
                    .border(1.dp, Color(206, 206, 206), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                TextField(
                    value = text,
                    onValueChange = { text = it },
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
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AttendiSpeechServiceExampleTheme {
        RecorderStreamingScreenView()
    }
}