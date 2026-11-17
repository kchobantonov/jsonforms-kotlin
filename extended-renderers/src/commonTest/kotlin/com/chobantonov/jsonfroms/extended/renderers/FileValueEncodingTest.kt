package com.chobantonov.jsonfroms.extended.renderers

import com.chobantonov.jsonfroms.core.models.JsonSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

class FileValueEncodingTest {
    private fun schema(json: String) = JsonSchema(Json.parseToJsonElement(json).jsonObject)

    @Test fun base64AndByteStorePayloadOnly() {
        assertEquals("YWJj", encodedFileValue(
            schema("""{"type":"string","contentEncoding":"base64"}"""), "YWJj", "text/plain", "a.txt"
        ).content)
        assertEquals("YWJj", encodedFileValue(
            schema("""{"type":"string","format":"byte"}"""), "YWJj", "text/plain", "a.txt"
        ).content)
    }

    @Test fun binaryWinsOverBase64AndRetainsEncodedFilename() {
        assertEquals(
            "data:text/plain;filename=hello%20%C3%A4.txt;base64,YWJj",
            encodedFileValue(
                schema("""{"type":"string","format":"binary","contentEncoding":"base64"}"""),
                "YWJj", "text/plain", "hello ä.txt",
            ).content,
        )
    }

    @Test fun storedFilenameDecodesUtf8AndEscapedPunctuation() {
        val value = encodedFileValue(schema("""{"type":"string","format":"binary"}"""), "YWJj", "text/plain", "hello ä+%.txt")
        assertEquals("hello ä+%.txt", fileNameFromStoredValue(value))
        assertNull(fileNameFromStoredValue(JsonPrimitive("YWJj")))
    }

    @Test fun missingMimeFallsBackToOctetStream() {
        assertEquals(
            "data:application/octet-stream;filename=a%2Bb.txt;base64,YWJj",
            encodedFileValue(schema("""{"type":"string","format":"binary"}"""), "YWJj", null, "a+b.txt").content,
        )
    }
}
