package nl.attendi.attendispeechserviceexample.examples.screens.streamingscreen

import android.content.Context
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import nl.attendi.attendispeechservice.audio.AudioRecordingConfig
import nl.attendi.attendispeechservice.components.attendimicrophone.microphone.AttendiMicrophonePlugin
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.AttendiVolumeFeedbackPlugin
import nl.attendi.attendispeechservice.components.attendirecorder.plugins.AttendiAudioNotificationPlugin
import nl.attendi.attendispeechservice.components.attendirecorder.plugins.AttendiErrorPlugin
import nl.attendi.attendispeechservice.components.attendirecorder.plugins.AttendiStopOnAudioFocusLossPlugin
import nl.attendi.attendispeechservice.components.attendirecorder.plugins.asynctranscribeplugin.AttendiAsyncTranscribePlugin
import nl.attendi.attendispeechservice.components.attendirecorder.plugins.asynctranscribeplugin.model.AttendiStreamState
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorder
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderFactory
import nl.attendi.attendispeechservice.services.asynctranscribe.AttendiAsyncTranscribeServiceFactory
import nl.attendi.attendispeechserviceexample.examples.plugins.ExampleErrorLoggerPlugin
import nl.attendi.attendispeechserviceexample.examples.services.ExampleAttendiTranscribeAPI
import nl.attendi.attendispeechserviceexample.examples.services.customtranscribeasync.CustomAsyncTranscribeService
import nl.attendi.attendispeechserviceexample.examples.services.customtranscribeasync.CustomMessageDecoder

class TwoMicrophonesStreamingScreenViewModel(applicationContext: Context) : ViewModel() {

    private val shortTextFieldRecorder: AttendiRecorder = AttendiRecorderFactory.create(
        audioRecordingConfig = AudioRecordingConfig(
            sampleRate = 32000
        ),
        plugins = listOf(
            AttendiAsyncTranscribePlugin(
                service = CustomAsyncTranscribeService,
                serviceMessageDecoder = CustomMessageDecoder,
                onStreamStarted = {
                    _model.value.shortTextFieldModel.onStreamStarted()
                },
                onStreamUpdated = { stream ->
                    _model.value.shortTextFieldModel.onStreamUpdated(stream)
                },
                onStreamCompleted = { stream, _ ->
                    _model.value.shortTextFieldModel.onStreamFinished(stream)
                }
            ),
            AttendiAudioNotificationPlugin(context = applicationContext),
            AttendiErrorPlugin(context = applicationContext),
            ExampleErrorLoggerPlugin()
        )
    )

    private val longTextFieldMicrophonePlugins: List<AttendiMicrophonePlugin> = listOf(
        AttendiVolumeFeedbackPlugin()
    )

    private val longTextFieldRecorder: AttendiRecorder = AttendiRecorderFactory.create(
        plugins = listOf(
            AttendiAsyncTranscribePlugin(
                service = AttendiAsyncTranscribeServiceFactory.create(
                    apiConfig = ExampleAttendiTranscribeAPI.transcribeAPIConfig
                ),
                onStreamStarted = {
                    _model.value.longTextFieldModel.onStreamStarted()
                },
                onStreamUpdated = { stream ->
                    _model.value.longTextFieldModel.onStreamUpdated(stream)
                },
                onStreamCompleted = { stream, _ ->
                    _model.value.longTextFieldModel.onStreamFinished(stream)
                }
            ),
            AttendiAudioNotificationPlugin(context = applicationContext),
            AttendiStopOnAudioFocusLossPlugin(context = applicationContext),
            AttendiErrorPlugin(context = applicationContext),
            ExampleErrorLoggerPlugin()
        )
    )

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

    private fun mapShortTextFieldModel(): TwoMicrophonesStreamingScreenModel.TextFieldModel {
        return mapTextFieldModel(
            getModel = { it.shortTextFieldModel },
            updateModel = { model, updated -> model.copy(shortTextFieldModel = updated) },
            data = dataStore.shortTextFieldData,
            delimiterAfterStream = " ",
            plugins = emptyList(),
            recorder = shortTextFieldRecorder,
            onMicrophoneTapCallback = {
                /**
                 * Stops the microphone associated with the LongTextField if it is currently recording.
                 *
                 * This is necessary to allow the microphone linked to the ShortTextField to start recording,
                 * since only one microphone can be active at a time. If an attempt is made to start a
                 * new microphone session while another is already running, an error will be thrown.
                 */
                if (longTextFieldRecorder.isAudioSessionActive) {
                    longTextFieldRecorder.stop()
                }
            }
        )
    }

    private fun mapLongTextFieldModel(): TwoMicrophonesStreamingScreenModel.TextFieldModel {
        return mapTextFieldModel(
            getModel = { it.longTextFieldModel },
            updateModel = { model, updated -> model.copy(longTextFieldModel = updated) },
            data = dataStore.longTextFieldData,
            delimiterAfterStream = "\n",
            plugins = longTextFieldMicrophonePlugins,
            recorder = longTextFieldRecorder,
            onMicrophoneTapCallback = {
                /**
                 * Stops the microphone associated with the ShortTextField if it is currently recording.
                 *
                 * This is necessary to allow the microphone linked to the LongTextField to start recording,
                 * since only one microphone can be active at a time. If an attempt is made to start a
                 * new microphone session while another is already running, an error will be thrown.
                 */
                if (shortTextFieldRecorder.isAudioSessionActive) {
                    shortTextFieldRecorder.stop()
                }
            }
        )
    }

    private fun mapTextFieldModel(
        getModel: (TwoMicrophonesStreamingScreenModel) -> TwoMicrophonesStreamingScreenModel.TextFieldModel,
        updateModel: (TwoMicrophonesStreamingScreenModel, TwoMicrophonesStreamingScreenModel.TextFieldModel) -> TwoMicrophonesStreamingScreenModel,
        data: DataStore.TextFieldData,
        delimiterAfterStream: String,
        recorder: AttendiRecorder,
        plugins: List<AttendiMicrophonePlugin>,
        onMicrophoneTapCallback: suspend () -> Unit
    ): TwoMicrophonesStreamingScreenModel.TextFieldModel {

        fun updateTextFieldOnStreamStateUpdate(streamState: AttendiStreamState) {
            data.currentStreamText = streamState.text
            _model.update { currentState ->
                updateModel(
                    currentState, getModel(currentState).copy(
                        text = data.text + data.currentStreamText,
                        annotations = streamState.annotations
                    )
                )
            }
        }

        fun updateTextFieldCursorPosition() {
            _model.update { currentState ->
                updateModel(
                    currentState, getModel(currentState).copy(
                        startStreamCharacterOffset = data.textCursor
                    )
                )
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
                    currentlyDisplayedText.last()
                        .toString() == delimiterAfterStream -> currentlyDisplayedText

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
            },
            plugins = plugins,
            recorder = recorder,
            onMicrophoneTapCallback = onMicrophoneTapCallback
        )
    }

    override fun onCleared() {
        // The reason runBlocking(Dispatchers.IO) is used here instead of CoroutineScope(Dispatchers.IO)
        // is because onCleared() is a synchronous, blocking function, and Kotlin does not allow suspending
        // functions or coroutine scopes directly in onCleared().
        runBlocking(Dispatchers.IO) {
            shortTextFieldRecorder.release()
            longTextFieldRecorder.release()
        }
        super.onCleared()
    }
}