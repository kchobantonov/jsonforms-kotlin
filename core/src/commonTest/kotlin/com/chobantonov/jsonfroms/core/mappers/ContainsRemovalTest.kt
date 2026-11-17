package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.validation.OptimumCodeJsonSchemaValidator
import com.chobantonov.jsonfroms.core.validation.ValidateFunction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class ContainsRemovalTest {
    private val validator = OptimumCodeJsonSchemaValidator()

    @Test fun blocksDeletingLastMatchButAllowsNonmatchAndAlreadyInvalidState() {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"rows":{"type":"array","contains":{"const":"yes"}}}}"""
        ).jsonObject)
        val arraySchema = requireNotNull(schema.properties).getValue("rows")
        val compiled = validator.compile(schema)
        val data = Json.parseToJsonElement("""{"rows":["yes","no"]}""")
        val items = data.jsonObject.getValue("rows").jsonArray
        assertFalse(canRemoveWithoutBreakingContains(arraySchema, compiled, data, "rows", items, 0))
        assertTrue(canRemoveWithoutBreakingContains(arraySchema, compiled, data, "rows", items, 1))

        val invalid = Json.parseToJsonElement("""{"rows":["no","other"]}""")
        assertTrue(canRemoveWithoutBreakingContains(arraySchema, compiled, invalid, "rows",
            invalid.jsonObject.getValue("rows").jsonArray, 0))
        assertTrue(canRemoveWithoutBreakingContains(arraySchema, null, data, "rows", items, 0))
        assertTrue(canRemoveWithoutBreakingContains(JsonSchema(), compiled, data, "rows", items, 0))
        assertTrue(canRemoveWithoutBreakingContains(arraySchema, compiled, data, "rows", items, 5))
    }

    @Test fun minContainsUsesResultingMatchCountAtNestedPath() {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"section":{"type":"object","properties":{"rows":{"type":"array","contains":{"const":"yes"},"minContains":2}}}}}"""
        ).jsonObject)
        val arraySchema = requireNotNull(requireNotNull(schema.properties).getValue("section").properties)
            .getValue("rows")
        val data = Json.parseToJsonElement("""{"section":{"rows":["yes","no","yes"]}}""")
        val items = data.jsonObject.getValue("section").jsonObject.getValue("rows").jsonArray
        val compiled = validator.compile(schema)
        assertFalse(canRemoveWithoutBreakingContains(arraySchema, compiled, data, "section.rows", items, 0))
        assertTrue(canRemoveWithoutBreakingContains(arraySchema, compiled, data, "section.rows", items, 1))
    }

    @Test fun oneGuardValidatesTheUnchangedRootOnlyOnce() {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"rows":{"type":"array","contains":{"const":"yes"}}}}"""
        ).jsonObject)
        val data = Json.parseToJsonElement("""{"rows":["yes","no"]}""")
        val delegate = validator.compile(schema)
        var validations = 0
        val counting = object : ValidateFunction {
            override val schema = delegate.schema
            override fun validate(data: kotlinx.serialization.json.JsonElement) =
                delegate.validate(data).also { validations++ }
        }
        val guard = ContainsRemovalGuard(requireNotNull(schema.properties).getValue("rows"),
            counting, data, "rows", data.jsonObject.getValue("rows").jsonArray)
        assertEquals(1, validations)
        assertFalse(guard.canRemove(0))
        assertTrue(guard.canRemove(1))
        assertEquals(3, validations)
    }

    @Test fun bulkRemovalPreservesSatisfiedContainsButAllowsAlreadyInvalidArray() {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"rows":{"type":"array","contains":{"const":"yes"}}}}"""
        ).jsonObject)
        val arraySchema = requireNotNull(schema.properties).getValue("rows")
        val compiled = validator.compile(schema)
        val valid = Json.parseToJsonElement("""{"rows":["yes","no"]}""")
        val guard = ContainsRemovalGuard(arraySchema, compiled, valid, "rows",
            valid.jsonObject.getValue("rows").jsonArray)
        assertFalse(guard.canReplaceWith(JsonArray(emptyList())))
        assertTrue(guard.canReplaceWith(Json.parseToJsonElement("""["yes"]""").jsonArray))
        val invalid = Json.parseToJsonElement("""{"rows":["no"]}""")
        assertTrue(ContainsRemovalGuard(arraySchema, compiled, invalid, "rows",
            invalid.jsonObject.getValue("rows").jsonArray).canReplaceWith(JsonArray(emptyList())))
    }
}
