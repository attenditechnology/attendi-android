package nl.attendi.attendispeechserviceexample.examples.streaming

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateReceivedTranscriptionMessagesTest {
    @Test
    fun addTentativeSegmentToEmptyMessagesShouldUpdateMessagesCorrectly() {
        val currentMessages = listOf<IncomingTranscriptionMessage>()
        val incomingMessage = IncomingTranscriptionMessage(
            IncomingTranscriptionMessageType.TentativeSegment,
            "Hello",
        )

        val expected = listOf(
            IncomingTranscriptionMessage(
                IncomingTranscriptionMessageType.TentativeSegment, "Hello",
            )
        )

        val result = updateReceivedTranscriptionMessages(currentMessages, incomingMessage)

        assertEquals(expected, result)
    }


    @Test
    fun addFinalSegmentToEmptyMessagesShouldUpdateMessagesCorrectly() {
        val currentMessages = listOf<IncomingTranscriptionMessage>()
        val incomingMessage = IncomingTranscriptionMessage(
            IncomingTranscriptionMessageType.FinalSegment,
            "Hello",
        )

        val expected = listOf(
            IncomingTranscriptionMessage(
                IncomingTranscriptionMessageType.FinalSegment, "Hello",
            )
        )

        val result = updateReceivedTranscriptionMessages(currentMessages, incomingMessage)

        assertEquals(expected, result)
    }

    @Test
    fun addCompletedStreamToEmptyMessagesShouldNotAddIt() {
        val currentMessages = listOf<IncomingTranscriptionMessage>()
        val incomingMessage = IncomingTranscriptionMessage(
            IncomingTranscriptionMessageType.CompletedStream,
            "Hello",
        )

        val expected = listOf<IncomingTranscriptionMessage>()

        val result = updateReceivedTranscriptionMessages(currentMessages, incomingMessage)

        assertEquals(expected, result)
    }

    @Test
    fun addTentativeSegmentWithPreviousTentativeSegmentShouldReplacePreviousMessage() {
        val currentMessages = listOf(
            IncomingTranscriptionMessage(
                IncomingTranscriptionMessageType.TentativeSegment,
                "world"
            ),
        )
        val incomingMessage = IncomingTranscriptionMessage(
            IncomingTranscriptionMessageType.TentativeSegment,
            "world!",
        )

        val expected = listOf(
            IncomingTranscriptionMessage(
                IncomingTranscriptionMessageType.TentativeSegment,
                "world!"
            ),
        )

        val result = updateReceivedTranscriptionMessages(currentMessages, incomingMessage)

        assertEquals(expected, result)
    }

    @Test
    fun addFinalSegmentWithPreviousTentativeSegmentShouldReplacePreviousMessage() {
        val currentMessages = listOf(
            IncomingTranscriptionMessage(
                IncomingTranscriptionMessageType.TentativeSegment,
                "world"
            ),
        )
        val incomingMessage = IncomingTranscriptionMessage(
            IncomingTranscriptionMessageType.FinalSegment,
            "world!",
        )

        val expected = listOf(
            IncomingTranscriptionMessage(IncomingTranscriptionMessageType.FinalSegment, "world!"),
        )

        val result = updateReceivedTranscriptionMessages(currentMessages, incomingMessage)

        assertEquals(expected, result)
    }

    @Test
    fun addTentativeSegmentWithPreviousFinalSegmentShouldAppendMessage() {
        val currentMessages = listOf(
            IncomingTranscriptionMessage(IncomingTranscriptionMessageType.FinalSegment, "Hello"),
        )
        val incomingMessage = IncomingTranscriptionMessage(
            IncomingTranscriptionMessageType.TentativeSegment,
            "world",
        )

        val expected = listOf(
            IncomingTranscriptionMessage(IncomingTranscriptionMessageType.FinalSegment, "Hello"),
            IncomingTranscriptionMessage(
                IncomingTranscriptionMessageType.TentativeSegment,
                "world"
            ),
        )

        val result = updateReceivedTranscriptionMessages(currentMessages, incomingMessage)

        assertEquals(expected, result)
    }

    @Test
    fun addFinalSegmentWithPreviousFinalSegmentShouldAppendMessage() {
        val currentMessages = listOf(
            IncomingTranscriptionMessage(IncomingTranscriptionMessageType.FinalSegment, "Hello"),
        )
        val incomingMessage = IncomingTranscriptionMessage(
            IncomingTranscriptionMessageType.FinalSegment,
            "world",
        )

        val expected = listOf(
            IncomingTranscriptionMessage(IncomingTranscriptionMessageType.FinalSegment, "Hello"),
            IncomingTranscriptionMessage(IncomingTranscriptionMessageType.FinalSegment, "world"),
        )

        val result = updateReceivedTranscriptionMessages(currentMessages, incomingMessage)

        assertEquals(expected, result)
    }

    @Test
    fun addCompletedStreamShouldClearAllMessages() {
        val currentMessages = listOf(
            IncomingTranscriptionMessage(IncomingTranscriptionMessageType.FinalSegment, "Hello"),
            IncomingTranscriptionMessage(
                IncomingTranscriptionMessageType.TentativeSegment,
                "world"
            ),
        )
        val incomingMessage = IncomingTranscriptionMessage(
            IncomingTranscriptionMessageType.CompletedStream,
            "Hello world!",
        )

        val expected = listOf<IncomingTranscriptionMessage>()

        val result = updateReceivedTranscriptionMessages(currentMessages, incomingMessage)

        assertEquals(expected, result)
    }

    @Test
    fun addTentativeSegmentWithEmptyTextShouldDoNothing() {
        val currentMessages = listOf(
            IncomingTranscriptionMessage(IncomingTranscriptionMessageType.FinalSegment, "Hello"),
        )

        IncomingTranscriptionMessageType.entries.forEach { type ->
            val incomingMessage = IncomingTranscriptionMessage(
                type,
                "",
            )

            val expected = listOf(
                IncomingTranscriptionMessage(
                    IncomingTranscriptionMessageType.FinalSegment,
                    "Hello"
                ),
            )

            val result = updateReceivedTranscriptionMessages(currentMessages, incomingMessage)

            assertEquals(expected, result)
        }
    }
}

class BuildStyledTranscriptTest {
    private val tentativeSegmentStyle = SpanStyle(
        color = Color.Red,
        fontStyle = FontStyle.Italic
    )
    private val finalSegmentStyle = SpanStyle(
        color = Color.Blue,
        fontStyle = FontStyle.Italic
    )
    private val messageStyles = mapOf(
        IncomingTranscriptionMessageType.TentativeSegment to tentativeSegmentStyle,
        IncomingTranscriptionMessageType.FinalSegment to finalSegmentStyle,
    )

    @Test
    fun `buildStyledTranscript with empty messages should return empty string`() {
        val messages = listOf<IncomingTranscriptionMessage>()

        val expected = buildAnnotatedString { }

        val result = buildStyledTranscript(messages, styles = messageStyles)

        assertEquals(expected, result)
    }

    @Test
    fun `buildStyledTranscript with single tentative segment should return correct styled string`() {
        val messages = listOf(
            IncomingTranscriptionMessage(IncomingTranscriptionMessageType.TentativeSegment, "Hello")
        )

        val expected = buildAnnotatedString {
            withStyle(tentativeSegmentStyle) { append("Hello") }
        }

        val result = buildStyledTranscript(messages, styles = messageStyles)

        assertEquals(expected, result)
    }

    @Test
    fun `buildStyledTranscript with single final segment should return correct styled string`() {
        val messages = listOf(
            IncomingTranscriptionMessage(IncomingTranscriptionMessageType.FinalSegment, "Hello")
        )

        val expected = buildAnnotatedString {
            withStyle(finalSegmentStyle) { append("Hello") }
        }

        val result = buildStyledTranscript(messages, styles = messageStyles)

        assertEquals(expected, result)
    }

    @Test
    fun `buildStyledTranscript with multiple messages should return styled string with spaces between`() {
        val messages = listOf(
            IncomingTranscriptionMessage(
                IncomingTranscriptionMessageType.FinalSegment,
                "Hello"
            ),
            IncomingTranscriptionMessage(
                IncomingTranscriptionMessageType.TentativeSegment,
                "how are you?"
            )
        )

        val expected = buildAnnotatedString {
            withStyle(finalSegmentStyle) {
                append("Hello")
            }
            append(" ")
            withStyle(tentativeSegmentStyle) {
                append("how are you?")
            }
        }

        val result = buildStyledTranscript(messages, styles = messageStyles)

        assertEquals(expected, result)
    }
}