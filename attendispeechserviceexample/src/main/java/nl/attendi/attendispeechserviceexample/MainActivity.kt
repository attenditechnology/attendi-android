package nl.attendi.attendispeechserviceexample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import nl.attendi.attendispeechserviceexample.examples.screens.onemicrophonesyncscreen.OneMicrophoneSyncScreen
import nl.attendi.attendispeechserviceexample.examples.screens.recorderscreen.RecorderStreamingScreen
import nl.attendi.attendispeechserviceexample.examples.screens.soapscreen.SoapScreen
import nl.attendi.attendispeechserviceexample.examples.screens.twomicrophonesstreamingscreen.TwoMicrophonesScreenStreamingScreen
import nl.attendi.attendispeechserviceexample.ui.theme.AttendiSpeechServiceExampleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AttendiSpeechServiceExampleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    ExampleApp()
                }
            }
        }
    }
}

private object InternalRoute {
    const val MAIN_ROUTE = "ExampleApp"

    const val TWO_MICROPHONES_STREAMING = MAIN_ROUTE + "Streaming"
    const val ONE_MICROPHONE_SYNC = MAIN_ROUTE + "OneMicrophoneSync"
    const val SOAP = MAIN_ROUTE + "SOAP"
    const val RECORDER = MAIN_ROUTE + "Recorder"
}

@Composable
fun ExampleApp() {
    val rootNavController: NavHostController = rememberNavController()
    var currentScreen by rememberSaveable { mutableStateOf(InternalRoute.TWO_MICROPHONES_STREAMING) }

    Column(
        modifier = Modifier
            .systemBarsPadding()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (currentScreen != InternalRoute.TWO_MICROPHONES_STREAMING) {
                Button(onClick = {
                    currentScreen = InternalRoute.TWO_MICROPHONES_STREAMING
                    rootNavController.apply {
                        popBackStack()
                        navigate(currentScreen)
                    }
                }, modifier = Modifier.width(IntrinsicSize.Min)) {
                    Text("Stream")
                }
            }

            if (currentScreen != InternalRoute.ONE_MICROPHONE_SYNC) {
                Button(onClick = {
                    currentScreen = InternalRoute.ONE_MICROPHONE_SYNC
                    rootNavController.apply {
                        popBackStack()
                        navigate(currentScreen)
                    }
                }) {
                    Text("Sync")
                }
            }

            if (currentScreen != InternalRoute.SOAP) {
                Button(onClick = {
                    currentScreen = InternalRoute.SOAP
                    rootNavController.apply {
                        popBackStack()
                        navigate(currentScreen)
                    }
                }, modifier = Modifier.width(IntrinsicSize.Min)) {
                    Text("SOAP")
                }
            }

            if (currentScreen != InternalRoute.RECORDER) {
                Button(onClick = {
                    currentScreen = InternalRoute.RECORDER
                    rootNavController.apply {
                        popBackStack()
                        navigate(currentScreen)
                    }
                }, modifier = Modifier.width(IntrinsicSize.Min)) {
                    Text("REC")
                }
            }
        }

        NavHost(
            navController = rootNavController,
            startDestination = currentScreen,
            route = InternalRoute.MAIN_ROUTE
        ) {
            composable(route = InternalRoute.TWO_MICROPHONES_STREAMING) {
                TwoMicrophonesScreenStreamingScreen()
            }
            composable(route = InternalRoute.ONE_MICROPHONE_SYNC) {
                OneMicrophoneSyncScreen()
            }
            composable(route = InternalRoute.SOAP) {
                SoapScreen()
            }
            composable(route = InternalRoute.RECORDER) {
                RecorderStreamingScreen()
            }
        }
    }
}