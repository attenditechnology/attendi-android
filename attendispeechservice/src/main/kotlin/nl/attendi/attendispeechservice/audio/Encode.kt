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

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Convert PCM audio data to the WAV file format by adding the WAV header.
 *
 * See e.g. http://soundfile.sapp.org/doc/WaveFormat/ for the WAV specification.
 *
 * Currently we assume 16-bit (signed integer) mono PCM audio data.
 */
@Throws(IOException::class)
fun pcmToWav(samples: List<Short>, sampleRate: Int): ByteArray {
    val output = ByteArrayOutputStream()
    // Assume 16-bit PCM
    val bitsPerSample = 16
    val bitsPerByte = 8
    // -> 2 bytes per sample
    val bytesPerSample: Int = bitsPerSample / bitsPerByte
    val totalDataLength = samples.size * bytesPerSample
    // How many bytes per second
    val byteRate = (bytesPerSample * sampleRate).toLong()

    val headerSize = 44
    // Values other than 1 indicate some form of compression.
    val formatPcm = 1
    // Mono
    val numChannels = 1
    // 1 * 2 = 2
    val blockAlign = numChannels * bytesPerSample

    // Write the WAV file header
    output.write("RIFF".toByteArray(Charsets.US_ASCII)) // chunk id
    output.write(intToByteArray(totalDataLength + headerSize - 8), 0, 4) // chunk size
    output.write("WAVE".toByteArray(Charsets.US_ASCII)) // format
    output.write("fmt ".toByteArray(Charsets.US_ASCII)) // subchunk 1 id
    output.write(intToByteArray(16), 0, 4) // subchunk 1 size
    output.write(shortToByteArray(formatPcm), 0, 2) // audio format (1 = PCM)
    output.write(shortToByteArray(numChannels), 0, 2) // number of channels
    output.write(intToByteArray(sampleRate), 0, 4) // sample rate
    output.write(intToByteArray(byteRate.toInt()), 0, 4) // byte rate
    output.write(shortToByteArray(blockAlign), 0, 2) // block align
    output.write(shortToByteArray(bitsPerSample), 0, 2) // bits per sample
    output.write("data".toByteArray(Charsets.US_ASCII)) // subchunk 2 id
    output.write(intToByteArray(totalDataLength), 0, 4) // subchunk 2 size

    // Write the audio data
    for (sample in samples) {
        output.write(shortToByteArray(sample.toInt()), 0, 2)
    }

    return output.toByteArray()
}

fun intToByteArray(i: Int): ByteArray {
    val buf = ByteBuffer.allocate(4)
    buf.order(ByteOrder.LITTLE_ENDIAN)
    buf.putInt(i)
    return buf.array()
}

fun shortToByteArray(s: Int): ByteArray {
    val buf = ByteBuffer.allocate(2)
    buf.order(ByteOrder.LITTLE_ENDIAN)
    buf.putShort(s.toShort())
    return buf.array()
}
