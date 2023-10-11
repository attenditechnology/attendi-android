/// Copyright 2023 Attendi Technology B.V.
/// 
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
/// 
///     http://www.apache.org/licenses/LICENSE-2.0
/// 
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Used to change aspects of the microphone's appearance.
 */
data class MicrophoneModifier(
    /**
     *  Sets the width and height of the microphone. If showOptions is false, the width and height
     *  will be equal. If showOptions is true, the width will be twice the height.
     */
    val size: Dp? = null,
    /**
     * Sets the corner radius of the microphone. If not set, the button will have a RoundedCornerShape
     * of 50 percent.
     */
    val cornerRadius: Dp? = null
)

@Immutable
data class AttendiMicrophoneColors internal constructor(
    val activeForegroundColor: Color,
    val activeBackgroundColor: Color,
    val inactiveForegroundColor: Color,
    val inactiveBackgroundColor: Color
)

const val defaultBaseColor = 0xFF1C69E8

/**
 * Contains the default values used by [AttendiMicrophone].
 */
@Immutable
object AttendiMicrophoneDefaults {
    val Size = 48.dp

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
        baseColor: Color = Color(defaultBaseColor),
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

enum class MicrophoneVariant {
    DEFAULT,
    TRANSPARENT,
    WHITE
}

enum class MicrophoneOptionsVariant {
    VISIBLE_WHEN_NOT_STARTED_RECORDING,
    ALWAYS_VISIBLE,
    HIDDEN
}

data class MicrophoneSettings(
    var size: Dp = 48.dp,
    var colors: AttendiMicrophoneColors = AttendiMicrophoneDefaults.colors(),
    var color: Color = Color(0xFF1C69E8),
    var showOptionsVariant: MicrophoneOptionsVariant = MicrophoneOptionsVariant.HIDDEN,
    var cornerRadius: Dp? = null,
    var variant: MicrophoneVariant = MicrophoneVariant.DEFAULT,
)
