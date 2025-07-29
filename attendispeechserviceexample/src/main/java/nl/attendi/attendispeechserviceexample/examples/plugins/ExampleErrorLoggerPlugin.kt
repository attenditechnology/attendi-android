package nl.attendi.attendispeechserviceexample.examples.plugins

import android.util.Log
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderModel
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderPlugin

/**
 * An example implementation of [AttendiRecorderPlugin] that collects errors during recording
 * and logs them.
 */
class ExampleErrorLoggerPlugin : AttendiRecorderPlugin {

    override suspend fun activate(model: AttendiRecorderModel) {
        model.onError { error ->
            Log.e("ExampleErrorLoggerPlugin", "Error: $error")
            Log.e("ExampleErrorLoggerPlugin", Log.getStackTraceString(error))
        }
    }
}
