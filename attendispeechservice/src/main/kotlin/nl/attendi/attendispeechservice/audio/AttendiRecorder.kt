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
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.math.sqrt

const val AUDIO_SAMPLE_RATE = 16000

private const val AUDIO_SOURCE = MediaRecorder.AudioSource.MIC
private const val AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_MONO
private const val AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT

// Calculate minimum buffer size
private val minBufferSize = AudioRecord.getMinBufferSize(
    AUDIO_SAMPLE_RATE, AUDIO_CHANNEL, AUDIO_ENCODING
)

private val bufferSize = if (minBufferSize <= 1) 2560 else 2 * minBufferSize

/**
 * Wraps the lower-level `AudioRecord` APIs to provide a convenient interface for recording audio
 * from the device. Currently the audio samples are sampled at a sample rate of 16KHz, and represented as
 * 16-bit signed integers. The samples are accumulated in a file at location [AttendiRecorder.bufferFile].
 */
@SuppressLint("MissingPermission")
class AttendiRecorder(private val bufferFile: File) {
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

    // We launch the actual recording in a coroutine, so we can update the UI while recording
    // and not block the main thread. We also use a Job to keep track of the coroutine, so we can
    // cancel it when the user stops recording.
    private var recordAudioJob: Job? = null

    // The recorder's buffer is currently implemented as a file. When recording audio, we write
    // the audio samples using this output stream.
    private var bufferFileOutputStream: FileOutputStream? = null

    var recordingState: RecordingState = RecordingState.Stopped
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
        recordingState = RecordingState.Recording

        if (bufferFileOutputStream == null) {
            bufferFileOutputStream = withContext(Dispatchers.IO) {
                FileOutputStream(bufferFile, true)
            }
        }

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

                    val byteArray = shortsToByteArray(samples)
                    bufferFileOutputStream?.let {
                        withContext(Dispatchers.IO) {
                            it.write(byteArray)
                        }
                    }
                }
            }
        }
    }

    /**
     * Get the current buffer of audio samples.
     */
    val buffer: List<Short>
        get() {
            if (!bufferFile.exists()) return emptyList()

            return FileInputStream(bufferFile).use {
                val bytes = it.readBytes()
                byteArrayToShorts(bytes)
            }
        }

    /**
     * Stop recording audio and release the recording resources.
     */
    fun stopRecording() {
        // It's important that the job is cancelled before the audioRecord is stopped and released,
        // since the job uses the AudioRecord instance. If an error accessing the released instance
        // happens in the recording loop, it just keeps running forever, leading to buggy behavior.
        recordAudioJob?.cancel()
        recordAudioJob = null

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        recordingState = RecordingState.Stopped
    }

    fun clearBuffer() {
        if (bufferFileOutputStream != null) {
            bufferFileOutputStream?.close()
            bufferFileOutputStream = null
        }

        if (bufferFile.exists()) bufferFile.delete()
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

// Assumes the byte array contains 16-bit signed integers.
private fun byteArrayToShorts(byteArray: ByteArray): List<Short> {
    return byteArray.asList().chunked(2).asSequence()
        .map { ((it[0].toInt() and 0xFF) shl 8) or (it[1].toInt() and 0xFF) }.map { it.toShort() }
        .toList()
}

private fun shortsToByteArray(shorts: List<Short>): ByteArray {
    // Each Short is 2 bytes
    val byteArray = ByteArray(shorts.size * 2)

    for (i in shorts.indices) {
        val shortValue = shorts[i]
        // Most significant byte
        byteArray[i * 2] = (shortValue.toInt() shr 8).toByte()

        // Least significant byte
        byteArray[i * 2 + 1] = shortValue.toByte()
    }

    return byteArray
}


