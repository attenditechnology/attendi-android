package nl.attendi.attendispeechservice.services

/** Bundles up the information necessary to communicate with Attendi's speech understanding APIs. */
data class AttendiTranscribeAPIConfig(
    /**
     * URL of the Attendi Speech Service API, e.g. `https://api.attendi.nl`
     */
    val apiBaseURL: String = "https://api.attendi.nl",
    /**
     * URL of the Attendi WebSocket Service API, e.g. `wss://api.attendi.nl`
     */
    val webSocketBaseURL: String = "wss://api.attendi.nl",
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
     * Which model to use, e.g. "ResidentialCare".
     */
    val modelType: String? = null
)