package nl.attendi.attendispeechservice.components.attendimicrophone.microphone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorder
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderState

/**
 * ViewModel responsible for managing the state, lifecycle, and behavior of the [AttendiMicrophone] component.
 *
 * This ViewModel is designed to survive configuration changes (such as screen rotations), ensuring that
 * the microphone state, active recording sessions, and attached plugins persist seamlessly without
 * interruption or loss of data.
 *
 * It serves as a centralized controller for coordinating microphone recording, handling plugin activation
 * and deactivation, and managing lifecycle-aware behaviors that support robust audio recording experiences.
 *
 * This allows UI components to remain stateless and reactive while delegating complex microphone logic
 * and plugin management to this ViewModel.
 *
 * @param recorder An [AttendiRecorder] instance that handles low-level audio recording operations.
 */
class AttendiMicrophoneViewModel(
    private val recorder: AttendiRecorder,
    private val microphoneSettings: AttendiMicrophoneSettings,
    private val onMicrophoneTap: () -> Unit,
    private val onRecordingPermissionDenied: () -> Unit
) : ViewModel() {

    private companion object {
        /**
         * Delay before showing the loading state to avoid flashing the animation
         * when recording starts almost immediately.
         */
        private const val LOADING_STATE_DELAY_MILLISECONDS = 150L
    }

    private val microphoneModel = AttendiMicrophoneModel()
    private var microphoneVolumeFeedbackPlugin: AttendiMicrophoneVolumeFeedbackPlugin? = null

    /**
     * A background job that updates the microphone state to "loading" if the recording does not
     * start within a specified delay ([LOADING_STATE_DELAY_MILLISECONDS]) after the user taps
     * the microphone to begin recording.
     *
     * This ensures that if the recording setup takes too long, a loading indicator is displayed
     * to provide visual feedback to the user. If recording begins before the delay expires, the
     * job is cancelled and the loading state is never shown.
     */
    private var loadingJob: Job? = null

    val microphoneUIState: StateFlow<AttendiMicrophoneUIState> by lazy {
        microphoneModel.uiState
    }

    init {
        setupPluginLifecycle()
        bindRecorderState()
    }

    fun onTap() {
        showLoadingState()
        microphoneModel.updateShouldVerifyAudioPermission(true)
        onMicrophoneTap()
    }

    fun onAlreadyGrantedRecordingPermissions() {
        microphoneModel.updateShouldVerifyAudioPermission(false)
        toggleRecording()
    }

    fun onJustGrantedRecordingPermissions() {
        microphoneModel.updateShouldVerifyAudioPermission(false)
        viewModelScope.launch {
            recorder.start()
        }
    }

    fun onDeniedPermissions() {
        loadingJob?.cancel()
        microphoneModel.updateShouldVerifyAudioPermission(false)
        microphoneModel.updateState(AttendiMicrophoneState.Idle)

        onRecordingPermissionDenied()
    }

    // Cancels loading state task and shows loading state if recording takes time to start.
    private fun showLoadingState() {
        if (recorder.recorderState.value != AttendiRecorderState.NotStartedRecording) {
            return
        }
        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
            delay(LOADING_STATE_DELAY_MILLISECONDS)

            if (loadingJob?.isCancelled == true) return@launch

            // A verification of the recorder state is done after the delay as the recorder
            // could already be recording and the loader should not be displayed in that case.
            if (recorder.recorderState.value == AttendiRecorderState.NotStartedRecording) {
                microphoneModel.updateState(AttendiMicrophoneState.Loading)
            }
        }
    }

    private fun toggleRecording() {
        viewModelScope.launch {
            if (recorder.recorderState.value == AttendiRecorderState.NotStartedRecording) {
                recorder.start()
            } else if (recorder.recorderState.value == AttendiRecorderState.Recording) {
                // Cancel the loading job before stopping the recorder to prevent
                // the microphone changing to loading state right after the delay.
                loadingJob?.cancel()
                recorder.stop()
            }
        }
    }

    private fun bindRecorderState() {
        viewModelScope.launch {
            recorder.recorderState.collectLatest {
                when (it) {
                    AttendiRecorderState.NotStartedRecording -> microphoneModel.updateState(
                        AttendiMicrophoneState.Idle
                    )

                    AttendiRecorderState.LoadingBeforeRecording -> microphoneModel.updateState(
                        AttendiMicrophoneState.Loading
                    )

                    AttendiRecorderState.Recording -> microphoneModel.updateState(
                        AttendiMicrophoneState.Recording
                    )

                    AttendiRecorderState.Processing -> microphoneModel.updateState(
                        AttendiMicrophoneState.Processing
                    )
                }
            }
        }
    }

    private fun setupPluginLifecycle() {
        viewModelScope.launch {
            recorder.model.onError {
                // Cancelling the loading job on error to prevent the microphone changing
                // to loading state in case the recorder has started recording.
                loadingJob?.cancel()
            }

            if (microphoneSettings.isVolumeFeedbackEnabled) {
                microphoneVolumeFeedbackPlugin = AttendiMicrophoneVolumeFeedbackPlugin(microphoneModel = microphoneModel)
                microphoneVolumeFeedbackPlugin?.activate(model = recorder.model)
            }
        }
    }

    override fun onCleared() {
        // Wait for plugin deactivation to finish and releasing the recorder before the viewModel
        // is destroyed. The reason runBlocking(Dispatchers.IO) is used here instead of CoroutineScope(Dispatchers.IO)
        // is because onCleared() is a synchronous, blocking function, and Kotlin does not allow suspending
        // functions or coroutine scopes directly in onCleared().
        runBlocking(Dispatchers.IO) {
            microphoneVolumeFeedbackPlugin?.deactivate(model = recorder.model)
            // Release recorder resources.
            recorder.release()
        }
        super.onCleared()
    }
}