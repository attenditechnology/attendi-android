package nl.attendi.attendispeechservice.components.attendirecorder.plugins

import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.RawRes
import kotlinx.coroutines.withTimeoutOrNull
import nl.attendi.attendispeechservice.R
import nl.attendi.attendispeechservice.components.attendimicrophone.microphone.AttendiMicrophone
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderPlugin
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderModel
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * A plugin for [AttendiMicrophone] that plays audible notification sounds when recording starts and stops.
 *
 * This plugin uses [MediaPlayer] to play short audio cues:
 * - A **start** sound is played before recording begins, with a brief delay to ensure the sound isn't recorded.
 * - A **stop** sound is played immediately after recording ends.
 *
 * The start sound playback is **awaited** before recording starts, to avoid capturing the sound itself in the recording.
 * A timeout of 2000ms ensures the app doesn't hang indefinitely if playback fails or doesn't complete.
 *
 */
class AttendiAudioNotificationPlugin(
    context: Context,
    @RawRes startNotificationSoundId: Int? = null,
    @RawRes stopNotificationSoundId: Int? = null
) : AttendiRecorderPlugin {

    /**
     * Application context, safe to hold without leaking Activity instances.
     * Don’t hold a reference to the context directly, as it may cause memory leaks.
     */
    private val appContext = context.applicationContext
    private val startNotificationSoundId: Int = startNotificationSoundId ?: R.raw.start_notification
    private val stopNotificationSoundId: Int = stopNotificationSoundId ?: R.raw.stop_notification

    private companion object {
        private const val START_NOTIFICATION_TIMEOUT_MILLISECONDS: Long = 2000
    }

    private var startNotificationSound: MediaPlayer? = null
    private var stopNotificationSound: MediaPlayer? = null

    override suspend fun activate(model: AttendiRecorderModel) {
        if (startNotificationSound == null) {
            startNotificationSound =
                MediaPlayer.create(appContext, startNotificationSoundId)
        }
        if (stopNotificationSound == null) {
            stopNotificationSound =
                MediaPlayer.create(appContext, stopNotificationSoundId)
        }

        model.onBeforeStartRecording {
            playNotificationSoundWithTimeout(startNotificationSound)
        }

        model.onStopRecording {
            playNotificationSoundWithTimeout(stopNotificationSound)
        }
    }

    private suspend fun playNotificationSoundWithTimeout(notificationSound: MediaPlayer?) {
        // We add a timeout since it's possible that the onCompletionListener is never called,
        // if something somehow goes wrong with the audio playback.
        withTimeoutOrNull(START_NOTIFICATION_TIMEOUT_MILLISECONDS) {
            suspendCoroutine { continuation ->
                if (notificationSound == null) continuation.resume(Unit)

                // We await until the audio has finished playing before starting recording,
                // to prevent the recorded audio from containing the notification sound. This was
                // leading to some erroneous transcriptions that added an 'o' at the beginning of the
                // transcript. This is done here by resuming the coroutine once the audio has finished
                // playing.
                notificationSound?.setOnCompletionListener {
                    continuation.resume(Unit)
                }

                notificationSound?.start()
            }
        }
    }

    override suspend fun deactivate(model: AttendiRecorderModel) {
        startNotificationSound?.release()
        stopNotificationSound?.release()

        startNotificationSound = null
        stopNotificationSound = null
    }
}
