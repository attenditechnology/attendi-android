package nl.attendi.attendispeechserviceexample.examples.streaming.twomicrophonesscreen

data class TwoMicrophonesStreamingScreenModel(
    val shortTextFieldModel: TextFieldModel,
    val longTextFieldModel: TextFieldModel
) {
    data class TextFieldModel(
        val text: String,
        val onTextChange: (String) -> Unit,
        val onFocusChange: ((Boolean) -> Unit)? = null,
        val onStreamTextChange: (String) -> Unit,
        val onStreamTextFinished: (String) -> Unit
    )
}