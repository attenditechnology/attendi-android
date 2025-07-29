package nl.attendi.attendispeechserviceexample.examples.screens.streamingscreen

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import nl.attendi.attendispeechservice.components.attendimicrophone.microphone.AttendiMicrophone
import nl.attendi.attendispeechservice.services.asynctranscribe.model.TranscribeAsyncAction
import nl.attendi.attendispeechservice.services.asynctranscribe.model.TranscribeAsyncAnnotationType

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
    /**
     * `shortTextFieldValue` holds a `TextFieldValue` locally within the view rather than in the ViewModel.
     * This is intentional to avoid exposing Compose-specific UI classes (`TextFieldValue`, `TextRange`)
     * to the ViewModel layer, maintaining separation of concerns.
     *
     * Behavior:
     * - When the underlying `model.shortTextFieldModel.text` changes programmatically, we update
     *   `shortTextFieldValue` accordingly, ensuring the cursor is placed at the end of the new text.
     * - This ensures a smooth UX where the cursor always jumps to the end on programmatic updates.
     * - If the user manually changes the cursor position or text, it is respected and persisted
     *   through `onValueChange`, which updates both the local state and the ViewModel.
     */
    var shortTextFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = model.shortTextFieldModel.text,
                selection = TextRange(model.shortTextFieldModel.text.length)
            )
        )
    }

    // Sync local value with model if the text changed programmatically
    LaunchedEffect(model.shortTextFieldModel.text) {
        if (model.shortTextFieldModel.text != shortTextFieldValue.text) {
            shortTextFieldValue = TextFieldValue(
                text = model.shortTextFieldModel.text,
                selection = TextRange(model.shortTextFieldModel.text.length) // move cursor to end
            )
        }
    }

    var longTextFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = model.longTextFieldModel.text,
                selection = TextRange(model.longTextFieldModel.text.length)
            )
        )
    }

    // Sync local value with model if the text changed programmatically
    LaunchedEffect(model.longTextFieldModel.text) {
        if (model.longTextFieldModel.text != longTextFieldValue.text) {
            longTextFieldValue = TextFieldValue(
                text = model.longTextFieldModel.text,
                selection = TextRange(model.longTextFieldModel.text.length) // move cursor to end
            )
        }
    }

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
                value = shortTextFieldValue,
                onValueChange = { newValue ->
                    shortTextFieldValue = newValue
                    model.shortTextFieldModel.onTextChange(newValue.text)
                },
                visualTransformation = mapAnnotatedTextTransformation(
                    annotations = model.shortTextFieldModel.annotations,
                    startStreamCharacterOffset = model.shortTextFieldModel.startStreamCharacterOffset
                ),
                singleLine = true,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(0.dp)
                    .onFocusChanged {
                        model.shortTextFieldModel.onFocusChange?.invoke(it.isFocused)
                    },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.width(16.dp))

            AttendiMicrophone(
                recorder = model.shortTextFieldModel.recorder,
                plugins = model.shortTextFieldModel.plugins,
                onMicrophoneTapCallback = model.shortTextFieldModel.onMicrophoneTapCallback
            )
        }

        Column(
            modifier = Modifier
                .border(1.dp, Color(206, 206, 206), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            TextField(
                value = longTextFieldValue,
                onValueChange = { newValue ->
                    longTextFieldValue = newValue
                    model.longTextFieldModel.onTextChange(newValue.text)
                },
                visualTransformation = mapAnnotatedTextTransformation(
                    annotations = model.longTextFieldModel.annotations,
                    startStreamCharacterOffset = model.longTextFieldModel.startStreamCharacterOffset
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(0.dp)
                    .onFocusChanged { model.longTextFieldModel.onFocusChange?.invoke(it.isFocused) }
            )

            AttendiMicrophone(
                recorder = model.longTextFieldModel.recorder,
                plugins = model.longTextFieldModel.plugins,
                onMicrophoneTapCallback = model.longTextFieldModel.onMicrophoneTapCallback
            )
        }
    }
}

private fun mapAnnotatedTextTransformation(
    annotations: List<TranscribeAsyncAction.AddAnnotation>,
    startStreamCharacterOffset: Int
): VisualTransformation {
    return VisualTransformation { originalText ->
        val builder = AnnotatedString.Builder(originalText.text)
        annotations.forEach { annotation ->
            val color = when (annotation.parameters.type) {
                is TranscribeAsyncAnnotationType.TranscriptionTentative -> Color.Cyan
                is TranscribeAsyncAnnotationType.Intent -> Color.Blue
                is TranscribeAsyncAnnotationType.Entity -> Color.Green
            }

            val start =
                (annotation.parameters.startCharacterIndex + startStreamCharacterOffset).coerceIn(
                    0,
                    builder.length
                )
            val end =
                (annotation.parameters.endCharacterIndex + startStreamCharacterOffset).coerceIn(
                    start,
                    builder.length
                )

            if (start in 0 until end) {
                builder.addStyle(
                    style = SpanStyle(color = color),
                    start = start,
                    end = end
                )
            }
        }

        TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}