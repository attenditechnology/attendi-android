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

package nl.attendi.attendispeechservice.components.attendimicrophone.plugins

import nl.attendi.attendispeechservice.components.attendimicrophone.AttendiMicrophoneState

/**
 * Describes a plugin for the AttendiMicrophone component. Plugins can be used to add functionality
 * to the microphone without having to modify the core component.
 *
 * For example, the [AttendiTranscribePlugin] adds transcription functionality.
 *
 * Plugins define their functionality in the [activate] method. This method is called by the
 * AttendiMicrophone component at initialization time. The [activate] method is passed the
 * [AttendiMicrophoneState] object, which can be used to access the microphone's state and
 * extend functionality. For all the available methods, see the [AttendiMicrophoneState] class.
 *
 * If a plugin needs to perform cleanup when the microphone is deactivated, it can do so in the
 * [deactivate] method. This method is called by the AttendiMicrophone component when the
 * microphone is disposed.
 */
interface AttendiMicrophonePlugin {
    /**
     * Called by the AttendiMicrophone component when the microphone is initialized. Your plugin
     * logic goes here.
     */
    fun activate(state: AttendiMicrophoneState) {}

    /**
     * Called by the AttendiMicrophone component when the microphone is disposed of. Any cleanup
     * logic goes here.
     */
    fun deactivate(state: AttendiMicrophoneState) {}
}
