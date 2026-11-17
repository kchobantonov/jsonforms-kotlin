package com.chobantonov.jsonfroms.core.validation

import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.util.getControlPath
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class PropertyDependencyTest {
    private val validator = OptimumCodeJsonSchemaValidator()

    @Test fun multipleDependentPropertiesRouteToEveryMissingControl() {
        for ((dialect, keyword) in listOf(
            "http://json-schema.org/draft-07/schema#" to "dependencies",
            "https://json-schema.org/draft/2020-12/schema" to "dependentRequired",
        )) {
            val schema = schema("""{
              "${'$'}schema":"$dialect", "type":"object",
              "properties":{"purchaseOrder":{"type":"string"},
                "billingAddress":{"type":"string"},"billingCity":{"type":"string"}},
              "$keyword":{"purchaseOrder":["billingAddress","billingCity"]}
            }""")
            val errors = validate(schema, """{"purchaseOrder":"PO-123"}""")
            assertEquals(listOf("billingAddress", "billingCity"),
                errors.map(::getControlPath).sorted(), errors.toString())
            assertEquals(listOf(keyword, keyword), errors.map { it.keyword })
            assertEquals(listOf("billingCity"), validate(schema,
                """{"purchaseOrder":"PO-123","billingAddress":"Main"}""")
                .map(::getControlPath))
            assertEquals(emptyList(), validate(schema,
                """{"purchaseOrder":"PO-123","billingAddress":"Main","billingCity":"Sofia"}"""))
        }
    }

    @Test fun groupedRequiredErrorsRouteSpecialPropertyNamesWithoutParsingThemFromText() {
        val schema = schema("""{
          "type":"object", "required":["a,b", "x/y"],
          "properties":{"a,b":{"type":"string"},"x/y":{"type":"string"}}
        }""")
        val errors = validate(schema, """{}""")
        assertEquals(listOf("a,b", "x/y"),
            errors.mapNotNull { it.params["missingProperty"] }.sorted(),
            errors.toString())
        assertEquals(listOf("a,b"), validate(schema, """{"x/y":"yes"}""")
            .map(::getControlPath))
    }

    @Test fun schemaDependenciesRouteEveryRequiredProperty() {
        for ((dialect, keyword) in listOf(
            "http://json-schema.org/draft-07/schema#" to "dependencies",
            "https://json-schema.org/draft/2020-12/schema" to "dependentSchemas",
        )) {
            val schema = schema("""{
              "${'$'}schema":"$dialect", "type":"object",
              "properties":{"purchaseOrder":{"type":"string"},
                "billingAddress":{"type":"string"},"billingCity":{"type":"string"}},
              "$keyword":{"purchaseOrder":{"required":["billingAddress","billingCity"]}}
            }""")
            val errors = validate(schema, """{"purchaseOrder":"PO-123"}""")
            assertEquals(listOf("billingAddress", "billingCity"),
                errors.map(::getControlPath).sorted(), errors.toString())
            assertEquals(listOf("required", "required"), errors.map { it.keyword })
        }
    }

    @Test fun draft07ArrayDependencyUsesPresenceAndTargetsMissingControl() {
        val schema = schema("""{
          "${'$'}schema":"http://json-schema.org/draft-07/schema#",
          "type":"object",
          "properties":{"purchaseOrder":{"type":"string"},"billingAddress":{"type":"string"}},
          "dependencies":{"purchaseOrder":["billingAddress"]}
        }""")
        assertEquals(emptyList(), validate(schema, """{}"""))
        assertEquals(emptyList(), validate(schema, """{"billingAddress":"Main Street"}"""))
        for (data in listOf("""{"purchaseOrder":"PO-123"}""", """{"purchaseOrder":""}""")) {
            val error = validate(schema, data).single()
            assertEquals("dependencies", error.keyword)
            assertEquals("billingAddress", getControlPath(error))
        }
        assertEquals(emptyList(), validate(schema, """{"purchaseOrder":"","billingAddress":""}"""))
    }

    @Test fun modernDependentRequiredUsesPresenceAndTargetsMissingControl() {
        val schema = schema("""{
          "${'$'}schema":"https://json-schema.org/draft/2020-12/schema",
          "type":"object",
          "properties":{"purchaseOrder":{"type":"string"},"billingAddress":{"type":"string"}},
          "dependentRequired":{"purchaseOrder":["billingAddress"]}
        }""")
        assertEquals(emptyList(), validate(schema, """{}"""))
        assertEquals(emptyList(), validate(schema, """{"billingAddress":"Main Street"}"""))
        val error = validate(schema, """{"purchaseOrder":""}""").single()
        assertEquals("dependentRequired", error.keyword)
        assertEquals("billingAddress", getControlPath(error))
        assertEquals(emptyList(), validate(schema, """{"purchaseOrder":"","billingAddress":""}"""))
    }

    @Test fun schemaDependenciesValidateTheWholeObject() {
        val draft07 = schema("""{
          "${'$'}schema":"http://json-schema.org/draft-07/schema#",
          "type":"object","properties":{"purchaseOrder":{"type":"string"},"billingAddress":{"type":"string"}},
          "dependencies":{"purchaseOrder":{"required":["billingAddress"]}}
        }""")
        val modern = schema("""{
          "${'$'}schema":"https://json-schema.org/draft/2020-12/schema",
          "type":"object","properties":{"purchaseOrder":{"type":"string"},"billingAddress":{"type":"string"}},
          "dependentSchemas":{"purchaseOrder":{"required":["billingAddress"]}}
        }""")
        for (schema in listOf(draft07, modern)) {
            assertEquals(emptyList(), validate(schema, """{}"""))
            assertEquals(emptyList(), validate(schema, """{"billingAddress":"Main Street"}"""))
            val error = validate(schema, """{"purchaseOrder":"PO-123"}""").single()
            assertEquals("required", error.keyword)
            assertEquals("billingAddress", getControlPath(error))
            assertEquals(emptyList(), validate(schema,
                """{"purchaseOrder":"PO-123","billingAddress":"Main Street"}"""))
        }
    }

    private fun schema(raw: String) = JsonSchema(Json.parseToJsonElement(raw).jsonObject)
    private fun validate(schema: JsonSchema, data: String) =
        validator.validate(schema, Json.parseToJsonElement(data))
}
