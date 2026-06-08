package nl.attendi.attendispeechserviceexample.examples.screens.twomicrophonesstreamingscreen

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

    /**
     * One-shot event emitted after both recorders have been fully released.
     * The screen collects this to trigger the actual navigation up, ensuring the
     * navigation only happens once cleanup is complete.
     */
    private val _navigateUpChannel = Channel<Unit>(Channel.CONFLATED)
    val navigateUpEvent: Flow<Unit> = _navigateUpChannel.receiveAsFlow()

    private val shortTextRecorder: AttendiRecorder = AttendiRecorderFactory.create()
    private val largeTextRecorder: AttendiRecorder = AttendiRecorderFactory.create()
    private val _model: MutableStateFlow<TwoMicrophonesStreamingScreenModel> =
        MutableStateFlow(
            TwoMicrophonesStreamingScreenModel(
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

    /**
     * Releases both recorder instances in parallel, then signals the screen to navigate up.
     *
     * Using [viewModelScope] here (rather than a detached [kotlinx.coroutines.CoroutineScope])
     * ties the cleanup work to the ViewModel's own lifecycle. The coroutine is guaranteed to
     * complete before the scope is cancelled, because navigation only happens after the channel
     * emits — which only happens after both recorders finish releasing.
     *
     * Both [shortTextRecorder] and [largeTextRecorder] are released in parallel to minimise the
     * time the user waits before the screen transitions.
     *
     * This method is the single entry point for leaving this screen and should be called from
     * both the TopAppBar back button and the system back gesture (via [BackHandler]).
     */
    fun onNavigateUp() {
        viewModelScope.launch(Dispatchers.IO) {
            awaitAll(
                async { shortTextRecorder.release() },
                async { largeTextRecorder.release() }
            )
            _navigateUpChannel.send(Unit)
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
        // Don't need to override.
        // Recorder resources are released explicitly in [onNavigateUp], which is called by both
        // the TopAppBar back button and the system back gesture. By the time this ViewModel is
        // cleared the recorders are already released, so no additional cleanup is needed here.
        super.onCleared()
    }
}