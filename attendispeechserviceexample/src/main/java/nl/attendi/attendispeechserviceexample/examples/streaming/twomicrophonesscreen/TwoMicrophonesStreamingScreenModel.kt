package nl.attendi.attendispeechserviceexample.examples.streaming.twomicrophonesscreen

import nl.attendi.attendispeechservice.domain.model.transcribeasync.TranscribeAsyncAction
import nl.attendi.attendispeechservice.domain.model.transcribestream.AttendiTranscribeStream

data class TwoMicrophonesStreamingScreenModel(
    val shortTextFieldModel: TextFieldModel,
    val longTextFieldModel: TextFieldModel
) {
    data class TextFieldModel(
        val text: String,
        val annotations: List<TranscribeAsyncAction.AddAnnotation>,
        val startStreamCharacterOffset: Int,
        val onTextChange: (String) -> Unit,
        val onFocusChange: ((Boolean) -> Unit)? = null,
        val onStreamStarted: () -> Unit,
        val onStreamUpdated: (AttendiTranscribeStream) -> Unit,
        val onStreamFinished: (AttendiTranscribeStream) -> Unit
    )
}