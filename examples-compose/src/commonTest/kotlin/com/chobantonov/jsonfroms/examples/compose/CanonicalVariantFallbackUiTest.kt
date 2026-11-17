package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.extended.renderers.cupertinoExtendedRenderers
import com.chobantonov.jsonfroms.extended.renderers.materialExtendedRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class CanonicalVariantFallbackUiTest {
    @Test fun materialInapplicableMultiSelectUsesOrdinaryArray() = arrayFallback(false)
    @Test fun cupertinoInapplicableMultiSelectUsesOrdinaryArray() = arrayFallback(true)
    @Test fun materialInapplicableChipsUsesOrdinaryIntegerArray() = integerChipsFallback(false)
    @Test fun cupertinoInapplicableChipsUsesOrdinaryIntegerArray() = integerChipsFallback(true)
    @Test fun materialUnrelatedCategorizationVariantUsesTabs() = categoryFallback(false)
    @Test fun cupertinoUnrelatedCategorizationVariantUsesTabs() = categoryFallback(true)

    private fun arrayFallback(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"tags":{"type":"array","items":{"type":"string"}}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"tags":["first"]}"""))
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/tags", options = mapOf(
                    "variant" to JsonPrimitive("multi-select"))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; writes++ })
        }
        onNodeWithText("Add choice").assertDoesNotExist()
        onNodeWithContentDescription("Add item").performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("first"), data.jsonObject.getValue("tags").jsonArray.first())
        assertEquals(2, data.jsonObject.getValue("tags").jsonArray.size)
        assertEquals(1, writes)
    }

    private fun integerChipsFallback(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"numbers":{"type":"array","items":{"type":"integer"}}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"numbers":[7]}"""))
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/numbers", options = mapOf(
                    "variant" to JsonPrimitive("chips"))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; writes++ })
        }
        onNodeWithText("Add choice").assertDoesNotExist()
        onNodeWithContentDescription("Add item").performClick()
        waitForIdle()
        assertEquals(JsonPrimitive(7), data.jsonObject.getValue("numbers").jsonArray.first())
        assertEquals(2, data.jsonObject.getValue("numbers").jsonArray.size)
        assertEquals(1, writes)
    }

    private fun categoryFallback(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"first":{"type":"string"},"second":{"type":"string"}}
        }""").jsonObject)
        val ui = CategorizationElement(
            options = mapOf("variant" to JsonPrimitive("chips")),
            elements = listOf(
                CategoryElement(label = "First", elements = listOf(
                    ControlElement("#/properties/first"))),
                CategoryElement(label = "Second", elements = listOf(
                    ControlElement("#/properties/second"))),
            ),
        )
        var writes = 0
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"first":"A","second":"B"}"""),
                schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { writes++ })
        }
        onNodeWithText("A").assertExists()
        onNodeWithText("Next").assertDoesNotExist()
        onNodeWithText("Second").performClick()
        onNodeWithText("B").assertExists()
        onNodeWithText("A").assertDoesNotExist()
        assertEquals(0, writes)
    }
}
