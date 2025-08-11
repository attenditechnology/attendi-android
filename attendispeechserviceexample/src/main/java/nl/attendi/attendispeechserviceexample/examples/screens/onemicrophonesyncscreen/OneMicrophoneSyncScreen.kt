package nl.attendi.attendispeechserviceexample.examples.screens.onemicrophonesyncscreen

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneMicrophoneSyncScreen(
    modifier: Modifier = Modifier
) {
    Column {
        CenterAlignedTopAppBar(
            title = {
                Text("One Microphone Sync")
            }
        )
        OneMicrophoneSyncScreenView(
            modifier = modifier
        )
    }
}