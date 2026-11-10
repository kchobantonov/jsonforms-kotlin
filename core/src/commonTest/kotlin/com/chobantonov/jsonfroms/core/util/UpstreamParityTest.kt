package com.chobantonov.jsonfroms.core.util

import com.chobantonov.jsonfroms.core.generators.generateDefaultUISchema
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UpstreamParityTest {
    @Test
    fun nullableObjectRootGeneratesChildControls() {
        val schema = JsonSchema(Json.parseToJsonElement("""
            {"type":["null","object"],"properties":{"name":{"type":"string"}}}
        """) as JsonObject)
        assertEquals(
            VerticalLayout(elements = listOf(ControlElement("#/properties/name"))),
            generateDefaultUISchema(schema),
        )
    }

    @Test
    fun requiredErrorAppendsPropertyEvenWhenParentEndsWithItsName() {
        assertEquals("name.name", getControlPath(ErrorObject(
            instancePath = "/name", keyword = "required", message = "required",
            params = mapOf("missingProperty" to "name"),
        )))
    }

    @Test
    fun idsReuseReleasedSlotsAndAvoidPrefixCollisions() {
        clearAllIds()
        try {
            assertEquals("item", createId("item"))
            assertEquals("item2", createId("item"))
            assertEquals("item3", createId("item"))
            assertEquals("item32", createId("item3")) // distinct prefix must avoid an occupied ID
            removeId("item2")
            assertEquals("item2", createId("item"))
        } finally {
            clearAllIds()
        }
    }

    @Test
    fun arrayPathsRejectLeadingZeroIndices() {
        assertFailsWith<IllegalArgumentException> {
            JsonPath.set(Json.parseToJsonElement("[]"), "01", Json.parseToJsonElement("1"))
        }
    }
}
