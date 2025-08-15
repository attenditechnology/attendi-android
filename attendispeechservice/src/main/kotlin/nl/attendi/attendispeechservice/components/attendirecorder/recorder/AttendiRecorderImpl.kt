package nl.attendi.attendispeechservice.components.attendirecorder.recorder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import nl.attendi.attendispeechservice.audio.AudioRecorder
import nl.attendi.attendispeechservice.audio.AudioRecorderImpl
import nl.attendi.attendispeechservice.audio.AudioRecordingConfig
import nl.attendi.attendispeechservice.utils.invokeAll
import kotlin.coroutines.cancellation.CancellationException

/**
 * Implementation of the [AttendiRecorder] interface.
 *
 * This class manages audio recording by delegating to a lower-level [AudioRecorder] implementation,
 * handling audio configuration, and coordinating a set of [AttendiRecorderPlugin] instances that
 * can extend or modify the recorder's behavior during its lifecycle.
 *
 * It is responsible for:
 * - Managing the recording state and audio data flow.
 * - Invoking plugin callbacks at appropriate lifecycle events.
 * - Ensuring proper resource management including releasing the recorder.
 *
 * @param audioRecordingConfig Configuration parameters for audio capture such as sample rate, encoding, and channels.
 * @param recorder The underlying low-level audio recorder used to capture audio. Defaults to [AudioRecorderImpl].
 * @param plugins A list of plugins to customize behavior, such as transcription, error handling, or audio focus management.
 * Note: Plugins are activated and deactivated in sync with the recorder lifecycle, enabling modular and reusable audio processing.
 * @param callbackDispatcher The [CoroutineDispatcher] used for delivering UI-related callbacks.
 *
 * Threading contract for AttendiRecorder callbacks:
 * **Main Thread (safe to modify the view hierarchy):**
 * These callbacks are dispatched to the Main Thread automatically before calling them. This allows consumers
 * to safely perform UI updates without manually switching threads:
 * 1. [AttendiRecorderModel.onStartRecording]
 * 2. [AttendiRecorderModel.onStopRecording]
 * Note: This is intentional because UI updates are common and expected.
 *
 * **Background Thread (not safe to modify the view hierarchy):**
 * These callbacks are dispatched on a background thread for performance reasons. Modifying the view hierarchy
 * directly in these callbacks will cause:
 * `android.view.ViewRootImpl$CalledFromWrongThreadException: Only the original thread that created a view hierarchy can touch its views.`
 * 1. [AttendiRecorderModel.onBeforeStartRecording]
 * 2. [AttendiRecorderModel.onBeforeStopRecording]
 * 3. [AttendiRecorderModel.onAudio]
 * 4. [AttendiRecorderModel.onError]
 * 5. [AttendiRecorderModel.onStateUpdate]
 * Note: This is intentional because UI updates are less expected and should be explicit if needed.
 *
 * If you need to update the UI in these background-thread callbacks, switch to the main thread manually:
 * ```
 * withContext(Dispatchers.Main) {
 *     myTextView.text = "Recording..."
 * }
 * ```
 */
internal class AttendiRecorderImpl(
    private val audioRecordingConfig: AudioRecordingConfig = AudioRecordingConfig(),
    private val recorder: AudioRecorder = AudioRecorderImpl,
    private var plugins: List<AttendiRecorderPlugin> = emptyList(),
    private val callbackDispatcher: CoroutineDispatcher = Dispatchers.Main
) : AttendiRecorder {

    override var model: AttendiRecorderModel = AttendiRecorderModel()
        private set

    /**
     * A mutual exclusion lock to ensure that [start], [stop], and [release] operations
     * are not executed concurrently. This protects against race conditions in the
     * recorder’s lifecycle methods.
     */
    private val startStopMutex = Mutex()

    /**
     * Tracks whether the recorder has been started.
     *
     * Used to prevent duplicate or invalid calls to [start] or [stop].
     */
    private var hasStarted: Boolean = false

    /**
     * Tracks whether the recorder has already been released.
     *
     * Prevents repeated cleanup logic from executing, making [release] idempotent.
     */
    private var isReleased = false

    /**
     * A reference to the coroutine job responsible for running the actual recording.
     *
     * This is canceled and cleared when recording is stopped or released.
     */
    private var recorderJob: Job? = null

    /**
     * Internal coroutine scope tied to the lifecycle of the recorder.
     *
     * Used to launch plugin activation and recording tasks.
     * It is canceled when the recorder is released.
     */
    private val internalScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        /**
         * The reason `onStartCalled` and `onStopCalled` are assigned here is to allow the consumer
         * to initiate recording imperatively via the model.
         *
         * When `start()` or `stop()` is called on the model, these callbacks are triggered internally,
         * ensuring proper plugin behavior without exposing implementation details.
         *
         * These callbacks are intentionally marked as `internal` to prevent external consumers from
         * overriding them directly when using the plugin, preserving encapsulation and consistency.
         */
        model.onStartCalled = {
            start()
        }

        model.onStopCalled = {
            stop()
        }

        internalScope.launch {
            plugins.forEach { it.activate(model) }
        }
    }

    override val recorderState: StateFlow<AttendiRecorderState> by lazy {
        model.state
    }

    override val isAudioSessionActive: Boolean
        get() = recorderState.value != AttendiRecorderState.NotStartedRecording

    override fun hasRecordAudioPermissionGranted(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    override suspend fun setPlugins(plugins: List<AttendiRecorderPlugin>) {
        this.plugins.forEach { it.deactivate(model) }

        plugins.forEach { it.activate(model) }

        this.plugins = plugins
    }

    override suspend fun start() = start(0)

    override suspend fun start(delayMilliseconds: Long) = handleErrors {
        startStopMutex.withLock {
            if (hasStarted || isReleased) {
                return@handleErrors
            }
            hasStarted = true
            recorderJob?.cancel()

            model.updateState(AttendiRecorderState.LoadingBeforeRecording)
            model.callbacks.onBeforeStartRecording.invokeAll()

            recorderJob = internalScope.launch {
                delay(delayMilliseconds)
                handleErrors {
                    recorder.startRecording(
                        audioRecordingConfig = audioRecordingConfig,
                        onAudio = { audioFrame ->
                            model.callbacks.onAudio.invokeAll(audioFrame)
                        }
                    )
                    model.updateState(AttendiRecorderState.Recording)
                    withContext(callbackDispatcher) {
                        model.callbacks.onStartRecording.invokeAll()
                    }
                }
            }
        }
    }

    override suspend fun stop() = stop(0)

    override suspend fun stop(delayMilliseconds: Long) = handleErrors {
        startStopMutex.withLock {
            if (!hasStarted || isReleased) {
                return@handleErrors
            }
            hasStarted = false

            model.updateState(AttendiRecorderState.Processing)
            model.callbacks.onBeforeStopRecording.invokeAll()

            // Ensures cleanup code runs even if the coroutine is cancelled during the delay.
            try {
                delay(delayMilliseconds)
            } finally {
                recorder.stopRecording()

                recorderJob?.cancel()
                recorderJob = null

                withContext(callbackDispatcher) {
                    model.callbacks.onStopRecording.invokeAll()
                }
                model.updateState(AttendiRecorderState.NotStartedRecording)
            }
        }
    }

    override suspend fun release() {
        startStopMutex.withLock {
            if (isReleased) {
                return
            }
            isReleased = true

            plugins.forEach { it.deactivate(model) }

            recorderJob?.cancel()
            recorderJob = null

            recorder.stopRecording()

            internalScope.cancel()
        }
    }

    private suspend fun handleErrors(toRun: suspend () -> Unit) {
        try {
            toRun()
        } catch (e: Exception) {
            if (e !is CancellationException) {
                hasStarted = false

                model.updateState(AttendiRecorderState.NotStartedRecording)
                model.callbacks.onError.invokeAll(e)
            }
        }
    }
}