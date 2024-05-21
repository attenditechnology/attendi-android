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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
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
 * - Different "categories" of messages should be displayed with different styles to make it clear
 * to the user that they are different. We can make a distinction between already existing text,
 * final segments, and tentative segments. In this demo we use:
 *   - Black for the original text: makes it clear that this is the original text, not the transcript.
 *   - Gray cursive for final segments: makes it clear that this is part of the transcript.
 *   - Bright blue cursive for tentative segments: makes it clear that this is part of the transcript,
 *   but is still tentative and might change. The bright color makes it stand out more than the
 *   final segments, making it easier for the user to focus on a small part. It also signals to the
 *   user that the `TentativeSegment` is (even) more tentative than `FinalSegment` (which
 *  is also tentative as it can be replaced by the final `CompletedStream` message).
 * of gray than messages of type `FinalSegment`.
 * - If a new `TentativeSegment` comes in, it should replace the previous `TentativeSegment`.
 * - Any `FinalSegment` should not be removed as new tentative and final
 * segments come in. For example, if the first `FinalSegment` is received,
 * any tentative and final segments received after it are shown after the first
 * `FinalSegment` in the text field, since a `FinalSegment` is not tentative.
 * - A message of type `CompletedStream` contains *all* the transcript of the stream
 * and might make some improvements on the transcription or do some extra processing.
 * It replaces any of the previous `TentativeSegment` and `FinalSegment` messages.
 * It is added to the existing text field, after which the color of the text should be
 * black again to indicate that the transcription is finished.
 *
 * In the current implementation we achieve this by keeping track of a list of
 * received messages so far, see e.g. [TwoMicrophonesScreenStreamingViewModel._largeTextReceivedMessages].
 * This allows us to keep track of the messages on the list level instead of directly manipulating strings.
 * See [updateReceivedTranscriptionMessages] for more details on how the list is updated.
 *
 * [buildStyledTranscript] then builds an `AnnotatedString` from the received messages
 * to color and style the text according to the above specifications.
 */
@Composable
fun TwoMicrophonesScreenStreaming(
    viewModel: TwoMicrophonesScreenStreamingViewModel = viewModel(),
) {
    val shortText by viewModel.shortText.collectAsStateWithLifecycle()
    val shortTextReceivedMessages by viewModel.shortTextReceivedMessages.collectAsStateWithLifecycle()
    var shortTextSelectionBeforeLoseFocusAndStreaming by remember { mutableStateOf<TextRange?>(null) }
    var shortTextFieldHasFocus by remember { mutableStateOf(false) }

    val largeText by viewModel.largeText.collectAsStateWithLifecycle()
    val largeTextReceivedMessages by viewModel.largeTextReceivedMessages.collectAsStateWithLifecycle()
    // The focus fields are needed for streaming at cursor or location
    var largeTextSelectionBeforeLoseFocusAndStreaming by remember { mutableStateOf<TextRange?>(null) }
    var largeTextFieldHasFocus by remember { mutableStateOf(false) }

    val shortTextIsShowingStreamingTranscript = shortTextReceivedMessages.isNotEmpty()
    val largeTextIsShowingStreamingTranscript = largeTextReceivedMessages.isNotEmpty()

    LaunchedEffect(shortText) {
        // Don't update the selection if the text field doesn't have focus. For example, when some
        // text is selected and the field loses focus, the information about what was selected is lost.
        // We want to keep this information since we want to insert the transcribed text at the correct
        // position, and we have to press the microphone button, which causes the text field to lose focus.
        // Also, don't update the selection if we're showing the streaming transcript. Currently, we change
        // the text's selection when we start streaming (if text is selected), to avoid showing the
        // selection while streaming which can be annoying / confusing.
        if (!shortTextFieldHasFocus || shortTextIsShowingStreamingTranscript) return@LaunchedEffect

        shortTextSelectionBeforeLoseFocusAndStreaming = shortText.selection
    }

    LaunchedEffect(largeText) {
        if (!largeTextFieldHasFocus || largeTextIsShowingStreamingTranscript) return@LaunchedEffect

        largeTextSelectionBeforeLoseFocusAndStreaming = largeText.selection
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
            TextField(
                value = shortText,
                onValueChange = { viewModel.updateShortText(it) },
                // We use the `visualTransformation` parameter to show formatted text when streaming
                // in the text field. There might be better ways to do this, but this seems to work
                // well enough.
                visualTransformation = {
                    // If we're not showing the streaming transcript, we don't need to transform the text.
                    if (!shortTextIsShowingStreamingTranscript) return@TextField TransformedText(
                        it, OffsetMapping.Identity
                    )

                    // Show a formatted text including transcript when streaming.
                    TransformedText(
                        text = buildStreamingTranscriptAnnotatedString(
                            shortText.text,
                            shortTextReceivedMessages,
                            shortTextSelectionBeforeLoseFocusAndStreaming
                        ),
                        // The `offsetMapping` is not completely correct actually. But since we only
                        // show this transformed text when streaming, it should not impact the editing
                        // experience. `TransformedText` requires `offsetMapping` to be supplied.
                        offsetMapping = OffsetMapping.Identity
                    )
                },
                singleLine = true,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(0.dp)
                    .onFocusChanged { shortTextFieldHasFocus = it.isFocused },
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
                    StopTranscriptionOnPausePlugin(viewModel.viewModelScope),
                    AttendiAsyncTranscribePlugin(apiConfig = exampleAPIConfig,
                        onIncomingMessage = { message, _ ->
                            viewModel.handleIncomingShortMessage(
                                message, shortTextSelectionBeforeLoseFocusAndStreaming
                            )
                        },
                        onSocketClosing = { _, code, _, _ ->
                            if (code == WEBSOCKET_NORMAL_CLOSURE_CODE) return@AttendiAsyncTranscribePlugin

                            viewModel.onShortTextSocketFailure(
                                shortTextSelectionBeforeLoseFocusAndStreaming
                            )
                        },
                        onSocketFailure = { _, _, _, _ ->
                            viewModel.onShortTextSocketFailure(
                                shortTextSelectionBeforeLoseFocusAndStreaming
                            )
                        })
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
                onValueChange = { viewModel.updateLargeText(it) },
                visualTransformation = {
                    if (!largeTextIsShowingStreamingTranscript) return@TextField TransformedText(
                        it, OffsetMapping.Identity
                    )

                    TransformedText(
                        text = buildStreamingTranscriptAnnotatedString(
                            largeText.text,
                            largeTextReceivedMessages,
                            largeTextSelectionBeforeLoseFocusAndStreaming
                        ), offsetMapping = OffsetMapping.Identity
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(0.dp)
                    .onFocusChanged { largeTextFieldHasFocus = it.isFocused }
                    .testTag("TwoMicrophonesScreenStreamingLargeTextField")
                ,
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
                    StopTranscriptionOnPausePlugin(viewModel.viewModelScope),
                    AttendiAsyncTranscribePlugin(apiConfig = exampleAPIConfig,
                        onIncomingMessage = { message, _ ->
                            viewModel.handleIncomingLargeMessage(
                                message, largeTextSelectionBeforeLoseFocusAndStreaming
                            )
                        },
                        onSocketClosing = { _, code, _, _ ->
                            if (code == WEBSOCKET_NORMAL_CLOSURE_CODE) return@AttendiAsyncTranscribePlugin

                            viewModel.onLargeTextSocketFailure(
                                largeTextSelectionBeforeLoseFocusAndStreaming
                            )
                        },
                        onSocketFailure = { _, _, _, _ ->
                            viewModel.onLargeTextSocketFailure(
                                largeTextSelectionBeforeLoseFocusAndStreaming
                            )
                        }),
                ),
                modifier = Modifier.testTag("TwoMicrophonesScreenStreamingLargeTextMicrophone")
            )
        }
    }
}

internal fun buildStreamingTranscriptAnnotatedString(
    currentText: String,
    receivedMessages: List<IncomingTranscriptionMessage>,
    selection: TextRange? = null
): AnnotatedString {
    val styledTranscript = buildStyledTranscript(receivedMessages)

    // If there is no selection, we add the new text at the end.
    if (selection == null) {
        return buildAnnotatedString {
            append(currentText)
            if (currentText.isNotEmpty() && styledTranscript.isNotEmpty()) append(" ")

            append(styledTranscript)
        }
    }

    val before = currentText.substring(0, selection.start)
    val after = currentText.substring(selection.end)

    // This is very simple, not taking into account a lot of edge cases like capitalization, punctuation, etc.
    val beforeSeparator = if (before.isNotEmpty() && !before.endsWith(" ")) " " else ""
    val afterSeparator = if (after.isNotEmpty() && !after.startsWith(" ")) " " else ""

    return buildAnnotatedString {
        append(before)
        append(beforeSeparator)

        append(styledTranscript)

        append(afterSeparator)
        append(after)
    }
}

/**
 * We explicitly use a ViewModel here.
 *
 * If we use `rememberSaveable` for saving the large text, we run into some issues in the
 * current setup on screen rotations. Namely, in the `AttendiAsyncTranscribePlugin` we define
 * a closure `onIncomingMessage` that updates the text field. When the screen rotates, the current
 * intended behavior is to stop the recording and save anything that was transcribed so far. To do
 * so, we stop recording and send an `EndOfAudioStream` message to the server. The server then sends
 * a `CompletedStream` message back, which contains the full transcript of the stream. We then use
 * this value to set the `largeText` value. If we would use `rememberSaveable`, the state of `largeText`
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

    private val _largeTextReceivedMessages =
        MutableStateFlow(emptyList<IncomingTranscriptionMessage>())
    val largeTextReceivedMessages: StateFlow<List<IncomingTranscriptionMessage>> =
        _largeTextReceivedMessages.asStateFlow()

    private val _shortText = MutableStateFlow(TextFieldValue())
    val shortText: StateFlow<TextFieldValue> = _shortText.asStateFlow()

    private val _shortTextReceivedMessages =
        MutableStateFlow(emptyList<IncomingTranscriptionMessage>())
    val shortTextReceivedMessages: StateFlow<List<IncomingTranscriptionMessage>> =
        _shortTextReceivedMessages.asStateFlow()

    fun updateLargeText(value: TextFieldValue) {
        _largeText.value = value
    }

    private fun updateLargeTextReceivedMessages(value: List<IncomingTranscriptionMessage>) {
        _largeTextReceivedMessages.value = value
    }

    fun handleIncomingLargeMessage(
        message: IncomingTranscriptionMessage, selectionBeforeStartingStreaming: TextRange?
    ) {
        val currentText = _largeText.value

        val newText = maybeUpdateText(
            message, currentText, _largeTextReceivedMessages.value, selectionBeforeStartingStreaming
        )

        val newReceivedMessages =
            updateReceivedTranscriptionMessages(_largeTextReceivedMessages.value, message)

        // Don't update the text if it's the same as the current text.
        if (newText != currentText) updateLargeText(newText)

        updateLargeTextReceivedMessages(newReceivedMessages)
    }

    fun onLargeTextSocketFailure(selectionBeforeStartingStreaming: TextRange?) {
        // There's a socket failure, but we haven't reached the end of the stream yet.
        // We should still merge what we can.
        if (_largeTextReceivedMessages.value.isNotEmpty()) {
            _largeTextReceivedMessages.value.joinToString(" ") { it.text }.let {
                updateLargeText(
                    mergeTextFieldValueAtSelection(
                        _largeText.value, it, selectionBeforeStartingStreaming
                    )
                )
            }
        }

        // Manually clear the received messages, since we can't continue the stream.
        updateLargeTextReceivedMessages(emptyList())
    }

    fun updateShortText(value: TextFieldValue) {
        _shortText.value = value
    }

    private fun updateShortTextReceivedMessages(value: List<IncomingTranscriptionMessage>) {
        _shortTextReceivedMessages.value = value
    }

    fun handleIncomingShortMessage(
        message: IncomingTranscriptionMessage, selectionBeforeStartingStreaming: TextRange?
    ) {
        val currentText = _shortText.value

        val newText = maybeUpdateText(
            message, currentText, _shortTextReceivedMessages.value, selectionBeforeStartingStreaming
        )

        val newReceivedMessages =
            updateReceivedTranscriptionMessages(_shortTextReceivedMessages.value, message)

        // Don't update the text if it's the same as the current text.
        if (newText != currentText) updateShortText(newText)

        updateShortTextReceivedMessages(newReceivedMessages)
    }

    fun onShortTextSocketFailure(selectionBeforeStartingStreaming: TextRange?) {
        if (_shortTextReceivedMessages.value.isNotEmpty()) {
            // There's a socket failure, but we haven't reached the end of the stream yet.
            // We should still merge what we can.
            _shortTextReceivedMessages.value.joinToString(" ") { it.text }.let {
                updateShortText(
                    mergeTextFieldValueAtSelection(
                        _shortText.value, it, selectionBeforeStartingStreaming
                    )
                )
            }
        }

        // Manually clear the received messages, since we can't continue the stream.
        updateShortTextReceivedMessages(emptyList())
    }

    /**
     * Return an updated text field value, updated based on an incoming message.
     *
     * If the message is a `CompletedStream` message, its text replaces the current text.
     * If the selection is not collapsed (i.e. a word is selected), we remove the selection
     * and set the cursor to the selection's start position. It doesn't make sense to keep
     * the selection when we start streaming, since the previously selected is replaced.
     *
     * Otherwise, return the original text.
     */
    private fun maybeUpdateText(
        message: IncomingTranscriptionMessage,
        currentText: TextFieldValue,
        receivedMessages: List<IncomingTranscriptionMessage>,
        selectionBeforeStartingStreaming: TextRange?
    ): TextFieldValue {
        var newText = currentText

        // If we start streaming at a selection, remove the selection by its start position. It can
        // be a bit annoying to have the selection still there when we start streaming.
        if (receivedMessages.isNotEmpty() && !currentText.selection.collapsed) {
            return currentText.copy(
                selection = TextRange(currentText.selection.start)
            )
        }

        // Replace all current text if we receive a CompletedStream message.
        if (message.type == IncomingTranscriptionMessageType.CompletedStream && message.text.isNotEmpty()) {
            newText = mergeTextFieldValueAtSelection(
                currentText, message.text, selectionBeforeStartingStreaming
            )
        }

        return newText
    }

    private fun mergeTextFieldValueAtSelection(
        currentTextFieldValue: TextFieldValue,
        newText: String,
        selection: TextRange? = null,
    ): TextFieldValue {
        // If there is no selection, we add the new text at the end.
        if (selection == null) {
            val maybeSeparator =
                if (currentTextFieldValue.text.isNotEmpty() && newText.isNotEmpty()) " " else ""

            return currentTextFieldValue.copy(
                currentTextFieldValue.text + maybeSeparator + newText,
                TextRange(currentTextFieldValue.text.length + maybeSeparator.length + newText.length)
            )
        }

        val start = selection.start
        val end = selection.end

        val before = currentTextFieldValue.text.substring(0, start)
        val after = currentTextFieldValue.text.substring(end)

        // This is very simple, not taking into account a lot of edge cases like capitalization, punctuation, etc.
        val beforeSeparator = if (before.isNotEmpty() && !before.endsWith(" ")) " " else ""
        val afterSeparator = if (after.isNotEmpty() && !after.startsWith(" ")) " " else ""

        return TextFieldValue(
            "$before$beforeSeparator$newText$afterSeparator$after",
            TextRange(before.length + beforeSeparator.length + newText.length)
        )
    }
}

/**
 * Return a new list of received messages, based on the current list of received transcription messages
 * and a new message.
 *
 * The rules are as follows:
 * - If the new message is of type `CompletedStream`, we clear the current list of received messages.
 * - If the previous message was a `TentativeSegment`, we replace it with the new message. Otherwise,
 * we add the new message to the list.
 * - `FinalSegment` messages are appended to the list and are never replaced (until a `CompletedStream`
 * message is received).
 *
 * For instance, if the current `receivedMessages` list is
 * ```
 * [
 *   IncomingTranscriptionMessage(IncomingTranscriptionMessageType.FinalSegment, "Hello"),
 *   IncomingTranscriptionMessage(IncomingTranscriptionMessageType.TentativeSegment, "world"),
 * ]
 * ```
 * and a new message of type `TentativeSegment` or `FinalSegment` with text "!"
 * comes in, we *replace* the last message in the list with the new message
 * (since the previous item is an `TentativeSegment`). If we added a `FinalSegment`,
 * the new list will then be
 * ```
 * [
 *  IncomingTranscriptionMessage(IncomingTranscriptionMessageType.FinalSegment, "Hello"),
 *  IncomingTranscriptionMessage(IncomingTranscriptionMessageType.FinalSegment, "!"),
 *  ]
 *  ```
 */
internal fun updateReceivedTranscriptionMessages(
    currentMessages: List<IncomingTranscriptionMessage>, message: IncomingTranscriptionMessage
): List<IncomingTranscriptionMessage> {
    val result = currentMessages.toMutableList()

    if (message.type == IncomingTranscriptionMessageType.CompletedStream) {
        // TODO: it seems like `CompletedStream` messages are sent twice by the transcription server, where
        //  the first message is empty text. This seems to be a bug in the server and should not happen. For now we
        //  filter out empty messages.
        if (currentMessages.isNotEmpty() && message.text.isEmpty()) {
            return result
        }

        // Reset the received messages, as the current stream is done.
        return emptyList()
    }

    // Ignore empty segments, which clutter the UX.
    if (message.text.isEmpty()) return result

    val previousMessage = currentMessages.lastOrNull() ?: return listOf(message)

    if (previousMessage.type == IncomingTranscriptionMessageType.TentativeSegment) {
        // We replace the last message if it was an TentativeSegment, since it
        // is tentative and should be overwritten by newer messages.
        return currentMessages.dropLast(1) + message
    }

    // Now we know that the previous message was a FinalSegment (since we returned early
    // for the other message types), so we only add a new message.
    return currentMessages + message
}

/**
 * Build an `AnnotatedString` from a list of received transcription messages.
 *
 * We give different styles to the different types of messages. These styles can be configured
 * with the [styles] parameter. By default uses shades of gray for the different message types.
 *
 * We add a space between the messages, but not after the last message.
 */
fun buildStyledTranscript(
    receivedMessages: List<IncomingTranscriptionMessage>,
    styles: Map<IncomingTranscriptionMessageType, SpanStyle>? = null
): AnnotatedString {
    return buildAnnotatedString {
        receivedMessages.forEachIndexed { index, message ->
            val messageStyle = styles?.get(message.type)

            when (message.type) {
                IncomingTranscriptionMessageType.TentativeSegment -> {
                    withStyle(
                        messageStyle ?: SpanStyle(
                            color = Color(24, 147, 255),
                            fontStyle = FontStyle.Italic
                        )
                    ) {
                        append(message.text)
                    }
                }

                IncomingTranscriptionMessageType.FinalSegment -> {
                    withStyle(
                        messageStyle ?: SpanStyle(
                            color = Color(118, 118, 118),
                            fontStyle = FontStyle.Italic
                        )
                    ) {
                        append(message.text)
                    }
                }

                // We should actually never see a `CompletedStream` here, as we clear the list
                // received messages when we receive it. We leave it in as an example.
                IncomingTranscriptionMessageType.CompletedStream -> {
                    withStyle(style = SpanStyle(color = Color.Black)) {
                        append(message.text)
                    }
                }
            }

            // Add a space between the messages, but not after the last message.
            if (index < receivedMessages.size - 1) {
                append(" ")
            }
        }
    }
}
