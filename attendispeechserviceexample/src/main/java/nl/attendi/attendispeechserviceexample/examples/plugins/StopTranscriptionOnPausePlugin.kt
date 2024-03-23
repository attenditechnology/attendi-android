package nl.attendi.attendispeechserviceexample.examples.plugins

import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import nl.attendi.attendispeechservice.components.attendimicrophone.AttendiMicrophoneState
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.AttendiMicrophonePlugin

class StopTranscriptionOnPausePlugin(private val scope: CoroutineScope) : AttendiMicrophonePlugin {
    override fun activate(state: AttendiMicrophoneState) {
        state.onLifecycle { event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    scope.launch {
                        state.stop(delayMilliseconds = 0)
                    }
                }

                else -> {}
            }
        }
    }
}
