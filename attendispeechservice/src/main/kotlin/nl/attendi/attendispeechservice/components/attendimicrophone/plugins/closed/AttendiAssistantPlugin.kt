/// Copyright 2023 Attendi Technology B.V.
///
/// Licensed according to LICENSE.txt in this folder.

package nl.attendi.attendispeechservice.components.attendimicrophone.plugins.closed

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import nl.attendi.attendispeechservice.R
import nl.attendi.attendispeechservice.components.attendimicrophone.AttendiMicrophoneState
import nl.attendi.attendispeechservice.components.attendimicrophone.MenuGroup
import nl.attendi.attendispeechservice.components.attendimicrophone.MenuItem
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.AttendiMicrophonePlugin

/**
 * Add functionality relating to Attendi's `schrijfhulp`, which communicates with Attendi's spoken
 * language understanding APIs.
 */
class AttendiAssistantPlugin : AttendiMicrophonePlugin {
    override fun activate(state: AttendiMicrophoneState) {
        state.addMenuGroup(
            MenuGroup(
                id = "assistant",
                title = state.context.getString(R.string.writing_assistant_group_title),
                priority = 1,
                icon = {
                    Icon(
                        Icons.Outlined.AutoAwesome,
                        contentDescription = "My Vector Image",
                    )
                }
            )
        )

        state.addMenuItem(
            groupId = "assistant",
            MenuItem(title = state.context.getString(R.string.writing_assistant_item_title),
                subtitle = state.context.getString(R.string.writing_assistant_item_subtitle),
                action = {
                    println("Schrijf mijn rapportage")
                },
                icon = {
                    Image(
                        painter = painterResource(R.drawable.icon_stamp),
                        contentDescription = "My Vector Image",
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = Color(0xFFD7D5D5),
                                shape = RoundedCornerShape(size = 4.dp)
                            )
                            .padding(6.dp)
                    )
                })
        )
    }
}