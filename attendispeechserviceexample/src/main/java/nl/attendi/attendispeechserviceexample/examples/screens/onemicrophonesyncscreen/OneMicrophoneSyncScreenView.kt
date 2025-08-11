package nl.attendi.attendispeechserviceexample.examples.screens.onemicrophonesyncscreen

import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import nl.attendi.attendispeechservice.components.attendimicrophone.microphone.AttendiMicrophone
import nl.attendi.attendispeechservice.components.attendimicrophone.microphone.AttendiMicrophoneDefaults
import nl.attendi.attendispeechservice.components.attendimicrophone.microphone.AttendiMicrophoneSettings
import nl.attendi.attendispeechservice.components.attendirecorder.plugins.AttendiAudioNotificationPlugin
import nl.attendi.attendispeechservice.components.attendirecorder.plugins.AttendiErrorPlugin
import nl.attendi.attendispeechservice.components.attendirecorder.plugins.AttendiStopOnAudioFocusLossPlugin
import nl.attendi.attendispeechservice.components.attendirecorder.plugins.transcribeplugin.AttendiSyncTranscribePlugin
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderFactory
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderPlugin
import nl.attendi.attendispeechservice.services.transcribe.AttendiTranscribeServiceFactory
import nl.attendi.attendispeechserviceexample.examples.plugins.ExampleErrorLoggerPlugin
import nl.attendi.attendispeechserviceexample.examples.plugins.ExampleWavTranscribePlugin
import nl.attendi.attendispeechserviceexample.examples.services.ExampleAttendiTranscribeAPI
import nl.attendi.attendispeechserviceexample.ui.ErrorAlertDialog
import nl.attendi.attendispeechserviceexample.ui.theme.AttendiSpeechServiceExampleTheme
import nl.attendi.attendispeechserviceexample.ui.theme.Colors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneMicrophoneSyncScreenView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isErrorAlertShown by remember { mutableStateOf(false) }

    val recorder = remember { AttendiRecorderFactory.create() }

    // Run once on first composition.
    LaunchedEffect(Unit) {
        recorder.setPlugins(
            createRecorderPlugins(
                context = context,
                onTranscriptCompleted = { transcript, error ->
                    if (error != null) {
                        errorMessage = error.localizedMessage ?: "Unknown error"
                        isErrorAlertShown = true
                    } else {
                        text = transcript ?: ""
                    }
                }
            )
        )
    }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .border(
                    width = 1.dp,
                    color = Colors.greyColor,
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            TextField(
                value = text,
                onValueChange = {
                    text = it
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                AttendiMicrophone(
                    recorder = recorder,
                    settings = AttendiMicrophoneSettings(
                        size = 56.dp,
                        colors = AttendiMicrophoneDefaults.colors(baseColor = Colors.pinkColor),
                        isVolumeFeedbackEnabled = false
                    ),
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        if (isErrorAlertShown) {
            ErrorAlertDialog(
                errorMessage = errorMessage,
                onDismissRequest = {
                    isErrorAlertShown = false
                    errorMessage = null
                }
            )
        }
    }
}

private fun createRecorderPlugins(
    context: Context,
    onTranscriptCompleted: (transcript: String?, error: Throwable?) -> Unit
): List<AttendiRecorderPlugin> = listOf(
    ExampleWavTranscribePlugin(context = context),
    ExampleErrorLoggerPlugin(),
    AttendiErrorPlugin(context = context),
    AttendiAudioNotificationPlugin(context = context),
    AttendiStopOnAudioFocusLossPlugin(context = context),
    AttendiSyncTranscribePlugin(
        service = AttendiTranscribeServiceFactory.create(
            apiConfig = ExampleAttendiTranscribeAPI.transcribeAPIConfig
        ),
        onTranscribeCompleted = onTranscriptCompleted
    )
)

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AttendiSpeechServiceExampleTheme {
        OneMicrophoneSyncScreenView()
    }
}