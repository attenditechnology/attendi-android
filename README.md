# Attendi Speech Service for Android

The Attendi Speech SDK provides tools for capturing and processing audio in Android applications. It includes `AttendiMicrophone`, a customizable Jetpack Compose component, and `AttendiRecorder`, a suspendable interface for low-level audio recording.

The SDK is designed with extensibility in mind, supporting plugins to customize behavior like transcription, feedback, and error handling.

## Getting started

The SDK is available as a Kotlin Android library package.

### Installation instructions

You can install the package by either downloading it directly from the package page on GitHub Packages, by updating your app's build.gradle file:

```kotlin
implementation "nl.attendi:attendispeechservice:<version>"
```

or you can update the `pom.xml` file by adding the following repository and dependency:

```
<repository>
  <id>github</id>
  <name>GitHub Attendi Technology Apache Maven Packages</name>
  <url>https://maven.pkg.github.com/attenditechnology/attendi-android</url>
</repository>

<dependency>
  <groupId>nl.attendi</groupId>
  <artifactId>attendispeechservice</artifactId>
  <version>0.1.0</version>
</dependency>
```

The package is not hosted on a public Maven repository yet, so you will need to authenticate with Attendi's private GitHub Packages repository.
First, create a personal access token with the `read:packages` scope. [This link](https://github.com/settings/tokens/new) should take you to the right page.
Then, add the following to your project's `settings.gradle` file:

```
// This block should already exist
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Add this part
        maven(
            "https://maven.pkg.github.com/attenditechnology/attendi-android") {
            credentials {
                // Your GitHub username
                username = ""
                // Use the personal access token you created earlier as the password
                password = ""
            }
        }
    }
}
```

## Core Components

### AttendiRecorder

A suspendable interface for recording audio using Android’s AudioRecord API. It supports:
* Start/stop recording (with optional delays)
* StateFlow-based state observation
* Resource management
* Plugin-driven behavior via AttendiRecorderPlugin

### AttendiMicrophone
A Jetpack Compose component for audio capture and plugin-driven behavior. Supports customization and user interaction hooks.

## Usage

After installing the package, you can use the microphone component and/or the recorder component in your project:

AttendiMicrophone Example: 
```kotlin
import nl.attendi.attendispeechservice.components.attendimicrophone.microphone.AttendiMicrophone

// within some ComponentActivity or Jetpack Composable

AttendiMicrophone(
    recorder = AttendiRecorderFactory.create(
        plugins = listOf(
            AttendiErrorPlugin(context = context),
            ExampleWavTranscribePlugin(context = context),
            AttendiTranscribePlugin(
                service = AttendiTranscribeServiceFactory.create(
                    ExampleAttendiTranscribeAPI.transcribeAPIConfig
                ),
                // Ignoring the returned error for brevity
                onTranscribeCompleted = { transcript, _ ->
                    shortText = transcript ?: ""
                }
            ),
            ExampleErrorLoggerPlugin()
        )
    ),
    plugins = listOf(
        AttendiVolumeFeedbackPlugin()
    )
)
```

AttendiRecorder Example:
```kotlin
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorderImpl

// within any class, usually inside a viewModel class

val recorder: AttendiRecorder = AttendiRecorderFactory.create(
    plugins = listOf(
        AttendiAsyncTranscribePlugin(
            transcribeAsyncService = CustomAsyncTranscribeService,
            serviceMessageDecoder = CustomMessageDecoder,
            onStreamStarted = {
                // Live stream started
            },
            onStreamUpdated = { stream ->
                // Live stream updated with stream model
                // You can access the stream text via stream.state.text
            },
            onStreamCompleted = { stream: AttendiTranscribeStream, error: Exception? ->
                // Live stream completed with stream or error. If no error occurred, `error` is `null`
            }
        ),
        AttendiAudioNotificationPlugin(context = applicationContext),
        AttendiStopOnAudioFocusLossPlugin(context = applicationContext),
        AttendiErrorPlugin(context = applicationContext),
        ExampleErrorLoggerPlugin()
    )
)
```

## Plugin System

Plugins implement AttendiRecorderPlugin or AttendiMicrophonePlugin equivalents and hook into lifecycle states for extensibility.

```kotlin

class MyCustomRecorderPlugin : AttendiRecorderPlugin {

    override suspend fun activate(model: AttendiRecorderModel) {
        model.onBeforeStartRecording {
            // Called just before a recording is about to start
        }

        model.onAudio { audioFrame ->
            // Called when the recorder captured an audio frame
        }

        model.onStopRecording {
            // Called when the recording stopped
        }

        model.onError { error ->
            // Called when an error occurred during the recording
        }
    }

    override suspend fun deactivate(model: AttendiRecorderModel) {
        // Called when the recorder is disposed.
        // Use this to clean up any ongoing resources or subscriptions
    }
}
```

```kotlin

class MyCustomMicrophonePlugin : AttendiMicrophonePlugin {

    override suspend fun activate(recorderModel: AttendiRecorderModel, microphoneModel: AttendiMicrophoneModel) {
        recorderModel.onAudio { audioFrame ->
           // Update microphone volume levels when receiving audioFrames
           microphoneModel.updateVolume(audioFrame.volume)
        }
    }

    override suspend fun deactivate(recorderModel: AttendiRecorderModel, microphoneModel: AttendiMicrophoneModel) {
        // Called when the microphone component is disposed.
        // Use this to clean up any ongoing resources or subscriptions
    }
}
```

We offer out of the box plugins to be used with specific purposes:

### Recorder Plugins

* AttendiTranscribePlugin – Sends audio to a backend sync transcription API, such as Attendi’s. Designed to be extensible to support other providers as well
* AttendiAsyncTranscribePlugin – Real-time transcription using a WebSocket-based connection, such as Attendi’s, with support for custom or alternative streaming APIs
* AttendiAudioNotificationPlugin – Audible start/stop cues
* AttendiErrorPlugin – Plays error sound and vibrates on failure
* AttendiStopOnAudioFocusLossPlugin – Stops recording on focus loss

### Microphone Plugins
* AttendiVolumeFeedbackPlugin – Visual volume-level feedback

## API Services

The Attendi SDK provides three core service interfaces to communicate with Attendi's backend systems or your own custom infrastructure:

* TranscribeService Interface & AttendiTranscribeServiceImpl (Default Implementation).
Use case: Used for synchronous, single-shot transcription. You provide the complete audio recording, and the service returns a text transcription.
Implementation: AttendiTranscribeServiceImpl connects to Attendi's backend to fulfill the request.

* AsyncTranscribeService Interface & AttendiAsyncTranscribeServiceImpl (Default Implementation)
Use case: Used for real-time or streaming transcription over WebSockets. You stream live audio, and the service returns a transcription object containing both text and rich annotations.
Implementation: AttendiAsyncTranscribeServiceImpl handles the streaming protocol and communication with Attendi's backend services.

* AttendiAuthenticationService
Defines the contract for authenticating with Attendi’s backend. Implementations are responsible for retrieving and refreshing valid access tokens to authorize requests.

## Development

### Testing
When testing AttendiSpeechService, use a physical Android device rather than an emulator. The Android emulator has limited or unreliable support for AudioRecord, which may lead to audio capture failures or inconsistent behavior during recording.

### Network Security Configuration

In the attendispeechserviceexample module, a custom network security configuration is defined via the following attribute in the AndroidManifest.xml:
`android:networkSecurityConfig="@xml/network_security_config"`

This configuration references an XML file that customizes the app’s handling of network security—particularly useful during development and debugging.
The current network_security_config.xml file is configured to trust user-installed certificate authorities (CAs). This is particularly helpful when using debugging tools such as Charles Proxy or Proxyman to inspect network traffic.
**Important:**
This configuration is strictly for development and debugging purposes only. It should never be used in production builds, as trusting user-installed certificates can pose serious security risks.
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <debug-overrides>
        <trust-anchors>
            <!-- Trust user added CAs while debuggable only -->
            <certificates src="user" />
        </trust-anchors>
    </debug-overrides>
</network-security-config>
```

## Issues

If you encounter any issues, don't hesitate to contact us at `omar@attendi.nl` or `emiliano@attendi.nl`.
