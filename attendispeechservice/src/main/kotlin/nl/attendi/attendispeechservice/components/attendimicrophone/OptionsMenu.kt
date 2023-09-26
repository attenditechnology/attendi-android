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

package nl.attendi.attendispeechservice.components.attendimicrophone

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import nl.attendi.attendispeechservice.R

data class MenuGroup(
    val id: String, val title: String, val priority: Int,
    // TODO: correct the type of the icon
    val icon: (@Composable () -> Unit)? = null,
)

data class MenuItem(
    val title: String,
    val subtitle: String? = null,
    val icon: (@Composable () -> Unit)? = null,
    val action: (suspend () -> Unit)? = null
)

@Composable
fun OptionsMenu(
    menuGroups: List<MenuGroup>,
    menuItems: MutableMap<String, MutableList<MenuItem>>,
    isOptionsMenuOpen: MutableState<Boolean>,
) {
    Column(
        modifier = Modifier
            .background(
                Color.Gray.copy(alpha = 0.05f)
            )
            .fillMaxWidth()
            .padding(16.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        for (menuGroup in menuGroups) {
            val menuItemsForGroup = menuItems[menuGroup.id] ?: listOf()
            if (menuItemsForGroup.isEmpty()) continue

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Menu group header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    menuGroup.icon?.let {
                        it()

                        Spacer(modifier = Modifier.width(width = 8.dp))
                    }

                    Text(menuGroup.title.uppercase())
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    for (menuItem in menuItemsForGroup) {
                        MenuItemView(
                            menuItem,
                            closeOptionsMenu = { isOptionsMenuOpen.value = false })
                    }
                }

            }
        }
    }
}


@Composable
fun MenuItemView(menuItem: MenuItem, closeOptionsMenu: () -> Unit) {
    val scope = rememberCoroutineScope()

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
        // TODO: only bottom shadow
        .shadow(elevation = 4.dp, shape = RoundedCornerShape(8.dp))
        .background(Color.White)
        .fillMaxWidth()
        .padding(8.dp)
        .height(48.dp)
        .clickable {
            menuItem.action?.let {
                scope.launch {
                    it()

                    closeOptionsMenu()
                }
            }
        }) {
        // TODO: is there better syntax for this?
        menuItem.icon?.let { it() }

        Spacer(modifier = Modifier.width(width = 8.dp))

        Column {
            Text(
                menuItem.title, style = TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight(500),
                )
            )

            menuItem.subtitle?.let {
                Text(
                    it, style = TextStyle(
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight(400),
                        color = Color(0xFF6B6B6B),
                    )
                )
            }

        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    OptionsMenu(
        menuGroups = listOf(
            MenuGroup(
                id = "reporting-methods",
                title = "Rapportagemethodes",
                priority = 1,
                // TODO: load icon from resources
                icon = {
                    Image(
                        painter = painterResource(R.drawable.icon_numeration),
                        contentDescription = "My Vector Image",
                        modifier = Modifier
                    )
                }
            ),
            MenuGroup(id = "assistant", title = "Schrijfhulp", priority = 1,
                // TODO: load icon from resources
                icon = {
                    Icon(
                        Icons.Outlined.AutoAwesome,
                        contentDescription = "My Vector Image",
                    )
                })
        ), menuItems = mutableMapOf(
            "reporting-methods" to mutableListOf(
                MenuItem(
                    title = "SOAP",
//                    subtitle = "Subjective, Objective, Assessment, Plan",
                    icon = null
                )
            ),
            "assistant" to mutableListOf(
                MenuItem(title = "Schrijf mijn rapportage",
                    subtitle = "Vertel wat moet worden verwerkt tot rapportage",
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
                    }),

                MenuItem(title = "Schrijf mijn rapportage twee",
                    subtitle = "Vertel wat moet worden verwerkt tot rapportage",
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
            ),
        ), isOptionsMenuOpen = remember { mutableStateOf(true) }
    )
}
