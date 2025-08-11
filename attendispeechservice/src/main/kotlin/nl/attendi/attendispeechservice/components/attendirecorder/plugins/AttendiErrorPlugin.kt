package nl.attendi.attendispeechservice.components.attendirecorder.plugins

import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.RawRes
import nl.attendi.attendispeechservice.R
import nl.attendi.attendispeechservice.audio.AudioRecorderException
import nl.attendi.attendispeechservice.components.attendimicrophone.microphone.AttendiMicrophone
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderModel
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderPlugin
import nl.attendi.attendispeechservice.utils.Vibrator

/**
 * A plugin for [AttendiMicrophone] that provides audio and haptic feedback when a recording error occurs.
 *
 * When an error is reported via the [AttendiRecorderModel.onError] callback, this plugin:
 * - Plays an error notification sound (from `R.raw.error_notification`)
 * - Triggers a short device vibration
 *
 * This enhances the user experience by giving immediate and clear feedback when something goes wrong
 * during audio recording (e.g., microphone access failure, unexpected termination).
 *
 * The plugin ensures that the [MediaPlayer] used for sound playback is properly released
 * when the microphone is deactivated.
 * @param context The Android [Context] used to access system services like [MediaPlayer] and [Vibrator].
 * @param errorNotificationSoundId The raw resource ID of the sound to be played on error.
 * Defaults to [R.raw.error_notification]. Must be annotated with [RawRes].
 */
class AttendiErrorPlugin(
    private val context: Context,
    @RawRes errorNotificationSoundId: Int? = null
) : AttendiRecorderPlugin {

    private val errorNotificationSoundId: Int = errorNotificationSoundId ?: R.raw.error_notification

    private var errorNotificationSound: MediaPlayer? = null

    override suspend fun activate(model: AttendiRecorderModel) {
        if (errorNotificationSound == null) {
            errorNotificationSound =
                MediaPlayer.create(context, errorNotificationSoundId)
        }

        model.onError { error ->
            if (error !is AudioRecorderException.AlreadyRecording) {
                errorNotificationSound?.start()
                Vibrator.vibrate(context)
            }
        }
    }

    override suspend fun deactivate(model: AttendiRecorderModel) {
        errorNotificationSound?.release()
        errorNotificationSound = null
    }
}
