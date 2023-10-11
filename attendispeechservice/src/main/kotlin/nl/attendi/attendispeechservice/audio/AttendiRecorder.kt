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

package nl.attendi.attendispeechservice.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

const val AUDIO_SAMPLE_RATE = 16000

private const val AUDIO_SOURCE = MediaRecorder.AudioSource.MIC
private const val AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_MONO
private const val AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT

// Calculate minimum buffer size
private val minBufferSize =
    AudioRecord.getMinBufferSize(
        AUDIO_SAMPLE_RATE,
        AUDIO_CHANNEL,
        AUDIO_ENCODING
    )

private val bufferSize = if (minBufferSize <= 1) 2560 else 2 * minBufferSize

/**
 * Wraps the lower-level `AudioRecord` APIs to provide a convenient interface for recording audio
 * from the device. Curently the audio samples are sampled at a sample rate of 16KHz, and represented as
 * 16-bit signed integers. The samples are accumulated in [AttendiRecorder._buffer].
 */
@SuppressLint("MissingPermission")
class AttendiRecorder {
    enum class RecordingState {
        Recording, Stopped
    }

    /**
     * The `AudioRecord` instance (Android low-level recording API) used to record audio.
     */
    private var audioRecord: AudioRecord? = null

    /**
     * Used to read data from the microphone / AudioRecord instance. Only holds one read worth of data.
     * The data is then appended to a list of all audio samples.
     */
    private val temporaryAudioBuffer = ShortArray(bufferSize)

    /**
     * The buffer that holds all audio samples. We use `_buffer` internally to make sure clients
     * only have read-access to the buffer.
     */
    private val _buffer = mutableListOf<Short>()

    /**
     * A read-only view of the audio buffer that can be accessed by clients.
     */
    val buffer: List<Short> get() = _buffer

    // We launch the actual recording in a coroutine, so we can update the UI while recording
    // and not block the main thread. We also use a Job to keep track of the coroutine, so we can
    // cancel it when the user stops recording.
    var recordAudioJob: Job? = null

    var state: RecordingState = RecordingState.Stopped
        private set

    /**
     * Start recording audio. The audio samples are sampled at a sample rate of 16KHz,
     * and represented as 16-bit signed integers. The samples are accumulated in [_buffer].
     */
    suspend fun startRecording() {
        audioRecord = AudioRecord(
            AUDIO_SOURCE, AUDIO_SAMPLE_RATE, AUDIO_CHANNEL, AUDIO_ENCODING, bufferSize
        )

        audioRecord?.startRecording()
        state = RecordingState.Recording

        coroutineScope {
            recordAudioJob = launch(Dispatchers.Default) {
                while (true) {
                    val readSize = audioRecord?.read(temporaryAudioBuffer, 0, bufferSize) ?: 0
                    if (readSize <= 0) continue

                    val samples = temporaryAudioBuffer.toList().take(readSize)
                    for (callback in audioFrameCallbacks) {
                        callback(samples)
                    }

                    val newSignalEnergy = rootMeanSquare(samples)
                    for (callback in signalEnergyCallbacks) {
                        callback(newSignalEnergy)
                    }

                    withContext(Dispatchers.Main) {
                        _buffer.addAll(samples)
                    }
                }
            }
        }
    }

    /**
     * Stop recording audio and release the recording resources.
     */
    fun stopRecording() {
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        recordAudioJob?.cancel()
        recordAudioJob = null
        state = RecordingState.Stopped
    }

    /** Clear the recorder's buffer's stored samples. */
    fun clearBuffer() {
        _buffer.clear()
    }

    private var signalEnergyCallbacks: MutableList<suspend (Double) -> Unit> = mutableListOf()
    private var audioFrameCallbacks: MutableList<suspend (List<Short>) -> Unit> = mutableListOf()

    /**
     * Register a callback that will be called when the signal energy (a measure of the volume)
     * changes. We currently measure the signal energy using RMS.
     *
     * @return A function that can be used to remove the added callback.
     */
    fun onSignalEnergy(callback: suspend (Double) -> Unit): () -> Unit {
        signalEnergyCallbacks.add(callback)
        return { signalEnergyCallbacks.remove(callback) }
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
        audioFrameCallbacks.add(callback)
        return { audioFrameCallbacks.remove(callback) }
    }
}

/**
 * RMS is a standard way to calculate the energy (volume) of the audio signal.
 */
fun rootMeanSquare(samples: List<Short>): Double {
    val sum = samples.fold(0.0) { acc, element -> acc + element * element }
    return sqrt(sum / samples.size)
}


