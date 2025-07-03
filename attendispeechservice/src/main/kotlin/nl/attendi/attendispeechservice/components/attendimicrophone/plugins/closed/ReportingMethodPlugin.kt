/// Copyright 2023 Attendi Technology B.V.
///
/// Licensed according to LICENSE.txt in this folder.

package nl.attendi.attendispeechservice.components.attendimicrophone.plugins.closed

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import nl.attendi.attendispeechservice.R
import nl.attendi.attendispeechservice.data.client.ModelType
import nl.attendi.attendispeechservice.data.client.TranscribeAPIConfig
import nl.attendi.attendispeechservice.components.attendimicrophone.AttendiMicrophone
import nl.attendi.attendispeechservice.components.attendimicrophone.AttendiMicrophoneState
import nl.attendi.attendispeechservice.components.attendimicrophone.MenuGroup
import nl.attendi.attendispeechservice.components.attendimicrophone.MenuItem
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.AttendiMicrophonePlugin
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.AttendiTranscribePlugin
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

/**
 * Healthcare professionals use certain reporting methods to give their medical reports more structure.
 * This plugin adds functionality for reporting using reporting methods that can be divided into clear steps
 * such as SOAP and SOEP.
 */
class ReportingMethodPlugin : AttendiMicrophonePlugin {
    override fun activate(state: AttendiMicrophoneState) {
        state.addMenuGroup(
            MenuGroup(id = "reporting-methods",
                title = state.context.getString(R.string.reporting_method_title),
                priority = 1,
                icon = {
                    Image(
                        painter = painterResource(R.drawable.icon_numeration),
                        contentDescription = state.context.getString(R.string.reporting_method_content_description),
                        modifier = Modifier
                    )
                })
        )

        state.addMenuItem(
            groupId = "reporting-methods",
            MenuItem(title = state.context.getString(R.string.soep_title), icon = {
                Image(
                    painter = painterResource(R.drawable.icon_soep),
                    contentDescription = state.context.getString(R.string.soep_display_name),
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = Color(0xFFD7D5D5),
                            shape = RoundedCornerShape(size = 4.dp)
                        )
                        .padding(6.dp)
                )
            }, action = {
                state.openBottomSheet {
                    LinearReportingMethodView(
                        viewModel = LinearReportingMethodViewModel(
                            createSoepReportingMethod(state.context)
                        ), onClose = {
                            state.closeBottomSheet()
                        }, state = state
                    ) {
                        state.onResult(it)
                        state.closeBottomSheet()
                    }
                }
            })
        )

        state.addMenuItem(
            groupId = "reporting-methods",
            MenuItem(title = state.context.getString(R.string.soap_title), icon = {
                Image(
                    painter = painterResource(R.drawable.icon_soap),
                    contentDescription = state.context.getString(R.string.soap_display_name),
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = Color(0xFFD7D5D5),
                            shape = RoundedCornerShape(size = 4.dp)
                        )
                        .padding(6.dp)
                )
            }, action = {
                state.openBottomSheet {
                    LinearReportingMethodView(
                        viewModel = LinearReportingMethodViewModel(
                            soapReportingMethod(state.context)
                        ), onClose = {
                            state.closeBottomSheet()
                        }, state = state
                    ) {
                        state.onResult(it)
                        state.closeBottomSheet()
                    }
                }
            })
        )
    }
}


@Composable
fun LinearReportingMethodView(
    viewModel: LinearReportingMethodViewModel,
    onClose: () -> Unit = { },
    state: AttendiMicrophoneState,
    onResult: (String) -> Unit = { },
) {
    val uiState by viewModel.uiState.collectAsState()

    var showConfirmCancelDialog by remember { mutableStateOf(false) }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 16.dp)
        ) {
            // Navigation buttons (left column)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                uiState.reportingMethod.steps.forEachIndexed { index, step ->
                    Button(
                        onClick = { viewModel.goToStep(index) },
                        shape = CircleShape,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (uiState.currentStep == index) state.settings.color else Color.LightGray
                        ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (viewModel.stepHasText(index)) state.settings.color.copy(
                                alpha = 0.1f
                            ) else Color.Transparent, contentColor = Color.Black
                        ),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(60.dp)
                    ) {
                        Text(text = step.symbol ?: "", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            // Step name, prompt, and editor (middle column)
            Column(
                modifier = Modifier.weight(5f)
            ) {
                // Step name
                Text(
                    text = viewModel.currentMethodStepName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )

                // Step prompt
                viewModel.currentMethodStepPrompt?.let {
                    Text(
                        text = it, style = MaterialTheme.typography.bodyMedium, color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Editor
                TextField(
                    value = viewModel.currentStepText,
                    onValueChange = { viewModel.setCurrentStepText(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(1.dp, Color.Gray, shape = RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
            }

            // Close button, microphone, and checkmark button (right column)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Exit button at the top
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(
                        onClick = { showConfirmCancelDialog = true },
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(color = Color(237, 237, 237))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            modifier = Modifier.size(24.dp),
                            tint = Color(89, 89, 89)
                        )
                    }
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    AttendiMicrophone(
                        // size = 58.dp,
                        plugins = listOf(
                            AttendiTranscribePlugin(
                                TranscribeAPIConfig(
                                    modelType = ModelType.DistrictCare,
                                    userAgent = "Android",
                                    customerKey = "ck_<key>",
                                    apiURL = "https://sandbox.api.attendi.nl",
                                    unitId = "unitId",
                                    userId = "userId",
                                )
                            )
                        ),
                        onResult = { viewModel.setCurrentStepText(it) },
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    IconButton(
                        onClick = { onResult(LinearReportingMethodOutputFormatter().format(uiState)) },
                        modifier = Modifier
                            .border(1.dp, state.settings.color, CircleShape)
                            .size(58.dp)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = stringResource(R.string.checkmark),
                            tint = state.settings.color
                        )
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(bottom = 32.dp)
                .width(LocalConfiguration.current.screenWidthDp.dp),
        ) {
            // Exit button
            Row {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(
                        onClick = { showConfirmCancelDialog = true },
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(color = Color(237, 237, 237))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            modifier = Modifier.size(24.dp),
                            tint = Color(89, 89, 89)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation buttons
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                uiState.reportingMethod.steps.forEachIndexed { index, step ->
                    Button(
                        onClick = { viewModel.goToStep(index) },
                        shape = CircleShape,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (uiState.currentStep == index) state.settings.color else Color.LightGray
                        ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (viewModel.stepHasText(index)) state.settings.color.copy(
                                alpha = 0.1f
                            ) else Color.Transparent, contentColor = Color.Black
                        ),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(60.dp)
                    ) {
                        Text(text = step.symbol ?: "", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step name
            Text(
                text = viewModel.currentMethodStepName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )

            // Step prompt
            viewModel.currentMethodStepPrompt?.let {
                Text(
                    text = it, style = MaterialTheme.typography.bodyMedium, color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Editor
            TextField(
                value = viewModel.currentStepText,
                onValueChange = { viewModel.setCurrentStepText(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, Color.Gray, shape = RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.weight(1f)) // This acts as the first button
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    AttendiMicrophone(
                        // size = 58.0,
                        plugins = listOf(
                            AttendiTranscribePlugin(
                                TranscribeAPIConfig(
                                    modelType = ModelType.DistrictCare,
                                    userAgent = "Android",
                                    customerKey = "ck_<key>",
                                    apiURL = "https://sandbox.api.attendi.nl",
                                    unitId = "unitId",
                                    userId = "userId",
                                )
                            )
                        ),
                        onResult = { viewModel.setCurrentStepText(it) },
                    )
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    IconButton(
                        onClick = { onResult(LinearReportingMethodOutputFormatter().format(uiState)) },
                        modifier = Modifier.border(1.dp, state.settings.color, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = stringResource(R.string.checkmark),
                            tint = state.settings.color
                        )
                    }
                }
            }
        }
    }

    if (showConfirmCancelDialog) {
        AlertDialog(onDismissRequest = { showConfirmCancelDialog = false },
            title = { Text(stringResource(R.string.cancel_reporting_method_dialog_body)) },
            text = { Text(stringResource(R.string.cancel_body)) },
            dismissButton = {
                Button(onClick = { showConfirmCancelDialog = false }) {
                    Text(stringResource(R.string.no))
                }
            },
            confirmButton = {
                Button(onClick = {
                    onClose()
                    showConfirmCancelDialog = false
                }) {
                    Text(stringResource(R.string.yes))
                }
            })
    }
}

class LinearReportingMethod(
    val name: String,
    val displayName: String,
    val steps: List<LinearReportingMethodStep>,
    val description: String? = null
)

class LinearReportingMethodStep(
    val name: String,
    val symbol: String? = null,
    val prompt: String? = null,
    val description: String? = null
)

fun createSoepReportingMethod(context: Context): LinearReportingMethod {
    return LinearReportingMethod(
        name = context.getString(R.string.soep_title),
        displayName = context.getString(R.string.soep_display_name),
        description = context.getString(R.string.soep_description),
        steps = listOf(
            LinearReportingMethodStep(
                symbol = context.getString(R.string.subjective_symbol),
                name = context.getString(R.string.subjective_title),
                prompt = context.getString(R.string.subjective_prompt),
            ),
            LinearReportingMethodStep(
                symbol = context.getString(R.string.objective_symbol),
                name = context.getString(R.string.objective_title),
                prompt = context.getString(R.string.objective_prompt),
            ),
            LinearReportingMethodStep(
                symbol = context.getString(R.string.evaluation_symbol),
                name = context.getString(R.string.evaluation_title),
                prompt = context.getString(R.string.evaluation_prompt),
            ),
            LinearReportingMethodStep(
                symbol = context.getString(R.string.plan_symbol),
                name = context.getString(R.string.plan_title),
                prompt = context.getString(R.string.plan_prompt),
            ),
        )
    )
}

fun soapReportingMethod(context: Context): LinearReportingMethod {
    return LinearReportingMethod(
        name = context.getString(R.string.soap_title),
        displayName = context.getString(R.string.soap_display_name),
        description = context.getString(R.string.soap_description),
        steps = listOf(
            LinearReportingMethodStep(
                symbol = context.getString(R.string.subjective_symbol),
                name = context.getString(R.string.subjective_title),
                prompt = context.getString(R.string.subjective_prompt),
            ),
            LinearReportingMethodStep(
                symbol = context.getString(R.string.objective_symbol),
                name = context.getString(R.string.objective_title),
                prompt = context.getString(R.string.objective_prompt),
            ),
            LinearReportingMethodStep(
                symbol = context.getString(R.string.analysis_symbol),
                name = context.getString(R.string.analysis_title),
                prompt = context.getString(R.string.analysis_prompt),
            ),
            LinearReportingMethodStep(
                symbol = context.getString(R.string.plan_symbol),
                name = context.getString(R.string.plan_title),
                prompt = context.getString(R.string.plan_prompt),
            ),
        )
    )
}

fun createSessionId(): String {
    return UUID.randomUUID().toString()
}

/**
 * Data class that represents linear reporthing method state
 */
data class LinearReportingMethodUIState(
    val currentStep: Int = 0,
    var stepTexts: List<String>,
    var sessionIds: List<String>,
    private var stepStartedEditingOrTranscribing: List<Boolean>,
    val reportingMethod: LinearReportingMethod,
    val nSteps: Int = reportingMethod.steps.size,
)

class LinearReportingMethodViewModel(val reportingMethod: LinearReportingMethod) : ViewModel() {
    private val _uiState = MutableStateFlow(
        LinearReportingMethodUIState(reportingMethod = reportingMethod,
            stepTexts = List(reportingMethod.steps.size) { "" },
            sessionIds = List(reportingMethod.steps.size) { createSessionId() },
            stepStartedEditingOrTranscribing = List(reportingMethod.steps.size) { false })
    )
    val uiState: StateFlow<LinearReportingMethodUIState> = _uiState.asStateFlow()

    fun goToStep(step: Int) {
        _uiState.value = _uiState.value.copy(currentStep = step)
    }

    fun setStepText(stepIndex: Int, value: String) {
        checkStepIndexInRange(stepIndex)
        val texts = _uiState.value.stepTexts.toMutableList()
        texts[stepIndex] = value
        _uiState.value = _uiState.value.copy(stepTexts = texts)
    }

    fun setCurrentStepText(value: String) {
        setStepText(_uiState.value.currentStep, value)
    }

    fun resetCurrentStepText() {
        setCurrentStepText("")
    }

    // TODO: sessionIds per step
//    fun stepSessionId(stepIndex: Int, sessionId: String) {
//        checkStepIndexInRange(stepIndex)
//        sessionIds[stepIndex] = sessionId
//    }
//
//    fun getStepSessionId(stepIndex: Int): String {
//        checkStepIndexInRange(stepIndex)
//        return sessionIds[stepIndex]
//    }
//
//    fun resetCurrentStepSessionId() {
//        val sessionId = createSessionId()
//        setCurrentStepSessionId(sessionId)
//    }
//
//    fun setCurrentStepSessionId(sessionId: String) {
//        stepSessionId(currentStep, sessionId)
//    }

    private fun clampStep(stepIndex: Int): Int {
        return clamp(stepIndex, 0, nSteps - 1)
    }

    fun addParagraphToStep(stepIndex: Int, paragraph: String) {
        val stepText = getStepText(stepIndex)
        val updatedTextValue = addParagraph(stepText, paragraph)
        setStepText(stepIndex, updatedTextValue)
    }

    fun addParagraphToCurrentStep(paragraph: String) {
        addParagraphToStep(_uiState.value.currentStep, paragraph)
    }

    fun getStepName(step: Int): String {
        if (step < 0 || step >= nSteps) {
            throw IndexOutOfBoundsException("step index should be in interval [0, ${nSteps - 1}]")
        }
        return reportingMethod.steps[step].name
    }

    fun getStepText(stepIndex: Int): String {
        checkStepIndexInRange(stepIndex)
        return _uiState.value.stepTexts[stepIndex]
    }

    val currentStepText: String
        get() = getStepText(_uiState.value.currentStep)

    fun stepHasText(index: Int): Boolean {
        return getStepText(index).length > 0
    }

    val currentStepSessionId: String
        get() = getSessionId(_uiState.value.currentStep)

    fun getSessionId(stepIndex: Int): String {
        checkStepIndexInRange(stepIndex)
        return _uiState.value.sessionIds[stepIndex]
    }

    val currentStepHasText: Boolean
        get() = getStepText(_uiState.value.currentStep).length > 0

    val currentMethodStepName: String
        get() = reportingMethod.steps[_uiState.value.currentStep].name

    val currentMethodStepPrompt: String?
        get() = reportingMethod.steps[_uiState.value.currentStep].prompt

    val nSteps: Int
        get() = _uiState.value.stepTexts.size

    val currentStepIsFinalStep: Boolean
        get() = _uiState.value.currentStep == nSteps - 1

    fun resetCurrentStep() {
//        resetCurrentStepSessionId()
        resetCurrentStepText()
    }

//    val currentStepStartedEditingOrTranscribing: Boolean
//        get() = startedEditingOrTranscribing(_uiState.value.currentStep)

//    fun startedEditingOrTranscribing(step: Int): Boolean {
//        checkStepIndexInRange(step)
//        return _uiState.value.stepStartedEditingOrTranscribing[step]
//    }

//    fun setCurrentStepStartedEditingOrTranscribing(value: Boolean) {
//        setStartedEditingOrTranscribing(currentStep, value)
//    }

//    fun setStartedEditingOrTranscribing(step: Int, value: Boolean) {
//        checkStepIndexInRange(step)
//        stepStartedEditingOrTranscribing[step] = value
//    }
//
//    fun resetCurrentStepStartedEditingOrTranscribing() {
//        resetStartedEditingOrTranscribing(currentStep)
//    }
//
//    fun resetStartedEditingOrTranscribing(step: Int) {
//        setStartedEditingOrTranscribing(step, false)
//    }

    private fun checkStepIndexInRange(stepIndex: Int) {
        if (stepIndex < 0 || stepIndex >= nSteps) {
            throw IndexOutOfBoundsException("step index should be in interval [0, ${nSteps - 1}]")
        }
    }
}

fun addParagraph(text: String, addedText: String): String {
    val addedNewlines: String = getAddedNewlines(text, addedText)

    return text + addedNewlines + addedText
}

fun getAddedNewlines(text: String, addedText: String) =
    if (text.isEmpty() || addedText.isEmpty()) "" else "\n\n"

fun clamp(value: Int, minValue: Int, maxValue: Int): Int {
    return min(max(value, minValue), maxValue)
}


/**
 * A formatter for the output text of a [LinearReportingMethod].
 *
 * `formatter.format(state)` will for example output the following:
 * ```
 * Subjectief:
 * This is some text
 *
 * Objectief:
 * This is some other text
 * ```
 *
 * if the user has filled in the two steps `Subjectief` and `Objectief`.
 */
class LinearReportingMethodOutputFormatter(
    val reportHeaderFormatter: (String) -> String = { "" },
    val stepHeaderFormatter: (String) -> String = { stepHeader -> "$stepHeader:\n" }
) {
    fun format(methodState: LinearReportingMethodUIState): String {
        var result = reportHeaderFormatter(methodState.reportingMethod.name)
        if (result.isNotEmpty()) result = addParagraphBreakToResult(result)

        for (i in 0 until methodState.nSteps) {
            val stepHeaderAndText = getStepHeaderAndText(methodState, i) ?: continue

            result += stepHeaderAndText

            if (!isFinalReportStep(i, methodState)) result = addParagraphBreakToResult(result)
        }

        return result
    }

    private fun isFinalReportStep(step: Int, methodState: LinearReportingMethodUIState) =
        isFinalStep(step, methodState.nSteps) || isLastStepWithText(
            step, methodState
        )

    private fun isFinalStep(step: Int, nSteps: Int) = step == nSteps - 1

    private fun isLastStepWithText(step: Int, methodState: LinearReportingMethodUIState) =
        step == getLastStepWithText(methodState)

    private fun getLastStepWithText(methodState: LinearReportingMethodUIState): Int {
        for (i in methodState.nSteps - 1 downTo 0) {
            if (methodState.stepTexts[i].isNotEmpty()) return i
        }
        return 0
    }

    private fun addParagraphBreakToResult(result: String) = result + "\n\n"

    private fun getStepHeaderAndText(
        methodState: LinearReportingMethodUIState, step: Int
    ): String? {
        var result = ""

        val stepHeader = methodState.reportingMethod.steps[step].name
        val stepText = methodState.stepTexts[step]

        if (stepText.isEmpty()) return null

        result += _getReportingMethodStepHeader(stepHeader)
        result += stepText

        return result
    }

    private fun _getReportingMethodStepHeader(stepDisplayName: String?): String {
        if (stepDisplayName == null) return ""

        return stepHeaderFormatter(stepDisplayName)
    }
}


// @Preview(showBackground = true)
// @Composable
// fun DefaultPreview() {
//     LinearReportingMethodView(
//         viewModel = LinearReportingMethodViewModel(createSoepReportingMethod(LocalContext.current))
//     )
// }