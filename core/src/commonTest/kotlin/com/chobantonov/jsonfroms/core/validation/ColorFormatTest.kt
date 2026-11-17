package com.chobantonov.jsonfroms.core.validation

import com.chobantonov.jsonfroms.core.models.JsonSchema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class ColorFormatTest {
    @Test fun schemaFormatUsesTheSameAcceptedNotationsAsTheRenderer() {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"shade":{"type":"string","format":"color"}}
        }""").jsonObject)
        val validator = OptimumCodeJsonSchemaValidator()
        for (valid in listOf("#abc", "#01020380", "rgb(1, 2, 3)",
            "hsl(120, 100%, 50%)", "hsba(120, 100%, 100%, 50%)")) {
            assertEquals(emptyList(), validator.validate(schema,
                Json.parseToJsonElement("""{"shade":"$valid"}""")), valid)
        }
        for (invalid in listOf("not-a-color", "#abcd", "rgb(256, 2, 3)",
            "rgb(1 2 3)", "hsb(120, 101%, 50%)")) {
            val errors = validator.validate(schema,
                Json.parseToJsonElement("""{"shade":"$invalid"}"""))
            assertEquals("/shade", errors.single().instancePath, invalid)
            assertEquals("format", errors.single().keyword, invalid)
        }
        val uiOnly = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"shade":{"type":"string"}}
        }""").jsonObject)
        assertEquals(emptyList(), validator.validate(uiOnly,
            Json.parseToJsonElement("""{"shade":"not-a-color"}""")))
    }
}
