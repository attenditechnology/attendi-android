package nl.attendi.attendispeechserviceexample.examples.screens.recorderscreen

data class RecorderStreamingScreenModel(
    val textFieldText: String = "",
    val onTextFieldTextChange: (String) -> Unit = { },
    val buttonTitle: String = "",
    val onStartRecordingTap: () -> Unit = { },
    val errorMessage: String? = null,
    val isErrorAlertShown: Boolean = false,
    val onAlertDialogDismiss: () -> Unit = { }
)