# Attendi Speech Service for Android

The Attendi Speech SDK provides tools for capturing and processing audio in Android applications. It includes `AttendiMicrophone`, a customizable Jetpack Compose component, and `AttendiRecorder`, a suspendable interface for low-level audio recording.

The SDK is designed with extensibility in mind, supporting plugins to customize behavior like transcription, feedback, and error handling.

## Getting started

The SDK is available as a Kotlin Android library package.

### Installation instructions

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
  <version>0.3.2</version>
</dependency>
```

or if you are using libs.versions.toml:
[versions]
attendiSpeechService = "0.3.2"

[libraries]
attendi-speechservice = { group = "nl.attendi", name = "attendispeechservice", version.ref = "attendiSpeechService" }

and in your module.gradle file:
implementation(libs.attendi.speechservice)

## Usage

After adding the dependency, you can use the microphone component and/or the recorder component in your project:

```kotlin
import nl.attendi.attendispeechservice.components.attendimicrophone.microphone.AttendiMicrophone
import nl.attendi.attendispeechservice.components.attendirecorder.recorder.AttendiRecorder
```

## Core Components

### AttendiRecorder

A suspendable interface for recording audio using Android’s AudioRecord API. It supports:
* Start/stop recording (with optional delays)
* StateFlow-based state observation
* Resource management
* Plugin-driven behavior via AttendiRecorderPlugin

Example Usage
```kotlin
let recorder = AttendiRecorderFactory.create()

private fun onButtonPressed() {
    viewModelScope.launch {
        if (recorder.recorderState.value == AttendiRecorderState.NotStartedRecording) {
            recorder.start()
        } else if (recorder.recorderState.value == AttendiRecorderState.Recording) {
            recorder.stop()
        }
    }
}
```

### AttendiMicrophone
A Jetpack Composable designed for audio capture using a visual microphone button. It integrates with an `AttendiRecorder` instance and supports plugin-driven behavior, visual feedback, and customization of appearance and interaction.

Example Usage: 
```kotlin
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

### Usage Examples

The following example screens demonstrate how to use `AttendiMicrophone` and `AttendiRecorder` in different real-world scenarios:

1. `OneMicrophoneSyncScreenView`:
Shows how to use `AttendiMicrophone` in a simple Jetpack Compose view without a ViewModel.

2. `RecorderStreamingScreenView`:
Demonstrates how to use the low-level `AttendiRecorder` directly, without integrating the `AttendiMicrophone` UI component.
Ideal for custom UIs or advanced use cases that require full control over recording flow.

3. `SoapScreenView`:
Integrates `AttendiMicrophone` into a complex Compose layout with multiple TextField views.
Also demonstrates:
a. How to disable the default permission denied alert (showsDefaultPermissionsDeniedAlert = false)
b. How to present a custom alert using the onRecordingPermissionDeniedCallback
 
4. `TwoMicrophonesStreamingScreenView`:
Illustrates how to use two `AttendiMicrophone` components in the same view.
Each microphone operates independently with its own configuration and recorder instance, useful for multi-source streaming or comparative audio capture scenarios.

### Recorder Plugins

We offer out of the box plugins to be used with specific purposes:

* `AttendiSyncTranscribePlugin` – Sends audio to a backend sync transcription API, such as Attendi’s. Designed to be extensible to support other providers as well
* `AttendiAsyncTranscribePlugin` – Real-time transcription using a WebSocket-based connection, such as Attendi’s, with support for custom or alternative streaming APIs
* `AttendiAudioNotificationPlugin` – Audible start/stop cues
* `AttendiErrorPlugin` – Plays error sound and vibrates on failure
* `AttendiStopOnAudioFocusLossPlugin` – Stops recording on focus loss

## Creating an AttendiRecorderPlugin

Plugins allow the `AttendiMicrophone` and `AttendiRecorder` component's functionality to be extended. The component exposes a plugin API consisting of functions that e.g. allow plugins to execute arbitrary logic at certain points in the component's lifecycle. A plugin is a class that inherits from the `AttendiRecorderPlugin` class.

The functionality of any plugin is implemented in its `activate` method. This method is called when the recorder is first initialized, and takes as input a reference to the recorderModel `AttendiRecorderModel`. Any logic that needs to run when the microphone is removed from the view should be implemented in the `deactivate` method. This might for instance be necessary when the plugin changes some global state. As an example, the `AttendiAsyncTranscribePlugin` plugin on activate it hooks the model to onStartRecording to create a service connection and on deactivate it closes the connection.

```kotlin
class AttendiAsyncTranscribePlugin(private val service: AsyncTranscribeService) {

    override suspend fun activate(model: AttendiRecorderModel) {
        model.onStartRecording {
           try {
             val serviceListener = createServiceListener(model = model)
             service.connect(listener = serviceListener)
           } catch (exception: Exception) { }
        }
    }

    override suspend fun deactivate(model: AttendiRecorderModel) {
        closeConnection()
    }
}
```

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

The project structure is organized as follows:

* AttendiSpeechService/
The core SDK framework target. This is the codebase for the Attendi Speech Service.

* AttendiSpeechServiceExample/
A sample Android app demonstrating how to integrate and use the SDK.

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
