package nl.attendi.attendispeechserviceexample.examples.streaming

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