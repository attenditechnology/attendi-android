package nl.attendi.attendispeechserviceexample.examples.screens.streamingscreen

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
fun TwoMicrophonesScreenStreamingScreen(
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

    Column {
        CenterAlignedTopAppBar(
            title = {
                Text("Streaming")
            }
        )
        TwoMicrophonesScreenStreamingView(
            model = model,
            modifier = modifier
        )
    }
}