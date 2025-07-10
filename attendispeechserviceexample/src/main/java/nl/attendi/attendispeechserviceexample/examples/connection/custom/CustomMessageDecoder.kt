package nl.attendi.attendispeechserviceexample.examples.connection.custom

import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.asynctranscribe.domain.decoder.AttendiTranscribeAsyncMessageDecoder
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.asynctranscribe.domain.model.transcribeasync.TranscribeAsyncAction
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.asynctranscribe.domain.model.transcribeasync.TranscribeAsyncActionData
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.asynctranscribe.domain.model.transcribeasync.TranscribeAsyncReplaceTextParameters

object CustomMessageDecoder : AttendiTranscribeAsyncMessageDecoder {

    override fun decode(response: String): List<TranscribeAsyncAction> {
        val list: MutableList<TranscribeAsyncAction> = mutableListOf()
        list.add(
            TranscribeAsyncAction.ReplaceText(
                actionData = TranscribeAsyncActionData(id = "1", index = 0),
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