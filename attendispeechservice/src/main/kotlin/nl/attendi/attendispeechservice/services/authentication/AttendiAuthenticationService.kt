package nl.attendi.attendispeechservice.services.authentication

import nl.attendi.attendispeechservice.services.AttendiTranscribeAPIConfig

/**
 * Interface for authenticating with Attendi's backend services.
 *
 * Implementations of this interface are responsible for retrieving a valid access token
 * that can be used to authorize API requests.
 *
 */
internal interface AttendiAuthenticationService {

    /**
     * Authenticates with the Attendi backend using the provided [apiConfig] and returns
     * a valid bearer token.
     *
     * @param apiConfig The configuration object containing credentials, client ID, or any
     * other information needed to authenticate with the API.
     * @return A valid access token to be used in subsequent API or WebSocket requests.
     * @throws Exception if the authentication fails or the response is invalid.
     */
    suspend fun authenticate(apiConfig: AttendiTranscribeAPIConfig): String
}