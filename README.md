# Attendi Speech Service for Android

The Attendi Speech Service Android SDK provides the `AttendiMicrophone` component: a `Jetpack Composable` microphone button that can be used to record audio and perform arbitrary tasks with that audio, such as audio transcription.

The component is built with extensibility in mind. It can be extended with plugins that add functionality to the component using the component's plugin APIs. Arbitrary logic can for instance be executed at certain points in the component's lifecycle, such as before recording starts, or when an error occurs.

The `AttendiClient` class provides an interface to easily communicate with the Attendi Speech Service backend APIs.

## Getting started

The SDK is available as a Kotlin Android library package.

### Installation instructions

You can install the package by either downloading it directly from the package page on GitHub Packages, by updating your app's build.gradle file:

```kotlin
implementation 'nl.attendi:attendispeechservice:<version>'
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

## Usage

After installing the package, you can use the microphone component in your project:

```kotlin
import nl.attendi.attendispeechservice.components.attendimicrophone.AttendiMicrophone

// within some ComponentActivity or Jetpack Composable

AttendiMicrophone(
    // We can use some general Compose modifiers here, such as `padding`, but some like
    // `size` won't work. In general, check first whether `MicrophoneModifier` has a parameter
    // to change the aspect of the microphone that you want to change.
    modifier = Modifier
        .border(1.dp, pinkColor, RoundedCornerShape(percent = 50))
        .background(Color.White),
    // Change aspects of the microphone's appearance, such as size and color,
    // using the `microphoneModifier` parameter
    microphoneModifier = MicrophoneModifier(size = 64.dp, color = pinkColor),
    // Add plugins if necessary. These extend the functionality of the microphone component.
    plugins = listOf(
        AttendiErrorPlugin(),
        AttendiTranscribePlugin(apiConfig = exampleAPIConfig),
    ),
    // Use `onState` to access the microphone's state that exposes its plugin APIs and other
    // useful information. In this case, we use it to listen to the microphone's UI state, which
    // we can use to show some UI conditional on this state.
    onState = { state ->
        state.onUIState {
            print(it)
        }
    },
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

In the example above, the `AttendiMicrophone` component is used to transcribe audio. The `AttendiTranscribePlugin` plugin adds the transcription functionality and the `AttendiErrorPlugin` plugin tells the component what to do when an error occurs.

For more details on the `AttendiMicrophone`'s API, see its docstring.

## Communicating with the `AttendiMicrophone` component

The `AttendiMicrophone` exposes two callbacks in its initializer: `onEvent` and `onResult`. The `onResult` callback can be called by plugins when they want to signal a result to the client when that result is in text (string) form. As seen in the example above, the text can be accessed by the client by providing a closure to the `onResult` parameter.

The `onEvent` callback can be called by plugins when they want to signal a more general event to the client. Plugins can call `onEvent` and pass it an event name and a result object. The client can then listen for these events by providing a closure to the `onEvent` parameter. The client can then check the event name and the result object to determine what to do.

## Styling

The microphone component can be styled using the `microphoneModifier` parameter. See the `MicrophoneModifier`'s docstring for more details.

## Creating a plugin

**Warning: the microphone's plugin APIs are still under development and subject to change.**

Plugins allow the microphone component's functionality to be extended. The component exposes a plugin API consisting of functions that e.g. allow plugins to execute arbitrary logic at certain points in the component's lifecycle. A plugin is a class that implements the `AttendiMicrophonePlugin` interface.

The functionality of any plugin is implemented in its `activate` method. This method is called when the microphone is first initialized, and takes as input a reference to the state of the corresponding microphone component, which allows us to modify that state and change the behavior of the component. Any logic that needs to run when the microphone is removed from the view should be implemented in the `deactivate` method. This might for instance be necessary when the plugin changes some global state. As an example, the `AttendiErrorPlugin` plugin is implemented as follows:

```kotlin
class AttendiErrorPlugin : AttendiMicrophonePlugin {
    // The `activate` method is called when the microphone is first initialized and takes as input a reference to the microphone component's state.
    override fun activate(state: AttendiMicrophoneState) {
        // Use the `mic.onError` plugin API to add a callback that is called when an error occurs.
        state.onError {
            // The `showDialog` API shows a dialog to the user.
            state.showDialog {
                AlertDialog(onDismissRequest = {
                    state.isDialogOpen = false
                },
                    title = { Text(state.context.getString(R.string.error_title)) },
                    text = { Text(state.context.getString(R.string.error_body, it.message)) },
                    confirmButton = {
                        Button(onClick = { state.isDialogOpen = false }) {
                            Text(state.context.getString(R.string.ok))
                        }
                    })
            }

            // Vibrate the device.
            state.vibrate()
        }
    }
}
```

## Issues

If you encounter any issues, don't hesitate to contact us at `omar@attendi.nl`.
