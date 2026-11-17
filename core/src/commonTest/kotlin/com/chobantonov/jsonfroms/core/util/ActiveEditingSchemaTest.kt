package com.chobantonov.jsonfroms.core.util

import com.chobantonov.jsonfroms.core.models.JsonSchema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ActiveEditingSchemaTest {
    @Test fun oneOfAndAnyOfUseOnlyUnambiguousStoredTypes() {
        for (keyword in listOf("oneOf", "anyOf")) {
            val root = JsonSchema(Json.parseToJsonElement("""{
                "type":"object","$keyword":[
                    {"properties":{"value":{"type":"string"}},"required":["value"]},
                    {"properties":{"value":{"type":"integer"}},"required":["value"]}
                ]
            }""").jsonObject)
            fun selected(value: kotlinx.serialization.json.JsonElement?) =
                activeEditingSchemaForScopedProperty(root, "#/properties/value", root, value)
                    ?.let { resolveSchema(it, "#/properties/value", root)?.get("type") }
            assertEquals(JsonPrimitive("string"), selected(JsonPrimitive("Ada")))
            assertEquals(JsonPrimitive("integer"), selected(JsonPrimitive(7)))
            assertEquals(JsonPrimitive("integer"),
                selected(Json.parseToJsonElement("123456789012345678901234567890")))
            assertNull(selected(null))
            assertNull(selected(Json.parseToJsonElement("null")))
            assertNull(activeEditingSchemaForScopedProperty(
                root, "#/properties/value/properties/inner", root, JsonPrimitive("Ada")))
        }
    }

    @Test fun overlappingNumericTypesRemainAmbiguousForIntegerValues() {
        val root = JsonSchema(Json.parseToJsonElement("""{
            "anyOf":[
                {"properties":{"value":{"type":"integer"}}},
                {"properties":{"value":{"type":"number"}}}
            ]
        }""").jsonObject)
        assertNull(activeEditingSchemaForScopedProperty(root, "#/properties/value", root, JsonPrimitive(7)))
        val decimal = activeEditingSchemaForScopedProperty(root, "#/properties/value", root, JsonPrimitive(7.5))
        assertEquals(JsonPrimitive("number"), resolveSchema(decimal, "#/properties/value", root)?.get("type"))
    }

    @Test fun sharedRootPropertyKeepsItsAuthoredContract() {
        val root = JsonSchema(Json.parseToJsonElement("""{
            "properties":{"value":{"type":"string","maxLength":3}},
            "oneOf":[
                {"properties":{"value":{"type":"string"}}},
                {"properties":{"value":{"type":"integer"}}}
            ]
        }""").jsonObject)
        assertNull(activeEditingSchemaForScopedProperty(root, "#/properties/value", root, JsonPrimitive(7)))
        val requiredRoot = JsonSchema(Json.parseToJsonElement("""{
            "required":["value"],
            "oneOf":[
                {"properties":{"value":{"type":"string"}}},
                {"properties":{"value":{"type":"integer"}}}
            ]
        }""").jsonObject)
        assertNull(activeEditingSchemaForScopedProperty(
            requiredRoot, "#/properties/value", requiredRoot, JsonPrimitive(7)))
    }

    @Test fun nestedPropertyPointerAndBranchSelectionPreserveEscapes() {
        assertEquals("/record/value", scopedPropertyDataPointer("#/properties/record/properties/value"))
        assertEquals("/first~1part/~0key", scopedPropertyDataPointer(
            "#/properties/first~1part/properties/~0key"))
        assertNull(scopedPropertyDataPointer("#/properties/record/items/0"))
        assertNull(scopedPropertyDataPointer("#/properties/record/properties/"))
        for (keyword in listOf("oneOf", "anyOf")) {
            val root = JsonSchema(Json.parseToJsonElement("""{
                "type":"object","$keyword":[
                    {"properties":{"record":{"type":"object","properties":{"value":{"type":"string"}}}}},
                    {"properties":{"record":{"type":"object","properties":{"value":{"type":"integer"}}}}}
                ]
            }""").jsonObject)
            val scope = "#/properties/record/properties/value"
            val selected = activeEditingSchemaForScopedProperty(root, scope, root, JsonPrimitive(7))
            assertEquals(JsonPrimitive("integer"), resolveSchema(selected, scope, root)?.get("type"))
        }
    }

    @Test fun requiredSiblingConstSelectsSameTypeBranchOnlyWhenUnique() {
        val root = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","oneOf":[
                {"properties":{"mode":{"const":"plain"},"value":{"type":"string"}},
                    "required":["mode","value"]},
                {"properties":{"mode":{"const":"secret"},
                    "value":{"type":"string","format":"password"}},
                    "required":["mode","value"]}
            ]
        }""").jsonObject)
        fun selected(context: String?) = activeEditingSchemaForScopedProperty(
            root, "#/properties/value", root, JsonPrimitive("Ada"),
            context?.let(Json::parseToJsonElement))
        assertEquals(JsonPrimitive("password"), resolveSchema(
            selected("""{"mode":"secret","value":"Ada"}"""), "#/properties/value", root)?.get("format"))
        assertNull(selected("""{"mode":"other","value":"Ada"}"""))
        assertNull(selected("""{"value":"Ada"}"""))
        assertNull(selected(null))
        val duplicate = JsonSchema(Json.parseToJsonElement("""{
            "oneOf":[
                {"properties":{"mode":{"const":"same"},"value":{"type":"string"}},
                    "required":["mode"]},
                {"properties":{"mode":{"const":"same"},"value":{"type":"string"}},
                    "required":["mode"]}
            ]
        }""").jsonObject)
        assertNull(activeEditingSchemaForScopedProperty(duplicate, "#/properties/value", duplicate,
            JsonPrimitive("Ada"), Json.parseToJsonElement("""{"mode":"same","value":"Ada"}""")))
    }

    @Test fun requiredNumericConstUsesSchemaValueEquality() {
        for (keyword in listOf("oneOf", "anyOf")) {
            val root = JsonSchema(Json.parseToJsonElement("""{
                "$keyword":[
                    {"properties":{"mode":{"const":1},"value":{"type":"string"}},
                        "required":["mode"]},
                    {"properties":{"mode":{"const":2},
                        "value":{"type":"string","format":"password"}},
                        "required":["mode"]}
                ]
            }""").jsonObject)
            fun selected(mode: String) = activeEditingSchemaForScopedProperty(
                root, "#/properties/value", root, JsonPrimitive("Ada"),
                Json.parseToJsonElement("""{"mode":$mode,"value":"Ada"}"""))
            assertEquals(JsonPrimitive("string"),
                resolveSchema(selected("1.0"), "#/properties/value", root)?.get("type"))
            assertNull(resolveSchema(selected("1.0"), "#/properties/value", root)?.get("format"))
            assertEquals(JsonPrimitive("password"),
                resolveSchema(selected("2e0"), "#/properties/value", root)?.get("format"))
            assertNull(selected("3"))
            assertNull(selected("1e2147483648"))
        }
        val overlapping = JsonSchema(Json.parseToJsonElement("""{
            "oneOf":[
                {"properties":{"mode":{"const":1},"value":{"type":"string"}},
                    "required":["mode"]},
                {"properties":{"mode":{"const":1.0},"value":{"type":"string"}},
                    "required":["mode"]}
            ]
        }""").jsonObject)
        assertNull(activeEditingSchemaForScopedProperty(overlapping, "#/properties/value",
            overlapping, JsonPrimitive("Ada"), Json.parseToJsonElement(
                """{"mode":1,"value":"Ada"}""")))
    }

    @Test fun requiredObjectConstUsesNestedNumericEquality() {
        for (keyword in listOf("oneOf", "anyOf")) {
            val root = JsonSchema(Json.parseToJsonElement("""{
                "$keyword":[
                    {"properties":{"mode":{"const":{"x":1}},
                        "value":{"type":"string"}},"required":["mode"]},
                    {"properties":{"mode":{"const":{"x":2}},
                        "value":{"type":"string","format":"password"}},"required":["mode"]}
                ]
            }""").jsonObject)
            fun selected(mode: String) = activeEditingSchemaForScopedProperty(
                root, "#/properties/value", root, JsonPrimitive("Ada"),
                Json.parseToJsonElement("""{"mode":$mode,"value":"Ada"}"""))
            assertEquals(JsonPrimitive("string"), resolveSchema(selected("""{"x":1.0}"""),
                "#/properties/value", root)?.get("type"))
            assertNull(resolveSchema(selected("""{"x":1.0}"""),
                "#/properties/value", root)?.get("format"))
            assertEquals(JsonPrimitive("password"), resolveSchema(selected("""{"x":2e0}"""),
                "#/properties/value", root)?.get("format"))
            assertNull(selected("""{"x":3}"""))
        }
    }

    @Test fun requiredSiblingConstSelectsMissingFieldWithoutInventingAStoredType() {
        for (keyword in listOf("oneOf", "anyOf")) {
            val root = JsonSchema(Json.parseToJsonElement("""{
                "$keyword":[
                    {"properties":{"mode":{"const":"plain"},"value":{"type":"string"}},
                        "required":["mode","value"]},
                    {"properties":{"mode":{"const":"secret"},
                        "value":{"type":"string","format":"password"}},
                        "required":["mode","value"]}
                ]
            }""").jsonObject)
            fun selected(context: String) = activeEditingSchemaForScopedProperty(
                root, "#/properties/value", root, null, Json.parseToJsonElement(context))
            assertEquals(JsonPrimitive("password"), resolveSchema(
                selected("""{"mode":"secret"}"""), "#/properties/value", root)?.get("format"))
            assertEquals(JsonPrimitive("string"), resolveSchema(
                selected("""{"mode":"plain"}"""), "#/properties/value", root)?.get("type"))
            assertNull(selected("""{"mode":"other"}"""))
            assertNull(selected("""{}"""))
        }
    }

    @Test fun branchOnlyFieldNeedsAUniqueMatchingSiblingConst() {
        for (keyword in listOf("oneOf", "anyOf")) {
            val root = JsonSchema(Json.parseToJsonElement("""{
                "$keyword":[
                    {"properties":{"mode":{"const":"plain"},"note":{"type":"string"}},
                        "required":["mode"]},
                    {"properties":{"mode":{"const":"secret"},
                        "secret":{"type":"string","format":"password"}},
                        "required":["mode"]}
                ]
            }""").jsonObject)
            fun selected(context: String, value: kotlinx.serialization.json.JsonElement? = null) =
                activeEditingSchemaForScopedProperty(root, "#/properties/secret", root, value,
                    Json.parseToJsonElement(context))
            assertEquals(JsonPrimitive("password"), resolveSchema(
                selected("""{"mode":"secret"}"""), "#/properties/secret", root)?.get("format"))
            assertEquals(JsonPrimitive("password"), resolveSchema(
                selected("""{"mode":"secret","secret":"Ada"}""", JsonPrimitive("Ada")),
                "#/properties/secret", root)?.get("format"))
            assertNull(selected("""{"mode":"plain"}"""))
            assertNull(selected("""{}"""))
            assertNull(selected("""{"mode":"other"}"""))
            assertNull(selected("""{"mode":"secret","secret":7}""", JsonPrimitive(7)))
        }
    }

    @Test fun nestedRequiredConstUsesTheEditedFieldsParentObject() {
        for (keyword in listOf("oneOf", "anyOf")) for ((name, escaped) in listOf(
            "record" to "record", "first/part" to "first~1part")) {
            val root = JsonSchema(Json.parseToJsonElement("""{
                "type":"object","$keyword":[
                    {"properties":{"$name":{"type":"object","properties":{
                        "mode":{"const":"plain"},"value":{"type":"string"}},
                        "required":["mode","value"]}}},
                    {"properties":{"$name":{"type":"object","properties":{
                        "mode":{"const":"secret"},
                        "value":{"type":"string","format":"password"}},
                        "required":["mode","value"]}}}
                ]
            }""").jsonObject)
            val scope = "#/properties/$escaped/properties/value"
            fun selected(mode: String) = activeEditingSchemaForScopedProperty(
                root, scope, root, JsonPrimitive("Ada"),
                Json.parseToJsonElement("""{"$name":{"mode":"$mode","value":"Ada"}}"""))
            assertEquals(JsonPrimitive("password"), resolveSchema(
                selected("secret"), scope, root)?.get("format"))
            assertNull(selected("other"))
        }
    }
}
