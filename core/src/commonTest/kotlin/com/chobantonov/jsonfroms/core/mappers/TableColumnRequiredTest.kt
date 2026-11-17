package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.JsonSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

class TableColumnRequiredTest {
    private val item = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","required":["name"],"properties":{"name":{"type":"string"},"note":{"type":"string"}}}"""
    ).jsonObject)

    private fun label(
        field: String?, options: JsonObject = JsonObject(emptyMap()),
        config: Map<String, Any?> = emptyMap(),
    ) = tableColumnHeaderLabel("Name", field, item, options, config)

    @Test fun requiredFieldGetsMarkerAndOptionalOrWholeRowDoesNot() {
        assertEquals("Name*", label("name"))
        assertEquals("Name", label("note"))
        assertEquals("Name", label(null))
    }

    @Test fun cellOverridesCollectionAndCollectionOverridesGlobalIncludingExplicitFalse() {
        val cellShow = Json.parseToJsonElement(
            """{"hideRequiredAsterisk":true,"cells":{"name":{"hideRequiredAsterisk":false}}}"""
        ).jsonObject
        assertEquals("Name*", label("name", cellShow, mapOf("hideRequiredAsterisk" to true)))
        val cellHide = Json.parseToJsonElement(
            """{"hideRequiredAsterisk":false,"cells":{"name":{"hideRequiredAsterisk":true}}}"""
        ).jsonObject
        assertEquals("Name", label("name", cellHide, mapOf("hideRequiredAsterisk" to false)))
        assertEquals("Name*", label("name", JsonObject(mapOf("hideRequiredAsterisk" to JsonPrimitive(false))),
            mapOf("hideRequiredAsterisk" to true)))
        assertEquals("Name", label("name", config = mapOf("hideRequiredAsterisk" to true)))
    }

    @Test fun quotedFlagsDoNotHideRequiredMarkerOrOverrideTypedCollectionFlag() {
        val quotedCell = Json.parseToJsonElement(
            """{"hideRequiredAsterisk":true,"cells":{"name":{"hideRequiredAsterisk":"false"}}}"""
        ).jsonObject
        assertEquals("Name", label("name", quotedCell))
        assertEquals("Name*", label("name", config = mapOf("hideRequiredAsterisk" to "true")))
    }
}
