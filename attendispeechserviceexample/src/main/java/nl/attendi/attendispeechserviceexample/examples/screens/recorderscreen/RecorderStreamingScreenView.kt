package nl.attendi.attendispeechserviceexample.examples.screens.recorderscreen

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import nl.attendi.attendispeechserviceexample.ui.ErrorAlertDialog
import nl.attendi.attendispeechserviceexample.ui.theme.AttendiSpeechServiceExampleTheme
import nl.attendi.attendispeechserviceexample.ui.theme.Colors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecorderStreamingScreenView(
    model: RecorderStreamingScreenModel,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                modifier = Modifier.padding(16.dp),
                onClick = {
                    model.onStartRecordingTap()
                }
            ) {
                Text(model.buttonTitle)
            }

            Column(
                modifier = Modifier
                    .border(1.dp, Colors.greyColor, RoundedCornerShape(8.dp))
            ) {
                TextField(
                    value = model.textFieldText,
                    onValueChange = {
                        model.onTextFieldTextChange(it)
                    },
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
fun DefaultPreview() {
    AttendiSpeechServiceExampleTheme {
        RecorderStreamingScreenView(model = RecorderStreamingScreenModel())
    }
}