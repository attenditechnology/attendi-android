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

package nl.attendi.attendispeechserviceexample

import AttendiMicrophoneDefaults
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import nl.attendi.attendispeechservice.client.ModelType
import nl.attendi.attendispeechservice.client.TranscribeAPIConfig
import nl.attendi.attendispeechservice.components.attendimicrophone.AttendiMicrophone
import nl.attendi.attendispeechservice.components.attendimicrophone.AttendiMicrophoneState
import nl.attendi.attendispeechservice.components.attendimicrophone.MicrophoneUIState
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.AttendiErrorPlugin
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.AttendiMicrophonePlugin
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.AttendiTranscribePlugin
import nl.attendi.attendispeechserviceexample.ui.theme.AttendiSpeechServiceExampleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AttendiSpeechServiceExampleTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    ExampleApp()
                }
            }
        }
    }
}

val exampleAPIConfig = TranscribeAPIConfig(
    modelType = ModelType.DistrictCare,
    userAgent = "Android",
    customerKey = "ck_key",
    apiURL = "https://sandbox.api.attendi.nl",
    unitId = "unitId",
    userId = "userId",
)

enum class Screen {
    TwoMicrophones, HoveringMicrophone,
}

@Composable
fun ExampleApp() {
    var screen by remember { mutableStateOf(Screen.HoveringMicrophone) }

    Column {
        Row(
            horizontalArrangement = Arrangement.End, modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Button(onClick = {
                screen =
                    if (screen == Screen.TwoMicrophones) Screen.HoveringMicrophone else Screen.TwoMicrophones
            }) {
                Text(if (screen == Screen.TwoMicrophones) "Vul SOAP in" else "Ga terug")
            }
        }

        if (screen == Screen.TwoMicrophones) {
            TwoMicrophonesScreen()
        } else {
            HoveringMicrophoneScreen()
        }
    }
}

/**
 * A modifier that applies [modifier] if [condition] is true.
 */
fun Modifier.conditional(condition: Boolean, modifier: Modifier.() -> Modifier): Modifier {
    return if (condition) {
        then(modifier(Modifier))
    } else {
        this
    }
}

@Composable
fun HoveringMicrophoneScreen() {
    var text1 by remember { mutableStateOf("") }
    var text2 by remember { mutableStateOf("") }
    var text3 by remember { mutableStateOf("") }
    var text4 by remember { mutableStateOf("") }

    val focusRequester1 = remember { FocusRequester() }
    val focusRequester2 = remember { FocusRequester() }
    val focusRequester3 = remember { FocusRequester() }
    val focusRequester4 = remember { FocusRequester() }

    // var microphoneState by remember { mutableStateOf<AttendiMicrophoneState?>(null) }
    var microphoneUIState by remember { mutableStateOf<MicrophoneUIState?>(null) }

    var focusedTextField by remember { mutableStateOf(0) }
    val targetTextField = if (focusedTextField == 0) 1 else focusedTextField

    fun shouldDisplayMicrophoneTarget(textField: Int) =
        ((microphoneUIState == MicrophoneUIState.Recording || microphoneUIState == MicrophoneUIState.Processing)
                && targetTextField == textField)

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
                        .then(displayMicrophoneTarget(1))
                )
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
            AttendiMicrophone(
                modifier = Modifier
                    .border(1.dp, pinkColor, RoundedCornerShape(percent = 50))
                    .background(Color.White),
                size = 64.dp,
                colors = AttendiMicrophoneDefaults.colors(
                    inactiveBackgroundColor = pinkColor,
                    inactiveForegroundColor = Color.White,
                    activeBackgroundColor = pinkColor,
                    activeForegroundColor = Color.White,
                ),
                plugins = listOf(
                    AttendiErrorPlugin(),
                    AttendiTranscribePlugin(apiConfig = exampleAPIConfig),
                    object : AttendiMicrophonePlugin {
                        override fun activate(state: AttendiMicrophoneState) {
                            state.onUIState {
                                microphoneUIState = it
                            }
                        }
                    }
                ),
                onResult = {
                    when (focusedTextField) {
                        1 -> text1 = addParagraph(text1, it)
                        2 -> text2 = addParagraph(text2, it)
                        3 -> text3 = addParagraph(text3, it)
                        4 -> text4 = addParagraph(text4, it)
                        else -> text1 = addParagraph(text1, it)
                    }
                },
            )
        }
    }
}

fun addParagraph(currentText: String, paragraph: String): String {
    val newLines = if (currentText.isNotEmpty()) "\n\n" else "";
    return currentText + newLines + paragraph;
}

@Preview(showBackground = true)
@Composable
fun HoveringMicrophoneScreenPreview() {
    AttendiSpeechServiceExampleTheme {
        HoveringMicrophoneScreen()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwoMicrophonesScreen() {
    var text by remember { mutableStateOf("") }
    var largeText by remember { mutableStateOf("") }

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
                value = text,
                onValueChange = { text = it },
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

            AttendiMicrophone(
                plugins = listOf(
                    AttendiErrorPlugin(),
                    AttendiTranscribePlugin(apiConfig = exampleAPIConfig),
                ),
                // Example of using `onEvent` to listen to arbitrary events
                onEvent = { name, data ->
                    when (name) {
                        // This name is specified by the plugin
                        "attendi-transcribe" -> {
                            (data as? String)?.let {
                                println("showcasing arbitrary events: $it")
                            }
                        }
                    }
                },
            ) {
                text = it
            }
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
            AttendiMicrophone(
                plugins = listOf(
                    AttendiErrorPlugin(),
                    AttendiTranscribePlugin(apiConfig = exampleAPIConfig),
                ),
            ) { largeText = it }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AttendiSpeechServiceExampleTheme {
        TwoMicrophonesScreen()
    }
}
