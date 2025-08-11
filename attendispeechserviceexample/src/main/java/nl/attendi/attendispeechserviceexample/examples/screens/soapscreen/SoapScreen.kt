package nl.attendi.attendispeechserviceexample.examples.screens.soapscreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SoapScreen(
    modifier: Modifier = Modifier
) {
    val applicationContext = LocalContext.current.applicationContext
    val viewModel: SoapScreenViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SoapScreenViewModel(
                    applicationContext = applicationContext
                ) as T
            }
        }
    )
    val model: SoapScreenModel by viewModel.model.collectAsState()

    SoapScreenView(
        model = model,
        modifier = modifier
    )
}