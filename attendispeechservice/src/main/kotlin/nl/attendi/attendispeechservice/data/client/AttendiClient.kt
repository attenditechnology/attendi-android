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

package nl.attendi.attendispeechservice.data.client

import kotlinx.serialization.Serializable
import java.util.UUID

/** This class knows how to communicate with Attendi's backend APIs. */
class AttendiClient(
    private val apiConfig: TranscribeAPIConfig? = null,
    private val reportId: String = UUID.randomUUID().toString(),
    private val sessionId: String = UUID.randomUUID().toString(),
) {

    /**
     * Transcribe a base-64 encoded wav file.
     *
     * @param audioEncoded base-64 encoded wav file recorded at a sample rate of 16 KHz,
     * with the audio data being represented using 16-bit signed integers.
     * @param apiConfigOverride Use to override the API config settings provided to the client
     * in [apiConfig] if necessary.
     */
    suspend fun transcribe(
        audioEncoded: String, apiConfigOverride: TranscribeAPIConfig? = null
    ): String? {
        if (apiConfigOverride == null && apiConfig == null) {
            throw Exception("No API config provided")
        }

        val audioTaskRequest = BaseAudioTaskRequestBody(
            audio = audioEncoded,
            userId = apiConfigOverride?.userId ?: apiConfig?.userId
            ?: throw Exception("No userId provided"),
            unitId = apiConfigOverride?.unitId ?: apiConfig?.unitId
            ?: throw Exception("No unitId provided"),
            metadata = Metadata(
                userAgent = apiConfigOverride?.userAgent ?: apiConfig?.userAgent
                ?: "userAgent-not-set"
            ),
            config = Config(
                model = (apiConfigOverride?.modelType ?: apiConfig?.modelType
                ?: throw Exception("No model type provided")).toString()
            ),
            sessionUuid = sessionId,
            reportUuid = reportId
        )

        return transcribe(
            requestBody = audioTaskRequest,
            customerKey = apiConfigOverride?.customerKey ?: apiConfig?.customerKey
            ?: throw Exception(
                "No customerKey provided"
            ),
            apiBaseURL = apiConfigOverride?.apiURL ?: apiConfig?.apiURL
        )
    }

    fun getApiURL(apiConfigOverride: TranscribeAPIConfig? = null): String? {
        return apiConfigOverride?.apiURL ?: apiConfig?.apiURL
    }


    /**
     * Request to receive a token that can be used to authenticate subsequent requests to different
     * API endpoints.
     *
     * @param apiConfigOverride Use to override the API config settings provided to the client
     * in [apiConfig] if necessary.
     */
    suspend fun authenticate(
        apiConfigOverride: TranscribeAPIConfig? = null
    ): String {
        if (apiConfigOverride == null && apiConfig == null) {
            throw Exception("No API config provided")
        }

        val authenticateRequest = AuthenticateRequestBody(
            userId = apiConfigOverride?.userId ?: apiConfig?.userId
            ?: throw Exception("No userId provided"),
            unitId = apiConfigOverride?.unitId ?: apiConfig?.unitId
            ?: throw Exception("No unitId provided"),
            userAgent = apiConfigOverride?.userAgent ?: apiConfig?.userAgent
        )

        return authenticate(
            requestBody = authenticateRequest,
            customerKey = apiConfigOverride?.customerKey ?: apiConfig?.customerKey
            ?: throw Exception(
                "No customerKey provided"
            ),
            apiBaseURL = apiConfigOverride?.apiURL ?: apiConfig?.apiURL
        ) ?: throw Exception("No token received")
    }
}

@Serializable
data class AuthenticateRequestBody(
    val userId: String,
    val unitId: String,
    val userAgent: String?
)

/** Bundles up the information necessary to communicate with Attendi's speech understanding APIs. */
data class TranscribeAPIConfig(
    /**
     * URL of the Attendi Speech Service API, e.g. `https://api.attendi.nl` or
     * `https://sandbox.api.attendi.nl`.
     */
    val apiURL: String = "https://api.attendi.nl",
    /**
     * Your customer API key.
     */
    val customerKey: String,
    /**
     * Unique id assigned (by you) to your user
     */
    val userId: String,
    /**
     * Unique id assigned (by you) to the team or location of your user.
     */
    val unitId: String,
    /**
     * User agent string identifying the user device, OS and browser.
     */
    val userAgent: String? = null,
    /**
     * Which model to use, e.g. ModelType.ResidentialCare or "ResidentialCare".
     */
    val modelType: ModelType,
)

/** Attendi serves multiple speech-to-text models. These are the types of models available. */
enum class ModelType {
    ResidentialCare, DistrictCare,
}
