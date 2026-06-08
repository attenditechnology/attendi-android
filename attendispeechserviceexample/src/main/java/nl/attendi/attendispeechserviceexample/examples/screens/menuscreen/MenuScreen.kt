package nl.attendi.attendispeechserviceexample.examples.screens.menuscreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MenuScreen(
    onNavigateToStreaming: () -> Unit,
    onNavigateToSync: () -> Unit,
    onNavigateToSoap: () -> Unit,
    onNavigateToRecorder: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Button(
            onClick = onNavigateToStreaming,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Two AttendiMicrophone [Streaming]")
        }
        Button(
            onClick = onNavigateToSync,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text("One AttendiMicrophone [Sync]")
        }
        Button(
            onClick = onNavigateToSoap,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text("SOAP Template - One AttendiMicrophone [Sync]")
        }
        Button(
            onClick = onNavigateToRecorder,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text("Custom Recorder [Streaming]")
        }
    }
}
