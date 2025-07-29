package nl.attendi.attendispeechservice.services.transcribe

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import nl.attendi.attendispeechservice.services.AttendiTranscribeAPIConfig
import nl.attendi.attendispeechservice.services.transcribe.dto.request.AttendiTranscribeRequestBody
import nl.attendi.attendispeechservice.services.transcribe.dto.request.AttendiTranscribeRequestConfig
import nl.attendi.attendispeechservice.services.transcribe.dto.request.AttendiTranscribeRequestMetadata
import nl.attendi.attendispeechservice.services.transcribe.dto.response.AttendiTranscribeResponse
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Default implementation of [TranscribeService] responsible for sending recorded audio
 * to Attendi's synchronous speech-to-text transcription API.
 *
 * This class encapsulates the details of HTTP communication with Attendi's backend,
 * including request formatting and endpoint construction. It uses the provided
 * [AttendiTranscribeAPIConfig] for authentication and base URL configuration.
 *
 * @param apiConfig Configuration for accessing the Attendi API (e.g., base URL, credentials).
 * @param reportId Unique identifier for the report being transcribed. Used for backend tracking and association.
 */
internal class AttendiTranscribeServiceImpl(
    private val apiConfig: AttendiTranscribeAPIConfig,
    private val reportId: String
) : TranscribeService {
    private companion object {
        /** Transcribe endpoint path for calling the Attendi transcribe service. */
        const val TRANSCRIBE_ENDPOINT = "v1/speech/transcribe"
    }

    override suspend fun transcribe(audioEncoded: String): String {
        val audioTaskRequest = AttendiTranscribeRequestBody(
            audio = audioEncoded,
            userId = apiConfig.userId,
            unitId = apiConfig.unitId,
            metadata = AttendiTranscribeRequestMetadata(
                userAgent = apiConfig.userAgent ?: "userAgent-not-set",
                reportId = reportId
            ),
            config = AttendiTranscribeRequestConfig(
                model = (apiConfig.modelType
                    ?: throw Exception("No model type provided")).toString()
            )
        )

        return transcribe(
            requestBody = audioTaskRequest,
            customerKey = apiConfig.customerKey,
            apiBaseURL = apiConfig.apiBaseURL
        )
    }

    /**
     * Transcribe audio using Attendi's transcribe API.
     */
    private suspend fun transcribe(
        requestBody: AttendiTranscribeRequestBody,
        customerKey: String,
        apiBaseURL: String,
    ): String {
        val requestBodyEncoded = Json.encodeToString(requestBody)

        return withContext(Dispatchers.IO) {
            val url = "$apiBaseURL/$TRANSCRIBE_ENDPOINT"

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
                val response = Json.decodeFromString<AttendiTranscribeResponse>(jsonResponse)
                return@withContext response.transcript
            }
            throw NullPointerException("No transcribe received")
        }
    }
}