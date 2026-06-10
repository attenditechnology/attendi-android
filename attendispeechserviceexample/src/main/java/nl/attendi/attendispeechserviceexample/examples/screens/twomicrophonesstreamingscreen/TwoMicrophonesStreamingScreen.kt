package nl.attendi.attendispeechserviceexample.examples.screens.twomicrophonesstreamingscreen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwoMicrophonesScreenStreamingScreen(
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val applicationContext = LocalContext.current.applicationContext
    val viewModel: TwoMicrophonesStreamingScreenViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TwoMicrophonesStreamingScreenViewModel(
                    applicationContext = applicationContext
                ) as T
            }
        }
    )
    val model: TwoMicrophonesStreamingScreenModel by viewModel.model.collectAsState()

    // Collect the one-shot navigate-up event emitted by the ViewModel after both recorders
    // have finished releasing. Using the viewModel instance as the key ensures the effect
    // restarts if the ViewModel is replaced (e.g. during testing).
    LaunchedEffect(viewModel) {
        viewModel.navigateUpEvent.collect { onNavigateUp() }
    }

    // Intercept the system back gesture so it follows the same release-then-navigate path
    // as the TopAppBar back button. Without this, the system back would pop the nav stack
    // immediately, skipping recorder cleanup.
    BackHandler { viewModel.onNavigateUp() }

    Column {
        CenterAlignedTopAppBar(
            title = {
                Text("Two Microphones Streaming")
            },
            navigationIcon = {
                IconButton(onClick = { viewModel.onNavigateUp() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate up"
                    )
                }
            }
        )
        TwoMicrophonesScreenStreamingView(
            model = model,
            modifier = modifier
        )
    }
}
