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

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Used to change aspects of the microphone's appearance.
 */
data class MicrophoneModifier(
    /**
     * The main color of the microphone.
     */
    val color: Color? = null,
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
    var color: Color = Color(0xFF1C69E8),
    var showOptionsVariant: MicrophoneOptionsVariant = MicrophoneOptionsVariant.HIDDEN,
    var cornerRadius: Dp? = null,
    var variant: MicrophoneVariant = MicrophoneVariant.DEFAULT,
)
