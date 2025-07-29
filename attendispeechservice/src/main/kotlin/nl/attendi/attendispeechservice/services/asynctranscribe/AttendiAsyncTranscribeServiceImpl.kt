package nl.attendi.attendispeechservice.services.asynctranscribe

import nl.attendi.attendispeechservice.services.AttendiTranscribeAPIConfig
import nl.attendi.attendispeechservice.services.authentication.AttendiAuthenticationService
import okhttp3.Request

/**
 * Default WebSocket-based implementation of [AsyncTranscribeService] used for real-time speech-to-text transcription
 * with the Attendi backend.
 *
 * This class establishes and manages a bi-directional WebSocket connection to stream audio data
 * and receive transcription updates in real time. It handles the entire session lifecycle, including:
 * - Authentication via [AttendiAuthenticationService] (if an access token is not provided).
 * - Connection initialization using a JSON configuration message.
 * - Streaming encoded audio data.
 * - Receiving intermediate and final transcription messages.
 * - Graceful shutdown via end-of-stream signaling.
 * - Error propagation for connection, authentication, or decoding failures.
 *
 * Consumers can use this class directly or implement their own version of [AsyncTranscribeService]
 * if they require a custom transport or different protocol handling.
 *
 * @param apiConfig The API configuration required to connect to the Attendi backend, including endpoint and model.
 * @param authenticationService The service used to acquire an access token if one is not provided directly.
 * @param accessToken Optional bearer token used for authenticating the WebSocket connection. If null, the
 * service will fetch one using the provided [authenticationService].
 */
internal class AttendiAsyncTranscribeServiceImpl(
    private val apiConfig: AttendiTranscribeAPIConfig,
    private val authenticationService: AttendiAuthenticationService,
    private val accessToken: String? = null
) : BaseAsyncTranscribeService() {

    private companion object {
        /** WebSocket endpoint path for calling the Attendi transcribe async service. */
        const val WEBSOCKET_ENDPOINT = "v1/speech/transcribe/stream"
    }

    override suspend fun createWebSocketRequest(): Request {
        val accessToken = accessToken ?: authenticationService.authenticate(apiConfig = apiConfig)
        return createAttendiWebSocketRequest(accessToken)
    }

    override suspend fun onRetryAttempt(
        retryAttempt: Int,
        previousRequest: Request?,
        exception: Exception?
    ): Request {
        val accessToken = authenticationService.authenticate(apiConfig = apiConfig)
        return createAttendiWebSocketRequest(accessToken)
    }

    override fun getOpenMessage(): String {
        return AttendiAsyncTranscribeServiceMessages.initialConfiguration()
    }

    override fun getCloseMessage(): String {
        return AttendiAsyncTranscribeServiceMessages.close()
    }

    private fun createAttendiWebSocketRequest(accessToken: String): Request {
        return Request.Builder()
            .url(getWebSocketUrl())
            .addHeader("Authorization", "Bearer $accessToken")
            .build()
    }

    private fun getWebSocketUrl(): String {
        return "${apiConfig.webSocketBaseURL}/$WEBSOCKET_ENDPOINT"
    }
}