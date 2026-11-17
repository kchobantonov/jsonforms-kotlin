package com.chobantonov.jsonfroms.core.util

import com.chobantonov.jsonfroms.core.models.JsonSchema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AlternativeObjectPropertiesTest {
    @Test fun authoredColumnsCanUseTheUnionOfDistinctAlternativeFields() {
        for (keyword in listOf("oneOf", "anyOf")) {
            val schema = JsonSchema(Json.parseToJsonElement("""{
                "$keyword":[
                    {"properties":{"mode":{"const":"plain"},"note":{"type":"string"}}},
                    {"properties":{"mode":{"const":"secret"},"secret":{"type":"string"}}}
                ]
            }""").jsonObject)
            val columns = alternativeObjectColumns(schema, schema)!!
            assertEquals(listOf("mode", "note", "secret"), columns.properties.keys.toList())
            assertEquals(setOf("note", "secret"), columns.branchSpecific)
            assertNull(commonAlternativeProperties(schema, schema))
            assertNull(selectableAlternativeObjectBranches(schema, schema))
            assertNull(alternativeObjectColumns(JsonSchema(Json.parseToJsonElement(
                """{"type":"object","properties":{"name":{"type":"string"}}}""").jsonObject), schema))
        }
    }

    @Test fun distinctFlatBranchesWithRequiredConstCanUseAutomaticColumns() {
        for (keyword in listOf("oneOf", "anyOf")) {
            val schema = JsonSchema(Json.parseToJsonElement("""{
                "type":"object","$keyword":[
                    {"title":"Plain","properties":{"mode":{"const":"plain"},
                        "note":{"type":"string"}},"required":["mode","note"]},
                    {"title":"Secret","properties":{"mode":{"const":"secret"},
                        "secret":{"type":"string","format":"password"}},
                        "required":["mode","secret"]}
                ]
            }""").jsonObject)
            val choices = selectableAlternativeObjectBranches(schema, schema)!!
            assertEquals(listOf("Plain", "Secret"), choices.map { it.title })
            assertEquals(listOf(JsonPrimitive("plain"), JsonPrimitive("secret")),
                choices.map { it.discriminatorValue })
            assertEquals("mode", choices.first().discriminatorField)
            assertEquals(listOf("mode", "note", "secret"),
                alternativeObjectColumns(schema, schema)!!.properties.keys.toList())
            assertTrue(hasFlatAlternativeObjectProperties(schema, schema))
        }
    }

    @Test fun sharedFlatColumnsWithRequiredConstStillOfferBranchChoices() {
        for (keyword in listOf("oneOf", "anyOf")) {
            val schema = JsonSchema(Json.parseToJsonElement("""{
                "$keyword":[
                    {"title":"Plain","properties":{"mode":{"const":"plain"},
                        "note":{"type":"string"}},"required":["mode"]},
                    {"title":"Secret","properties":{"mode":{"const":"secret"},
                        "note":{"type":"string"}},"required":["mode"]}
                ]
            }""").jsonObject)
            val columns = alternativeObjectColumns(schema, schema)!!
            assertTrue(columns.branchSpecific.isEmpty())
            assertEquals(listOf("mode", "note"), columns.properties.keys.toList())
            assertEquals(listOf(JsonPrimitive("plain"), JsonPrimitive("secret")),
                selectableAlternativeObjectBranches(schema, schema)!!.map { it.discriminatorValue })
            assertTrue(hasFlatAlternativeObjectProperties(schema, schema))
        }
    }

    @Test fun ambiguousOrNestedAlternativesStayExpandable() {
        for (other in listOf(
            """{"properties":{"mode":{"const":"plain"},"secret":{"type":"string"}},
                "required":["mode","secret"]}""",
            """{"properties":{"mode":{"const":"secret"},"secret":{"type":"object",
                "properties":{"value":{"type":"string"}}}},"required":["mode","secret"]}""",
        )) {
            val schema = JsonSchema(Json.parseToJsonElement("""{
                "oneOf":[
                    {"properties":{"mode":{"const":"plain"},"note":{"type":"string"}},
                        "required":["mode","note"]},
                    $other
                ]
            }""").jsonObject)
            assertNull(selectableAlternativeObjectBranches(schema, schema))
        }
    }

    @Test fun numericallyEqualConstValuesDoNotIdentifyDifferentBranches() {
        for ((first, second) in listOf(
            "1" to "1.0", "1e2" to "100", "-0" to "0",
        )) {
            val schema = JsonSchema(Json.parseToJsonElement("""{
                "oneOf":[
                    {"properties":{"mode":{"const":$first},"note":{"type":"string"}},
                        "required":["mode"]},
                    {"properties":{"mode":{"const":$second},"note":{"type":"string"}},
                        "required":["mode"]}
                ]
            }""").jsonObject)
            assertNull(selectableAlternativeObjectBranches(schema, schema))
            assertNull(alternativeObjectColumns(schema, schema)!!.properties["mode"]
                ?.jsonObject?.get("enum"))
        }
        val distinctTypes = JsonSchema(Json.parseToJsonElement("""{
            "oneOf":[
                {"properties":{"mode":{"const":1}},"required":["mode"]},
                {"properties":{"mode":{"const":"1"}},"required":["mode"]}
            ]
        }""").jsonObject)
        assertEquals(listOf(JsonPrimitive(1), JsonPrimitive("1")),
            selectableAlternativeObjectBranches(distinctTypes, distinctTypes)!!
                .map { it.discriminatorValue })
        assertNull(schemaConstEquivalent(Json.parseToJsonElement("1e2147483648"), JsonPrimitive(2)))
        assertTrue(jsonSchemaChoiceEquals(JsonPrimitive(1), JsonPrimitive(1.0)))
        assertFalse(jsonSchemaChoiceEquals(JsonPrimitive(1), JsonPrimitive("1")))
        assertTrue(jsonSchemaChoiceEquals(
            Json.parseToJsonElement("""{"x":[1.0,{"y":-0}]}"""),
            Json.parseToJsonElement("""{"x":[1,{"y":0}]}""")))
        assertFalse(jsonSchemaChoiceEquals(
            Json.parseToJsonElement("""{"x":[1.0,{"y":1}]}"""),
            Json.parseToJsonElement("""{"x":[1,{"y":0}]}""")))
        assertNull(schemaConstEquivalent(
            Json.parseToJsonElement("""[1e2147483648]"""),
            Json.parseToJsonElement("""[2]""")))
    }
}
