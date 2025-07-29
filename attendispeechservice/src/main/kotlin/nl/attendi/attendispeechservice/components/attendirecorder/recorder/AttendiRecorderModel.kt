package nl.attendi.attendispeechservice.components.attendirecorder.recorder

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import nl.attendi.attendispeechservice.audio.AudioFrame
import nl.attendi.attendispeechservice.utils.invokeAll
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Represents the various lifecycle states of an [AttendiRecorder].
 */
enum class AttendiRecorderState {
    NotStartedRecording, LoadingBeforeRecording, Recording, Processing
}

/**
 * A container for asynchronous callback lists that allow clients to
 * react to recorder lifecycle events.
 *
 * Each callback list holds suspend functions invoked at specific points
 * in the recording lifecycle or when audio frames or errors occur.
 */
class AttendiRecorderCallbacks {

    /** Called whenever the recorder state changes. */
    val onStateUpdate = CopyOnWriteArrayList<suspend (AttendiRecorderState) -> Unit>()

    /** Called just before recording is started. */
    val onBeforeStartRecording = CopyOnWriteArrayList<suspend () -> Unit>()

    /** Called immediately after recording has started. */
    val onStartRecording = CopyOnWriteArrayList<suspend () -> Unit>()

    /** Called just before recording is stopped. */
    val onBeforeStopRecording = CopyOnWriteArrayList<suspend () -> Unit>()

    /** Called immediately after recording has stopped. */
    val onStopRecording = CopyOnWriteArrayList<suspend () -> Unit>()

    /** Called when an error occurs during recording. */
    val onError = CopyOnWriteArrayList<suspend (Exception) -> Unit>()

    /** Called when a new audio frame is available during recording. */
    val onAudio = CopyOnWriteArrayList<suspend (AudioFrame) -> Unit>()
}

/**
 * Model class managing the state and callbacks of an [AttendiRecorder].
 *
 * @param callbacks The callbacks container used to notify listeners of recorder events.
 * @param onStartCalled Optional suspend function to invoke when recording starts.
 * @param onStopCalled Optional suspend function to invoke when recording stops.
 */
class AttendiRecorderModel(
    internal val callbacks: AttendiRecorderCallbacks = AttendiRecorderCallbacks(),
    internal var onStartCalled: (suspend () -> Unit)? = null,
    internal var onStopCalled: (suspend () -> Unit)? = null
) {

    private val _state = MutableStateFlow(AttendiRecorderState.NotStartedRecording)

    /**
     * The current state of the recorder exposed as a [StateFlow].
     * Observers can collect this to react to state changes.
     */
    val state: StateFlow<AttendiRecorderState> = _state.asStateFlow()

    /**
     * Starts the recorder by invoking the optional [onStartCalled] suspend function.
     *
     * This triggers the start logic for the recorder, including any callbacks registered.
     */
    suspend fun start() {
        onStartCalled?.invoke()
    }

    /**
     * Stops the recorder by invoking the optional [onStopCalled] suspend function.
     *
     * This triggers the stop logic for the recorder, including any callbacks registered.
     */
    suspend fun stop() {
        onStopCalled?.invoke()
    }

    /**
     * Updates the internal recorder state and notifies all registered
     * state update callbacks.
     *
     * @param state The new recorder state to set.
     */
    suspend fun updateState(state: AttendiRecorderState) {
        _state.value = state
        callbacks.onStateUpdate.invokeAll(state)
    }

    /**
     * Register a callback to be invoked whenever the recorder state updates.
     *
     * @param callback The suspend function to invoke on state updates.
     * @return A function that removes the registered callback when invoked.
     */
    fun onStateUpdate(callback: suspend (AttendiRecorderState) -> Unit): () -> Unit {
        callbacks.onStateUpdate.add(callback)
        return { callbacks.onStateUpdate.remove(callback) }
    }

    /**
     * Register a callback that will be called before recording of audio starts.
     *
     * @param callback The suspend function to invoke before recording starts.
     * @return A function that can be used to remove the added callback.
     */
    fun onBeforeStartRecording(callback: suspend () -> Unit): () -> Unit {
        callbacks.onBeforeStartRecording.add(callback)
        return { callbacks.onBeforeStartRecording.remove(callback) }
    }

    /**
     * Register a callback that will be called just after recording of audio starts.
     *
     * @param callback The suspend function to invoke after recording starts.
     * @return A function that can be used to remove the added callback.
     */
    fun onStartRecording(callback: suspend () -> Unit): () -> Unit {
        callbacks.onStartRecording.add(callback)
        return { callbacks.onStartRecording.remove(callback) }
    }

    /**
     * Register a callback that will be called just before the recording of audio stops.
     *
     * @param callback The suspend function to invoke before recording stops.
     * @return A function that can be used to remove the added callback.
     */
    fun onBeforeStopRecording(callback: suspend () -> Unit): () -> Unit {
        callbacks.onBeforeStopRecording.add(callback)
        return { callbacks.onBeforeStopRecording.remove(callback) }
    }

    /**
     * Register a callback that will be called just after the recording of audio stops.
     *
     * @param callback The suspend function to invoke after recording stops.
     * @return A function that can be used to remove the added callback.
     */
    fun onStopRecording(callback: suspend () -> Unit): () -> Unit {
        callbacks.onStopRecording.add(callback)
        return { callbacks.onStopRecording.remove(callback) }
    }

    /**
     * Register a callback that will be called when an error occurs.
     *
     * @param callback The suspend function to invoke when an exception happens.
     * @return A function that can be used to remove the added callback.
     */
    fun onError(callback: suspend (Exception) -> Unit): () -> Unit {
        callbacks.onError.add(callback)
        return { callbacks.onError.remove(callback) }
    }

    /**
     * Register a callback that will be called when a new audio frame (a set of samples) is available.
     *
     * Useful for real-time processing, streaming, or analysis of audio frames.
     *
     * @param callback The suspend function to invoke with each new audio frame.
     * @return A function that can be used to remove the added callback.
     */
    fun onAudio(callback: suspend (AudioFrame) -> Unit): () -> Unit {
        callbacks.onAudio.add(callback)
        return { callbacks.onAudio.remove(callback) }
    }
}