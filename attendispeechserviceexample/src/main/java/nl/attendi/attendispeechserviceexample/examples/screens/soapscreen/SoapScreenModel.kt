package nl.attendi.attendispeechserviceexample.examples.screens.soapscreen

import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorder

data class SoapScreenModel(
    val recorder: AttendiRecorder,
    val text1: String = "",
    val text2: String = "",
    val text3: String = "",
    val text4: String = "",
    val onTextChange: (String) -> Unit = { },
    val onFocusedTextFieldIndexChange: (Int?) -> Unit = { },
    val focusedTextFieldIndex: Int? = 0,
    val canDisplayFocusedTextField: Boolean = false,
    val errorMessage: String? = null,
    val isErrorAlertShown: Boolean = false,
    val onAlertDialogDismiss: () -> Unit = { }
)