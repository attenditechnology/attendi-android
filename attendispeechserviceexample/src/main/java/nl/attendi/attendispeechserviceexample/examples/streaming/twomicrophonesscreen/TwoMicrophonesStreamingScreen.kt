package nl.attendi.attendispeechserviceexample.examples.streaming.twomicrophonesscreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun TwoMicrophonesScreenStreamingScreen(
    modifier: Modifier = Modifier
) {
    val viewModel: TwoMicrophonesStreamingScreenViewModel = viewModel()
    val model: TwoMicrophonesStreamingScreenModel by viewModel.model.collectAsState()

    TwoMicrophonesScreenStreamingView(
        model = model,
        modifier = modifier
    )
}