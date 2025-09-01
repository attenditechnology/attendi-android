package nl.attendi.attendispeechservice.components.attendirecorder.plugins

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderPlugin
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorder
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderModel

/**
 * A plugin for [AttendiRecorder] that automatically stops recording when audio focus is lost.
 *
 * In Android, audio focus is a system mechanism that manages access to audio output resources.
 * When an app gains audio focus, it's allowed to play or record audio. If another app requests focus,
 * the system notifies the current app that it has lost audio focus, either temporarily or permanently.
 *
 * This plugin uses Android’s [AudioManager] to listen for audio focus changes and reacts to two key cases:
 * - [AudioManager.AUDIOFOCUS_LOSS]: Permanent loss (e.g., another app starts long-form playback).
 * - [AudioManager.AUDIOFOCUS_LOSS_TRANSIENT]: Temporary loss (e.g., incoming call, notification).
 * When either occurs, the plugin stops the active recording to ensure respectful behavior and avoid
 * recording during interruptions.
 *
 * Use this plugin if you want your app to behave well in shared audio environments by automatically
 * stopping recording in response to audio interruptions.
 *
 * @param context The Android context used to access system audio services.
 */
class AttendiStopOnAudioFocusLossPlugin(
    context: Context
) : AttendiRecorderPlugin {

    /**
     * Application context, safe to hold without leaking Activity instances.
     * Don’t hold a reference to the context directly, as it may cause memory leaks.
     */
    private val appContext = context.applicationContext
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun activate(model: AttendiRecorderModel) {
        val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setOnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        coroutineScope.launch {
                            model.stop()
                        }
                    }
                }
            }
            .build()

        audioFocusRequest = focusRequest

        model.onStartRecording {
            audioManager.requestAudioFocus(focusRequest)
        }

        model.onStopRecording {
            audioManager.abandonAudioFocusRequest(focusRequest)
        }
    }

    override suspend fun deactivate(model: AttendiRecorderModel) {
        audioFocusRequest?.let {
            audioManager?.abandonAudioFocusRequest(it)
        }
        audioFocusRequest = null
        audioManager = null
        coroutineScope.cancel()
    }
}
