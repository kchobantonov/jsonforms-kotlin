package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.isSelected
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class TemporalAncientDatePickerUiTest {
    @Test fun typedAncientDateOpensAtItsSelectedMonth() = runComposeUiTest {
        val schema = schema("date")
        var data by mutableStateOf(Json.parseToJsonElement("{}"))
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value"),
                renderers = materialRenderers,
                onChange = { data = it.data })
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("03/12/1220")
        assertEquals("1220-03-12", (data.jsonObject["value"] as JsonPrimitive).content)
        onNodeWithContentDescription("Choose date").performClick()
        onAllNodes(hasText("March 1220", substring = true)).onFirst().assertExists()
        onAllNodes(hasText("Thursday, March 12, 1220") and isSelected()).onFirst().assertExists()
    }

    @Test fun typedAncientDateTimeOpensAtItsSelectedMonth() = runComposeUiTest {
        val schema = schema("date-time")
        var data by mutableStateOf(Json.parseToJsonElement("{}"))
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value"),
                renderers = materialRenderers,
                onChange = { data = it.data })
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("03/12/1220 10:05 AM")
        assertTrue((data.jsonObject["value"] as JsonPrimitive).content.startsWith("1220-03-12T"))
        onNodeWithContentDescription("Choose date and time").performClick()
        onAllNodes(hasText("March 1220", substring = true)).onFirst().assertExists()
        onAllNodes(hasText("Thursday, March 12, 1220") and isSelected()).onFirst().assertExists()
    }

    private fun schema(format: String) = JsonSchema(Json.parseToJsonElement("""{
        "type":"object","properties":{"value":{"type":"string","format":"$format"}}
    }""").jsonObject)
}
