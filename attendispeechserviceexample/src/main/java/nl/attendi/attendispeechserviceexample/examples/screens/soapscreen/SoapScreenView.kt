package nl.attendi.attendispeechserviceexample.examples.screens.soapscreen

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import nl.attendi.attendispeechservice.components.attendimicrophone.microphone.AttendiMicrophone
import nl.attendi.attendispeechservice.components.attendimicrophone.microphone.AttendiMicrophoneDefaults
import nl.attendi.attendispeechservice.components.attendimicrophone.microphone.AttendiMicrophoneSettings
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.AttendiVolumeFeedbackPlugin
import nl.attendi.attendispeechservice.components.attendirecorder.plugins.transcribeplugin.AttendiTranscribePlugin
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorder
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderFactory
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderState
import nl.attendi.attendispeechservice.services.transcribe.AttendiTranscribeServiceFactory
import nl.attendi.attendispeechserviceexample.examples.plugins.ExampleErrorLoggerPlugin
import nl.attendi.attendispeechserviceexample.examples.services.ExampleAttendiTranscribeAPI
import nl.attendi.attendispeechserviceexample.ui.theme.AttendiSpeechServiceExampleTheme

@Composable
fun HoveringMicrophoneScreen() {
    var text1 by rememberSaveable { mutableStateOf("") }
    var text2 by rememberSaveable { mutableStateOf("") }
    var text3 by rememberSaveable { mutableStateOf("") }
    var text4 by rememberSaveable { mutableStateOf("") }

    val focusRequester1 = remember { FocusRequester() }
    val focusRequester2 = remember { FocusRequester() }
    val focusRequester3 = remember { FocusRequester() }
    val focusRequester4 = remember { FocusRequester() }

    val microphoneUIState by remember { mutableStateOf<AttendiRecorderState?>(null) }

    var focusedTextField by remember { mutableIntStateOf(0) }
    val targetTextField = if (focusedTextField == 0) 1 else focusedTextField

    fun shouldDisplayMicrophoneTarget(textField: Int) =
        ((microphoneUIState == AttendiRecorderState.Recording || microphoneUIState == AttendiRecorderState.Processing) && targetTextField == textField)

    fun displayMicrophoneTarget(textField: Int) =
        Modifier.conditional(shouldDisplayMicrophoneTarget(textField)) {
            border(1.dp, Color.Red)
        }

    Box {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .padding(16.dp)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            Text("SOAP rapportage")

            Column {
                Text("S:")
                TextField(
                    value = text1,
                    onValueChange = { text1 = it },
                    minLines = 5,
                    modifier = Modifier
                        .focusRequester(focusRequester1)
                        .onFocusChanged { if (it.isFocused) focusedTextField = 1 }
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(0.dp)
                        .then(displayMicrophoneTarget(1)))
                if (shouldDisplayMicrophoneTarget(1)) Text("Aan het opnemen..", color = Color.Red)
            }

            Column {
                Text("O:")
                TextField(
                    value = text2,
                    onValueChange = { text2 = it },
                    minLines = 5,
                    modifier = Modifier
                        .focusRequester(focusRequester2)
                        .onFocusChanged { if (it.isFocused) focusedTextField = 2 }
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(0.dp)
                        .then(displayMicrophoneTarget(2)),
                )
                if (shouldDisplayMicrophoneTarget(2)) Text("Aan het opnemen..", color = Color.Red)
            }

            Column {
                Text("A:")
                TextField(
                    value = text3,
                    onValueChange = { text3 = it },
                    minLines = 5,
                    modifier = Modifier
                        .focusRequester(focusRequester3)
                        .onFocusChanged { if (it.isFocused) focusedTextField = 3 }
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(0.dp)
                        .then(displayMicrophoneTarget(3)),
                )
                if (shouldDisplayMicrophoneTarget(3)) Text("Aan het opnemen..", color = Color.Red)
            }

            Column {
                Text("P:")
                TextField(
                    value = text4,
                    onValueChange = { text4 = it },
                    minLines = 5,
                    modifier = Modifier
                        .focusRequester(focusRequester4)
                        .onFocusChanged { if (it.isFocused) focusedTextField = 4 }
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(0.dp)
                        .then(displayMicrophoneTarget(4)),
                )
                if (shouldDisplayMicrophoneTarget(4)) Text("Aan het opnemen..", color = Color.Red)
            }
        }

        val pinkColor = Color(240, 43, 131)

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            val recorder: AttendiRecorder = AttendiRecorderFactory.create(
                plugins = listOf(
                    AttendiTranscribePlugin(
                        service = AttendiTranscribeServiceFactory.create(
                            apiConfig = ExampleAttendiTranscribeAPI.transcribeAPIConfig,
                        ),
                        onTranscribeCompleted = { transcribe, _ ->
                            when (focusedTextField) {
                                1 -> text1 = transcribe ?: ""
                                2 -> text2 = transcribe ?: ""
                                3 -> text3 = transcribe ?: ""
                                4 -> text4 = transcribe ?: ""
                                else -> {}
                            }
                        }
                    ),
                    ExampleErrorLoggerPlugin()
                )
            )

            AttendiMicrophone(
                modifier = Modifier
                    .border(1.dp, pinkColor, RoundedCornerShape(percent = 50)),
                settings = AttendiMicrophoneSettings(
                    size = 64.dp,
                    colors = AttendiMicrophoneDefaults.colors(
                        inactiveBackgroundColor = pinkColor,
                        inactiveForegroundColor = Color.White,
                        activeBackgroundColor = pinkColor,
                        activeForegroundColor = Color.White,
                    )
                ),
                recorder = recorder,
                plugins = listOf(
                    AttendiVolumeFeedbackPlugin()
                )
            )
        }
    }
}

/**
 * A modifier that applies [modifier] if [condition] is true.
 */
private fun Modifier.conditional(condition: Boolean, modifier: Modifier.() -> Modifier): Modifier {
    return if (condition) {
        then(modifier(Modifier))
    } else {
        this
    }
}

@Preview(showBackground = true)
@Composable
fun HoveringMicrophoneScreenPreview() {
    AttendiSpeechServiceExampleTheme {
        HoveringMicrophoneScreen()
    }
}