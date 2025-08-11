package nl.attendi.attendispeechserviceexample.examples.screens.twomicrophonesstreamingscreen

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import nl.attendi.attendispeechservice.components.attendirecorder.plugins.AttendiAudioNotificationPlugin
import nl.attendi.attendispeechservice.components.attendirecorder.plugins.AttendiErrorPlugin
import nl.attendi.attendispeechservice.components.attendirecorder.plugins.AttendiStopOnAudioFocusLossPlugin
import nl.attendi.attendispeechservice.components.attendirecorder.plugins.asynctranscribeplugin.AttendiAsyncTranscribePlugin
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorder
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderFactory
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderPlugin
import nl.attendi.attendispeechservice.services.asynctranscribe.AttendiAsyncTranscribeServiceFactory
import nl.attendi.attendispeechserviceexample.examples.plugins.ExampleErrorLoggerPlugin
import nl.attendi.attendispeechserviceexample.examples.plugins.ExampleWavTranscribePlugin
import nl.attendi.attendispeechserviceexample.examples.services.ExampleAttendiTranscribeAPI

class TwoMicrophonesStreamingScreenViewModel(private val applicationContext: Context) :
    ViewModel() {

    val model: StateFlow<TwoMicrophonesStreamingScreenModel> by lazy {
        _model.asStateFlow()
    }

    private val shortTextRecorder: AttendiRecorder = AttendiRecorderFactory.create()
    private val largeTextRecorder: AttendiRecorder = AttendiRecorderFactory.create()
    private val _model: MutableStateFlow<TwoMicrophonesStreamingScreenModel> =
        MutableStateFlow(TwoMicrophonesStreamingScreenModel(
                shortTextFieldModel = TwoMicrophonesStreamingScreenModel.TextFieldModel(recorder = shortTextRecorder),
                longTextFieldModel = TwoMicrophonesStreamingScreenModel.TextFieldModel(recorder = largeTextRecorder),
                onAlertDialogDismiss = {
                    onAlertDialogDismiss()
                }
            )
        )

    init {
        viewModelScope.launch {
            shortTextRecorder.setPlugins(createSmallRecorderPlugins())
            largeTextRecorder.setPlugins(createLargeRecorderPlugins())
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

    private fun createSmallRecorderPlugins(): List<AttendiRecorderPlugin> {
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
                            shortTextFieldModel = currentValue.shortTextFieldModel.copy(
                                text = stream.state.text,
                                annotations = stream.state.annotations
                            )
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
                                shortTextFieldModel = currentValue.shortTextFieldModel.copy(
                                    text = stream.state.text,
                                    annotations = emptyList()
                                )
                            )
                        }
                    }
                }
            )
        )
    }

    private fun createLargeRecorderPlugins(): List<AttendiRecorderPlugin> {
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
                            longTextFieldModel = currentValue.longTextFieldModel.copy(
                                text = stream.state.text,
                                annotations = stream.state.annotations
                            )
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
                                longTextFieldModel = currentValue.longTextFieldModel.copy(
                                    text = stream.state.text,
                                    annotations = emptyList()
                                )
                            )
                        }
                    }
                }
            )
        )
    }

    override fun onCleared() {
        // The reason runBlocking(Dispatchers.IO) is used here instead of CoroutineScope(Dispatchers.IO)
        // is because onCleared() is a synchronous, blocking function, and Kotlin does not allow suspending
        // functions or coroutine scopes directly in onCleared().
        runBlocking(Dispatchers.IO) {
            shortTextRecorder.release()
            largeTextRecorder.release()
        }
        super.onCleared()
    }
}