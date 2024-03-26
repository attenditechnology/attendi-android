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

package nl.attendi.attendispeechserviceexample.examples.streaming

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import nl.attendi.attendispeechservice.components.attendimicrophone.AttendiMicrophone
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.AttendiErrorPlugin
import nl.attendi.attendispeechserviceexample.exampleAPIConfig
import nl.attendi.attendispeechserviceexample.examples.plugins.StopTranscriptionOnPausePlugin

/**
 * We explicitly use a ViewModel here.
 *
 * If we use `rememberSaveable` for saving the large text, we run into some issues in the
 * current setup on screen rotations. Namely, in the `AttendiAsyncTranscribePlugin` we define
 * a closure `onIncomingMessage` that updates the text field. When the screen rotates, the current
 * intended behavior is to stop the recording and save anything that was transcribed so far. To do
 * so, we stop recording and send an `EndOfAudioStream` message to the server. The server then sends
 * a `ProcessedStream` message back, which contains the full transcript of the stream. We then use
 * this value to set te `largeText` value. If we would use `rememberSaveable`, the state of `largeText`
 * is saved to a `Bundle` and restored on screen rotations. However, `largeText` would now refer to a
 * new variable, located at a different memory address. This would render any updates to `largeText`
 * useless.
 *
 * By using a `ViewModel`, we hoist the state up so it's not destroyed on a screen rotation, avoiding
 * this issue.
 */
class TwoMicrophonesScreenStreamingViewModel : ViewModel() {
    private val _largeText = MutableStateFlow(TextFieldValue())
    val largeText: StateFlow<TextFieldValue> = _largeText.asStateFlow()

    private val _largeTextReceivedMessages = MutableStateFlow(emptyList<IncomingMessage>())
    val largeTextReceivedMessages: StateFlow<List<IncomingMessage>> =
        _largeTextReceivedMessages.asStateFlow()

    private val _shortText = MutableStateFlow("")
    val shortText: StateFlow<String> = _shortText.asStateFlow()

    private val _shortTextReceivedMessages = MutableStateFlow(emptyList<IncomingMessage>())
    val shortTextReceivedMessages: StateFlow<List<IncomingMessage>> =
        _shortTextReceivedMessages.asStateFlow()

    fun updateLargeText(value: TextFieldValue) {
        _largeText.value = value
    }

    fun updateLargeTextReceivedMessages(value: List<IncomingMessage>) {
        _largeTextReceivedMessages.value = value
    }

    fun updateshortText(value: String) {
        _shortText.value = value
    }

    fun updateshortTextReceivedMessages(value: List<IncomingMessage>) {
        _shortTextReceivedMessages.value = value
    }
}

/**
 * This screen and the async transcribe plugin implementation below serves as an example how the streaming
 * API can be configured for your use case by defining what happens
 * when a websocket message is received, when the socket is closing, and when the socket fails.
 *
 * Currently we do the following to allow for a nice user experience:
 * - We have a text field, which might already have some text in it. This text is
 * saved in a variable `textValue: TextFieldValue`. We use a `TextFieldValue` instead
 * of a string so we can keep track of the current selection. This is useful when
 * we for instance want to replace our current selection with the transcription,
 * or to place the transcription at the cursor location.
 * - We now want to add any incoming transcript data to the text field to give
 * immediate feedback to the user. For now, we assume the original text color is black.
 * - Any text that is added due to transcription should be displayed in gray,
 * or some shade of color lighter than the already existing text. This makes it
 * clear to the user that this text is part of the transcript, and is tentative,
 * possibly still changing.
 * - Messages of type `UnprocessedSegment` should show in an even lighter shade
 * of gray than messages of type `ProcessedSegment`. This signals to the user that
 * the `UnprocessedSegment` is (even) more tentative than `ProcessedSegment` (which
 * is also tentative as it can be replaced by the final `ProcessedStream` message).
 * - If a new `UnprocessedSegment` comes in, it should replace the previous `UnprocessedSegment`.
 * - Any `ProcessedSegment` should not be removed as new unprocessed and processed
 * segments come in. For example, if the first `ProcessedSegment` is received,
 * any unprocessed and processed segments received after it are shown after the first
 * `ProcessedSegment` in the text field, since a `ProcessedSegment` is not tentative.
 * - The ProcessedStream  So at the end we should replace all the incoming streamed transcript by the contents of it. The color should now be black again.
 * - A message of type `ProcessedStream` contains *all* the transcript of the stream
 * and might make some improvements on the transcription or do some extra processing.
 * It replaces any of the previous `UnprocessedSegment` and `ProcessedSegment` messages.
 * It is added to the existing text field, after which the color of the text should be
 * black again to indicate that the transcription is finished.
 *
 * In the current implementation we achieve this by keeping track of a list of
 * received messages so far, see `receivedMessages`. This allows us to keep track
 * of the messages on the list level instead of directly manipulating any strings.
 * For instance, if the current `receivedMessages` list is
 * ```
 * [
 *   IncomingMessage(IncomingMessageType.ProcessedSegment, "Hello"),
 *   IncomingMessage(IncomingMessageType.UnprocessedSegment, "world"),
 * ]
 * ```
 * and a new message of type `UnprocessedSegment` or `ProcessedSegment` with text "!"
 * comes in, we *replace* the last message in the list with the new message
 * (since the previous item is an `UnprocessedSegment`). If we added a `ProcessedSegment`,
 * the new list will then be
 * ```
 * [
 *  IncomingMessage(IncomingMessageType.ProcessedSegment, "Hello"),
 *  IncomingMessage(IncomingMessageType.ProcessedSegment, "!"),
 *  ]
 *  ```
 *
 * We then build an `AnnotatedString` from the received messages
 * to color and style the text according to the above specifications.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwoMicrophonesScreenStreaming(
    viewModel: TwoMicrophonesScreenStreamingViewModel = viewModel(),
) {
    val largeText by viewModel.largeText.collectAsStateWithLifecycle()
    // For the short text we keep it simple and don't implement streaming at cursor or selection.
    val shortText by viewModel.shortText.collectAsStateWithLifecycle()

    val shortTextReceivedMessages by viewModel.shortTextReceivedMessages.collectAsStateWithLifecycle()
    val largeTextReceivedMessages by viewModel.largeTextReceivedMessages.collectAsStateWithLifecycle()

    // The focus fields are needed for streaming at cursor or location
    var largeTextSelectionBeforeLoseFocus by remember { mutableStateOf<TextRange?>(null) }
    var largeTextFieldHasFocus by remember { mutableStateOf(false) }

    // Required for the BasicTextField.
    val shortTextInteractionSource = remember { MutableInteractionSource() }
    val largeTextInteractionSource = remember { MutableInteractionSource() }

    val shortTextIsShowingStreamingTranscript = shortTextReceivedMessages.isNotEmpty()
    val largeTextIsShowingStreamingTranscript = largeTextReceivedMessages.isNotEmpty()

    LaunchedEffect(largeText) {
        // Don't update the selection if the text field doesn't have focus. For example, when some
        // text is selected and the field loses focus, the information about what was selected is lost.
        // We want to keep this information since we want to insert the transcribed text at the correct
        // position, and we have to press the microphone button, which causes the text field to lose focus.
        if (!largeTextFieldHasFocus) return@LaunchedEffect

        largeTextSelectionBeforeLoseFocus = largeText.selection
    }

    Column(
        modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier
                .border(1.dp, Color(206, 206, 206), RoundedCornerShape(8.dp))
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (shortTextIsShowingStreamingTranscript) {
                Text(
                    buildStreamingTranscriptAnnotatedString(
                        shortText,
                        shortTextReceivedMessages,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(0.dp),
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize
                )
            } else {
                // We can't change the content padding of a normal TextField,
                // so we use a [BasicTextField]. We do this to make the text field and text component
                // have the same padding and look like each other. This way the user doesn't notice
                // that we use a different component.
                BasicTextField(
                    value = shortText,
                    onValueChange = { viewModel.updateshortText(it) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(0.dp),
                    interactionSource = shortTextInteractionSource,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    singleLine = true
                ) { innerTextField ->
                    TextFieldDefaults.DecorationBox(
                        value = shortText,
                        visualTransformation = VisualTransformation.None,
                        innerTextField = innerTextField,
                        singleLine = true,
                        enabled = true,
                        interactionSource = shortTextInteractionSource,
                        contentPadding = PaddingValues(0.dp), // this is how you can remove the padding
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            AttendiMicrophone(
                plugins = listOf(
                    AttendiErrorPlugin(),
                    StopTranscriptionOnPausePlugin(viewModel.viewModelScope),
                    AttendiAsyncTranscribePlugin(apiConfig = exampleAPIConfig,
                        onIncomingMessage = { message ->
                            println("incoming message: $message")

                            if (message.messageType == IncomingMessageType.ProcessedStream) {
                                // TODO: it seems like `ProcessedStream` messages are sent twice by the transcription server, where
                                //  the first message is empty text. This seems to be a bug in the server and should not happen. For now we
                                //  filter out empty messages.
                                if (shortTextReceivedMessages.isNotEmpty() && message.text.isEmpty()) {
                                    return@AttendiAsyncTranscribePlugin
                                }

                                viewModel.updateshortText(
                                    mergeTextAtSelection(
                                        shortText,
                                        message.text,
                                        selection = null
                                    )
                                )

                                // Reset the received messages, as the current stream is done.
                                viewModel.updateshortTextReceivedMessages(emptyList())
                                return@AttendiAsyncTranscribePlugin
                            }

                            // Ignore empty segments, which clutter the UX.
                            if (message.text.isEmpty()) return@AttendiAsyncTranscribePlugin

                            val previousMessage = shortTextReceivedMessages.lastOrNull()

                            if (previousMessage == null) {
                                viewModel.updateshortTextReceivedMessages(
                                    shortTextReceivedMessages + message
                                )
                                return@AttendiAsyncTranscribePlugin
                            }

                            if (previousMessage.messageType == IncomingMessageType.UnprocessedSegment) {
                                // We replace the last message if it was an UnprocessedSegment, since it
                                // is tentative and should be overwritten by newer messages.
                                viewModel.updateshortTextReceivedMessages(
                                    shortTextReceivedMessages.dropLast(1) + message
                                )
                                return@AttendiAsyncTranscribePlugin
                            }

                            // Now we know that the previous message was a ProcessedSegment (since we returned early
                            // for the other message types), so we only add a new message.
                            viewModel.updateshortTextReceivedMessages(
                                shortTextReceivedMessages + message
                            )
                        },
                        onSocketClosing = { _, code, reason ->
                            // TODO: replace this with the actual normal closing reason
                            if (reason != "normal_closing_reason") {
                                // TODO: show an error to the user that something went wrong
                                println("not closed normally!")
                            }

                            // TODO: uncomment and fix this later
                            // // The socket's closing, but we haven't reached the end of the stream yet.
                            // // We should still merge what we can.
                            // if (shortTextReceivedMessages.isNotEmpty()) {
                            //     // There's a socket failure, but we haven't reached the end of the stream yet.
                            //     // We should still merge what we can.
                            //     shortTextReceivedMessages.joinToString(" ") { it.text }.let {
                            //         viewModel.updateshortText(
                            //             mergeTextAtSelection(
                            //                 shortText,
                            //                 it,
                            //             )
                            //         )
                            //         // shortText = mergeTextAtSelection(
                            //         //     shortText,
                            //         //     it,
                            //         // )
                            //     }
                            // }

                            viewModel.updateshortTextReceivedMessages(emptyList())
                            // shortTextReceivedMessages = mutableListOf()
                            println("closing websocket with code $code and reason $reason")
                        },
                        onSocketFailure = { _, t, response ->
                            if (shortTextReceivedMessages.isNotEmpty()) {
                                // There's a socket failure, but we haven't reached the end of the stream yet.
                                // We should still merge what we can.
                                shortTextReceivedMessages.joinToString(" ") { it.text }.let {
                                    viewModel.updateshortText(
                                        mergeTextAtSelection(
                                            shortText,
                                            it,
                                        )
                                    )
                                }
                            }

                            // Manually clear the received messages, since we can't continue the stream.
                            viewModel.updateshortTextReceivedMessages(emptyList())

                            println("websocket failure: $t, response: $response")

                            // TODO: make sure an error is displayed to the user at this point
                        })
                )
            )
        }

        Column(
            modifier = Modifier
                .border(1.dp, Color(206, 206, 206), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            // For now we show the formatted text in a Text component (only during streaming),
            // since it doesn't seem straightforward to show an AnnotatedString in a TextField.
            if (largeTextIsShowingStreamingTranscript) {
                Text(
                    buildStreamingTranscriptAnnotatedString(
                        largeText.text,
                        largeTextReceivedMessages,
                        largeTextSelectionBeforeLoseFocus
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(0.dp),
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize
                )
            } else {
                // We can't change the content padding of a normal TextField,
                // so we use a [BasicTextField]. We do this to make the text field and text component
                // have the same padding and look like each other. This way the user doesn't notice
                // that we use a different component.
                BasicTextField(
                    value = largeText,
                    onValueChange = { viewModel.updateLargeText(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(0.dp)
                        .onFocusChanged { largeTextFieldHasFocus = it.isFocused },
                    interactionSource = largeTextInteractionSource,
                    textStyle = MaterialTheme.typography.bodyLarge,
                ) { innerTextField ->
                    TextFieldDefaults.DecorationBox(
                        value = largeText.text,
                        visualTransformation = VisualTransformation.None,
                        innerTextField = innerTextField,
                        singleLine = false,
                        enabled = true,
                        interactionSource = largeTextInteractionSource,
                        contentPadding = PaddingValues(0.dp), // this is how you can remove the padding
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                    )
                }
            }
            AttendiMicrophone(
                plugins = listOf(
                    AttendiErrorPlugin(),
                    StopTranscriptionOnPausePlugin(viewModel.viewModelScope),
                    // TODO: rename message types to new names
                    AttendiAsyncTranscribePlugin(apiConfig = exampleAPIConfig,
                        onIncomingMessage = { message ->
                            println("incoming message: $message")

                            if (message.messageType == IncomingMessageType.ProcessedStream) {
                                // TODO: it seems like `ProcessedStream` messages are sent twice by the transcription server, where
                                //  the first message is empty text. This seems to be a bug in the server and should not happen. For now we
                                //  filter out empty messages.
                                if (largeTextReceivedMessages.isNotEmpty() && message.text.isEmpty()) {
                                    return@AttendiAsyncTranscribePlugin
                                }

                                viewModel.updateLargeText(
                                    largeText.copy(
                                        text = mergeTextAtSelection(
                                            largeText.text,
                                            message.text,
                                            largeTextSelectionBeforeLoseFocus
                                        )
                                    )
                                )

                                // Reset the received messages, as the current stream is done.
                                viewModel.updateLargeTextReceivedMessages(
                                    emptyList()
                                )
                                return@AttendiAsyncTranscribePlugin
                            }

                            // Ignore empty segments, which clutter the UX.
                            if (message.text.isEmpty()) return@AttendiAsyncTranscribePlugin

                            val previousMessage = largeTextReceivedMessages.lastOrNull()

                            if (previousMessage == null) {
                                viewModel.updateLargeTextReceivedMessages(
                                    largeTextReceivedMessages + message
                                )
                                // largeTextReceivedMessages = largeTextReceivedMessages + message
                                return@AttendiAsyncTranscribePlugin
                            }

                            if (previousMessage.messageType == IncomingMessageType.UnprocessedSegment) {
                                // We replace the last message if it was an UnprocessedSegment, since it
                                // is tentative and should be overwritten by newer messages.
                                viewModel.updateLargeTextReceivedMessages(
                                    largeTextReceivedMessages.dropLast(1) + message
                                )
                                return@AttendiAsyncTranscribePlugin
                            }

                            // Now we know that the previous message was a ProcessedSegment (since we returned early
                            // for the other message types), so we only add a new message.
                            viewModel.updateLargeTextReceivedMessages(
                                largeTextReceivedMessages + message
                            )
                        },
                        onSocketClosing = { _, code, reason ->
                            // TODO: replace this with the actual normal closing reason
                            if (reason != "normal_closing_reason") {
                                // TODO: show an error to the user that something went wrong
                                println("not closed normally!")
                            }

                            // TODO: uncomment and fix this later
                            // // The socket's closing, but we haven't reached the end of the stream yet.
                            // // We should still merge what we can.
                            // if (largeTextReceivedMessages.isNotEmpty()) {
                            //     // There's a socket failure, but we haven't reached the end of the stream yet.
                            //     // We should still merge what we can.
                            //     largeTextReceivedMessages.joinToString(" ") { it.text }.let {
                            //         viewModel.updateLargeText(
                            //             largeText.copy(
                            //                 text = mergeTextAtSelection(
                            //                     largeText.text,
                            //                     it,
                            //                     largeTextSelectionBeforeLoseFocus
                            //                 )
                            //             )
                            //         )
                            //     }
                            // }
                            //
                            viewModel.updateLargeTextReceivedMessages(emptyList())
                            println("closing websocket with code $code and reason $reason")
                        },
                        onSocketFailure = { _, t, response ->
                            if (largeTextReceivedMessages.isNotEmpty()) {
                                // There's a socket failure, but we haven't reached the end of the stream yet.
                                // We should still merge what we can.
                                largeTextReceivedMessages.joinToString(" ") { it.text }.let {
                                    viewModel.updateLargeText(
                                        largeText.copy(
                                            text = mergeTextAtSelection(
                                                largeText.text,
                                                it,
                                                largeTextSelectionBeforeLoseFocus
                                            )
                                        )
                                    )
                                }
                            }

                            // Manually clear the received messages, since we can't continue the stream.
                            viewModel.updateLargeTextReceivedMessages(emptyList())

                            println("websocket failure: $t, response: $response")

                            // TODO: make sure an error is displayed to the user at this point
                        }),
                ),
            )
        }
    }
}

private fun buildStreamingTranscriptAnnotatedString(
    currentText: String, receivedMessages: List<IncomingMessage>, selection: TextRange? = null
): AnnotatedString {
    val styledTranscript = buildStyledTranscriptString(receivedMessages)

    // If there is no selection, we add the new text at the end.
    if (selection == null) {
        return buildAnnotatedString {
            append(currentText)
            if (currentText.isNotEmpty() && styledTranscript.isNotEmpty()) append(" ")

            append(buildStyledTranscriptString(receivedMessages))
        }
    }

    val start = selection.start
    val end = selection.end

    val before = currentText.substring(0, start)
    val after = currentText.substring(end)
    val beforeSeparator = if (before.isNotEmpty() && !before.endsWith(" ")) " " else ""
    val afterSeparator = if (after.isNotEmpty() && !after.startsWith(" ")) " " else ""

    val styledTranscriptString = buildStyledTranscriptString(receivedMessages)

    return buildAnnotatedString {
        append(before)
        append(beforeSeparator)

        append(styledTranscriptString)

        append(afterSeparator)
        append(after)
    }
}

private fun mergeTextAtSelection(
    currentText: String,
    newText: String,
    selection: TextRange? = null,
): String {
    if (selection == null) {
        val maybeSeparator = if (currentText.isNotEmpty() && newText.isNotEmpty()) " " else ""

        return currentText + maybeSeparator + newText
    }

    val start = selection.start
    val end = selection.end

    val before = currentText.substring(0, start)
    val after = currentText.substring(end)
    val beforeSeparator = if (before.isNotEmpty() && !before.endsWith(" ")) " " else ""
    val afterSeparator = if (after.isNotEmpty() && !after.startsWith(" ")) " " else ""

    return "$before$beforeSeparator$newText$afterSeparator$after"
}

fun buildStyledTranscriptString(receivedMessages: List<IncomingMessage>): AnnotatedString {
    return buildAnnotatedString {
        receivedMessages.forEachIndexed { index, message ->
            when (message.messageType) {
                IncomingMessageType.UnprocessedSegment -> {
                    withStyle(
                        style = SpanStyle(
                            color = Color(170, 170, 170), fontStyle = FontStyle.Italic
                        )
                    ) {
                        append(message.text)
                    }
                }

                IncomingMessageType.ProcessedSegment -> {
                    withStyle(
                        style = SpanStyle(
                            color = Color(118, 118, 118), fontStyle = FontStyle.Italic
                        )
                    ) {
                        append(message.text)
                    }
                }

                // TODO: this actually never happens, since when we receive a ProcessedStream message, we
                //  clear the incomingMessages list. So this case is not needed.
                IncomingMessageType.ProcessedStream -> {
                    withStyle(style = SpanStyle(color = Color.Black)) {
                        append(message.text)
                    }
                }
            }

            if (index < receivedMessages.size - 1) {
                append(" ")
            }
        }
    }
}
