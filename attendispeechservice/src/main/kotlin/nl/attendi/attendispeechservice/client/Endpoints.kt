/// Copyright 2023 Attendi Technology B.V.
/// 
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
/// 
///     http://www.apache.org/licenses/LICENSE-2.0
/// 
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.

package nl.attendi.attendispeechservice.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

const val defaultAttendiBaseURL = "https://api.attendi.nl"

const val transcribeEndpoint = "v1/speech/transcribe"
const val authenticateEndpoint = "v1/identity/authenticate"

/**
 * Transcribe audio using Attendi's transcribe API.
 */
suspend fun transcribe(
    requestBody: BaseAudioTaskRequestBody,
    customerKey: String,
    apiBaseURL: String? = null,
): String? {
    val apiURL = apiBaseURL ?: defaultAttendiBaseURL
    val requestBodyEncoded = Json.encodeToString(requestBody)

    return withContext(Dispatchers.IO) { // runs at I/O level and frees the Main thread
        val url = "$apiURL/$transcribeEndpoint"

        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json; utf-8")
        // Use the customer key to authenticate the request
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
                Json.decodeFromString(TranscriptionResponse.serializer(), jsonResponse)
            return@withContext response.transcript
        }
        return@withContext null
    }
}

/**
 * Request an authentication token from Attendi's identity service.
 *
 * If the request is successful, the token is returned. Otherwise, `null` is returned.
 */
suspend fun authenticate(
    requestBody: AuthenticateRequestBody,
    customerKey: String,
    apiBaseURL: String? = null,
): String? {
    val apiURL = apiBaseURL ?: defaultAttendiBaseURL
    val requestBodyEncoded = Json.encodeToString(requestBody)

    return withContext(Dispatchers.IO) { // runs at I/O level and frees the Main thread
        val url = "$apiURL/$authenticateEndpoint"

        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json; utf-8")
        // Use the customer key to authenticate the request
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
                Json.decodeFromString(AuthenticationResponse.serializer(), jsonResponse)
            return@withContext response.token
        }
        return@withContext null
    }
}

/**
 * Requests to Attendi's transcribe-like APIs usually require the same base request body.
 */
@Serializable
data class BaseAudioTaskRequestBody(
    val audio: String,
    val userId: String,
    val unitId: String,
    val metadata: Metadata,
    val config: Config,
    val sessionUuid: String,
    val reportUuid: String
)

@Serializable
data class Metadata(
    val userAgent: String
)

@Serializable
data class Config(
    val model: String
)

@Serializable
data class TranscriptionResponse(val transcript: String)

@Serializable
data class AuthenticationResponse(val token: String)
