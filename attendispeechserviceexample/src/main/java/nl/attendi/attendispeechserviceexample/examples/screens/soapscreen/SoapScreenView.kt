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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import nl.attendi.attendispeechservice.components.attendimicrophone.microphone.AttendiMicrophone
import nl.attendi.attendispeechservice.components.attendimicrophone.microphone.AttendiMicrophoneDefaults
import nl.attendi.attendispeechservice.components.attendimicrophone.microphone.AttendiMicrophoneSettings
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderFactory
import nl.attendi.attendispeechserviceexample.ui.ErrorAlertDialog
import nl.attendi.attendispeechserviceexample.ui.theme.AttendiSpeechServiceExampleTheme
import nl.attendi.attendispeechserviceexample.ui.theme.Colors
import nl.attendi.attendispeechserviceexample.ui.theme.Colors.pinkColor

@Composable
fun SoapScreenView(
    model: SoapScreenModel,
    modifier: Modifier = Modifier
) {
    var isMissingPermissionsAlertPresented by remember { mutableStateOf(false) }

    val focusRequester1 = remember { FocusRequester() }
    val focusRequester2 = remember { FocusRequester() }
    val focusRequester3 = remember { FocusRequester() }
    val focusRequester4 = remember { FocusRequester() }

    fun shouldDisplayMicrophoneTarget(tag: Int): Boolean {
        return model.canDisplayFocusedTextField && model.focusedTextFieldIndex == tag
    }

    Box(modifier = modifier) {
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
                    value = model.text1,
                    onValueChange = { model.onTextChange(it) },
                    minLines = 5,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .focusRequester(focusRequester1)
                        .onFocusChanged {
                            if (it.isFocused)
                                model.onFocusedTextFieldIndexChange(0)
                        }
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(0.dp)
                        .border(
                            width = 1.dp,
                            color = if (shouldDisplayMicrophoneTarget(0)) pinkColor else Colors.greyColor,
                            shape = RoundedCornerShape(8.dp)
                        )
                )
                if (shouldDisplayMicrophoneTarget(0)) Text("Aan het opnemen..", color = pinkColor)
            }

            Column {
                Text("O:")
                TextField(
                    value = model.text2,
                    onValueChange = { model.onTextChange(it) },
                    minLines = 5,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .focusRequester(focusRequester2)
                        .onFocusChanged {
                            if (it.isFocused)
                                model.onFocusedTextFieldIndexChange(1)
                        }
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(0.dp)
                        .border(
                            width = 1.dp,
                            color = if (shouldDisplayMicrophoneTarget(1)) pinkColor else Colors.greyColor,
                            shape = RoundedCornerShape(8.dp)
                        )
                )
            }

            Column {
                Text("A:")
                TextField(
                    value = model.text3,
                    onValueChange = { model.onTextChange(it) },
                    minLines = 5,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .focusRequester(focusRequester3)
                        .onFocusChanged {
                            if (it.isFocused)
                                model.onFocusedTextFieldIndexChange(2)
                        }
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(0.dp)
                        .border(
                            width = 1.dp,
                            color = if (shouldDisplayMicrophoneTarget(2)) pinkColor else Colors.greyColor,
                            shape = RoundedCornerShape(8.dp)
                        )
                )
            }

            Column {
                Text("P:")
                TextField(
                    value = model.text4,
                    onValueChange = { model.onTextChange(it) },
                    minLines = 5,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .focusRequester(focusRequester4)
                        .onFocusChanged {
                            if (it.isFocused)
                                model.onFocusedTextFieldIndexChange(3)
                        }
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(0.dp)
                        .border(
                            width = 1.dp,
                            color = if (shouldDisplayMicrophoneTarget(3)) pinkColor else Colors.greyColor,
                            shape = RoundedCornerShape(8.dp)
                        )
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            AttendiMicrophone(
                recorder = model.recorder,
                modifier = Modifier
                    .border(1.dp, pinkColor, RoundedCornerShape(percent = 50)),
                settings = AttendiMicrophoneSettings(
                    size = 64.dp,
                    colors = AttendiMicrophoneDefaults.colors(
                        inactiveBackgroundColor = pinkColor,
                        inactiveForegroundColor = Color.White,
                        activeBackgroundColor = pinkColor,
                        activeForegroundColor = Color.White,
                    ),
                    showsDefaultPermissionsDeniedDialog = false
                ),
                onRecordingPermissionDenied = {
                    isMissingPermissionsAlertPresented = true
                }
            )
        }

        if (isMissingPermissionsAlertPresented) {
            AlertDialog(
                onDismissRequest = {
                    isMissingPermissionsAlertPresented = false
                },
                title = {
                    Text(text = "Missing Permissions")
                },
                text = {
                    Text(text = "Recording Permissions have to be granted in order to use the microphone")
                },
                confirmButton = {
                    TextButton(onClick = {
                        isMissingPermissionsAlertPresented = false
                    }) {
                        Text("OK")
                    }
                }
            )
        }

        if (model.isErrorAlertShown) {
            ErrorAlertDialog(
                errorMessage = model.errorMessage,
                onDismissRequest = {
                    model.onAlertDialogDismiss()
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HoveringMicrophoneScreenPreview() {
    AttendiSpeechServiceExampleTheme {
        SoapScreenView(model = SoapScreenModel(recorder = AttendiRecorderFactory.create()))
    }
}