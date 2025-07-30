package nl.attendi.attendispeechservice.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.coroutineContext

/**
 * Default implementation of [AudioRecorder] that handles low-level audio recording operations.
 *
 * [AudioRecorderImpl] is designed as a singleton to ensure safe and consistent access to the underlying
 * [AudioRecord] instance, which supports only one active recording session at a time.
 * By encapsulating the recording logic within a singleton object, we prevent conflicts and maintain control
 * over resource usage and lifecycle management.
 */
internal object AudioRecorderImpl : AudioRecorder {

    /**
     * The `AudioRecord` instance (Android low-level recording API) used to record audio.
     */
    private var audioRecord: AudioRecord? = null

    /**
     * We launch the actual recording in a coroutine, so we can update the UI while recording
     * and not block the main thread. We also use a Job to keep track of the coroutine, so we can
     * cancel it when the user stops recording.
     */
    private val recorderMutex = Mutex()

    /**
     * Used to read data from the microphone / AudioRecord instance. Only holds one read worth of data.
     * The data is then appended to a list of all audio samples.
     */
    private var temporaryAudioBuffer: ShortArray = ShortArray(0)

    override suspend fun isRecording(): Boolean {
        return isRecordingInternal
    }

    private var isRecordingInternal: Boolean = false

    override suspend fun startRecording(
        audioRecordingConfig: AudioRecordingConfig,
        onAudio: suspend (AudioFrame) -> Unit
    ) {
        recorderMutex.withLock {
            if (isRecordingInternal) {
                throw AudioRecorderException.AlreadyRecording
            }

            verifyAudioRecordingConfig(audioRecordingConfig)

            /**
             * This returns the minimum number of bytes that AudioRecord needs to avoid underruns
             * (i.e., gaps in recording).
             */
            val minBufferSize = AudioRecord.getMinBufferSize(
                audioRecordingConfig.sampleRate,
                audioRecordingConfig.channel,
                audioRecordingConfig.encoding
            )

            /**
             * If minBufferSize is invalid or zero, use a fallback (2560 bytes).
             * Otherwise, use double the minimum buffer size for better stability.
             * This helps avoid missed samples or glitches in low-latency or noisy environments.
             */
            val bufferSize = if (minBufferSize <= 1) 2560 else 2 * minBufferSize
            temporaryAudioBuffer = ShortArray(bufferSize)

            try {
                @SuppressLint("MissingPermission")
                audioRecord = AudioRecord(
                    audioRecordingConfig.audioSource,
                    audioRecordingConfig.sampleRate,
                    audioRecordingConfig.channel,
                    audioRecordingConfig.encoding,
                    bufferSize
                ).apply {
                    startRecording()
                }
            } catch (e: IllegalArgumentException) {
                throw AudioRecorderException.UnsupportedAudioFormat(e.message ?: "Invalid Audio Format")
            }

            isRecordingInternal = true
        }

        try {
            while (true) {
                /**
                 * Ensures that job in the current context is active.
                 * If the job is no longer active, throws CancellationException and breaks the while loop.
                 */
                coroutineContext.ensureActive()
                val readSize =
                    audioRecord?.read(temporaryAudioBuffer, 0, temporaryAudioBuffer.size) ?: 0
                if (readSize <= 0) {
                    continue
                }
                val samples = temporaryAudioBuffer.toList().take(readSize)
                val audioFrame = AudioFrame(samples)
                onAudio(audioFrame)
            }
        } finally {
            stopRecording()
        }
    }

    private fun verifyAudioRecordingConfig(audioRecordingConfig: AudioRecordingConfig) {
        if (audioRecordingConfig.channel != AudioFormat.CHANNEL_IN_MONO) {
            throw AudioRecorderException.UnsupportedAudioFormat("Currently the only supported audio channel is AudioFormat.CHANNEL_IN_MONO")
        }

        if (audioRecordingConfig.encoding != AudioFormat.ENCODING_PCM_16BIT) {
            throw AudioRecorderException.UnsupportedAudioFormat("Currently the only supported audio encoding is AudioFormat.ENCODING_PCM_16BIT")
        }

        if (audioRecordingConfig.audioSource != MediaRecorder.AudioSource.MIC) {
            throw AudioRecorderException.UnsupportedAudioFormat("Currently the only supported audio source is MediaRecorder.AudioSource.MIC")
        }
    }

    override suspend fun stopRecording() {
        if (!isRecordingInternal) {
            return
        }
        isRecordingInternal = false

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
}