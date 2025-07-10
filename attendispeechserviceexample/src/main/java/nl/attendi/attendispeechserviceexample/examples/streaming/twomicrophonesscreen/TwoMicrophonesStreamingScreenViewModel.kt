package nl.attendi.attendispeechserviceexample.examples.streaming.twomicrophonesscreen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.asynctranscribe.domain.model.transcribestream.AttendiStreamState

class TwoMicrophonesStreamingScreenViewModel: ViewModel() {

    private class DataStore {
        var shortTextFieldData = TextFieldData()
        var longTextFieldData = TextFieldData()

        data class TextFieldData(
            var text: String = "",
            var currentStreamText: String = "",
            var textCursor: Int = 0
        )
    }

    val model: StateFlow<TwoMicrophonesStreamingScreenModel> by lazy {
        _model.asStateFlow()
    }

    private val _model by lazy {
        MutableStateFlow(
            TwoMicrophonesStreamingScreenModel(
                shortTextFieldModel = mapShortTextFieldModel(),
                longTextFieldModel = mapLongTextFieldModel()
            )
        )
    }

    private var dataStore = DataStore()

    private fun mapShortTextFieldModel() : TwoMicrophonesStreamingScreenModel.TextFieldModel {
        return mapTextFieldModel(
            getModel = { it.shortTextFieldModel },
            updateModel = { model, updated -> model.copy(shortTextFieldModel = updated) },
            data = dataStore.shortTextFieldData,
            delimiterAfterStream = " "
        )
    }

    private fun mapLongTextFieldModel() : TwoMicrophonesStreamingScreenModel.TextFieldModel {
        return mapTextFieldModel(
            getModel = { it.longTextFieldModel },
            updateModel = { model, updated -> model.copy(longTextFieldModel = updated) },
            data = dataStore.longTextFieldData,
            delimiterAfterStream = "\n"
        )
    }

    private fun mapTextFieldModel(
        getModel: (TwoMicrophonesStreamingScreenModel) -> TwoMicrophonesStreamingScreenModel.TextFieldModel,
        updateModel: (TwoMicrophonesStreamingScreenModel, TwoMicrophonesStreamingScreenModel.TextFieldModel) -> TwoMicrophonesStreamingScreenModel,
        data: DataStore.TextFieldData,
        delimiterAfterStream: String
    ): TwoMicrophonesStreamingScreenModel.TextFieldModel {

        fun updateTextFieldOnStreamStateUpdate(streamState: AttendiStreamState) {
            data.currentStreamText = streamState.text
            _model.update { currentState ->
                updateModel(currentState, getModel(currentState).copy(
                    text = data.text + data.currentStreamText,
                    annotations = streamState.annotations
                ))
            }
        }

        fun updateTextFieldCursorPosition() {
            _model.update { currentState ->
                updateModel(currentState, getModel(currentState).copy(
                    startStreamCharacterOffset = data.textCursor
                ))
            }
        }

        return TwoMicrophonesStreamingScreenModel.TextFieldModel(
            text = "",
            annotations = emptyList(),
            startStreamCharacterOffset = 0,
            onTextChange = { text ->
                data.text = text
                _model.update { currentState ->
                    updateModel(currentState, getModel(currentState).copy(text = text))
                }
            },
            onStreamStarted = {
                val currentlyDisplayedText = getModel(_model.value).text
                data.text = when {
                    currentlyDisplayedText.isEmpty() -> ""
                    currentlyDisplayedText.last().toString() == delimiterAfterStream -> currentlyDisplayedText
                    else -> "$currentlyDisplayedText$delimiterAfterStream"
                }
                data.textCursor = data.text.length
                updateTextFieldCursorPosition()
            },
            onStreamUpdated = { stream ->
                updateTextFieldOnStreamStateUpdate(stream.state)
            },
            onStreamFinished = { stream ->
                updateTextFieldOnStreamStateUpdate(stream.state)
                data.currentStreamText = ""
            }
        )
    }
}