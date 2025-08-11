package nl.attendi.attendispeechserviceexample.examples.screens.twomicrophonesstreamingscreen

import nl.attendi.attendispeechservice.services.asynctranscribe.model.TranscribeAsyncAction
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorder

data class TwoMicrophonesStreamingScreenModel(
    val shortTextFieldModel: TextFieldModel,
    val longTextFieldModel: TextFieldModel,
    val errorMessage: String? = null,
    val isErrorAlertShown: Boolean = false,
    val onAlertDialogDismiss: () -> Unit = { }
) {
    data class TextFieldModel(
        val text: String = "",
        val recorder: AttendiRecorder,
        val annotations: List<TranscribeAsyncAction.AddAnnotation> = emptyList()
    )
}