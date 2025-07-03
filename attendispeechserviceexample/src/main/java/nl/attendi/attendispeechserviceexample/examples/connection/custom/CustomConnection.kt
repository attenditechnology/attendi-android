package nl.attendi.attendispeechserviceexample.examples.connection.custom

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.attendi.attendispeechservice.domain.connection.AttendiConnection
import nl.attendi.attendispeechservice.domain.connection.AttendiConnectionListener

object CustomConnection : AttendiConnection {

    private var listener: AttendiConnectionListener? = null

    private var isConnected = false
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun connect(listener: AttendiConnectionListener) {
        this.listener = listener

        listener.onOpen()
        isConnected = true

        scope.launch {
            delay(1000L)
            listener.onMessage("Message 1")

            delay(2000L)
            listener.onMessage("Message 2")

            delay(3000L)
            listener.onMessage("Message 3")
        }
    }

    override fun disconnect() {
        scope.cancel()
        isConnected = false
    }

    override fun isConnected(): Boolean = isConnected

    override suspend fun send(message: ByteArray): Boolean {
        return true
    }

    override suspend fun send(message: String): Boolean {
        return true
    }
}