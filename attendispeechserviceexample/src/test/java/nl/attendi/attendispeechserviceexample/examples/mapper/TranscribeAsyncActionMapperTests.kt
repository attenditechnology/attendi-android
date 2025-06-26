package nl.attendi.attendispeechserviceexample.examples.mapper

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import nl.attendi.attendispeechserviceexample.examples.data.transcribeasyncservice.dto.response.TranscribeAsyncResponse
import nl.attendi.attendispeechserviceexample.examples.domain.model.TranscribeAsyncAction
import nl.attendi.attendispeechserviceexample.examples.domain.model.TranscribeAsyncAddAnnotationEntityType
import nl.attendi.attendispeechserviceexample.examples.domain.model.TranscribeAsyncAddAnnotationIntentStatus
import nl.attendi.attendispeechserviceexample.examples.domain.model.TranscribeAsyncAddAnnotationType
import nl.attendi.attendispeechserviceexample.examples.domain.model.TranscribeAsyncUpdateAnnotationEntityType
import nl.attendi.attendispeechserviceexample.examples.domain.model.TranscribeAsyncUpdateAnnotationType
import nl.attendi.attendispeechserviceexample.examples.utils.JsonFileReader
import org.junit.Assert
import org.junit.Test

class TranscribeAsyncActionMapperTests {

    private val json = Json { ignoreUnknownKeys = true }

    private fun makeResponse(fileName: String) : TranscribeAsyncResponse {
        val jsonResponse = JsonFileReader.read("data/transcribeasyncservice/dto/response/${fileName}")
        return json.decodeFromString<TranscribeAsyncResponse>(jsonResponse)
    }

    @Test
    fun map_whenResponseContainsAddAnnotationTranscriptionTentative_returnsListWithAddAnnotationTranscriptionTentativeAction() {
        val response = makeResponse("TranscribeAsyncSchemaAddAnnotationTranscriptionTentative")

        val model = TranscribeAsyncActionMapper.map(response)

        Assert.assertEquals(1, model.size)
        val addAnnotation = model[0] as TranscribeAsyncAction.AddAnnotation
        Assert.assertEquals("0d42a586-9f65-4bc1-925c-4361ef4a33cc", addAnnotation.action.id)
        Assert.assertEquals(0, addAnnotation.action.index)
        Assert.assertEquals(TranscribeAsyncAddAnnotationType.TranscriptionTentative, addAnnotation.parameters.type)
        Assert.assertEquals("0e74a828-9f62-448f-842c-45bff04d99a3", addAnnotation.parameters.id)
        Assert.assertEquals(0, addAnnotation.parameters.startCharacterIndex)
        Assert.assertEquals(5, addAnnotation.parameters.endCharacterIndex)
    }

    @Test
    fun map_whenResponseContainsAddAnnotationIntent_returnsListWithAddAnnotationIntentAction() {
        val response = makeResponse("TranscribeAsyncSchemaAddAnnotationIntent")

        val model = TranscribeAsyncActionMapper.map(response)

        Assert.assertEquals(2, model.size)
        val addAnnotationPending = model[0] as TranscribeAsyncAction.AddAnnotation
        Assert.assertEquals("07ca2023-cc1a-4f33-a077-9401ba621c15", addAnnotationPending.action.id)
        Assert.assertEquals(0, addAnnotationPending.action.index)
        val intentPending = addAnnotationPending.parameters.type as TranscribeAsyncAddAnnotationType.Intent
        Assert.assertEquals(TranscribeAsyncAddAnnotationIntentStatus.PENDING, intentPending.status)
        Assert.assertEquals("af262d26-80bd-41d9-97c1-1f9876fa7730", addAnnotationPending.parameters.id)
        Assert.assertEquals(0, addAnnotationPending.parameters.startCharacterIndex)
        Assert.assertEquals(8, addAnnotationPending.parameters.endCharacterIndex)

        val addAnnotationRecognized = model[1] as TranscribeAsyncAction.AddAnnotation
        Assert.assertEquals("07ca2023-cc1a-4f33-a077-9401ba621c16", addAnnotationRecognized.action.id)
        Assert.assertEquals(1, addAnnotationRecognized.action.index)
        val intentRecognized = addAnnotationRecognized.parameters.type as TranscribeAsyncAddAnnotationType.Intent
        Assert.assertEquals(TranscribeAsyncAddAnnotationIntentStatus.RECOGNIZED, intentRecognized.status)
        Assert.assertEquals("af262d26-80bd-41d9-97c1-1f9876fa7731", addAnnotationRecognized.parameters.id)
        Assert.assertEquals(0, addAnnotationRecognized.parameters.startCharacterIndex)
        Assert.assertEquals(8, addAnnotationRecognized.parameters.endCharacterIndex)
    }

    @Test
    fun map_whenResponseContainsAddAnnotationEntity_returnsListWithAddAnnotationEntityAction() {
        val response = makeResponse("TranscribeAsyncSchemaAddAnnotationEntity")

        val model = TranscribeAsyncActionMapper.map(response)

        Assert.assertEquals(1, model.size)
        val addAnnotation = model[0] as TranscribeAsyncAction.AddAnnotation
        Assert.assertEquals("17ca2023-cc1a-4f33-a077-9401ba621c15", addAnnotation.action.id)
        Assert.assertEquals(0, addAnnotation.action.index)
        val entity = addAnnotation.parameters.type as TranscribeAsyncAddAnnotationType.Entity
        Assert.assertEquals(TranscribeAsyncAddAnnotationEntityType.NAME, entity.type)
        Assert.assertEquals("Albert", entity.text)
        Assert.assertEquals("rf262d26-80bd-41d9-97c1-1f9876fa7730", addAnnotation.parameters.id)
        Assert.assertEquals(0, addAnnotation.parameters.startCharacterIndex)
        Assert.assertEquals(0, addAnnotation.parameters.endCharacterIndex)
    }

    @Test
    fun map_whenResponseContainsUpdateAnnotationTranscriptionTentative_returnsListWithUpdateAnnotationTranscriptionTentativeAction() {
        val response = makeResponse("TranscribeAsyncSchemaUpdateAnnotationTranscriptionTentative")

        val model = TranscribeAsyncActionMapper.map(response)

        Assert.assertEquals(1, model.size)
        val updateAnnotation = model[0] as TranscribeAsyncAction.UpdateAnnotation
        Assert.assertEquals("0d42a586-9f65-4bc1-925c-4361ef4a33cc", updateAnnotation.action.id)
        Assert.assertEquals(0, updateAnnotation.action.index)
        Assert.assertEquals(TranscribeAsyncUpdateAnnotationType.TranscriptionTentative, updateAnnotation.parameters.type)
        Assert.assertEquals("0e74a828-9f62-448f-842c-45bff04d99a3", updateAnnotation.parameters.id)
        Assert.assertEquals(0, updateAnnotation.parameters.startCharacterIndex)
        Assert.assertEquals(5, updateAnnotation.parameters.endCharacterIndex)
    }

    @Test
    fun map_whenResponseContainsUpdateAnnotationEntity_returnsListWithUpdateAnnotationEntityAction() {
        val response = makeResponse("TranscribeAsyncSchemaUpdateAnnotationEntity")

        val model = TranscribeAsyncActionMapper.map(response)

        Assert.assertEquals(1, model.size)
        val updateAnnotation = model[0] as TranscribeAsyncAction.UpdateAnnotation
        Assert.assertEquals("17ca2023-cc1a-4f33-a077-9401ba621c15", updateAnnotation.action.id)
        Assert.assertEquals(0, updateAnnotation.action.index)
        val entity = updateAnnotation.parameters.type as TranscribeAsyncUpdateAnnotationType.Entity
        Assert.assertEquals(TranscribeAsyncUpdateAnnotationEntityType.NAME, entity.type)
        Assert.assertEquals("Albert", entity.text)
        Assert.assertEquals("rf262d26-80bd-41d9-97c1-1f9876fa7730", updateAnnotation.parameters.id)
        Assert.assertEquals(0, updateAnnotation.parameters.startCharacterIndex)
        Assert.assertEquals(0, updateAnnotation.parameters.endCharacterIndex)
    }

    @Test
    fun map_whenResponseContainsReplaceText_returnsListWithReplaceTextAction() {
        val response = makeResponse("TranscribeAsyncSchemaReplaceText")

        val model = TranscribeAsyncActionMapper.map(response)

        Assert.assertEquals(1, model.size)
        val replaceTextAnnotation = model[0] as TranscribeAsyncAction.ReplaceText
        Assert.assertEquals("b05bddd0-0577-47d6-b65b-13170c27596a", replaceTextAnnotation.action.id)
        Assert.assertEquals(0, replaceTextAnnotation.action.index)
        Assert.assertEquals("hallo", replaceTextAnnotation.parameters.text)
        Assert.assertEquals(0, replaceTextAnnotation.parameters.startCharacterIndex)
        Assert.assertEquals(0, replaceTextAnnotation.parameters.endCharacterIndex)
    }

    @Test
    fun map_whenResponseContainsRemoveAnnotation_returnsListWithRemoveAnnotationAction() {
        val response = makeResponse("TranscribeAsyncSchemaRemoveAnnotation")

        val model = TranscribeAsyncActionMapper.map(response)

        Assert.assertEquals(1, model.size)
        val removeAnnotation = model[0] as TranscribeAsyncAction.RemoveAnnotation
        Assert.assertEquals("1488702d-86a5-4313-89a4-e0589d724933", removeAnnotation.action.id)
        Assert.assertEquals(0, removeAnnotation.action.index)
        Assert.assertEquals("0e74a828-9f62-448f-842c-45bff04d99a3", removeAnnotation.parameters.id)
    }

    @Test
    fun map_whenResponseContainsMixedAnnotations_returnsListWithMixedAnnotationActions() {
        val response = makeResponse("TranscribeAsyncSchemaMixedAnnotations")

        val model = TranscribeAsyncActionMapper.map(response)

        Assert.assertEquals(3, model.size)
        val removeAnnotation = model[0] as TranscribeAsyncAction.RemoveAnnotation
        Assert.assertEquals("f59412fd-90db-402d-a96c-09ece06aba0f", removeAnnotation.action.id)
        Assert.assertEquals(35, removeAnnotation.action.index)
        Assert.assertEquals("22ab4ed5-1ed2-4e6a-bde4-7e5e4005f129", removeAnnotation.parameters.id)

        val replaceTextAnnotation = model[1] as TranscribeAsyncAction.ReplaceText
        Assert.assertEquals("08665124-0100-45ff-97b0-fff6dd118a88", replaceTextAnnotation.action.id)
        Assert.assertEquals(36, replaceTextAnnotation.action.index)
        Assert.assertEquals(" een mg", replaceTextAnnotation.parameters.text)
        Assert.assertEquals(19, replaceTextAnnotation.parameters.startCharacterIndex)
        Assert.assertEquals(22, replaceTextAnnotation.parameters.endCharacterIndex)

        val addAnnotation = model[2] as TranscribeAsyncAction.AddAnnotation
        Assert.assertEquals("3f34eb1a-4d10-4bc3-9c34-b40a7624d57b", addAnnotation.action.id)
        Assert.assertEquals(37, addAnnotation.action.index)
        Assert.assertEquals(TranscribeAsyncAddAnnotationType.TranscriptionTentative, addAnnotation.parameters.type)
        Assert.assertEquals("e870b00f-c9d0-435c-8997-686bc6c9cb86", addAnnotation.parameters.id)
        Assert.assertEquals(19, addAnnotation.parameters.startCharacterIndex)
        Assert.assertEquals(26, addAnnotation.parameters.endCharacterIndex)
    }
}