/// Copyright 2023 Attendi Technology B.V.
/// 
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
/// 
///     http://www.apache.org/licenses/LICENSE-2.0
/// 
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.

package nl.attendi.attendispeechservice.components.attendimicrophone

import AttendiMicrophoneColors
import AttendiMicrophoneDefaults
import MicrophoneOptionsVariant
import MicrophoneSettings
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.times
import androidx.lifecycle.Lifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.*
import nl.attendi.attendispeechservice.R
import nl.attendi.attendispeechservice.audio.AUDIO_SAMPLE_RATE
import nl.attendi.attendispeechservice.audio.AttendiRecorder
import nl.attendi.attendispeechservice.audio.pcmToWav
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.AttendiMicrophonePlugin
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.AudioNotificationPlugin
import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.VolumeFeedbackPlugin
import java.io.File
import java.util.*

enum class MicrophoneUIState {
    NotStartedRecording, LoadingBeforeRecording, Recording, Processing
}

private const val START_RECORDING_DELAY_MILLISECONDS: Long = 500
private const val STOP_RECORDING_DELAY_MILLISECONDS: Long = 200

private const val AUDIO_BUFFER_FILE_PREFIX = "attendi_recorder_samples_"

// Delete audio files that are older than 20 minutes. These are files that were not properly
// cleaned up previously, for example when an error occurred while recording.
private const val OLD_AUDIO_FILE_THRESHOLD_MINUTES = 20

val LocalMicrophoneState =
    compositionLocalOf<AttendiMicrophoneState> { error("No MicrophoneState found!") }

val LocalMicrophoneUIState =
    compositionLocalOf<MicrophoneUIState> { error("No MicrophoneUIState found!") }

/**
 * The [AttendiMicrophone] is a button that can be used to record audio and then perform tasks
 * with that audio, such as transcription. Recording is started by clicking the button, and
 * the recording can be stopped by clicking the button again.
 *
 * The component is built with extensibility in mind. It can be extended with plugins that
 * add functionality to the component using the component's plugin APIs. Arbitrary logic can
 * for instance be executed at certain points in the component's lifecycle, such as before
 * recording starts, or when an error occurs, by registering callbacks using
 * the [AttendiMicrophoneState.onBeforeStartRecording] or [AttendiMicrophoneState.onError] methods.
 * See the [AttendiMicrophonePlugin] interface for more information.
 *
 * Example:
 * ```kotlin
 * AttendiMicrophone(
 *  size = 64.dp,
 *  colors = AttendiMicrophoneDefaults.colors(baseColor = Color.Red),
 *  cornerRadius = 16.dp,
 *  plugins = listOf(
 *    AttendiErrorPlugin(),
 *    AttendiTranscribePlugin()
 *  )) {
 *    // use `onResult` callback to access text results
 *    text = it
 *  }
 * ```
 *
 * @param modifier While the modifier allows changing some visual properties of the contained components,
 * it is recommended to use this component's parameters instead where possible, which contains styling options
 * specific to the [AttendiMicrophone].
 * @param size Sets the width and height of the microphone. If showOptions is false,
 * the width and height will be equal. If showOptions is true, the width will be twice the height.
 * @param cornerRadius Sets the corner radius of the microphone. If not set, the button will have a
 * RoundedCornerShape of 50 percent.
 * @param colors Use this to change the colors of the microphone. To see what colors can be changed,
 * see [AttendiMicrophoneDefaults.colors].
 * @param plugins Functionality can be added to this component through a plugin system.
 * See the [AttendiMicrophonePlugin] interface for more information.
 * @param silent By default, the component will play a sound when the recording is
 * started and stopped, and when an error occurs. This can be disabled by setting
 * this attribute to `true`.
 * @param showOptions Currently not used. If set to `true`, the component will expand to show an options button.
 * When clicked, an options menu is shown in a bottom sheet.
 * @param onEvent This callback allows plugins to send arbitrary events to the [AttendiMicrophone]'s
 * caller. This can be useful when the result is not just a string, but an arbitrary
 * data structure. The caller can branch on the event name to handle the event(s) it
 * cares about.
 * @param onResult Use this callback to access any results that can be represented
 * as a string such as transcriptions. Plugins are able to call this callback
 * to return results.
 */
@SuppressLint("MissingPermission")
@OptIn(
    ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class
)
@Composable
fun AttendiMicrophone(
    modifier: Modifier = Modifier,
    cornerRadius: Dp? = null,
    size: Dp = 48.dp,
    colors: AttendiMicrophoneColors = AttendiMicrophoneDefaults.colors(),
    plugins: List<AttendiMicrophonePlugin> = listOf(),
    silent: Boolean = false,
    showOptions: Boolean = false,
    onEvent: (name: String, Any) -> Unit = { _, _ -> },
    onResult: (String) -> Unit = { },
) {
    val context = LocalContext.current

    val settings = MicrophoneSettings(
        size = size, colors = colors, cornerRadius = cornerRadius
    )

    // TODO: implement showOptions properly
    // if (showOptions) {
    if (false) {
        settings.showOptionsVariant = MicrophoneOptionsVariant.VISIBLE_WHEN_NOT_STARTED_RECORDING
    } else {
        settings.showOptionsVariant = MicrophoneOptionsVariant.HIDDEN
    }

    // Bottom sheet
    val isOptionsMenuOpen = rememberSaveable { mutableStateOf(false) }
    val optionsMenuBottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true,
        // TODO: this should be decided by the plugin
        confirmValueChange = {
            it != SheetValue.Hidden
        })

    val coroutineScope = rememberCoroutineScope()

    // Handle to the file used to store the audio samples when recording.
    val recorderBufferFile = rememberSaveable {
        File(context.filesDir, "$AUDIO_BUFFER_FILE_PREFIX${UUID.randomUUID()}.pcm")
    }

    val recorder by remember {
        mutableStateOf(
            AttendiRecorder(bufferFile = recorderBufferFile)
        )
    }

    // Used to launch activities, such as going to the settings to grant the microphone permission.
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        onResult = {})

    // Used so we can fire a callback only on the first interaction with the microphone.
    var firstClickHappened by rememberSaveable { mutableStateOf(false) }

    var microphoneUIState by rememberSaveable {
        mutableStateOf(MicrophoneUIState.NotStartedRecording)
    }

    val recordAudioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    // The state is used so that we have a hook-in point for which plugins can perform some
    // operations to change the state of the microphone.
    val microphoneState by remember {
        mutableStateOf(
            AttendiMicrophoneState(
                microphoneUIState = microphoneUIState,
                recordAudioPermissionState = recordAudioPermissionState,
                launcher = launcher,
                onEvent = onEvent,
                onResult = onResult,
                context = context,
                silent = silent,
                coroutineScope = coroutineScope,
                bottomSheetState = bottomSheetState,
                optionsMenuBottomSheetState = optionsMenuBottomSheetState,
                settings = settings,
                recorder = recorder
            )
        )
    }

    LaunchedEffect(Unit) {
        // We do this since `microphoneUIState` is a `rememberSaveable`, which persists over
        // configuration changes. The `microphoneState` is recreated on configuration changes,
        // since it's more difficult to persist (though this might be interesting to do instead).
        microphoneState.onUIState {
            microphoneUIState = it
        }
    }

    val defaultPlugins = listOf(
        AudioNotificationPlugin(),
        VolumeFeedbackPlugin(),
    )
    val allPlugins = defaultPlugins + plugins

    LaunchedEffect(Unit) {
        allPlugins.forEach { plugin ->
            plugin.activate(microphoneState)
        }
    }

    // Currently, the Attendi Recorder uses the file system to save audio samples when recording.
    // Here we delete old audio files if they exist, just in case any files were not properly cleaned
    // previously. This might for example happen when some error occurs while recording, such that
    // `clearBuffer` is not called.
    LaunchedEffect(Unit) {
        val files = context.filesDir.listFiles()?.toList() ?: return@LaunchedEffect

        val oldAudioBufferFiles =
            files.filter { it.name.startsWith(AUDIO_BUFFER_FILE_PREFIX) }.filter {
                System.currentTimeMillis() - it.lastModified() > (OLD_AUDIO_FILE_THRESHOLD_MINUTES * 60 * 1000)
            }

        oldAudioBufferFiles.forEach { it.delete() }
    }

    // Deactivate plugins when the microphone leaves the composition
    DisposableEffect(Unit) {
        onDispose {
            coroutineScope.launch {
                allPlugins.forEach {
                    it.deactivate(microphoneState)
                }

                // If we're using a file to store the audio samples,
                // we need to take extra care to delete it.
                recorder.clearBuffer()
            }
        }
    }

    // Keeps track of whether the recording was interrupted by lifecycle events such as backgrounding or rotation.
    // This is used to resume recording when the app is foregrounded again. We need it since
    // `Lifecycle.Event.ON_RESUME` is also called when the composable enters the view, and doesn't
    // necessarily mean that the recording was interrupted
    var recordingInterruptedByLifecycle by rememberSaveable { mutableStateOf(false) }

    // Handle backgrounding and foregrounding of the app. The current intended behavior is that
    // recording is paused when the app is backgrounded, and resumed when the app is foregrounded.
    OnLifecycleEvent { _, event ->
        for (callback in microphoneState.lifecycleCallbacks) {
            // TODO: is there a way we can prevent using GlobalScope here?
            // When using `coroutineScope`, sometimes the callbacks are not called here.
            GlobalScope.launch {
                callback(event)
            }
        }
    }


    suspend fun handleErrors(toRun: suspend () -> Unit) {
        try {
            toRun()
        } catch (e: Exception) {
            microphoneState.microphoneUIState = MicrophoneUIState.NotStartedRecording
            for (errorCallback in microphoneState.errorCallbacks) {
                errorCallback(e)
            }
        }
    }

    fun onClick() {
        if (!firstClickHappened) {
            firstClickHappened = true
            coroutineScope.launch {
                for (callback in microphoneState.firstClickCallbacks) {
                    callback()
                }
            }
        }

        when (microphoneUIState) {
            MicrophoneUIState.NotStartedRecording -> {
                coroutineScope.launch {
                    handleErrors { microphoneState.start() }
                }
            }

            MicrophoneUIState.Recording -> {
                coroutineScope.launch {
                    handleErrors { microphoneState.stop() }
                }
            }

            else -> Unit
        }
    }

    CompositionLocalProvider(
        LocalMicrophoneState provides microphoneState,
        LocalMicrophoneUIState provides microphoneUIState
    ) {
        AttendiMicrophoneView(
            modifier = modifier,
            onClick = { onClick() },
            openMenu = { run { isOptionsMenuOpen.value = true } },
        )
    }

    if (microphoneState.isBottomSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { microphoneState.isBottomSheetOpen = false },
            sheetState = bottomSheetState,
            containerColor = Color.White,
            dragHandle = null,
            modifier = Modifier.fillMaxWidth()
        ) {
            microphoneState.bottomSheetContent?.let { it() }
        }
    }

    if (microphoneState.isDialogOpen) {
        microphoneState.dialogContent?.let { it() }
    }

    if (showOptions && isOptionsMenuOpen.value) {
        ModalBottomSheet(
            onDismissRequest = { isOptionsMenuOpen.value = false },
            sheetState = optionsMenuBottomSheetState,
        ) {
            OptionsMenu(
                menuGroups = microphoneState.menuGroups,
                menuItems = microphoneState.menuItems,
                isOptionsMenuOpen = isOptionsMenuOpen,
            )
        }
    }
}

class AttendiMicrophoneState @OptIn(
    ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class
) constructor(
    microphoneUIState: MicrophoneUIState = MicrophoneUIState.NotStartedRecording,
    private val launcher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    private val recordAudioPermissionState: PermissionState,
    val onEvent: (String, Any) -> Unit,
    val onResult: (String) -> Unit,
    val context: Context,
    val silent: Boolean = false,
    val coroutineScope: CoroutineScope,
    val bottomSheetState: SheetState,
    val optionsMenuBottomSheetState: SheetState,
    val settings: MicrophoneSettings = MicrophoneSettings(),
    val recorder: AttendiRecorder,
) {
    private var askPermissionCount: Int = 0

    var microphoneUIState: MicrophoneUIState = microphoneUIState
        set(value) {
            field = value
            for (callback in UIStateCallbacks) {
                callback(value)
            }
        }

    internal var menuGroups by mutableStateOf(listOf<MenuGroup>())

    fun addMenuGroup(menuGroup: MenuGroup) {
        menuGroups = menuGroups + menuGroup
        menuGroups = menuGroups.sortedWith(compareBy { it.priority })
    }

    internal var menuItems = mutableMapOf<String, MutableList<MenuItem>>()

    fun addMenuItem(groupId: String, menuItem: MenuItem): () -> Unit {
        // Add the menu item to the group
        menuItems.getOrPut(groupId) { mutableListOf() }.add(menuItem)

        // Return a function that removes this item from the menu
        return {
            menuItems[groupId]?.removeIf { it.title == menuItem.title }
        }
    }

    var beforeStartRecordingCallbacks: MutableList<suspend () -> Unit> = mutableListOf()
    var startRecordingCallbacks: MutableList<suspend () -> Unit> = mutableListOf()
    var UIStateCallbacks: MutableList<(MicrophoneUIState) -> Unit> = mutableListOf()
    var firstClickCallbacks: MutableList<suspend () -> Unit> = mutableListOf()
    var beforeStopRecordingCallbacks: MutableList<suspend () -> Unit> = mutableListOf()
    var stopRecordingCallbacks: MutableList<suspend () -> Unit> = mutableListOf()
    var errorCallbacks: MutableList<suspend (Exception) -> Unit> = mutableListOf()
    var lifecycleCallbacks: MutableList<suspend (Lifecycle.Event) -> Unit> = mutableListOf()

    /**
     * Register a callback that will be called before recording of audio starts.
     *
     * @return A function that can be used to remove the added callback.
     */
    fun onBeforeStartRecording(callback: suspend () -> Unit): () -> Unit {
        beforeStartRecordingCallbacks.add(callback)
        return { beforeStartRecordingCallbacks.remove(callback) }
    }

    /**
     * Register a callback that will be called just after recording of audio starts.
     *
     * @return A function that can be used to remove the added callback.
     */
    fun onStartRecording(callback: suspend () -> Unit): () -> Unit {
        startRecordingCallbacks.add(callback)
        return { startRecordingCallbacks.remove(callback) }
    }

    /**
     * Register a callback that will be called when the UI state ([MicrophoneUIState]) of the microphone changes.
     *
     * @return A function that can be used to remove the added callback.
     */
    fun onUIState(callback: (MicrophoneUIState) -> Unit): () -> Unit {
        UIStateCallbacks.add(callback)
        return { UIStateCallbacks.remove(callback) }
    }

    /**
     * Register a callback that will be called when the microphone is clicked for the first time.
     * Useful for logic that should only be executed once.
     *
     * @return A function that can be used to remove the added callback.
     */
    fun onFirstClick(callback: suspend () -> Unit): () -> Unit {
        firstClickCallbacks.add(callback)
        return { firstClickCallbacks.remove(callback) }
    }

    /**
     * Register a callback that will be called just before the recording of audio stops.
     *
     * @return A function that can be used to remove the added callback.
     */
    fun onBeforeStopRecording(callback: suspend () -> Unit): () -> Unit {
        beforeStopRecordingCallbacks.add(callback)
        return { beforeStopRecordingCallbacks.remove(callback) }
    }

    /**
     * Register a callback that will be called just after the recording of audio stops.
     *
     * @return A function that can be used to remove the added callback.
     */
    fun onStopRecording(callback: suspend () -> Unit): () -> Unit {
        stopRecordingCallbacks.add(callback)
        return { stopRecordingCallbacks.remove(callback) }
    }

    /**
     * Register a callback that will be called when an error occurs.
     *
     * @return A function that can be used to remove the added callback.
     */
    fun onError(callback: suspend (Exception) -> Unit): () -> Unit {
        errorCallbacks.add(callback)
        return { errorCallbacks.remove(callback) }
    }

    /**
     * Register a callback that will be called when the underlying activity undergoes a lifecycle
     * event such as `Lifecycle.Event.ON_START`, `Lifecycle.Event.ON_RESUME`, etc.
     *
     * CAUTION: Take special care with the `ON_CREATE`, `ON_START`, and `ON_RESUME` events. Currently,
     * when the screen is rotated, all callbacks added using this function are cleared, since the
     * `microphoneState` is not persisted over rotations. This means that these events
     * will *NOT* be called after a screen rotation. This is a limitation of the current implementation.
     *
     * @return A function that can be used to remove the added callback.
     */
    fun onLifecycle(callback: suspend (Lifecycle.Event) -> Unit): () -> Unit {
        lifecycleCallbacks.add(callback)
        return { lifecycleCallbacks.remove(callback) }
    }

    // ========= Recorder callbacks =============

    /**
     * Register a callback that will be called when the signal energy (a measure of the volume)
     * changes. We currently measure the signal energy using RMS.
     *
     * @return A function that can be used to remove the added callback.
     */
    fun onSignalEnergy(callback: suspend (Double) -> Unit): () -> Unit {
        return recorder.onSignalEnergy(callback)
    }

    /**
     * Register a callback that will be called when a new audio frame (a set of samples) is available.
     *
     * This is useful when you want to do something with the audio frames in real-time, such as
     * streaming them to a server.
     *
     * @return A function that can be used to remove the added callback.
     */
    fun onAudioFrames(callback: suspend (List<Short>) -> Unit): () -> Unit {
        return recorder.onAudioFrames(callback)
    }

    // ========= Behavior =============

    @OptIn(ExperimentalPermissionsApi::class)
    suspend fun start(delayMilliseconds: Long = START_RECORDING_DELAY_MILLISECONDS) {
        when {
            recordAudioPermissionState.hasPermission -> {
                microphoneUIState = MicrophoneUIState.LoadingBeforeRecording

                for (callback in beforeStartRecordingCallbacks) {
                    callback()
                }

                coroutineScope.launch {
                    recorder.startRecording()
                }

                for (callback in startRecordingCallbacks) {
                    callback()
                }

                val startRecordingDelayMilliseconds =
                    delayMilliseconds - shortenShowRecordingDelayByMilliseconds

                // simulate loading time before recording
                coroutineScope.launch(Dispatchers.IO) {
                    delay(startRecordingDelayMilliseconds)
                    microphoneUIState = MicrophoneUIState.Recording

                    shortenShowRecordingDelayByMilliseconds = 0
                }
            }

            // The rationale is false when asking permission the first time, true when asking
            // permission the second time (so after denying for the first time), and false again
            // when asking permission the third time. This makes it very hard to determine when
            // to show the rationale dialog. We use a counter to keep track of how many times
            // we've asked for permission, and only show the rationale dialog when we've asked
            // for permission more than once and `shouldShowRationale` returns false.
            !recordAudioPermissionState.shouldShowRationale && askPermissionCount > 0 -> showPermissionRequestDialog()

            else -> {
                recordAudioPermissionState.launchPermissionRequest()
                askPermissionCount++
            }
        }
    }

    // This refers not to the native Android permission dialog, but to the dialog that is shown
    // when the user has denied permission multiple times. This dialog explains why the permission
    // is needed and explains that the user has to go to the settings to grant the permission.
    private fun showPermissionRequestDialog() {
        vibrate()
        showDialog {
            AlertDialog(onDismissRequest = { isDialogOpen = false },
                title = { Text(context.getString(R.string.no_microphone_permission_dialog_title)) },
                text = { Text(context.getString(R.string.no_microphone_permission_dialog_body)) },
                dismissButton = {
                    Button(onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri: Uri = Uri.fromParts("package", context.packageName, null)
                        intent.data = uri
                        launcher.launch(intent)
                        isDialogOpen = false
                    }) {
                        Text(context.getString(R.string.no_microphone_permission_dialog_go_to_settings))
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        isDialogOpen = false
                    }) {
                        Text("OK")
                    }
                })
        }
    }

    suspend fun stop(
        delayMilliseconds: Long = STOP_RECORDING_DELAY_MILLISECONDS, runAudioTasks: Boolean = true
    ) {
        if (microphoneUIState != MicrophoneUIState.Recording) return

        for (callback in beforeStopRecordingCallbacks) {
            callback()
        }

        microphoneUIState = MicrophoneUIState.Processing

        // The last word is oftentimes not correctly transcribed.
        // This might be a result of a user still uttering the last syllable
        // while already pressing the stop button. The timeout allows recording
        // for just a bit longer before stopping the recorder, so that we also get
        // the last bit of spoken audio.
        if (delayMilliseconds > 0) delay(delayMilliseconds)

        recorder.stopRecording()

        for (callback in stopRecordingCallbacks) {
            callback()
        }

        val wav = pcmToWav(recorder.buffer, sampleRate = AUDIO_SAMPLE_RATE)
        recorder.clearBuffer()

        if (runAudioTasks) {
            for (audioTask in activeAudioTasks) {
                try {
                    audioTask(wav)
                } catch (e: Exception) {
                    for (errorCallback in errorCallbacks) {
                        errorCallback(e)
                    }
                }
            }
        }

        microphoneUIState = MicrophoneUIState.NotStartedRecording
    }

    // ========= Bottom sheet =========

    var bottomSheetContent: (@Composable () -> Unit)? = null
    var isBottomSheetOpen by mutableStateOf(false)

    /**
     * Open a bottom sheet with the given content.
     */
    fun openBottomSheet(content: @Composable () -> Unit) {
        bottomSheetContent = content
        isBottomSheetOpen = true
    }

    @OptIn(ExperimentalMaterial3Api::class)
    fun closeBottomSheet() {
        coroutineScope.launch {
            bottomSheetState.hide()
            isBottomSheetOpen = false
            bottomSheetContent = null
        }
    }

    /**
     * Vibrate the device (e.g. for haptic feedback).
     */
    fun vibrate(durationMilliseconds: Long = 200) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION") context.getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        vibrator.vibrate(
            VibrationEffect.createOneShot(
                durationMilliseconds, VibrationEffect.DEFAULT_AMPLITUDE
            )
        )
    }

    // ========== Audio tasks ==========

    /**
     * An audio task represents something we want to do with the audio recorded by the microphone.
     * It is a list of callbacks that take as input the recorded audio data in a wav representation. Clients can
     * call [registerAudioTask] and [setActiveAudioTask] to register and set the audio tasks
     * they want to perform.
     */
    private val registeredAudioTasks: MutableMap<String, AudioTask> = mutableMapOf()

    /**
     * Clients can register multiple audio tasks with the component. But sometimes we only want to
     * perform a subset of the registered tasks. The concept of active audio tasks exist to
     * separate the audio tasks we register from the one we want to perform at a moment in time.
     */
    val activeAudioTasks: List<AudioTask>
        get() = registeredAudioTasks.filterKeys { activeAudioTaskIds.contains(it) }.values.toList()
    private var activeAudioTaskIds: Set<String> = setOf()

    /**
     * Keeping track of the history is necessary as plugins can change the active audio task.
     * But they don't necessarily know what other audio tasks exist. To undo their changes, they
     * can use the [activeAudioTaskIdHistory].
     */
    private val activeAudioTaskIdHistory: MutableList<Set<String>> = mutableListOf()

    /**
     * Register a task (callback) that should do something with the audio when the user stops recording.
     * Use this to perform custom actions with the available audio.
     * The newly available audio is passed to the callback as a 16-bit signed integer,
     * 16 KHz WAV-encoded [ByteArray]. A registered audio task needs to be activated by calling
     * [setActiveAudioTask] with its task id.
     *
     * @return A function that de-registers the added task.
     */
    fun registerAudioTask(taskId: String, audioTask: AudioTask): () -> Unit {
        registeredAudioTasks[taskId] = audioTask
        return { removeAudioTask(taskId) }
    }

    /**
     * The microphone can have multiple registered audio tasks, but only the *active* audio tasks are
     * performed. Use this function to set a registered audio task to *active*.
     */
    fun setActiveAudioTasks(taskIds: List<String>) {
        if (activeAudioTaskIds == taskIds.toSet()) {
            println("activeAudioTasks is already set to $taskIds")
            return
        }

        activeAudioTaskIds = taskIds.toSet()
        activeAudioTaskIdHistory.add(activeAudioTaskIds.toSet())
    }

    /**
     * The microphone can have multiple registered audio tasks, but only the *active* audio tasks are
     * performed. Use this function to set a registered audio task to *active*.
     */
    fun setActiveAudioTask(taskId: String) {
        activeAudioTaskIds = setOf(taskId)
        activeAudioTaskIdHistory.add(activeAudioTaskIds.toSet())
    }

    /** De-registers an audio task. */
    fun removeAudioTask(taskToRemoveId: String) {
        registeredAudioTasks.remove(taskToRemoveId)
        if (activeAudioTaskIds.contains(taskToRemoveId)) {
            activeAudioTaskIds = activeAudioTaskIds - taskToRemoveId
            activeAudioTaskIdHistory.add(activeAudioTaskIds.toSet())
        }
    }

    /**
     * Plugins can set new audio tasks to active, but they might want to undo their actions. Since they
     * don't necessarily have knowledge of what the previous active audio task was, they can call this method
     * instead.
     */
    fun goBackInActiveAudioTaskHistory() {
        activeAudioTaskIdHistory.removeAt(activeAudioTaskIdHistory.size - 1)
        val previousActiveAudioTasks = activeAudioTaskIdHistory.lastOrNull()
        if (previousActiveAudioTasks != null) {
            activeAudioTaskIds = previousActiveAudioTasks
        }
    }

    // ========== Dialog ==========

    var dialogContent: (@Composable () -> Unit)? = null
    var isDialogOpen by mutableStateOf(false)

    // Maybe `showElement` is a more useful abstraction? There is nothing specific to
    // AlertDialog in the function itself.
    /**
     * Show a dialog with the given content.
     */
    fun showDialog(content: @Composable () -> Unit) {
        dialogContent = content
        isDialogOpen = true
    }

    // ========== Animatable elements ==========

    private var _animatedMicrophoneFillLevel by mutableStateOf(0.0)

    /**
     * The microphone fill level is a value between 0 and 1, that is used to give feedback to
     * the user about the volume of their voice.
     */
    var animatedMicrophoneFillLevel: Double
        get() = _animatedMicrophoneFillLevel
        set(value) {
            _animatedMicrophoneFillLevel = value
        }

    // ========== Other ==========

    /**
     * The component already starts recording, only showing the actual recording UI after
     * a slight delay. This variable can be used to shorten that delay.
     */
    var shortenShowRecordingDelayByMilliseconds = 0
}

typealias AudioTask = suspend (ByteArray) -> Unit

@Composable
fun AttendiMicrophoneView(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    openMenu: () -> Unit,
) {
    val microphoneState = LocalMicrophoneState.current
    val microphoneUIState = LocalMicrophoneUIState.current

    val settings = microphoneState.settings

    val isRecording =
        microphoneUIState == MicrophoneUIState.Recording || microphoneUIState == MicrophoneUIState.Processing

    fun shouldShowOptions(): Boolean {
        return settings.showOptionsVariant == MicrophoneOptionsVariant.ALWAYS_VISIBLE || (settings.showOptionsVariant == MicrophoneOptionsVariant.VISIBLE_WHEN_NOT_STARTED_RECORDING && !isRecording)
    }

    val width = if (shouldShowOptions()) 2 * settings.size else settings.size
    val height = settings.size

    val roundedCornerShape =
        if (settings.cornerRadius == null) RoundedCornerShape(percent = 50) else RoundedCornerShape(
            settings.cornerRadius!!
        )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(roundedCornerShape)
            .then(modifier)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            when (microphoneUIState) {
                MicrophoneUIState.NotStartedRecording -> NotStartedRecordingView()
                MicrophoneUIState.LoadingBeforeRecording -> LoadingBeforeRecordingView()
                MicrophoneUIState.Recording -> RecordingView()
                MicrophoneUIState.Processing -> ProcessingView()
            }
        }

        if (shouldShowOptions()) {
            Divider(
                color = settings.color,
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight(fraction = 0.5f)
            )

            IconButton(onClick = openMenu) {
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = "Options",
                    modifier = Modifier.fillMaxWidth(0.5f),
                    tint = settings.color,
                )
            }
        }
    }
}


@Composable
fun NotStartedRecordingView() {
    val settings = LocalMicrophoneState.current.settings

    val foregroundColor = settings.colors.inactiveForegroundColor

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(settings.colors.inactiveBackgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.microphone),
            contentDescription = "Microphone icon",
            modifier = Modifier
                .fillMaxSize(0.5f)
                .aspectRatio(1f),
            tint = foregroundColor
        )
    }
}

@Preview
@Composable
fun NotStartedRecordingViewPreview() {
    CompositionLocalProvider(LocalMicrophoneState provides getPreviewMicrophoneState()) {
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            NotStartedRecordingView()
        }
    }
}

@Composable
fun LoadingBeforeRecordingView() {
    val settings = LocalMicrophoneState.current.settings

    val backgroundColor = settings.colors.inactiveBackgroundColor
    val foregroundColor = settings.colors.inactiveForegroundColor

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
            imageVector = ImageVector.vectorResource(id = R.drawable.attendilogo),
            contentDescription = "LoadingIcon",
            tint = foregroundColor,
            modifier = Modifier
                .fillMaxSize(0.33f)
                .graphicsLayer(rotationZ = rotation),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun LoadingBeforeRecordingPreview() {
    CompositionLocalProvider(LocalMicrophoneState provides getPreviewMicrophoneState()) {
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            LoadingBeforeRecordingView()
        }
    }
}


@Composable
fun RecordingView() {
    val microphoneState = LocalMicrophoneState.current
    val settings = microphoneState.settings

    val backgroundColor = settings.colors.activeBackgroundColor
    val foregroundColor = settings.colors.activeForegroundColor

    val showOptions = settings.showOptionsVariant == MicrophoneOptionsVariant.ALWAYS_VISIBLE

    val roundedCornerShape =
        if (settings.cornerRadius == null) RoundedCornerShape(percent = 50) else RoundedCornerShape(
            settings.cornerRadius!!,
            if (showOptions) 0.dp else settings.cornerRadius!!,
            if (showOptions) 0.dp else settings.cornerRadius!!,
            settings.cornerRadius!!
        )

    val modifier = if (showOptions) Modifier.clip(roundedCornerShape) else Modifier

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .then(modifier),
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

            val microphoneFillPercentage =
                startYPercentage - (startYPercentage - endYPercentage) * microphoneState.animatedMicrophoneFillLevel

            // Make the animation a bit smoother by tweening between the current and the target value
            val animatedMicrophoneFillPercentage: Float by animateFloatAsState(
                targetValue = microphoneFillPercentage.toFloat(),
                animationSpec = tween(150),
                label = "attendiMicrophoneFillPercentage"
            )

            // Microphone volume fill
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
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                tint = foregroundColor
            )
        }
    }
}

@Preview
@Composable
fun RecordingViewPreview() {
    CompositionLocalProvider(LocalMicrophoneState provides getPreviewMicrophoneState()) {
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            RecordingView()
        }
    }
}


@Composable
fun ProcessingView() {
    val settings = LocalMicrophoneState.current.settings

    val backgroundColor = settings.colors.activeBackgroundColor
    val foregroundColor = settings.colors.activeForegroundColor

    val showOptions = settings.showOptionsVariant == MicrophoneOptionsVariant.ALWAYS_VISIBLE

    val roundedCornerShape =
        if (settings.cornerRadius == null) RoundedCornerShape(percent = 50) else RoundedCornerShape(
            settings.cornerRadius!!,
            if (showOptions) 0.dp else settings.cornerRadius!!,
            if (showOptions) 0.dp else settings.cornerRadius!!,
            settings.cornerRadius!!
        )

    val modifier = if (showOptions) Modifier.clip(roundedCornerShape) else Modifier

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .then(modifier),
        contentAlignment = Alignment.Center,
    ) {
        ProcessingAnimation(settings, foregroundColor)
    }
}

@Preview
@Composable
fun ProcessingViewPreview() {
    CompositionLocalProvider(LocalMicrophoneState provides getPreviewMicrophoneState()) {
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            ProcessingView()
        }
    }
}

@Composable
fun ProcessingAnimation(settings: MicrophoneSettings, foregroundColor: Color) {
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

@Composable
fun AnimatedRectangle(index: Int, color: Color) {
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

/**
 * Helper function to create a [AttendiMicrophoneState] object with the given properties
 * for the preview composable functions.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
private fun getPreviewMicrophoneState(): AttendiMicrophoneState {
    return AttendiMicrophoneState(
        launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            onResult = {}),
        recordAudioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO),
        onEvent = { _, _ -> },
        onResult = { },
        context = LocalContext.current,
        silent = false,
        coroutineScope = rememberCoroutineScope(),
        bottomSheetState = rememberModalBottomSheetState(),
        optionsMenuBottomSheetState = rememberModalBottomSheetState(),
        settings = MicrophoneSettings(),
        recorder = AttendiRecorder(
            File(
                LocalContext.current.filesDir, "attendi_recorder_samples_${UUID.randomUUID()}"
            )
        )
    )
}
