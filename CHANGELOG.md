# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.3.1 - 2025-08-12]
### Changed
- Replaced the permissionsDeniedPermanentlyView setting in AttendiMicrophoneSettings with a new flag, showsDefaultPermissionsDeniedDialog, to control whether the default Attendi error dialog appears when recording permissions are denied.
- To customize the error view, combine showsDefaultPermissionsDeniedDialog with the onRecordingPermissionDeniedCallback callback from AttendiMicrophone.
- Renamed `AttendiMicrophone` API methods
  * `onMicrophoneTapCallback` -> `onMicrophoneTap`
  * `onRecordingPermissionDeniedCallback` -> `onRecordingPermissionDenied`

### Improved
- Enhanced error handling across AudioRecorder, AttendiRecorder, and BaseAsyncTranscribeService.
- Updated SDK example to cover and validate all core functionalities, including audio recording, live transcription, plugin integration, and error handling.
- Added UserAgentProvider in AttendiTranscribeService to set a default user agent if none is specified.

### Removed
- Removed unused assets and string resources.
- Deprecated AttendiMicrophonePlugin and integrated microphone-specific plugins directly into AttendiMicrophone to simplify usage and reduce complexity.

### Breaking Changes
```kotlin
// Old:
AttendiMicrophone(
    recorder = recorderInstance,
    settings = AttendiMicrophoneSettings(
        size = 64.dp,
        colors = AttendiMicrophoneDefaults.colors(baseColor = Colors.Red),
        isVolumeFeedbackEnabled = false
    ),
    onMicrophoneTapCallback = {
        print("Microphone tapped")
    },
    onRecordingPermissionDeniedCallback = {
        print("Microphone access denied")
    }
)
```

```kotlin
// New:
AttendiMicrophone(
    recorder = recorderInstance,
    settings = AttendiMicrophoneSettings(
        size = 64.dp,
        colors = AttendiMicrophoneDefaults.colors(baseColor = Colors.Red),
        isVolumeFeedbackEnabled = false
    ),
    onMicrophoneTap = {
        print("Microphone tapped")
    },
    onRecordingPermissionDenied = {
        print("Microphone access denied")
    }
)
```

## [0.3.0 - 2025-07-29]

### Added
- AudioRecorder and AudioRecorderImpl: Wrap the lower-level AudioRecord APIs to provide a convenient, suspendable interface for capturing audio from the device, ensuring coroutine support and streamlined usage.
- AttendiRecorder and AttendiRecorderImpl: High-level recording interfaces that manage audio capture, plugin coordination, and lifecycle events without requiring direct UI interaction.
- AsyncTranscribeService: Enables integration with real-time or streaming transcription services through a coroutine-friendly interface.
- New plugin system for recorders: Introduced AttendiRecorderPlugin, decoupled from AttendiMicrophonePlugin, allowing more granular control over recording behavior.
- New plugins:
  * AttendiAsyncTranscribePlugin: Supports real-time transcription by integrating with AsyncTranscribeService.
  * AttendiStopOnAudioFocusLossPlugin: Adds graceful handling of audio focus interruptions to prevent audio clashes with other apps (e.g., incoming call).

### Changed
- Refactored SDK architecture: Major reorganization to improve separation of concerns and encapsulation:
  * Clear distinctions between AudioRecorder, AttendiRecorder, and AttendiMicrophone layers.
  * Organized internal boundaries and modular package layout for improved maintainability.
- AttendiTranscribePlugin now supports injecting a TranscribeService and AudioEncoder for improved extensibility, supporting alternative implementations beyond the default Attendi transcription service.
- In-memory audio buffering: Audio recordings are now captured in memory instead of being written to disk, reducing I/O overhead and increasing performance for audio capture scenarios.

### Improved
- Configuration resilience: Recording is now preserved across configuration changes (e.g. screen rotation).
- Dependency updates: Upgraded Gradle and library dependencies for better compatibility and stability.
- Thread safety and lifecycle handling:
  * Enhanced lifecycle-aware resource cleanup using proper coroutine scope management.
  * Improved stability and reliability of audio operations in AudioRecorder and AttendiRecorder, preventing memory leaks and avoiding illegal state transitions.

### Breaking Changes
- Package structure refactoring:
import nl.attendi.attendispeechservice.client.TranscribeAPIConfig  
→ import nl.attendi.attendispeechservice.services.AttendiTranscribeAPIConfig

import nl.attendi.attendispeechservice.components.attendimicrophone.AttendiMicrophone  
→ import nl.attendi.attendispeechservice.components.attendimicrophone.microphone.AttendiMicrophone

import nl.attendi.attendispeechservice.components.attendimicrophone.AttendiMicrophoneState  
→ import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderState

import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.AttendiErrorPlugin  
→ import nl.attendi.attendispeechservice.components.attendirecorder.plugins.AttendiErrorPlugin

import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.AttendiMicrophonePlugin  
→ import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderPlugin

import nl.attendi.attendispeechservice.components.attendimicrophone.plugins.AttendiTranscribePlugin  
→ import nl.attendi.attendispeechservice.components.attendirecorder.plugins.transcribeplugin.AttendiTranscribePlugin

- Class renaming:
MicrophoneUIState → AttendiRecorderState
TranscribeAPIConfig → AttendiTranscribeAPIConfig

- AttendiTranscribeAPIConfig updated fields:
```kotlin
// Old:
val apiURL: String
val modelType: ModelType

// New:
val apiBaseURL: String
val modelType: String? = null
```

- AttendiMicrophone parameters moved into settings:
```kotlin
// Old:
AttendiMicrophone(
    size = 64.dp,
    colors = AttendiMicrophoneDefaults.colors(
        inactiveBackgroundColor = pinkColor,
        inactiveForegroundColor = Color.White,
        activeBackgroundColor = pinkColor,
        activeForegroundColor = Color.White,
    )
)

// New:
AttendiMicrophone(
    settings = AttendiMicrophoneSettings(
        size = 64.dp,
        colors = AttendiMicrophoneDefaults.colors(
            inactiveBackgroundColor = pinkColor,
            inactiveForegroundColor = Color.White,
            activeBackgroundColor = pinkColor,
            activeForegroundColor = Color.White,
        )
    )
)
```

- Plugin system migration from AttendiMicrophone to AttendiRecorder:
```kotlin
// Old:
AttendiMicrophone(
    plugins = listOf(
        AttendiErrorPlugin(),
        AttendiTranscribePlugin(apiConfig = exampleAPIConfig),
        object : AttendiMicrophonePlugin {
            override fun activate(state: AttendiMicrophoneState) {
                state.onUIState {
                    // ...
                }
            }
        }
    ),
    onResult = { transcribe: String -> 
        // ...
    }
)

// New:
val recorder: AttendiRecorder = AttendiRecorderFactory.create(
    plugins = listOf(
        AttendiTranscribePlugin(
            service = AttendiTranscribeServiceFactory.create(
                apiConfig = ExampleAttendiTranscribeAPI.transcribeAPIConfig,
            ),
            onTranscribeCompleted = { transcribe: String?, error: Exception? ->
                // ...
            }
        ),
        AttendiErrorPlugin(context),
        object : AttendiRecorderPlugin {
            override suspend fun activate(model: AttendiRecorderModel) {
                model.onStateUpdate {
                    // ...
                }
            }
        }
    )
)
AttendiMicrophone(
    recorder = recorder
)
```

## [0.2.3 - 2023-11-27]

### Fixed

- Play audio after rotations

  Before, the audio notification plugin would not play audio after rotations. This is now fixed.

## [0.2.2 - 2023-11-23]

### Modified

- Move stop function to microphone state.

  This allows the stop function to be called by plugins or at the component's call-site. This
  gives more options to the caller for calling the stop button when they want. Call using `state.stop()`.

- Allow handling lifecycle events in client

  We move the start and stop functions to `microphoneState` such that
  they can be called by plugins to programmatically control starting and stopping
  instead of only exposing it through a click.
  We add a plugin API `onLifeCycle` that registers a callback that is called
  when a lifecycle event such as `ON_PAUSE` or `ON_RESUME` happens. Here,
  the caller can decide what should happen at certain key moments of the
  activity lifecycle. For example, to stop the recording when the activity is paused.
  Currently, the events `ON_START`, `ON_RESUME`, `ON_CREATE` can not be used
  on screen rotations, since the microphoneState is lost after screen rotation.
  This is something we have to try to correct in the future.

## [0.2.1] - 2023-11-17

### Modified

- The moment the start and stop sounds are played is changed such that they are not present in the recording anymore
- When the screen is rotated, the recording state is persisted instead of lost

## [0.2.0] - 2023-10-16

This release introduces some simplifies styling the AttendiMicrophone by removing the MicrophoneModifier abstraction. This is a breaking change. It also introduces an API that exposes more granular control over the color theming of the microphone.

An example:

```kotlin
import nl.attendi.attendispeechservice.components.attendimicrophone.AttendiMicrophone

// within some ComponentActivity or Jetpack Composable

AttendiMicrophone(
    // We can use some general Compose modifiers here, such as `padding`, but some like
    // `size` won't work. In general, check first whether `AttendiMicrophone` has a parameter
    // to change the aspect of the microphone that you want to change.
    modifier = Modifier
        .border(1.dp, pinkColor, RoundedCornerShape(percent = 50))
        .background(Color.White),
    // Change the microphone's size
    size = 64.dp,
    // Change the microphone's color
    colors = AttendiMicrophoneDefaults.colors(baseColor = myColor),
    // Or do this if you need more control over the microphone's colors
    // colors = AttendiMicrophoneDefaults.colors(
    //     inactiveBackgroundColor = myColor,
    //     inactiveForegroundColor = Color.White,
    //     activeBackgroundColor = myColor,
    //     activeForegroundColor = Color.White,
    // ),
    // Add plugins if necessary. These extend the functionality of the microphone component.
    plugins = listOf(
        AttendiErrorPlugin(),
        AttendiTranscribePlugin(apiConfig = exampleAPIConfig),
        // Anonymous objects allow us to create a plugin without having to create a new class,
        // thereby giving access to our view's state.
        object : AttendiMicrophonePlugin {
            override suspend fun activate(state: AttendiMicrophoneState) {
                state.onUIState {
                    microphoneUIState = it
                }
            }
        }
    ),
    // Use `onEvent` to listen to arbitrary events
    onEvent = { name, data ->
        when (name) {
            // This name is specified by the plugin firing the event
            "attendi-transcribe" -> {
                (data as? String)?.let {
                    println("showcasing arbitrary events: $it")
                }
            }
        }
    },
    // We could also have done this instead of defining the closure below
    // onResult = { print(it) }
) { print(it) }
```

## [0.1.0] - 2023-10-11

First release of the package.
