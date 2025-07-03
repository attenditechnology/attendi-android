package nl.attendi.attendispeechserviceexample.examples.streaming.twomicrophonesscreen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TwoMicrophonesStreamingScreenViewModel: ViewModel() {

    private class DataStore {
        var shortTextFieldText: String = ""
        var currentShortTextFieldStreamText: String = ""

        var longTextFieldText: String = ""
        var currentLongTextFieldStreamText: String = ""
    }

    val model: StateFlow<TwoMicrophonesStreamingScreenModel> by lazy {
        _model.asStateFlow()
    }

    private val _model by lazy {
        MutableStateFlow(
            TwoMicrophonesStreamingScreenModel(
                shortTextFieldModel = mapShortTextFieldModel(),
                longTextFieldModel = mapLongTextFieldModel(),
            )
        )
    }

    private var dataStore = DataStore()

    private fun mapShortTextFieldModel() : TwoMicrophonesStreamingScreenModel.TextFieldModel {
        fun updateTextFieldOnStreamTextChange(text: String) {
            dataStore.currentShortTextFieldStreamText = text
            _model.update { currentState ->
                currentState.copy(
                    shortTextFieldModel = currentState.shortTextFieldModel.copy(
                        text = dataStore.shortTextFieldText + dataStore.currentShortTextFieldStreamText
                    )
                )
            }
        }

        return TwoMicrophonesStreamingScreenModel.TextFieldModel(
            text = "",
            onTextChange = { text ->
                dataStore.shortTextFieldText = text
                _model.update { currentState ->
                    currentState.copy(
                        shortTextFieldModel = currentState.shortTextFieldModel.copy(
                            text = text
                        )
                    )
                }
            },
            onStreamTextChange = { text ->
                updateTextFieldOnStreamTextChange(text)
            },
            onStreamTextFinished = { text ->
                updateTextFieldOnStreamTextChange(text)
                val currentlyDisplayedText = _model.value.shortTextFieldModel.text
                dataStore.shortTextFieldText = if (currentlyDisplayedText.isEmpty()) "" else "$currentlyDisplayedText "
                dataStore.currentShortTextFieldStreamText = ""
            }
        )
    }

    private fun mapLongTextFieldModel() : TwoMicrophonesStreamingScreenModel.TextFieldModel {
        fun updateTextFieldOnStreamTextChange(text: String) {
            dataStore.currentLongTextFieldStreamText = text
            _model.update { currentState ->
                currentState.copy(
                    longTextFieldModel = currentState.longTextFieldModel.copy(
                        text = dataStore.longTextFieldText + dataStore.currentLongTextFieldStreamText
                    )
                )
            }
        }

        return TwoMicrophonesStreamingScreenModel.TextFieldModel(
            text = "",
            onTextChange = { text ->
                dataStore.longTextFieldText = text
                _model.update { currentState ->
                    currentState.copy(
                        longTextFieldModel = currentState.longTextFieldModel.copy(
                            text = text
                        )
                    )
                }
            },
            onStreamTextChange = { text ->
                updateTextFieldOnStreamTextChange(text)
            },
            onStreamTextFinished = { text ->
                updateTextFieldOnStreamTextChange(text)
                val currentlyDisplayedText = _model.value.longTextFieldModel.text
                dataStore.longTextFieldText = if (currentlyDisplayedText.isEmpty()) "" else "$currentlyDisplayedText\n"
                dataStore.currentLongTextFieldStreamText = ""
            }
        )
    }
}