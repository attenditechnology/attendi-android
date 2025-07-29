package nl.attendi.attendispeechserviceexample.examples.services.customtranscribeasync

import nl.attendi.attendispeechservice.components.attendirecorder.plugins.asynctranscribeplugin.decoder.AsyncTranscribeMessageDecoder
import nl.attendi.attendispeechservice.services.asynctranscribe.model.TranscribeAsyncAction
import nl.attendi.attendispeechservice.services.asynctranscribe.model.TranscribeAsyncActionData
import nl.attendi.attendispeechservice.services.asynctranscribe.model.TranscribeAsyncReplaceTextParameters

object CustomMessageDecoder : AsyncTranscribeMessageDecoder {

    override fun decode(response: String): List<TranscribeAsyncAction> {
        val list: MutableList<TranscribeAsyncAction> = mutableListOf()
        list.add(
            TranscribeAsyncAction.ReplaceText(
                actionData = TranscribeAsyncActionData(
                    id = "1",
                    index = 0
                ),
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