package nl.attendi.attendispeechservice.components.attendimicrophone.microphone

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Manages the reactive UI state for the [AttendiMicrophone], exposing microphone state, input volume,
 * and permission verification status as a [StateFlow] to enable reactive UI updates.
 *
 * This model maintains an internal mutable state flow of [AttendiMicrophoneUIState], which encapsulates
 * the current microphone state (e.g., idle, recording), the animated fill level when the microphone
 * is recording, and a flag indicating whether audio permission should be verified.
 *
 * Use this class to observe and respond to microphone-related state changes in a lifecycle-aware and
 * reactive manner.
 */
class AttendiMicrophoneModel {
    private val _uiState = MutableStateFlow(AttendiMicrophoneUIState())
    val uiState: StateFlow<AttendiMicrophoneUIState> = _uiState.asStateFlow()

    fun updateState(state: AttendiMicrophoneState) {
        _uiState.update { it.copy(state = state) }
    }

    fun updateAnimatedMicrophoneFillLevel(fillLevel: Double) {
        _uiState.update { it.copy(animatedMicrophoneFillLevel = fillLevel) }
    }

    fun updateShouldVerifyAudioPermission(enabled: Boolean) {
        _uiState.update { it.copy(shouldVerifyAudioPermission = enabled) }
    }
}

/**
 * Represents the reactive UI state of the [AttendiMicrophone], including the current microphone state,
 * audio input volume, and whether audio recording permissions should be checked.
 *
 * This data class is used to drive UI components that react to microphone activity, volume levels,
 * and permission requirements.
 *
 * @property state The current [AttendiMicrophoneState] of the microphone.
 * @property animatedMicrophoneFillLevel A normalized and smoothed volume level used to animate
 * visual microphone feedback. This value ranges between 0.0 and 1.0.
 * @property shouldVerifyAudioPermission Whether the UI should prompt the user to verify that
 * audio recording permissions are granted.
 */
data class AttendiMicrophoneUIState(
    val state: AttendiMicrophoneState = AttendiMicrophoneState.Idle,
    val animatedMicrophoneFillLevel: Double = 0.0,
    val shouldVerifyAudioPermission: Boolean = false
)

/**
 * Represents the various states of the [AttendiMicrophone], used to drive UI behavior
 * and reflect the current stage of the recording lifecycle.
 *
 * This enum is used within [AttendiMicrophoneUIState] to indicate what the microphone is
 * currently doing, allowing the UI to respond appropriately.
 */
enum class AttendiMicrophoneState {
    /** The default state when the microphone is inactive and idle. */
    Idle,

    /** Indicates that the microphone is preparing to start recording (e.g., permission check or warm-up). */
    Loading,

    /** Indicates that audio recording is actively in progress. */
    Recording,

    /** Indicates that the recorded audio is being processed (e.g., transcription or analysis). */
    Processing
}