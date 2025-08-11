package nl.attendi.attendispeechserviceexample.examples.screens.recorderscreen

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecorderStreamingScreen(
    modifier: Modifier = Modifier
) {
    val applicationContext = LocalContext.current.applicationContext
    val viewModel: RecorderStreamingScreenViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RecorderStreamingScreenViewModel(
                    applicationContext = applicationContext
                ) as T
            }
        }
    )
    val model: RecorderStreamingScreenModel by viewModel.model.collectAsState()

    Column {
        CenterAlignedTopAppBar(
            title = {
                Text("Recorder")
            }
        )
        RecorderStreamingScreenView(
            model = model,
            modifier = modifier
        )
    }
}