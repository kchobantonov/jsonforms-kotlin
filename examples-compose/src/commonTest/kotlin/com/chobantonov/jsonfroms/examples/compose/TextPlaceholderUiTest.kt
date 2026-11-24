package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class TextPlaceholderUiTest {
    @Test fun materialHintUpdateRetainsEditingSession() = hintUpdate(false)
    @Test fun cupertinoHintUpdateRetainsEditingSession() = hintUpdate(true)

    private fun hintUpdate(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"value":{"type":"string","title":"Value"}}}"""
        ).jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("{}"))
        var control by mutableStateOf(ControlElement("#/properties/value", options = mapOf(
            "placeholder" to JsonPrimitive("First hint"),
        )))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = control,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        fun field() = onAllNodes(hasSetTextAction()).onFirst()
        assertTrue(field().fetchSemanticsNode().config[SemanticsProperties.StateDescription]
            .contains("First hint"))
        field().performTextInput("Ada")
        field().performClick()
        field().performSemanticsAction(SemanticsActions.SetSelection) { it(1, 1, false) }
        waitForIdle()
        assertEquals(1, changes)
        runOnIdle { control = control.copy(options = mapOf("placeholder" to JsonPrimitive("Updated hint"))) }
        field().assertIsFocused()
        assertEquals(1, field().fetchSemanticsNode()
            .config[SemanticsProperties.TextSelectionRange].start)
        assertEquals(JsonPrimitive("Ada"), data.jsonObject["value"])
        assertEquals(1, changes)
        field().performTextInput("X")
        waitForIdle()
        assertEquals(JsonPrimitive("AXda"), data.jsonObject["value"])
        assertEquals(2, changes)
    }
}
