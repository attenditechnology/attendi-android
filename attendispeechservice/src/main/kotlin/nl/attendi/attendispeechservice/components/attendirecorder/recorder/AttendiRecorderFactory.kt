package nl.attendi.attendispeechservice.components.attendirecorder.recorder

import nl.attendi.attendispeechservice.audio.AudioRecorder
import nl.attendi.attendispeechservice.audio.AudioRecorderFactory
import nl.attendi.attendispeechservice.audio.AudioRecordingConfig

/**
 * Factory object for creating instances of [AttendiRecorderImpl].
 */
object AttendiRecorderFactory {

    /**
     * Creates a new [AttendiRecorder] instance with optional configuration and plugin support.
     *
     * By default:
     * - Uses a new instance of [AudioRecordingConfig] with default parameters.
     * - Delegates to [AudioRecorderFactory.create] to obtain the default low-level recorder.
     * - Attaches no plugins unless explicitly provided.
     *
     * @param audioRecordingConfig Configuration for audio format, sample rate, etc.
     * @param recorder The low-level [AudioRecorder] implementation to use.
     * @param plugins Optional plugins for extending recording behavior (e.g., filters, analytics).
     * @return A fully constructed [AttendiRecorder] instance.
     */
    fun create(
        audioRecordingConfig: AudioRecordingConfig = AudioRecordingConfig(),
        recorder: AudioRecorder = AudioRecorderFactory.create(),
        plugins: List<AttendiRecorderPlugin> = emptyList()
    ): AttendiRecorder {
        return AttendiRecorderImpl(
            audioRecordingConfig = audioRecordingConfig,
            recorder = recorder,
            plugins = plugins
        )
    }
}