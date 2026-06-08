package nl.attendi.attendispeechserviceexample.examples.screens.recorderscreen

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import nl.attendi.attendispeechservice.components.attendirecorder.plugins.AttendiAudioNotificationPlugin
import nl.attendi.attendispeechservice.components.attendirecorder.plugins.AttendiErrorPlugin
import nl.attendi.attendispeechservice.components.attendirecorder.plugins.AttendiStopOnAudioFocusLossPlugin
import nl.attendi.attendispeechservice.components.attendirecorder.plugins.asynctranscribeplugin.AttendiAsyncTranscribePlugin
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorder
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderFactory
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderPlugin
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderState
import nl.attendi.attendispeechservice.services.asynctranscribe.AttendiAsyncTranscribeServiceFactory
import nl.attendi.attendispeechserviceexample.examples.plugins.ExampleErrorLoggerPlugin
import nl.attendi.attendispeechserviceexample.examples.plugins.ExampleWavTranscribePlugin
import nl.attendi.attendispeechserviceexample.examples.services.ExampleAttendiTranscribeAPI

class RecorderStreamingScreenViewModel(private val applicationContext: Context) : ViewModel() {

    val model: StateFlow<RecorderStreamingScreenModel> by lazy {
        _model.asStateFlow()
    }

    private val recorder: AttendiRecorder = AttendiRecorderFactory.create()
    private val _model: MutableStateFlow<RecorderStreamingScreenModel> =
        MutableStateFlow(
            RecorderStreamingScreenModel(
                onTextFieldTextChange = {
                    onTextFieldTextChange(it)
                },
                onStartRecordingTap = {
                    onButtonPressed()
                },
                onAlertDialogDismiss = {
                    onAlertDialogDismiss()
                }
            ))

    init {
        setupInitialConfiguration()
    }

    private fun setupInitialConfiguration() {
        viewModelScope.launch {
            recorder.model.onError { error ->
                _model.update { currentValue ->
                    currentValue.copy(
                        errorMessage = error.message,
                        isErrorAlertShown = true
                    )
                }
            }
            recorder.setPlugins(createRecorderPlugins())

            recorder.recorderState.collectLatest {
                onRecorderStateChange(newRecorderState = it)
            }
        }
    }

    private fun onRecorderStateChange(newRecorderState: AttendiRecorderState) {
        val buttonTitle = when (newRecorderState) {
            AttendiRecorderState.NotStartedRecording ->
                "Start Recording"

            AttendiRecorderState.LoadingBeforeRecording ->
                "Loading"

            AttendiRecorderState.Recording ->
                "Stop Recording"

            AttendiRecorderState.Processing ->
                "Processing"
        }
        _model.update { currentValue ->
            currentValue.copy(
                buttonTitle = buttonTitle
            )
        }
    }

    private fun onTextFieldTextChange(text: String) {
        _model.update { currentValue ->
            currentValue.copy(
                textFieldText = text
            )
        }
    }

    private fun onButtonPressed() {
        viewModelScope.launch {
            if (recorder.recorderState.value == AttendiRecorderState.NotStartedRecording) {
                recorder.start()
            } else if (recorder.recorderState.value == AttendiRecorderState.Recording) {
                recorder.stop()
            }
        }
    }

    private fun onAlertDialogDismiss() {
        _model.update { currentValue ->
            currentValue.copy(
                errorMessage = null,
                isErrorAlertShown = false
            )
        }
    }

    private fun createRecorderPlugins(): List<AttendiRecorderPlugin> {
        return listOf(
            ExampleWavTranscribePlugin(context = applicationContext),
            ExampleErrorLoggerPlugin(),
            AttendiErrorPlugin(context = applicationContext),
            AttendiAudioNotificationPlugin(context = applicationContext),
            AttendiStopOnAudioFocusLossPlugin(context = applicationContext),
            AttendiAsyncTranscribePlugin(
                service = AttendiAsyncTranscribeServiceFactory.create(
                    apiConfig = ExampleAttendiTranscribeAPI.transcribeAPIConfig
                ),
                onStreamUpdated = { stream ->
                    _model.update { currentValue ->
                        currentValue.copy(
                            textFieldText = stream.state.text
                        )
                    }
                },
                onStreamCompleted = { stream, error ->
                    error?.let {
                        _model.update { currentValue ->
                            currentValue.copy(
                                errorMessage = error.message,
                                isErrorAlertShown = true
                            )
                        }
                    } ?: run {
                        _model.update { currentValue ->
                            currentValue.copy(
                                textFieldText = stream.state.text
                            )
                        }
                    }
                }
            )
        )
    }

    override fun onCleared() {
        /**
         * Releases the recorder resources owned by this ViewModel in a detached Coroutine scope.
         *
         * The ViewModel that creates recorder instances and their associated plugins is
         * responsible for releasing them when they are no longer needed.
         *
         * This implementation performs the cleanup from [onCleared] because the example
         * screen navigates away immediately. If needed, the process of releasing
         * recorder resources can be achieved on a custom scope using the suspend function [recorder.release].
         * Check [TwoMicrophonesStreamingScreenViewModel] how the recorder release is handled
         * and embed into the screen lifecycle.
         */
        CoroutineScope(Dispatchers.IO).launch {
            recorder.release()
        }
        super.onCleared()
    }
}