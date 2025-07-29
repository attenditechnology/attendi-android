package nl.attendi.attendispeechservice.utils

import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Utility object to handle device vibration in a backward-compatible way.
 *
 * Provides a simple method to vibrate the device for a specified duration,
 * using the appropriate APIs depending on the Android version.
 */
object Vibrator {

    fun vibrate(context: Context, durationMilliseconds: Long = 200) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION") context.getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        vibrator.vibrate(
            VibrationEffect.createOneShot(
                durationMilliseconds, VibrationEffect.DEFAULT_AMPLITUDE
            )
        )
    }
}