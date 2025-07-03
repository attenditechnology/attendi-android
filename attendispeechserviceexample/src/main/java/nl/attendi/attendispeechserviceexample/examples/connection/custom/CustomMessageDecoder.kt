package nl.attendi.attendispeechserviceexample.examples.connection.custom

import nl.attendi.attendispeechservice.domain.decoder.AttendiMessageDecoder
import nl.attendi.attendispeechservice.domain.model.transcribeasync.TranscribeAsyncAction
import nl.attendi.attendispeechservice.domain.model.transcribeasync.TranscribeAsyncActionData
import nl.attendi.attendispeechservice.domain.model.transcribeasync.TranscribeAsyncReplaceTextParameters

object CustomMessageDecoder : AttendiMessageDecoder {

    override fun decode(response: String): List<TranscribeAsyncAction> {
        val list: MutableList<TranscribeAsyncAction> = mutableListOf()
        list.add(
            TranscribeAsyncAction.ReplaceText(
                action = TranscribeAsyncActionData(id = "1", index = 0),
                parameters = TranscribeAsyncReplaceTextParameters(
                    startCharacterIndex = 0,
                    endCharacterIndex = 0,
                    text = response
                )
            )
        )
        return list.toList()
    }
}