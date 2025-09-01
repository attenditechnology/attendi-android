package nl.attendi.attendispeechservice.audio

import kotlin.math.sqrt

/**
 * Represents a single frame of recorded audio data, which contains multiple samples.
 *
 * @property samples The raw 16-bit PCM audio samples in this frame. In the future we will
 * allow different types of samples supporting other PCM configurations.
 */
data class AudioFrame(
    val samples: List<Short>
) {
    /**
     * Retrieves the volume of the audio signal using the rootMeanSquare (RMS) standard.
     */
    fun getVolume(): Double {
        if (samples.isEmpty()) return 0.0
        val sum = samples.fold(0.0) { acc, element -> acc + element * element }
        val rms = sqrt(sum / samples.size)
        return rms.takeIf { it.isFinite() } ?: 0.0
    }
}
