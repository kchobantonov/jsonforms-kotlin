package com.chobantonov.jsonfroms.core.validation

import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import com.chobantonov.jsonfroms.core.models.JsonSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class OptimumCodeJsonSchemaValidatorTest {
    private val validator = OptimumCodeJsonSchemaValidator()

    @Test
    fun formatAssertionsValidateNestedValuesInModernSchemas() {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{
                "contact":{"type":"string","format":"email"},
                "website":{"type":"string","format":"uri"}
            }
        }""").jsonObject)

        val invalid = validator.validate(schema,
            Json.parseToJsonElement("""{"contact":"not-an-email","website":"not a uri"}"""))
        assertEquals(setOf("/contact", "/website"), invalid.map { it.instancePath }.toSet())
        assertTrue(invalid.all { it.keyword == "format" }, invalid.toString())
        assertEquals(emptyList(), validator.validate(schema,
            Json.parseToJsonElement("""{"contact":"someone@example.com","website":"https://example.com"}""")))
    }

    @Test
    fun timeFormatRequiresOffsetWhileUtcTimePasses() {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"appointment":{"type":"string","format":"time"}}
        }""").jsonObject)

        val invalid = validator.validate(schema,
            Json.parseToJsonElement("""{"appointment":"09:30:00"}"""))
        assertEquals("format", invalid.single().keyword)
        assertEquals("/appointment", invalid.single().instancePath)
        assertEquals(emptyList(), validator.validate(schema,
            Json.parseToJsonElement("""{"appointment":"09:30:00Z"}""")))
    }

    @Test
    fun temporalBoundsValidateNestedValuesWithoutRenderedControls() {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"hidden":{"type":"string","format":"date",
                "formatMinimum":"2025-01-10","formatExclusiveMaximum":"2025-01-20"}}
        }""").jsonObject)
        val before = validator.validate(schema, Json.parseToJsonElement("""{"hidden":"2025-01-09"}"""))
        assertEquals("formatMinimum", before.single().keyword, before.toString())
        assertEquals("/hidden", before.single().instancePath)
        assertEquals("/properties/hidden/formatMinimum", before.single().schemaPath)
        val upper = validator.validate(schema, Json.parseToJsonElement("""{"hidden":"2025-01-20"}"""))
        assertEquals("formatExclusiveMaximum", upper.single().keyword, upper.toString())
        assertEquals(emptyList(), validator.validate(schema,
            Json.parseToJsonElement("""{"hidden":"2025-01-10"}""")))
    }

    @Test
    fun temporalDataBoundsUseCurrentRootAndExactArrayPaths() {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"items":{"type":"array","items":{
                "type":"object","properties":{"end":{"type":"string","format":"date",
                    "formatMinimum":{"${'$'}data":"1/start"}}}
            }}}
        }""").jsonObject)
        val compiled = validator.compile(schema)
        val invalid = compiled.validate(Json.parseToJsonElement("""{
            "items":[{"start":"2025-02-10","end":"2025-02-09"}]
        }"""))
        assertEquals("formatMinimum", invalid.single().keyword, invalid.toString())
        assertEquals("/items/0/end", invalid.single().instancePath)
        assertEquals("2025-02-10", invalid.single().params["bound"])
        assertEquals(emptyList(), compiled.validate(Json.parseToJsonElement("""{
            "items":[{"start":"2025-02-08","end":"2025-02-09"}]
        }""")))
        val wrongType = compiled.validate(Json.parseToJsonElement("""{
            "items":[{"start":12,"end":"2025-02-09"}]
        }"""))
        assertEquals("formatMinimum", wrongType.single().keyword, wrongType.toString())
        assertEquals("/items/0/end", wrongType.single().instancePath)
        assertEquals(emptyList(), compiled.validate(Json.parseToJsonElement("""{
            "items":[{"end":"2025-02-09"}]
        }""")))
    }

    @Test
    fun temporalBoundComparesOffsetsAsInstants() {
        val dateTime = JsonSchema(Json.parseToJsonElement("""{
            "type":"string","format":"date-time","formatMinimum":"2025-01-10T10:00:00Z"
        }""").jsonObject)
        assertEquals(emptyList(), validator.validate(dateTime, JsonPrimitive("2025-01-10T12:00:00+02:00")))
        assertEquals("formatMinimum", validator.validate(dateTime,
            JsonPrimitive("2025-01-10T11:59:59+02:00")).single().keyword)
        val time = JsonSchema(Json.parseToJsonElement("""{
            "type":"string","format":"time","formatExclusiveMinimum":"09:30:00Z"
        }""").jsonObject)
        assertEquals("formatExclusiveMinimum", validator.validate(time,
            JsonPrimitive("10:30:00+01:00")).single().keyword)
        assertEquals(emptyList(), validator.validate(time, JsonPrimitive("10:30:01+01:00")))
        val fractional = JsonSchema(Json.parseToJsonElement("""{
            "type":"string","format":"time","formatExclusiveMinimum":"09:30:00.1234567892Z"
        }""").jsonObject)
        assertTrue(validator.validate(fractional,
            JsonPrimitive("09:30:00.1234567891Z")).any { it.keyword == "formatExclusiveMinimum" })
        assertTrue(validator.validate(fractional,
            JsonPrimitive("09:30:00.1234567893Z")).none { it.keyword == "formatExclusiveMinimum" })
    }

    @Test
    fun temporalDataPointerPreservesEscapedAndEmptyPropertyNames() {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"dates":{"type":"object",
                "additionalProperties":{"type":"object","properties":{
                    "end":{"type":"string","format":"date",
                        "formatMinimum":{"${'$'}data":"1#"}}
                }}}}
        }""").jsonObject)
        val errors = validator.validate(schema, Json.parseToJsonElement("""{
            "dates":{"2025-01-10":{"end":"2025-01-09"},
                "":{"end":"2025-01-09"},
                "a/b":{"end":"2025-01-09"}}
        }"""))
        assertEquals(listOf("/dates/2025-01-10/end", "/dates/a~1b/end"),
            errors.map { it.instancePath }, errors.toString())
        val escaped = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"end":{"type":"string","format":"date",
                "formatMinimum":{"${'$'}data":"/a~1b"}}}
        }""").jsonObject)
        val escapedErrors = validator.validate(escaped, Json.parseToJsonElement("""{
            "a/b":"2025-01-10","end":"2025-01-09"
        }"""))
        assertEquals("formatMinimum", escapedErrors.single().keyword, escapedErrors.toString())
        assertEquals("/end", escapedErrors.single().instancePath)
    }

    @Test
    fun malformedTemporalDataReferencesReportSchemaKeywordPath() {
        fun schema(reference: String) = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"end":{"type":"string","format":"date",
                "formatMinimum":{"${'$'}data":"$reference"}}}
        }""").jsonObject)
        val data = Json.parseToJsonElement("""{"start":"2025-01-10","end":"2025-01-09"}""")
        for (reference in listOf("2/start", "1#")) {
            val error = validator.validate(schema(reference), Json.parseToJsonElement("{}")).single()
            assertEquals("formatMinimum", error.keyword, reference)
            assertEquals("", error.instancePath, reference)
            assertEquals("/properties/end/formatMinimum", error.schemaPath, reference)
            assertTrue(error.message.contains("data depth"), error.toString())
        }
        assertEquals(emptyList(), validator.validate(schema("/missing"), data))
        assertEquals(emptyList(), validator.validate(schema("1/missing"), data))
    }

    @Test
    fun malformedTemporalPointerSyntaxFailsWithAbsentData() {
        for (reference in listOf("/sta~2rt", "1/sta~2rt", "x/start", "")) {
            val schema = JsonSchema(Json.parseToJsonElement("""{
                "type":"object","properties":{"end":{"type":"string","format":"date",
                    "formatMinimum":{"${'$'}data":"$reference"}}}
            }""").jsonObject)
            val error = validator.validate(schema, Json.parseToJsonElement("{}")).single()
            assertEquals("formatMinimum", error.keyword, reference)
            assertEquals("/properties/end/formatMinimum", error.schemaPath, reference)
            assertTrue(error.message.contains("data reference"), error.toString())
        }
    }

    @Test
    fun relativeTemporalReferenceInsideDefinitionUsesAppliedDataDepth() {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","${'$'}defs":{"bounded":{"type":"string","format":"date",
                "formatMinimum":{"${'$'}data":"1/start"}}},
            "properties":{"end":{"${'$'}ref":"#/${'$'}defs/bounded"}}
        }""").jsonObject)
        assertEquals("formatMinimum", validator.validate(schema,
            Json.parseToJsonElement("""{"start":"2025-01-10","end":"2025-01-09"}"""))
            .single().keyword)
        assertEquals(emptyList(), validator.validate(schema,
            Json.parseToJsonElement("""{"start":"2025-01-10","end":"2025-01-10"}""")))
    }

    @Test
    fun temporalComparisonWithoutFormatIsASchemaError() {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"end":{"type":"string",
                "formatMinimum":"2025-01-10"}}
        }""").jsonObject)
        val compiled = validator.compile(schema)
        for (data in listOf(
            Json.parseToJsonElement("""{"end":"2025-01-09"}"""),
            Json.parseToJsonElement("""{"end":"2025-01-11"}"""),
        )) {
            val error = compiled.validate(data).single()
            assertEquals("formatMinimum", error.keyword)
            assertEquals("/properties/end/formatMinimum", error.schemaPath)
            assertTrue(error.message.contains("requires a string format keyword"), error.toString())
        }
    }

    @Test
    fun temporalComparisonWithoutRegisteredComparatorIsASchemaError() {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"contact":{"type":"string","format":"email",
                "formatMaximum":"z@example.com"}}
        }""").jsonObject)
        val error = validator.validate(schema,
            Json.parseToJsonElement("""{"contact":"a@example.com"}""")).single()
        assertEquals("formatMaximum", error.keyword)
        assertEquals("/properties/contact/formatMaximum", error.schemaPath)
        assertTrue(error.message.contains("no comparator is registered"), error.toString())
    }

    @Test
    fun isoTemporalFormatsValidateAndCompareLikeAjvFormats() {
        val time = JsonSchema(Json.parseToJsonElement("""{
            "type":"string","format":"iso-time","formatMinimum":"10:00:00+02:00"
        }""").jsonObject)
        assertEquals("formatMinimum", validator.validate(time,
            JsonPrimitive("09:59:59-05:00")).single().keyword)
        assertEquals(emptyList(), validator.validate(time,
            JsonPrimitive("10:00:00-05:00")))
        assertEquals(emptyList(), validator.validate(time,
            JsonPrimitive("10:00:00")))
        assertEquals(emptyList(), validator.validate(time,
            JsonPrimitive("23:59:60")))
        assertEquals("format", validator.validate(time,
            JsonPrimitive("25:00:00")).single().keyword)

        val dateTime = JsonSchema(Json.parseToJsonElement("""{
            "type":"string","format":"iso-date-time",
            "formatMinimum":"2025-01-10T09:00:00Z"
        }""").jsonObject)
        assertEquals("formatMinimum", validator.validate(dateTime,
            JsonPrimitive("2025-01-09T23:59:00-12:00")).single().keyword)
        assertEquals("formatMinimum", validator.validate(dateTime,
            JsonPrimitive("2025-01-10T09:59:59+01:00")).single().keyword)
        assertEquals(emptyList(), validator.validate(dateTime,
            JsonPrimitive("2025-01-10T10:00:00+01:00")))
        val localDateTime = JsonSchema(Json.parseToJsonElement("""{
            "type":"string","format":"iso-date-time",
            "formatMinimum":"2025-01-10T09:00:00"
        }""").jsonObject)
        assertEquals(emptyList(), validator.validate(localDateTime,
            JsonPrimitive("2025-01-10 10:00:00")))
        assertEquals("format", validator.validate(dateTime,
            JsonPrimitive("2025-13-10T10:00:00")).single().keyword)

        val milliseconds = JsonSchema(Json.parseToJsonElement("""{
            "type":"string","format":"iso-date-time",
            "formatExclusiveMinimum":"2025-01-10T09:00:00.0003Z"
        }""").jsonObject)
        assertEquals("formatExclusiveMinimum", validator.validate(milliseconds,
            JsonPrimitive("2025-01-10T09:00:00.0004Z")).single().keyword)
        assertEquals(emptyList(), validator.validate(milliseconds,
            JsonPrimitive("2025-01-10T09:00:00.001Z")))
        val shortOffset = JsonSchema(Json.parseToJsonElement("""{
            "type":"string","format":"iso-date-time",
            "formatMinimum":"2025-01-10T10:00:00+02"
        }""").jsonObject)
        assertEquals(emptyList(), validator.validate(shortOffset,
            JsonPrimitive("2025-01-10T09:00:00Z")))
        val malformedTimeBound = JsonSchema(Json.parseToJsonElement("""{
            "type":"string","format":"iso-time","formatMinimum":"99:00:00"
        }""").jsonObject)
        assertEquals("formatMinimum", validator.validate(malformedTimeBound,
            JsonPrimitive("09:00:00")).single().keyword)
        val malformedDateTimeBound = JsonSchema(Json.parseToJsonElement("""{
            "type":"string","format":"iso-date-time",
            "formatMinimum":"2025-01-11Tbad"
        }""").jsonObject)
        assertEquals("formatMinimum", validator.validate(malformedDateTimeBound,
            JsonPrimitive("2025-01-10T09:00:00Z")).single().keyword)
        val malformedSameDateBound = JsonSchema(Json.parseToJsonElement("""{
            "type":"string","format":"iso-date-time",
            "formatMinimum":"2025-01-10Tbad"
        }""").jsonObject)
        assertEquals(emptyList(), validator.validate(malformedSameDateBound,
            JsonPrimitive("2025-01-10T09:00:00Z")))
    }

    @Test
    fun nonStringLiteralTemporalBoundFailsEvenWhenFieldIsAbsent() {
        for (literal in listOf("42", "null", "[]", "{}")) {
            val schema = JsonSchema(Json.parseToJsonElement("""{
                "type":"object","properties":{"hidden":{"type":"string","format":"date",
                    "formatMinimum":$literal}}
            }""").jsonObject)
            val error = validator.validate(schema, Json.parseToJsonElement("{}")).single()
            assertEquals("formatMinimum", error.keyword, literal)
            assertEquals("/properties/hidden/formatMinimum", error.schemaPath, literal)
            assertTrue(error.message.contains("must be a string"), error.toString())
        }

        val invalidStringBound = JsonSchema(Json.parseToJsonElement("""{
            "type":"string","format":"date","formatMinimum":"not-a-date"
        }""").jsonObject)
        assertEquals("formatMinimum", validator.validate(invalidStringBound,
            JsonPrimitive("2025-01-10")).single().keyword)
    }

    @Test
    fun temporalBoundsFollowReferencesAndApplicableBranches() {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","${'$'}defs":{"dated":{"type":"string","format":"date",
                "formatMinimum":"2025-01-10"}},
            "properties":{"value":{"${'$'}ref":"#/${'$'}defs/dated"}},
            "if":{"properties":{"kind":{"const":"strict"}}},
            "then":{"properties":{"other":{"type":"string","format":"date",
                "formatMaximum":"2025-01-20"}}}
        }""").jsonObject)
        val relaxed = validator.validate(schema,
            Json.parseToJsonElement("""{"kind":"relaxed","value":"2025-01-09","other":"2025-01-21"}"""))
        assertEquals(listOf("/value"), relaxed.map { it.instancePath }, relaxed.toString())
        val strict = validator.validate(schema,
            Json.parseToJsonElement("""{"kind":"strict","value":"2025-01-09","other":"2025-01-21"}"""))
        assertEquals(setOf("/value", "/other"), strict.map { it.instancePath }.toSet(), strict.toString())
        assertEquals(setOf("formatMinimum", "formatMaximum"), strict.map { it.keyword }.toSet())
    }

    @Test
    fun genericSchemaUriUsesLatestSupportedDialect() {
        val schema = buildJsonSchema {
            put("\$schema", "http://json-schema.org/schema#")
            put("type", "string")
            put("minLength", 3)
        }

        val errors = validator.validate(schema, JsonPrimitive("x"))

        assertEquals(1, errors.size)
        assertEquals("minLength", errors.single().keyword)
    }

    @Test
    fun nestedTupleFieldErrorHasAnAddressableInstancePath() {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":[{"type":"object","properties":{"street":{"type":"string","minLength":3}}}]}"""
        ).jsonObject)
        val errors = validator.validate(schema, Json.parseToJsonElement("""[{"street":"x"}]"""))
        assertEquals(1, errors.size)
        assertEquals("minLength", errors.single().keyword, errors.single().toString())
        assertEquals("/0/street", errors.single().instancePath)
    }

    @Test
    fun legacyTupleExcessItemKeepsValidatorItemPath() {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":[{"type":"string"},{"type":"integer"}],"additionalItems":false}"""
        ).jsonObject)
        val errors = validator.validate(schema, Json.parseToJsonElement("""["Code",3,true]"""))
        assertEquals(1, errors.size, errors.toString())
        assertEquals("additionalItems", errors.single().keyword, errors.toString())
        assertEquals("/2", errors.single().instancePath, errors.toString())
    }

    @Test
    fun explicitModernTupleDialectKeepsPrefixItemsSemantics() {
        val raw = Json.parseToJsonElement(
            """{"type":"array","prefixItems":[{"type":"string","minLength":3}]}"""
        ).jsonObject
        val schema = JsonSchema(JsonObject(raw + ("\$schema" to JsonPrimitive(
            "https://json-schema.org/draft/2020-12/schema",
        ))))
        val errors = validator.validate(schema, Json.parseToJsonElement("""["x"]"""))
        assertEquals(1, errors.size, errors.toString())
        assertEquals("minLength", errors.single().keyword)
        assertEquals("/0", errors.single().instancePath)
    }

    @Test
    fun modernTupleFalseTailValidatesOnlyItemsAfterPrefix() {
        val raw = Json.parseToJsonElement(
            """{"type":"array","prefixItems":[{"type":"string","minLength":3}],"items":false}"""
        ).jsonObject
        val schema = JsonSchema(JsonObject(raw + ("\$schema" to JsonPrimitive("https://json-schema.org/draft/2020-12/schema"))))
        val validPrefix = validator.validate(schema, Json.parseToJsonElement("""["Code"]"""))
        assertEquals(emptyList(), validPrefix)
        val extra = validator.validate(schema, Json.parseToJsonElement("""["Code",3]"""))
        assertEquals(1, extra.size, extra.toString())
        assertEquals("/1", extra.single().instancePath)
        val invalidPrefix = validator.validate(schema, Json.parseToJsonElement("""["x"]"""))
        assertEquals(1, invalidPrefix.size, invalidPrefix.toString())
        assertEquals("/0", invalidPrefix.single().instancePath)
        assertEquals("minLength", invalidPrefix.single().keyword)
        val bothInvalid = validator.validate(schema, Json.parseToJsonElement("""["x",3]"""))
        assertEquals(setOf("/0", "/1"), bothInvalid.map { it.instancePath }.toSet(), bothInvalid.toString())
        assertEquals(2, bothInvalid.size)
    }

    @Test
    fun modernTupleTypedTailDoesNotValidatePrefixAgainstTailSchema() {
        val raw = Json.parseToJsonElement(
            """{"type":"array","prefixItems":[{"type":"string","minLength":3}],"items":{"type":"integer"}}"""
        ).jsonObject
        val schema = JsonSchema(JsonObject(raw + ("\$schema" to JsonPrimitive("https://json-schema.org/draft/2020-12/schema"))))
        assertEquals(emptyList(), validator.validate(schema, Json.parseToJsonElement("""["Code",3]""")))
        val invalidTail = validator.validate(schema, Json.parseToJsonElement("""["Code","wrong"]"""))
        assertEquals(1, invalidTail.size, invalidTail.toString())
        assertEquals("/1", invalidTail.single().instancePath)
        val invalidPrefix = validator.validate(schema, Json.parseToJsonElement("""["x"]"""))
        assertEquals(1, invalidPrefix.size, invalidPrefix.toString())
        assertEquals("/0", invalidPrefix.single().instancePath)
        assertEquals("minLength", invalidPrefix.single().keyword)
        val bothInvalid = validator.validate(schema, Json.parseToJsonElement("""["x","wrong"]"""))
        assertEquals(setOf("/0", "/1"), bothInvalid.map { it.instancePath }.toSet(), bothInvalid.toString())
        assertEquals(2, bothInvalid.size)
    }

    @Test
    fun nestedModernTupleKeepsSiblingArrayErrorsSeparate() {
        val raw = Json.parseToJsonElement(
            """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"array","prefixItems":[{"type":"string","minLength":3}],"items":false}}}}"""
        ).jsonObject
        val schema = JsonSchema(JsonObject(raw + ("\$schema" to JsonPrimitive("https://json-schema.org/draft/2020-12/schema"))))
        val errors = validator.validate(schema, Json.parseToJsonElement("""{"rows":[["x"],["Code",3]]}"""))
        assertEquals(setOf("/rows/0/0", "/rows/1/1"), errors.map { it.instancePath }.toSet(), errors.toString())
        assertEquals(2, errors.size)
    }

    @Test
    fun modernTupleReferencedPrefixDoesNotAcquireFalseTailError() {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"${'$'}schema":"https://json-schema.org/draft/2020-12/schema","type":"array","${'$'}defs":{"short":{"type":"string","minLength":3}},"prefixItems":[{"${'$'}ref":"#/${'$'}defs/short"}],"items":false}"""
        ).jsonObject)
        val errors = validator.validate(schema, Json.parseToJsonElement("""["x"]"""))
        assertEquals(1, errors.size, errors.toString())
        assertEquals("minLength", errors.single().keyword)
    }

    @Test
    fun nestedLegacyItemsUsesDraftSevenWithoutInspectingExampleData() {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"rows":{"type":"array","items":[{"type":"string","minLength":3}]}},"default":{"items":["example"]}}"""
        ).jsonObject)
        val errors = validator.validate(schema, Json.parseToJsonElement("""{"rows":["x"]}"""))
        assertEquals(1, errors.size, errors.toString())
        assertEquals("minLength", errors.single().keyword)
        assertEquals("/rows/0", errors.single().instancePath)
    }

    @Test
    fun itemsNamedArrayInDefaultDoesNotChangeModernDialect() {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","prefixItems":[{"type":"string","minLength":3}],"default":{"items":["example"]}}"""
        ).jsonObject)
        val errors = validator.validate(schema, Json.parseToJsonElement("""["x"]"""))
        assertEquals(1, errors.size, errors.toString())
        assertEquals("minLength", errors.single().keyword)
        assertEquals("/0", errors.single().instancePath)
    }

    @Test
    fun unsupportedSchemaUriBecomesValidationError() {
        val schema = buildJsonSchema {
            put("\$schema", "https://example.com/not-a-json-schema-dialect")
            put("type", "object")
        }

        val errors = validator.validate(schema, buildJsonObject {})

        assertEquals(1, errors.size)
        assertEquals("\$schema", errors.single().keyword)
        assertEquals("/\$schema", errors.single().schemaPath)
        assertTrue(errors.single().message.startsWith("Unable to compile JSON Schema:"))
    }
}
