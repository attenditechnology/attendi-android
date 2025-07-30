package nl.attendi.attendispeechserviceexample.examples.services

import nl.attendi.attendispeechservice.services.AttendiTranscribeAPIConfig
import nl.attendi.attendispeechserviceexample.BuildConfig

object ExampleAttendiTranscribeAPI {

    val transcribeAPIConfig = AttendiTranscribeAPIConfig(
        apiBaseURL = "https://sandbox.api.attendi.nl",
        webSocketBaseURL = "wss://sandbox.api.attendi.nl",
        modelType = "DistrictCare",
        userAgent = "Android",
        customerKey = BuildConfig.ATTENDI_CUSTOMER_KEY,
        unitId = "unitId",
        userId = "userId",
    )
}