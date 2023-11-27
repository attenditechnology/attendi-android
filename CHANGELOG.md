# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
            override fun activate(state: AttendiMicrophoneState) {
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
