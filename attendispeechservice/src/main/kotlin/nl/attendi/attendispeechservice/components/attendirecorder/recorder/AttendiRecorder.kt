package nl.attendi.attendispeechservice.components.attendirecorder.recorder

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface defining the contract for an audio recorder.
 *
 * Provides functionality to start and stop recording with optional delays,
 * expose the current recorder state as a [StateFlow], and release resources.
 *
 * The [release] method **must be called** when the recorder is no longer needed
 * to properly clean up resources such as audio inputs, threads, or native handles.
 * Failing to call [release] will result in resource leaks, which can cause
 * memory leaks, unexpected behavior, or exhaustion of system audio resources.
 */
interface AttendiRecorder {

    /**
     * The core model containing callbacks and state update hooks used to drive plugin behavior.
     * Plugins and UI components can observe or register to react to events like start, stop,
     * audio frame emission, or errors.
     */
    val model: AttendiRecorderModel

    /**
     * A [StateFlow] that emits the current [AttendiRecorderState], allowing observers
     * to react to changes in the recorder's lifecycle state.
     */
    val recorderState: StateFlow<AttendiRecorderState>

    /**
     * Indicates whether the recorder is currently engaged in an active audio session.
     *
     * This includes recording, loading, or processing audio. It does **not** distinguish
     * between these individual states—only that the recorder is busy and not idle.
     */
    val isAudioSessionActive: Boolean

    /**
     * Utility function to check whether the user has granted permission to record audio.
     *
     * @param context A valid [Context], typically from an Activity or Application.
     * @return `true` if RECORD_AUDIO permission is granted, `false` otherwise.
     */
    fun hasRecordAudioPermissionGranted(context: Context): Boolean

    /**
     * Sets the plugins for the [AttendiRecorder].
     *
     * This method should be called *after* the recorder is created rather than
     * during its initialization. The reason is not related to memory leaks,
     * but to avoid cases where certain plugins may hold a reference to the
     * recorder instance during their construction. Such a reference could
     * interfere with creating the recorder directly alongside those plugins.
     *
     * Calling this method will deactivate all previously set plugins and
     * activate the new ones provided.
     *
     * @Param plugins A list of [AttendiRecorderPlugin] instances to attach to the recorder.
     */
    suspend fun setPlugins(plugins: List<AttendiRecorderPlugin>)

    /**
     * Starts recording.
     */
    suspend fun start()

    /**
     * Starts recording after a delay specified by [delayMilliseconds].
     *
     * @param delayMilliseconds Delay before starting recording, in milliseconds.
     */
    suspend fun start(delayMilliseconds: Long)

    /**
     * Stops recording.
     */
    suspend fun stop()

    /**
     * Stops recording after a delay specified by [delayMilliseconds].
     *
     * @param delayMilliseconds Delay before stopping recording, in milliseconds.
     */
    suspend fun stop(delayMilliseconds: Long)

    /**
     * Releases any resources associated with the recorder.
     *
     * Should be called when the recorder is no longer needed to avoid resource leaks.
     */
    suspend fun release()
}