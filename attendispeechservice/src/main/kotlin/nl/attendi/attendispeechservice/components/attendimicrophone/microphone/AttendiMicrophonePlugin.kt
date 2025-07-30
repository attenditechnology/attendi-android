package nl.attendi.attendispeechservice.components.attendimicrophone.microphone

import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderModel

/**
 * Extended plugin interface for full integration with the microphone UI and recorder lifecycle.
 *
 * This allows you to hook into both the recorder and microphone state and is useful for complex
 * plugins like transcribers, voice command processors, or UI overlays.
 */
interface AttendiMicrophonePlugin {
    /**
     * Called when the microphone component is initialized.
     *
     * @param recorderModel Access to the audio recorder.
     * @param microphoneModel Access to the microphone UI state and controls.
     */
    suspend fun activate(recorderModel: AttendiRecorderModel, microphoneModel: AttendiMicrophoneModel) {}

    /**
     * Called when the microphone component is disposed.
     *
     * @param recorderModel The audio recorder instance used during activation.
     * @param microphoneModel The microphone UI model.
     */
    suspend fun deactivate(recorderModel: AttendiRecorderModel, microphoneModel: AttendiMicrophoneModel) {}
}