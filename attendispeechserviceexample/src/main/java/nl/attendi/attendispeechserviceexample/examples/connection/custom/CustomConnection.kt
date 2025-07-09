package nl.attendi.attendispeechserviceexample.examples.connection.custom

import kotlinx.coroutines.delay
import nl.attendi.attendispeechservice.domain.connection.AttendiConnection
import nl.attendi.attendispeechservice.domain.connection.AttendiConnectionListener

object CustomConnection : AttendiConnection {

    private var listener: AttendiConnectionListener? = null

    override suspend fun connect(listener: AttendiConnectionListener) {
        this.listener = listener

        listener.onOpen()

        delay(1000L)
        listener.onMessage("Message 1")

        delay(2000L)
        listener.onMessage("Message 2")

        delay(3000L)
        listener.onMessage("Message 3")

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