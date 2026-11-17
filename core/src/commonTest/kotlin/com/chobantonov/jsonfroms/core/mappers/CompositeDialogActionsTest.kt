package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.validation.OptimumCodeJsonSchemaValidator
import com.chobantonov.jsonfroms.core.validation.ValidateFunction
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

class CompositeDialogActionsTest {
    @Test fun cellOptionsOverrideCollectionAndTopLevelDefaults() {
        val cell = Json.parseToJsonElement("""{
          "showEmptyButton":false,"showRemoveButton":true,
          "okLabel":"cell.save","cancelLabel":"cell.cancel"
        }""").jsonObject
        val collection = Json.parseToJsonElement("""{
          "showEmptyButton":true,"showRemoveButton":false,
          "okLabel":"collection.save","emptyLabel":"collection.empty"
        }""").jsonObject
        assertEquals(CompositeDialogActions(false, true, "cell.save", "cell.cancel",
            "collection.empty", "form.remove"), resolveCompositeDialogActions(cell, collection,
            mapOf("showEmptyButton" to true, "showRemoveButton" to true,
                "okLabel" to "form.save", "removeLabel" to "form.remove")))
    }

    @Test fun defaultsAndExplicitFalseAreDistinct() {
        assertEquals(CompositeDialogActions(false, false, null, null, null, null),
            resolveCompositeDialogActions(null, emptyMap(), emptyMap()))
        assertEquals(false, resolveCompositeDialogActions(
            Json.parseToJsonElement("""{"showEmptyButton":false}""").jsonObject,
            emptyMap(), mapOf("showEmptyButton" to true)).showEmpty)
        assertEquals(false, resolveCompositeDialogActions(null, emptyMap(),
            mapOf("showEmptyButton" to "true")).showEmpty)
        assertEquals(true, resolveCompositeDialogActions(null, emptyMap(), emptyMap()).clearable)
        assertEquals(false, resolveCompositeDialogActions(
            Json.parseToJsonElement("""{"clearable":false}""").jsonObject,
            emptyMap(), mapOf("clearable" to true)).clearable)
    }

    @Test fun restrictBlocksEmptyOnlyForProtectedContainerStructure() {
        val root = JsonSchema()
        val objectValue = Json.parseToJsonElement("""{"city":"Boston"}""")
        val arrayValue = Json.parseToJsonElement("""[1]""")
        val requiredChild = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","required":["city"]}""").jsonObject)
        val minProperties = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","minProperties":1}""").jsonObject)
        val minItems = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","minItems":1}""").jsonObject)
        assertEquals(false, canEmptyCompositeDraft(objectValue, requiredChild, root, true))
        assertEquals(false, canEmptyCompositeDraft(objectValue, minProperties, root, true))
        assertEquals(false, canEmptyCompositeDraft(arrayValue, minItems, root, true))
        assertEquals(true, canEmptyCompositeDraft(objectValue, requiredChild, root, false))
        assertEquals(true, canEmptyCompositeDraft(arrayValue, minItems, root, false))
        assertEquals(true, canEmptyCompositeDraft(objectValue, JsonSchema(), root, true))
    }

    @Test fun containsUsesActiveDialectWhenEmptyingNestedArray() {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"tags":{"type":"array","contains":{"const":"keep"},"items":{"type":"string"}}}}"""
        ).jsonObject)
        val tags = requireNotNull(schema.properties).getValue("tags")
        val data = Json.parseToJsonElement("""{"tags":["keep","other"]}""")
        val value = Json.parseToJsonElement("""["keep","other"]""")
        val validator = OptimumCodeJsonSchemaValidator().compile(schema)
        assertEquals(false, canEmptyCompositeDraft(value, tags, schema, true,
            validator, data, "tags"))
        assertEquals(true, canEmptyCompositeDraft(value, tags, schema, false,
            validator, data, "tags"))
        val zeroMinimum = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"tags":{"type":"array","contains":{"const":"keep"},"minContains":0,"items":{"type":"string"}}}}"""
        ).jsonObject)
        assertEquals(true, canEmptyCompositeDraft(value,
            requireNotNull(zeroMinimum.properties).getValue("tags"), zeroMinimum, true,
            OptimumCodeJsonSchemaValidator().compile(zeroMinimum), data, "tags"))
    }

    @Test fun composedStructuralMinimumAlsoBlocksEmptyDraft() {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"tags":{"type":"array","allOf":[{"minItems":1}]},"address":{"type":"object","allOf":[{"required":["city"]}]}}}"""
        ).jsonObject)
        val validator = OptimumCodeJsonSchemaValidator().compile(schema)
        val data = Json.parseToJsonElement("""{"tags":["keep"],"address":{"city":"Sofia"}}""")
        val tags = requireNotNull(schema.properties).getValue("tags")
        val address = requireNotNull(schema.properties).getValue("address")
        assertEquals(false, canEmptyCompositeDraft(Json.parseToJsonElement("""["keep"]"""),
            tags, schema, true, validator, data, "tags"))
        assertEquals(false, canEmptyCompositeDraft(Json.parseToJsonElement("""{"city":"Sofia"}"""),
            address, schema, true, validator, data, "address"))
    }

    @Test fun plainCompositeDoesNotRevalidateTheWholeFormToCheckEmpty() {
        val rootSchema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"tags":{"type":"array","items":{"type":"string"}}}}"""
        ).jsonObject)
        val data = Json.parseToJsonElement("""{"tags":["one"]}""")
        var calls = 0
        val counting = object : ValidateFunction {
            override val schema = rootSchema
            override fun validate(data: JsonElement): List<ErrorObject> {
                calls++
                return emptyList()
            }
        }
        assertEquals(true, canEmptyCompositeDraft(Json.parseToJsonElement("[\"one\"]"),
            requireNotNull(rootSchema.properties).getValue("tags"), rootSchema, true,
            counting, data, "tags"))
        assertEquals(0, calls)
    }

    @Test fun booleanContainsSchemasUseTheValidatorForEmpty() {
        for (contains in listOf(true, false)) {
            val schema = JsonSchema(Json.parseToJsonElement(
                """{"type":"object","properties":{"tags":{"type":"array","contains":$contains}}}"""
            ).jsonObject)
            val data = Json.parseToJsonElement("""{"tags":[1]}""")
            assertEquals(false, canEmptyCompositeDraft(Json.parseToJsonElement("[1]"),
                requireNotNull(schema.properties).getValue("tags"), schema, true,
                OptimumCodeJsonSchemaValidator().compile(schema), data, "tags"),
                "contains=$contains")
        }
    }
}
