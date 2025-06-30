package nl.attendi.attendispeechserviceexample.examples.utils

import java.nio.charset.StandardCharsets

/**
 * Utility object for reading JSON files from the classpath.
 *
 * This is primarily intended for use in unit tests where mock JSON responses
 * or configurations are stored as resource files.
 *
 * Usage example:
 * ```
 * val json = JsonFileReader.read("mock_response") // Reads mock_response.json from resources
 * ```
 *
 * @throws IllegalStateException if the specified JSON file cannot be found in the classpath.
 */
object JsonFileReader {

    /**
     * Reads the contents of a JSON file located in the test resources directory.
     *
     * The file should be placed under `src/test/resources` and should not include the `.json`
     * extension when passed to this method (it is appended automatically).
     *
     * @param fileName The base name of the JSON file (without `.json` extension).
     * @return The contents of the file as a [String], encoded in UTF-8.
     * @throws IllegalStateException if the file is not found in the classpath.
     */
    fun read(fileName: String): String {
        val inputStream = javaClass.classLoader?.getResourceAsStream("${fileName}.json") ?: error("JSON file not found")
        val json = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        return json
    }
}