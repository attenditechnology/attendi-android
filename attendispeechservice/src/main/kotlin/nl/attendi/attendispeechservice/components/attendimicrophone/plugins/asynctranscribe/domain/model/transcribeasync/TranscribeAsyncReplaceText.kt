package nl.attendi.attendispeechservice.components.attendimicrophone.plugins.asynctranscribe.domain.model.transcribeasync

data class TranscribeAsyncReplaceTextParameters(
    val startCharacterIndex: Int,
    val endCharacterIndex: Int,
    val text: String
)

object TranscribeAsyncReplaceTextMapper {
    /**
     * Replaces a portion of the text between the given indices with new content.
     *
     * @throws IllegalArgumentException if the indices are out of bounds.
     */
    fun map(
        original: String,
        action: TranscribeAsyncAction.ReplaceText
    ): String {
        val params = action.parameters
        require(params.startCharacterIndex in 0..original.length) { "startCharacterIndex out of bounds" }
        require(params.endCharacterIndex in params.startCharacterIndex..original.length) { "endCharacterIndex out of bounds" }

        return original.substring(0, params.startCharacterIndex) +
                params.text +
                original.substring(params.endCharacterIndex)
    }
}