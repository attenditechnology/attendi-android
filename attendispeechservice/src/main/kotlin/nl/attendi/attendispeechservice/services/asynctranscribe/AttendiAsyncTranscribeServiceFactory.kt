package nl.attendi.attendispeechservice.services.asynctranscribe

import nl.attendi.attendispeechservice.services.AttendiTranscribeAPIConfig
import nl.attendi.attendispeechservice.services.authentication.AttendiAuthenticationServiceImpl

/**
 * Factory object for creating instances of [AttendiAsyncTranscribeServiceImpl].
 */
object AttendiAsyncTranscribeServiceFactory {

    /**
     * Constructs a default implementation of [AsyncTranscribeService] using the provided [apiConfig].
     *
     * This service manages the WebSocket connection, authentication, and audio streaming.
     *
     * @param apiConfig Configuration for authentication and endpoint setup.
     * @param accessToken Optional pre-obtained access token. If provided, it will be used directly for
     * authentication instead of requesting a new token from the authentication service.
     * @return A fully configured instance of [AsyncTranscribeService].
     */
    fun create(
        apiConfig: AttendiTranscribeAPIConfig,
        accessToken: String? = null
    ): AsyncTranscribeService {
        return AttendiAsyncTranscribeServiceImpl(
            apiConfig = apiConfig,
            authenticationService = AttendiAuthenticationServiceImpl,
            accessToken = accessToken
        )
    }
}