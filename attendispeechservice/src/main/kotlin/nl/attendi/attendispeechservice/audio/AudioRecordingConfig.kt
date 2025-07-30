package nl.attendi.attendispeechservice.audio

import android.media.AudioFormat
import android.media.MediaRecorder

/**
 * Configuration for recording raw audio from the microphone.
 *
 * @property sampleRate The sample rate in Hz. Default is 16000 Hz, which is commonly used for voice processing.
 * @property channel The audio channel configuration. Default is [AudioFormat.CHANNEL_IN_MONO].
 * @property encoding The audio encoding format. Default is [AudioFormat.ENCODING_PCM_16BIT].
 * @property audioSource The source of audio input. Default is [MediaRecorder.AudioSource.MIC].
 *
 * This config is used by low-level audio components such as [AudioRecorder] to set up
 * the recording parameters.
 *
 * Important: Currently, only the default configuration values are supported.
 * Changing these may cause the application to crash or behave unpredictably.
 * Support for custom configurations may be added in future versions.
 * Consumers are advised to rely on defaults.
 */
data class AudioRecordingConfig(
    val sampleRate: Int = 16000,
    val channel: Int = AudioFormat.CHANNEL_IN_MONO,
    val encoding: Int = AudioFormat.ENCODING_PCM_16BIT,
    val audioSource: Int = MediaRecorder.AudioSource.MIC
)