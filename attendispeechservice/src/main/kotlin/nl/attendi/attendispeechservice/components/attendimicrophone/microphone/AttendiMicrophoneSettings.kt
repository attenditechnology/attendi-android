package nl.attendi.attendispeechservice.components.attendimicrophone.microphone

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Configuration settings for customizing the appearance and behavior of the [AttendiMicrophone].
 *
 * @property size Sets the width and height of the microphone button.
 * @property cornerRadius Optional corner radius. If null, a fully rounded (50%) shape is used.
 * @property colors Visual color states for active/inactive modes.
 * @property isVolumeFeedbackEnabled A flag indicating whether visual volume feedback is enabled during recording.
 * When set to true, the UI provides additional feedback to the user by displaying the current volume
 * level of the audio signal. This is typically visualized by filling the inside of the
 * microphone's cone in sync with the detected volume level, giving users a real-time indication
 * that the microphone is actively recording.
 * This feature enhances user confidence that recording is working properly, especially in scenarios
 * where audio input may otherwise be silent or subtle.
 * @property showsDefaultPermissionsDeniedDialog A flag indicating whether the default permissions dialog will be shown.
 * By default is set to true, if a custom view needs to be displayed set this flag to false.
 */
data class AttendiMicrophoneSettings(
    val size: Dp = 48.dp,
    val cornerRadius: Dp? = null,
    val colors: AttendiMicrophoneColors = AttendiMicrophoneDefaults.colors(),
    val isVolumeFeedbackEnabled: Boolean = true,
    val showsDefaultPermissionsDeniedDialog: Boolean = true
)

/**
 * Defines the color scheme for different microphone states.
 *
 * @property activeForegroundColor Color of the icon/text when the microphone is active.
 * @property activeBackgroundColor Background color when the microphone is active.
 * @property inactiveForegroundColor Color of the icon/text when the microphone is inactive.
 * @property inactiveBackgroundColor Background color when the microphone is inactive.
 */
@Immutable
data class AttendiMicrophoneColors(
    val activeForegroundColor: Color,
    val activeBackgroundColor: Color,
    val inactiveForegroundColor: Color,
    val inactiveBackgroundColor: Color
)

/**
 * Contains the default values used by [AttendiMicrophone].
 */
@Immutable
object AttendiMicrophoneDefaults {

    private const val BASE_ATTENDI_COLOR = 0xFF1C69E8

    /**
     * Creates a [AttendiMicrophoneColors] that represents the default
     * colors used in a [AttendiMicrophone].
     *
     * @param baseColor Other colors are derived from this color. Convenient if not
     * wanting to specify all colors.
     * @param inactiveBackgroundColor The background color of the microphone when it is not recording
     * or loading.
     * @param inactiveForegroundColor The foreground color of the microphone when it is not recording
     * or loading.
     * @param activeBackgroundColor The background color of the microphone when it is recording or
     * processing the recording.
     * @param activeForegroundColor The foreground color of the microphone when it is recording or
     * processing the recording.
     */
    fun colors(
        baseColor: Color = Color(BASE_ATTENDI_COLOR),
        inactiveBackgroundColor: Color = Color.Transparent,
        inactiveForegroundColor: Color = baseColor,
        activeBackgroundColor: Color = baseColor,
        activeForegroundColor: Color = Color.White
    ): AttendiMicrophoneColors =
        AttendiMicrophoneColors(
            inactiveBackgroundColor = inactiveBackgroundColor,
            inactiveForegroundColor = inactiveForegroundColor,
            activeBackgroundColor = activeBackgroundColor,
            activeForegroundColor = activeForegroundColor,
        )
}
