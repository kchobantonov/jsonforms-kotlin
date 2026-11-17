package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.i18n.translateConditionalFieldDiagnostic
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.validation.OptimumCodeJsonSchemaValidator
import com.chobantonov.jsonfroms.core.util.Resolve
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class ConditionalObjectFieldsTest {
    private val validator = OptimumCodeJsonSchemaValidator()
    private fun schema(source: String) = JsonSchema(Json.parseToJsonElement(source).jsonObject)
    private fun data(source: String) = Json.parseToJsonElement(source).jsonObject
    private fun fields(schema: JsonSchema, value: JsonObject) =
        projectConditionalObjectFields(schema, schema, value, validator)

    @Test fun `local option overrides global and absent option defaults off`() {
        val enabled = mapOf<String, Any?>("jsonformsExtended" to data("""{"conditionalFields":true}"""))
        assertTrue(conditionalFieldsEnabled(ControlElement("#"), enabled))
        assertFalse(conditionalFieldsEnabled(ControlElement("#", options = mapOf(
            "conditionalFields" to JsonPrimitive(false))), enabled))
        assertFalse(conditionalFieldsEnabled(ControlElement("#"), emptyMap()))
        assertTrue(conditionalFieldsEnabled(ControlElement("#", options = mapOf(
            "conditionalFields" to JsonPrimitive("false"))), enabled))
        assertFalse(conditionalFieldsEnabled(ControlElement("#", options = mapOf(
            "conditionalFields" to JsonPrimitive("true"))), emptyMap()))
    }

    @Test fun `quoted boolean condition is diagnosed without selecting a branch`() {
        val original = schema("""{"type":"object","if":"false",
          "then":{"properties":{"yes":{"type":"string"}}},
          "else":{"properties":{"no":{"type":"string"}}}}""")
        val projected = fields(original, data("""{}"""))
        assertEquals(ConditionalFieldDiagnostic.ConditionCompilationFailure, projected.diagnostic)
        assertEquals(setOf("yes", "no"), projected.inactiveProperties)
    }

    @Test fun `quoted readonly facet is not promoted into projected schema`() {
        val original = schema("""{"type":"object",
          "properties":{"shared":{"type":"string","readOnly":"true"}},
          "allOf":[{"properties":{"shared":{"minLength":2}}}]}""")
        val shared = fields(original, data("""{}"""))
            .activeSchema.toJsonObject()["properties"]!!.jsonObject["shared"]!!.jsonObject
        assertEquals(null, shared["readOnly"])
    }

    @Test fun `nested allOf condition switches branches and retains overlapping constraints`() {
        val original = schema("""{
          "type":"object", "properties":{"kind":{"type":"string"},"shared":{"type":"string","minLength":2}},
          "allOf":[{"if":{"properties":{"kind":{"const":"a"}},"required":["kind"]},
            "then":{"properties":{"shared":{"maxLength":4,"readOnly":true},"alpha":{"type":"string"}},"required":["alpha"]},
            "else":{"properties":{"beta":{"type":"string"}}}}]
        }""")
        val authored = original.toJsonObject()
        val alpha = fields(original, data("""{"kind":"a"}"""))
        assertEquals(setOf("beta"), alpha.inactiveProperties)
        assertEquals(listOf("shared", "alpha", "beta"), alpha.reservedSchema.toJsonObject()["properties"]!!.jsonObject.keys.filter { it != "kind" })
        assertEquals(2, alpha.activeSchema.toJsonObject()["properties"]!!.jsonObject["shared"]!!.jsonObject["allOf"]!!.jsonArray.size)
        val shared = alpha.activeSchema.toJsonObject()["properties"]!!.jsonObject["shared"]!!.jsonObject
        assertEquals(JsonPrimitive("string"), shared["type"])
        assertEquals(JsonPrimitive(4), shared["maxLength"])
        assertEquals(JsonPrimitive(2), shared["minLength"])
        assertEquals(JsonPrimitive(true), shared["readOnly"])
        assertTrue(alpha.activeSchema.toJsonObject()["required"]!!.jsonArray.contains(JsonPrimitive("alpha")))
        assertEquals(setOf("alpha"), fields(original, data("""{"kind":"b"}""")).inactiveProperties)
        assertEquals(authored, original.toJsonObject())
    }

    @Test fun `dependency activates on null and required markers do not invent fields`() {
        val original = schema("""{
          "type":"object", "properties":{"trigger":{"type":"string"}},
          "dependencies":{"trigger":{"properties":{"extra":{"type":"string"}}},"missing":["needed"]},
          "dependentRequired":{"trigger":["other"]}
        }""")
        val projected = fields(original, data("""{"trigger":null,"missing":false}"""))
        assertTrue("extra" in projected.activeSchema.toJsonObject()["properties"]!!.jsonObject)
        assertEquals(setOf("needed", "other"), projected.activeSchema.toJsonObject()["required"]!!.jsonArray.map { it.toString().trim('"') }.toSet())
        assertFalse("needed" in projected.activeSchema.toJsonObject()["properties"]!!.jsonObject)
        assertEquals(setOf("extra"), fields(original, data("""{}""")).inactiveProperties)
    }

    @Test fun `condition compile failure is diagnostic and does not select else`() {
        val original = schema("""{"type":"object","if":{"${'$'}ref":"#/definitions/missing"},
          "then":{"properties":{"a":{"type":"string"}}},
          "else":{"properties":{"b":{"type":"string"}}}}""")
        val projected = fields(original, data("""{}"""))
        assertNotNull(projected.diagnostic)
        assertEquals(setOf("a", "b"), projected.inactiveProperties)
    }

    @Test fun `boolean conditions select the matching branch`() {
        val alwaysFalse = schema("""{"type":"object","if":false,
          "then":{"properties":{"yes":{"type":"string"}}},
          "else":{"properties":{"no":{"type":"string"}}}}""")
        val projected = fields(alwaysFalse, data("""{}"""))
        assertEquals(setOf("yes"), projected.inactiveProperties)
        assertTrue("no" in projected.activeSchema.toJsonObject()["properties"]!!.jsonObject)
        assertEquals(null, projected.diagnostic)
    }

    @Test fun `condition resolves definitions from the original root`() {
        val original = schema("""{"type":"object",
          "${'$'}defs":{"business":{"properties":{"kind":{"const":"business"}},"required":["kind"]}},
          "if":{"${'$'}ref":"#/${'$'}defs/business"},
          "then":{"properties":{"taxId":{"type":"string"}}},
          "else":{"properties":{"nickname":{"type":"string"}}}}""")
        val projected = fields(original, data("""{"kind":"business"}"""))
        assertTrue("taxId" in projected.activeSchema.toJsonObject()["properties"]!!.jsonObject)
        assertEquals(setOf("nickname"), projected.inactiveProperties)
        assertEquals(null, projected.diagnostic)
    }

    @Test fun `diagnostic uses Bulgarian or English fallback`() {
        val diagnostic = ConditionalFieldDiagnostic.ConditionCompilationFailure
        assertEquals("Условието за полетата не може да бъде оценено.",
            translateConditionalFieldDiagnostic(JsonFormsI18nState(locale = "bg-BG"), diagnostic))
        assertEquals("Unable to evaluate the conditional field schema.",
            translateConditionalFieldDiagnostic(JsonFormsI18nState(locale = "de-DE"), diagnostic))
    }

    @Test fun `inactive retained keys are reserved from additional properties`() {
        val original = schema("""{"type":"object","properties":{"kind":{"type":"string"}},
          "additionalProperties":{"type":"string"},
          "if":{"properties":{"kind":{"const":"a"}},"required":["kind"]},
          "then":{"properties":{"branch":{"type":"string"}}}}""")
        val value = data("""{"kind":"b","branch":"retained","free":"dynamic"}""")
        val projected = fields(original, value)
        assertEquals(setOf("branch"), projected.inactiveProperties)
        assertEquals(listOf("branch", "free"), additionalPropertyKeys(projected.activeSchema, value))
        assertEquals(listOf("free"), additionalPropertyKeys(projected.reservedSchema, value))
    }

    @Test fun `three overlapping declarations expose the strictest text limit`() {
        val original = schema("""{"type":"object",
          "properties":{"kind":{"type":"string"},"shared":{"type":"string"}},
          "allOf":[{"properties":{"shared":{"maxLength":3}}}],
          "if":{"properties":{"kind":{"const":"business"}},"required":["kind"]},
          "then":{"properties":{"shared":{"minLength":3}}}}""")
        val projected = fields(original, data("""{"kind":"business","shared":"abc"}"""))
        assertEquals(3, Resolve.schema(projected.activeSchema, "#/properties/shared", original)?.maxLength)
    }
}
