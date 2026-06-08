package nl.attendi.attendispeechserviceexample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import nl.attendi.attendispeechserviceexample.examples.screens.menuscreen.MenuScreen
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

    const val MENU = MAIN_ROUTE + "Menu"
    const val TWO_MICROPHONES_STREAMING = MAIN_ROUTE + "Streaming"
    const val ONE_MICROPHONE_SYNC = MAIN_ROUTE + "OneMicrophoneSync"
    const val SOAP = MAIN_ROUTE + "SOAP"
    const val RECORDER = MAIN_ROUTE + "Recorder"
}

@Composable
fun ExampleApp() {
    val rootNavController: NavHostController = rememberNavController()

    NavHost(
        navController = rootNavController,
        startDestination = InternalRoute.MENU,
        route = InternalRoute.MAIN_ROUTE
    ) {
        composable(route = InternalRoute.MENU) {
            MenuScreen(
                onNavigateToStreaming = {
                    rootNavController.navigate(InternalRoute.TWO_MICROPHONES_STREAMING) {
                        popUpTo(InternalRoute.MENU) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onNavigateToSync = {
                    rootNavController.navigate(InternalRoute.ONE_MICROPHONE_SYNC) {
                        popUpTo(InternalRoute.MENU) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onNavigateToSoap = {
                    rootNavController.navigate(InternalRoute.SOAP) {
                        popUpTo(InternalRoute.MENU) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onNavigateToRecorder = {
                    rootNavController.navigate(InternalRoute.RECORDER) {
                        popUpTo(InternalRoute.MENU) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(route = InternalRoute.TWO_MICROPHONES_STREAMING) {
            TwoMicrophonesScreenStreamingScreen(
                onNavigateUp = { rootNavController.navigateUp() }
            )
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
