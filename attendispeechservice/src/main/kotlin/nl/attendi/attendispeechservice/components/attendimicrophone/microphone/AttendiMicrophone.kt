package nl.attendi.attendispeechservice.components.attendimicrophone.microphone

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.times
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import nl.attendi.attendispeechservice.R
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorder
import nl.attendi.attendispeechservice.utils.RecordingPermissionStatus
import nl.attendi.attendispeechservice.utils.VerifyAudioPermissionView
import nl.attendi.attendispeechservice.utils.Vibrator.vibrate
import java.util.UUID

/**
 * A customizable microphone button component for recording audio and triggering plugin-based behavior,
 * such as transcription, audio feedback, or contextual actions.
 *
 * This component coordinates with a provided [AttendiRecorder] instance and manages visual state and user
 * interactions.
 *
 * Example usage:
 * ```
 * AttendiMicrophone(
 *   recorder = recorderInstance,
 *   settings = AttendiMicrophoneSettings(
 *     size = 64.dp,
 *     cornerRadius = 16.dp,
 *     colors = AttendiMicrophoneDefaults.colors(baseColor = Color.Red)
 *   )
 * )
 * ```
 *
 * @param recorder The [AttendiRecorder] instance that handles the low-level recording logic.
 * This instance is required and provides the interface for starting and stopping audio capture.
 *
 * @param modifier Optional [Modifier] for positioning and styling this composable. For visual customization,
 * prefer using [settings].
 *
 * @param settings An [AttendiMicrophoneSettings] instance used to configure the appearance, shape, and
 * feedback behavior (e.g. color, size, corner radius) of the microphone button.
 *
 * @param onMicrophoneTap An optional callback invoked after the microphone is activated.
 * Use this to trigger custom logic following a tap interaction.
 *
 * @param onRecordingPermissionDenied An optional callback invoked when the app fails to obtain
 * the required microphone permissions.
 * When [AttendiMicrophoneSettings.showsDefaultPermissionsDeniedDialog] is set to false,
 * use this callback to handle denied access, show alerts, or guide the user to settings.
 */
@Composable
fun AttendiMicrophone(
    recorder: AttendiRecorder,
    modifier: Modifier = Modifier,
    settings: AttendiMicrophoneSettings = AttendiMicrophoneSettings(),
    onMicrophoneTap: () -> Unit = { },
    onRecordingPermissionDenied: () -> Unit = { }
) {
    val coroutineScope = rememberCoroutineScope()
    var showPermanentlyDeniedDialog by remember { mutableStateOf(false) }

    // Creates a unique viewModel for each AttendiMicrophone instance that survives even after a
    // configuration change happens. Having a unique id set to the viewModel, prevents
    // sharing the same viewModel when more than one AttendiMicrophone is loaded on the same
    // composable.
    val uniqueId = rememberSaveable { UUID.randomUUID().toString() }
    val viewModel: AttendiMicrophoneViewModel = viewModel(
        key = uniqueId,
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AttendiMicrophoneViewModel(
                    recorder = recorder,
                    microphoneSettings = settings,
                    onMicrophoneTap = onMicrophoneTap,
                    onRecordingPermissionDenied = onRecordingPermissionDenied
                ) as T
            }
        }
    )

    val microphoneUIState by viewModel.microphoneUIState.collectAsState()

    if (microphoneUIState.shouldVerifyAudioPermission) {
        VerifyAudioPermissionView { result ->
            coroutineScope.launch {
                when (result) {
                    RecordingPermissionStatus.ALREADY_GRANTED -> {
                        viewModel.onAlreadyGrantedRecordingPermissions()
                    }

                    RecordingPermissionStatus.JUST_GRANTED -> {
                        viewModel.onJustGrantedRecordingPermissions()
                    }

                    RecordingPermissionStatus.DENIED_PERMANENTLY -> {
                        viewModel.onDeniedPermissions()
                        if (settings.showsDefaultPermissionsDeniedDialog) {
                            showPermanentlyDeniedDialog = true
                        }
                    }

                    // The user can tap the microphone again to request for permissions.
                    RecordingPermissionStatus.DENIED_TEMPORARILY -> {
                        viewModel.onDeniedPermissions()
                    }
                }
            }
        }
    }

    if (showPermanentlyDeniedDialog) {
        AudioPermissionDeniedPermanentlyDialog(onDismiss = {
            showPermanentlyDeniedDialog = false
        })
    }

    AttendiMicrophoneView(
        settings = settings,
        microphoneUIState = microphoneUIState,
        onTap = {
            viewModel.onTap()
        },
        modifier = modifier
    )
}

@Composable
private fun AudioPermissionDeniedPermanentlyDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity

    vibrate(context)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(context.getString(R.string.noMicrophone_permission_dialog_title)) },
        text = { Text(context.getString(R.string.noMicrophone_permission_dialog_body)) },
        confirmButton = {
            TextButton(onClick = {
                // Open app settings.
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", activity?.packageName, null)
                }
                activity?.startActivity(intent)
                onDismiss()
            }) {
                Text(context.getString(R.string.noMicrophone_permission_dialog_goToSettings_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(context.getString(R.string.noMicrophone_permission_dialog_cancel_button))
            }
        }
    )
}

@Composable
private fun AttendiMicrophoneView(
    settings: AttendiMicrophoneSettings,
    microphoneUIState: AttendiMicrophoneUIState,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val width = settings.size
    val height = settings.size

    val roundedCornerShape = settings.cornerRadius?.let {
        RoundedCornerShape(it, it, it, it)
    } ?: RoundedCornerShape(percent = 50)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .width(width)
            .height(height)
            .clip(roundedCornerShape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onTap() },
            contentAlignment = Alignment.Center
        ) {
            when (microphoneUIState.state) {
                AttendiMicrophoneState.Idle -> NotStartedRecordingView(settings)
                AttendiMicrophoneState.Loading -> LoadingBeforeRecordingView(settings)
                AttendiMicrophoneState.Recording -> RecordingView(
                    settings,
                    microphoneUIState.animatedMicrophoneFillLevel
                )

                AttendiMicrophoneState.Processing -> ProcessingView(settings)
            }
        }
    }
}

@Composable
private fun NotStartedRecordingView(
    settings: AttendiMicrophoneSettings
) {
    val foregroundColor = settings.colors.inactiveForegroundColor
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(settings.colors.inactiveBackgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.microphone),
            contentDescription = context.getString(R.string.microphone_notRecording_title),
            modifier = Modifier
                .fillMaxSize(0.5f)
                .aspectRatio(1f),
            tint = foregroundColor
        )
    }
}

@Composable
private fun LoadingBeforeRecordingView(
    settings: AttendiMicrophoneSettings
) {
    val backgroundColor = settings.colors.inactiveBackgroundColor
    val foregroundColor = settings.colors.inactiveForegroundColor
    val context = LocalContext.current

    val infiniteTransition = rememberInfiniteTransition(label = "attendiLoadingTransition")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(
            animation = tween(800, easing = CubicBezierEasing(0.715f, 0.15f, 0.175f, 0.84f)),
            repeatMode = RepeatMode.Restart
        ), label = ""
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.loading),
            contentDescription = context.getString(R.string.microphone_loading_title),
            tint = foregroundColor,
            modifier = Modifier
                .fillMaxSize(0.33f)
                .graphicsLayer(rotationZ = rotation),
        )
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun RecordingView(
    settings: AttendiMicrophoneSettings,
    animatedMicrophoneFillLevel: Double
) {
    val backgroundColor = settings.colors.activeBackgroundColor
    val foregroundColor = settings.colors.activeForegroundColor
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize(0.5f)
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            val startYPercentage = 0.61
            val endYPercentage = 0.14

            val rawFillPercentage: Double =
                startYPercentage - (startYPercentage - endYPercentage) * animatedMicrophoneFillLevel

            // Preventing a crash if rawFillPercentage returns NaN, Infinity or too large/small for Float.
            val safeFillPercentage: Float =
                if (rawFillPercentage.isFinite()) rawFillPercentage.toFloat() else 0f

            // Make the animation a bit smoother by tweening between the current and the target value.
            val animatedMicrophoneFillPercentage: Float by animateFloatAsState(
                targetValue = safeFillPercentage,
                animationSpec = tween(150),
                label = "attendiMicrophoneFillPercentage"
            )

            // Microphone volume fill.
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = foregroundColor,
                    start = Offset((0.5 * maxWidth).toPx(), (0.61 * maxHeight).toPx()),
                    end = Offset(
                        (0.5 * maxWidth).toPx(),
                        (animatedMicrophoneFillPercentage * maxHeight).toPx()
                    ),
                    strokeWidth = (0.33 * maxWidth).toPx(),
                )
            }

            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.microphone),
                contentDescription = context.getString(R.string.microphone_recording_title),
                modifier = Modifier.fillMaxSize(),
                tint = foregroundColor
            )
        }
    }
}

@Composable
private fun ProcessingView(
    settings: AttendiMicrophoneSettings
) {
    val backgroundColor = settings.colors.activeBackgroundColor
    val foregroundColor = settings.colors.activeForegroundColor
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .semantics {
                contentDescription = context.getString(R.string.microphone_processing_title)
            }
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        ProcessingAnimation(foregroundColor)
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun AnimatedRectangle(index: Int, color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "attendiInfiniteTransition")

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.1f, targetValue = 1f, animationSpec = infiniteRepeatable(
            tween(
                durationMillis = 800, easing = FastOutSlowInEasing, delayMillis = 100 * index
            ), repeatMode = RepeatMode.Reverse
        ), label = "attendiInfiniteTransition$index"
    )

    BoxWithConstraints {
        val width = min(0.1 * maxWidth, 3.dp)
        val height = 0.4 * maxHeight

        Box(
            modifier = Modifier
                .size(width = width, height = height * scale)
                .background(color)
        )
    }
}

@Composable
private fun ProcessingAnimation(foregroundColor: Color) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.6f)
            .animateContentSize()
    ) {
        for (index in 0 until 5) {
            AnimatedRectangle(index, foregroundColor)
        }
    }
}

@Preview
@Composable
private fun MicrophoneViewPreview() {
    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        AttendiMicrophoneView(
            settings = AttendiMicrophoneSettings(
                colors = AttendiMicrophoneDefaults.colors(baseColor = Color.Red),
                size = 80.dp,
                cornerRadius = 10.dp
            ),
            microphoneUIState = AttendiMicrophoneUIState(AttendiMicrophoneState.Idle, 1.0),
            onTap = { }
        )
    }
}

@Preview
@Composable
private fun ProcessingViewPreview() {
    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        ProcessingView(settings = AttendiMicrophoneSettings())
    }
}

@Preview
@Composable
private fun LoadingBeforeRecordingPreview() {
    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        LoadingBeforeRecordingView(settings = AttendiMicrophoneSettings())
    }
}

@Preview
@Composable
private fun RecordingViewPreview() {
    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        RecordingView(settings = AttendiMicrophoneSettings(), animatedMicrophoneFillLevel = 0.5)
    }
}

@Preview
@Composable
private fun NotStartedRecordingViewPreview() {
    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        NotStartedRecordingView(settings = AttendiMicrophoneSettings())
    }
}