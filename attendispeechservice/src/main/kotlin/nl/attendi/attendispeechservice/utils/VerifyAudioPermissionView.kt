package nl.attendi.attendispeechservice.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Represents the current status of the audio recording permission.
 */
enum class RecordingPermissionStatus {
    /** Permission was just granted by the user. */
    JUST_GRANTED,

    /** Permission was already granted previously. */
    ALREADY_GRANTED,

    /** Permission was denied permanently (user selected "Don't ask again"). */
    DENIED_PERMANENTLY,

    /** Permission was denied temporarily (user can be prompted again). */
    DENIED_TEMPORARILY
}

/**
 * A Compose UI component that requests and verifies the RECORD_AUDIO permission.
 *
 * This component automatically launches the permission request when the lifecycle
 * owner enters the `ON_START` state. The result of the permission request is reported
 * via the [onComplete] callback, passing a [RecordingPermissionStatus] to indicate
 * the outcome.
 *
 * @param onComplete Callback invoked once the permission request flow is complete,
 * with the corresponding [RecordingPermissionStatus].
 */
@Composable
fun VerifyAudioPermissionView(
    onComplete: (RecordingPermissionStatus) -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity
    val permission = Manifest.permission.RECORD_AUDIO

    val isRecordingPermissionGranted =
        ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    if (isRecordingPermissionGranted) {
        onComplete(RecordingPermissionStatus.ALREADY_GRANTED)
        return
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onComplete(RecordingPermissionStatus.JUST_GRANTED)
            return@rememberLauncherForActivityResult
        }

        val shouldShowRationale =
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        onComplete(if (shouldShowRationale) RecordingPermissionStatus.DENIED_TEMPORARILY else RecordingPermissionStatus.DENIED_PERMANENTLY)
    }

    /**
     * Launches a permission request when the [LocalLifecycleOwner] enters the STARTED state, using a
     * [LifecycleEventObserver] registered inside a [DisposableEffect].
     *
     * We observe the lifecycleOwner inside a DisposableEffect to:
     * Trigger side effects only when the composable enters the screen or becomes active.
     * Clean up the observer automatically when the composable leaves the screen.
     * Use lifecycleOwner as a key so that if the screen changes (e.g., navigation), the observer is reset to avoid leaks or stale context.
     *
     * Permissions must be requested from an active UI state. Launching the permission dialog:
     * On ON_START ensures the app is visible and interactive, avoiding crashes or ignored requests.
     */
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(key1 = lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                permissionLauncher.launch(permission)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}