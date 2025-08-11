package nl.attendi.attendispeechservice.utils

import android.os.Build
import nl.attendi.attendispeechservice.BuildConfig

/**
 * [UserAgentProvider] provides metadata about the project and device.
 */
object UserAgentProvider {

    /** The name of the project. */
    const val PROJECT_NAME = "AttendiSpeechService"

    /** The name of the operating system. Always "Android" for Android apps. */
    const val OPERATING_SYSTEM = "Android"

    /** Returns the project's version number, set in build.gradle. */
    const val PROJECT_VERSION: String = BuildConfig.PROJECT_VERSION

    /** The phone's manufacturer, e.g. "Samsung", "Google". */
    val phoneManufacturer: String = Build.MANUFACTURER

    /** The Android OS version currently running on the device (e.g. "13"). */
    val androidVersion: String = Build.VERSION.RELEASE

    /** The model name of the device (e.g. "Pixel 7"). */
    val phoneModel: String = Build.MODEL

    /**
     * Returns a formatted User-Agent string using app and device information.
     *
     * Format: "AppName/AppVersion (DeviceModel; OS OSVersion; Manufacturer)"
     * Example: "AttendiSpeechService/1.0.0 (Pixel 7; Android 13; Google)"
     *
     * @param context Context to get app version info.
     * @return User-Agent string.
     */
    fun getUserAgent(): String {
        return "$PROJECT_NAME/$PROJECT_VERSION ($phoneModel; $OPERATING_SYSTEM $androidVersion; $phoneManufacturer)"
    }
}