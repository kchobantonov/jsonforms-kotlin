package com.chobantonov.jsonfroms.extended.renderers

import com.chobantonov.jsonfroms.core.models.JsonSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

class FilePickerEventsTest {
    @Test fun inclusiveBoundsAcceptEqualityAndRejectOutside() {
        val options = mapOf("formatMinimum" to JsonPrimitive(10), "formatMaximum" to JsonPrimitive(20))
        assertNull(fileSizeViolation(10, options))
        assertNull(fileSizeViolation(20, options))
        assertEquals("formatMinimum", fileSizeViolation(9, options)?.option)
        assertEquals("formatMaximum", fileSizeViolation(21, options)?.option)
    }

    @Test fun exclusiveBoundsRejectEquality() {
        val options = mapOf(
            "formatExclusiveMinimum" to JsonPrimitive(10),
            "formatExclusiveMaximum" to JsonPrimitive(20),
        )
        assertEquals("formatExclusiveMinimum", fileSizeViolation(10, options)?.option)
        assertEquals("formatExclusiveMaximum", fileSizeViolation(20, options)?.option)
        assertNull(fileSizeViolation(15, options))
    }

    @Test fun numericStringsAndInvalidBounds() {
        assertEquals("formatMaximum", fileSizeViolation(
            21, mapOf("formatMaximum" to JsonPrimitive("20"))
        )?.option)
        assertNull(fileSizeViolation(21, mapOf("formatMaximum" to JsonPrimitive("-1"))))
        assertNull(fileSizeViolation(21, mapOf("formatMaximum" to JsonPrimitive("Infinity"))))
    }

    @Test fun restrictIsExplicit() {
        assertTrue(fileRestrictionEnabled(mapOf("restrict" to JsonPrimitive(true))))
        assertEquals(false, fileRestrictionEnabled(emptyMap()))
    }

    private val binarySchema = JsonSchema(Json.parseToJsonElement("""{"type":"string","format":"binary"}""").jsonObject)

    @Test fun missingMetadataStillUsesReadByteCount() {
        val options = mapOf("formatMaximum" to JsonPrimitive(2), "restrict" to JsonPrimitive(true))
        assertNull(fileSizeViolation(-1, options))
        val result = selectedFileEvent(binarySchema, options, "abc".encodeToByteArray(), "text/plain", "a.txt")
        assertEquals("formatMaximum", (result as FilePickerEvent.RejectedSize).violation.option)
        assertEquals(3L, result.violation.actualBytes)
    }

    @Test fun unrestrictedReadPublishesActualByteCountAndEncodedValue() {
        val options = mapOf("formatMaximum" to JsonPrimitive(2))
        val result = selectedFileEvent(binarySchema, options, "abc".encodeToByteArray(), "text/plain", "a.txt")
            as FilePickerEvent.Selected
        assertEquals(3L, result.originalBytes)
        assertEquals(JsonPrimitive("data:text/plain;filename=a.txt;base64,YWJj"), result.value)
        assertEquals("formatMaximum", fileSizeViolation(result.originalBytes, options)?.option)
    }

    @Test fun batchKeepsAcceptedChooserOrderAndSizeWarning() {
        val rejected = FileSizeViolation("formatMaximum", 2.0, 3)
        val result = batchFileEvent(listOf(
            FilePickerEvent.Selected(JsonPrimitive("first"), 1),
            FilePickerEvent.RejectedSize(rejected),
            FilePickerEvent.Selected(JsonPrimitive("second"), 2),
        )) as FilePickerEvent.SelectedBatch
        assertEquals(listOf(JsonPrimitive("first"), JsonPrimitive("second")), result.files.map { it.value })
        assertEquals(rejected, result.rejectedSize)
    }

    @Test fun anyReadFailureAbortsBatch() {
        val result = batchFileEvent(listOf(
            FilePickerEvent.Selected(JsonPrimitive("first"), 1),
            FilePickerEvent.ReadFailed("second file"),
        ))
        assertEquals(FilePickerEvent.ReadFailed("second file"), result)
    }
}
