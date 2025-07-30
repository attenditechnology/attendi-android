package nl.attendi.attendispeechservice.services.authentication

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import nl.attendi.attendispeechservice.services.authentication.dto.request.AttendiAuthenticationRequestBody
import nl.attendi.attendispeechservice.services.authentication.dto.response.AttendiAuthenticationResponse
import nl.attendi.attendispeechservice.services.AttendiTranscribeAPIConfig
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Default implementation of [AttendiAuthenticationService] provided by the Attendi SDK.
 *
 * This implementation knows how to communicate with Attendi's backend to obtain
 * a valid access token for transcription-related services.
 *
 */
internal object AttendiAuthenticationServiceImpl : AttendiAuthenticationService {

    /** Authenticate endpoint path for calling the Attendi authenticate service. */
    private const val AUTHENTICATE_ENDPOINT = "v1/identity/authenticate"

    override suspend fun authenticate(apiConfig: AttendiTranscribeAPIConfig): String {
        val authenticateRequest = AttendiAuthenticationRequestBody(
            userId = apiConfig.userId,
            unitId = apiConfig.unitId,
            userAgent = apiConfig.userAgent
        )

        val token = authenticate(
            requestBody = authenticateRequest,
            customerKey = apiConfig.customerKey,
            apiBaseURL = apiConfig.apiBaseURL
        )
        return token
    }

    /**
     * Request an authentication token from Attendi's identity service.
     *
     * If the request is successful, the token is returned. Otherwise, `null` is returned.
     */
    private suspend fun authenticate(
        requestBody: AttendiAuthenticationRequestBody,
        customerKey: String,
        apiBaseURL: String,
    ): String {
        val requestBodyEncoded = Json.encodeToString(requestBody)

        return withContext(Dispatchers.IO) {
            val url = "$apiBaseURL/$AUTHENTICATE_ENDPOINT"

            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; utf-8")
            // Use the customer key to authenticate the request.
            connection.setRequestProperty("x-api-key", customerKey)

            connection.doOutput = true
            connection.outputStream.use { os ->
                val writer = OutputStreamWriter(os, "UTF-8")
                writer.write(requestBodyEncoded)
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val jsonResponse = connection.inputStream.bufferedReader().use { it.readText() }
                val response =
                    Json.decodeFromString(AttendiAuthenticationResponse.serializer(), jsonResponse)
                return@withContext response.token
            }

            throw NullPointerException("No token received")
        }
    }
}