package nl.attendi.attendispeechserviceexample.examples.screens.streamingscreen

import nl.attendi.attendispeechservice.components.attendimicrophone.microphone.AttendiMicrophonePlugin
import nl.attendi.attendispeechservice.services.asynctranscribe.model.TranscribeAsyncAction
import nl.attendi.attendispeechservice.components.attendirecorder.plugins.asynctranscribeplugin.model.AttendiTranscribeStream
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorder

data class TwoMicrophonesStreamingScreenModel(
    val shortTextFieldModel: TextFieldModel,
    val longTextFieldModel: TextFieldModel
) {
    data class TextFieldModel(
        val text: String,
        val recorder: AttendiRecorder,
        val plugins: List<AttendiMicrophonePlugin>,
        val annotations: List<TranscribeAsyncAction.AddAnnotation>,
        val startStreamCharacterOffset: Int,
        val onTextChange: (String) -> Unit,
        val onFocusChange: ((Boolean) -> Unit)? = null,
        val onMicrophoneTapCallback: suspend () -> Unit,
        val onStreamStarted: () -> Unit,
        val onStreamUpdated: (AttendiTranscribeStream) -> Unit,
        val onStreamFinished: (AttendiTranscribeStream) -> Unit
    )
}