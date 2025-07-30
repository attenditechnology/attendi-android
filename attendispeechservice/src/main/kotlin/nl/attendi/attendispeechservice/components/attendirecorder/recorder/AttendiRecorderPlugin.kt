package nl.attendi.attendispeechservice.components.attendirecorder.recorder

/**
 * Plugin interface for components that wish to react to the lifecycle of the recorder.
 *
 * Use this when your plugin only depends on the audio recording state, not the full microphone UI state.
 */
interface AttendiRecorderPlugin {
    /**
     * Called when the recorder is initialized.
     *
     * @param model Provides access to recorder-related state and operations.
     */
    suspend fun activate(model: AttendiRecorderModel) {}

    /**
     * Called when the the recorder is disposed.
     *
     * Use this to clean up any ongoing resources or subscriptions.
     *
     * @param model The recorder model instance used during activation.
     */
    suspend fun deactivate(model: AttendiRecorderModel) {}
}