package nl.attendi.attendispeechserviceexample.examples.screens.soapscreen

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
import nl.attendi.attendispeechservice.components.attendirecorder.plugins.transcribeplugin.AttendiSyncTranscribePlugin
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorder
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderFactory
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderPlugin
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderState
import nl.attendi.attendispeechservice.services.transcribe.AttendiTranscribeServiceFactory
import nl.attendi.attendispeechserviceexample.examples.plugins.ExampleErrorLoggerPlugin
import nl.attendi.attendispeechserviceexample.examples.plugins.ExampleWavTranscribePlugin
import nl.attendi.attendispeechserviceexample.examples.services.ExampleAttendiTranscribeAPI

class SoapScreenViewModel(private val applicationContext: Context) : ViewModel() {

    val model: StateFlow<SoapScreenModel> by lazy {
        _model.asStateFlow()
    }

    private val recorder: AttendiRecorder = AttendiRecorderFactory.create()
    private val _model: MutableStateFlow<SoapScreenModel>

    init {
        _model = MutableStateFlow(
            SoapScreenModel(
                recorder = recorder,
                onTextChange = {
                    onTextChange(it)
                },
                onFocusedTextFieldIndexChange = {
                    onFocusedTextFieldIndexChange(it)
                },
                onAlertDialogDismiss = {
                    onAlertDialogDismiss()
                }
            ))

        viewModelScope.launch {
            recorder.setPlugins(createRecorderPlugins())

            recorder.recorderState.collectLatest {
                onRecorderStateChange(newRecorderState = it)
            }
        }
    }

    private fun onRecorderStateChange(newRecorderState: AttendiRecorderState) {
        _model.update { currentValue ->
            currentValue.copy(
                canDisplayFocusedTextField = newRecorderState == AttendiRecorderState.Recording || newRecorderState == AttendiRecorderState.Processing
            )
        }
    }

    private fun onTextChange(text: String) {
        val updateText: (SoapScreenModel, String) -> SoapScreenModel =
            when (_model.value.focusedTextFieldIndex) {
                0 -> { current, t -> current.copy(text1 = t) }
                1 -> { current, t -> current.copy(text2 = t) }
                2 -> { current, t -> current.copy(text3 = t) }
                3 -> { current, t -> current.copy(text4 = t) }
                else -> { current, _ -> current }
            }
        _model.update { currentValue ->
            updateText(currentValue, text)
        }
    }

    private fun onFocusedTextFieldIndexChange(index: Int?) {
        _model.update { currentValue ->
            currentValue.copy(
                focusedTextFieldIndex = index
            )
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
            AttendiSyncTranscribePlugin(
                service = AttendiTranscribeServiceFactory.create(
                    apiConfig = ExampleAttendiTranscribeAPI.transcribeAPIConfig
                ),
                onTranscribeCompleted = { transcript, error ->
                    error?.let {
                        _model.update { currentValue ->
                            currentValue.copy(
                                errorMessage = error.message,
                                isErrorAlertShown = true
                            )
                        }
                    } ?: run {
                        val updateText: (SoapScreenModel, String) -> SoapScreenModel =
                            when (_model.value.focusedTextFieldIndex) {
                                0 -> { current, t -> current.copy(text1 = t) }
                                1 -> { current, t -> current.copy(text2 = t) }
                                2 -> { current, t -> current.copy(text3 = t) }
                                3 -> { current, t -> current.copy(text4 = t) }
                                else -> { current, _ -> current }
                            }

                        _model.update { currentValue ->
                            updateText(currentValue, transcript ?: "")
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