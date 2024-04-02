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

import androidx.activity.compose.setContent
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import nl.attendi.attendispeechserviceexample.examples.streaming.IncomingTranscriptionMessage
import nl.attendi.attendispeechserviceexample.examples.streaming.IncomingTranscriptionMessageType
import nl.attendi.attendispeechserviceexample.examples.streaming.TwoMicrophonesScreenStreaming
import nl.attendi.attendispeechserviceexample.examples.streaming.TwoMicrophonesScreenStreamingViewModel
import nl.attendi.attendispeechserviceexample.examples.streaming.buildStreamingTranscriptAnnotatedString
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class TwoMicrophonesScreenStreamingTest {
    @JvmField
    @Rule
    var mRuntimePermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.RECORD_AUDIO)

    private val textFieldTag = "TwoMicrophonesScreenStreamingLargeTextField"
    private val microphoneTag = "TwoMicrophonesScreenStreamingLargeTextMicrophone"

    @get:Rule
    val composeTestRule =
        createAndroidComposeRule<MainActivity>() // Use your activity that hosts the Composable

    private lateinit var viewModel: TwoMicrophonesScreenStreamingViewModel

    @Before
    fun setup() {
        viewModel = TwoMicrophonesScreenStreamingViewModel()

        composeTestRule.activity.runOnUiThread {
            composeTestRule.activity.setContent {
                TwoMicrophonesScreenStreaming(viewModel = viewModel)
            }
        }
    }

    // TODO: Test the visual transformation. This doesn't seem immediately accessible from the
    //  test API.
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testLargeTextFieldWithTranscription() {
        val initialText = "Initial text here."

        // Type initial text.
        composeTestRule.onNodeWithTag(textFieldTag).performTextInput(initialText)
        composeTestRule.onNodeWithTag(textFieldTag).assertTextEquals(initialText)

        // Select "text" in initial text
        val selectionBeforeStartingStreaming = TextRange(8, 12)
        composeTestRule.onNodeWithTag(textFieldTag)
            .performTextInputSelection(selectionBeforeStartingStreaming)

        // For now we don't actually test the streaming, just the logic for updating the
        // TextFieldValue of the `largeText`. Therefore we can send a message directly to the
        // viewModel.
        // First message:
        viewModel.handleIncomingLargeMessage(
            IncomingTranscriptionMessage(
                IncomingTranscriptionMessageType.TentativeSegment, "transcribe"
            ), selectionBeforeStartingStreaming = selectionBeforeStartingStreaming
        )

        assertEquals(
            TextFieldValue(
                // Text shouldn't have changed yet since it's not a `CompletedStream` message
                text = initialText,
                // Currently the selection is not cleared on the first message, as it's a bit tricky
                // to do this in the right order. This is something that can be improved.
                selection = selectionBeforeStartingStreaming
            ), viewModel.largeText.value
        )

        assertVisualTransformationEquals(
            "Initial transcribe here.",
            selectionBeforeStartingStreaming
        )

        // Second message:
        viewModel.handleIncomingLargeMessage(
            IncomingTranscriptionMessage(
                IncomingTranscriptionMessageType.TentativeSegment, "transcribe text"
            ), selectionBeforeStartingStreaming = selectionBeforeStartingStreaming
        )

        assertEquals(
            TextFieldValue(
                // Text shouldn't have changed yet since it's not a `CompletedStream` message
                text = initialText,
                // The selection should be cleared after the first transcription message.
                selection = TextRange(viewModel.largeText.value.selection.start)
            ), viewModel.largeText.value
        )

        assertVisualTransformationEquals(
            "Initial transcribe text here.",
            selectionBeforeStartingStreaming
        )

        // Third message: FinalSegment
        viewModel.handleIncomingLargeMessage(
            IncomingTranscriptionMessage(
                IncomingTranscriptionMessageType.FinalSegment, "Transcribed text."
            ), selectionBeforeStartingStreaming = selectionBeforeStartingStreaming
        )

        assertEquals(
            TextFieldValue(
                // Text shouldn't have changed yet since it's not a `CompletedStream` message
                text = initialText,
                // The selection should be cleared after the first transcription message.
                selection = TextRange(viewModel.largeText.value.selection.start)
            ), viewModel.largeText.value
        )

        assertVisualTransformationEquals(
            "Initial Transcribed text. here.",
            selectionBeforeStartingStreaming
        )

        // Fourth and last message: CompletedStream
        viewModel.handleIncomingLargeMessage(
            IncomingTranscriptionMessage(
                IncomingTranscriptionMessageType.CompletedStream, "Transcribed text all."
            ), selectionBeforeStartingStreaming = selectionBeforeStartingStreaming
        )

        assertEquals(
            TextFieldValue(
                // Text should now be updated with the final transcribed text.
                text = "Initial Transcribed text all. here.",
                // The selection should be set to the end of the inserted text.
                selection = TextRange("Initial Transcribed text all.".length)
            ), viewModel.largeText.value
        )
    }

    private fun assertVisualTransformationEquals(
        expectedText: String, selectionBeforeStartingStreaming: TextRange
    ) {
        assertEquals(
            expectedText, buildStreamingTranscriptAnnotatedString(
                viewModel.largeText.value.text,
                viewModel.largeTextReceivedMessages.value,
                selectionBeforeStartingStreaming
            ).text
        )
    }
}
