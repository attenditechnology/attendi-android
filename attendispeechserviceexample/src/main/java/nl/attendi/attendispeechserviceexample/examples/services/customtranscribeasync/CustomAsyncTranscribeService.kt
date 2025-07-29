package nl.attendi.attendispeechserviceexample.examples.services.customtranscribeasync

import kotlinx.coroutines.delay
import nl.attendi.attendispeechservice.services.asynctranscribe.AsyncTranscribeService
import nl.attendi.attendispeechservice.services.asynctranscribe.AsyncTranscribeServiceListener

object CustomAsyncTranscribeService : AsyncTranscribeService {

    private var listener: AsyncTranscribeServiceListener? = null

    override suspend fun connect(listener: AsyncTranscribeServiceListener) {
        CustomAsyncTranscribeService.listener = listener

        listener.onOpen()

        delay(1000L)
        listener.onMessage("Test Message")

        listener.onClose()
    }

    override suspend fun disconnect() {
        listener?.onClose()
    }

    override suspend fun send(message: ByteArray): Boolean {
        return true
    }

    override suspend fun send(message: String): Boolean {
        return true
    }
}