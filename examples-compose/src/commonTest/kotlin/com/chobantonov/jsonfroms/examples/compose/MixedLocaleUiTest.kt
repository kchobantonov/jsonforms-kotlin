package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class MixedLocaleUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"value":{"type":["object","array"]}}}"""
    ).jsonObject)
    private val data = Json.parseToJsonElement("""{"value":{"child":1}}""")

    @Test fun materialMixedTreeUsesLocaleDefaults() = tree(false)
    @Test fun cupertinoMixedTreeUsesLocaleDefaults() = tree(true)

    private fun tree(cupertino: Boolean) = runComposeUiTest {
        var locale by mutableStateOf("en-US")
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement(scope = "#/properties/value"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale), onChange = { changes++ })
        }
        onNodeWithText("Search tree…").assertExists()
        assertEquals("Expanded", onNodeWithContentDescription("Collapse")
            .fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        onNodeWithContentDescription("Collapse").performClick()
        onNodeWithText("Search tree…").assertDoesNotExist()
        runOnIdle { locale = "bg-BG" }
        onNodeWithContentDescription("Разгъни").performClick()
        onNodeWithText("Търси в дървото…").assertExists()
        onNodeWithContentDescription("Свий").assertExists()
        assertEquals("Разгъната", onNodeWithContentDescription("Свий")
            .fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        assertEquals(0, changes)
    }

    @Test fun materialMixedPromptUsesScopedOverride() = prompt(false)
    @Test fun cupertinoMixedPromptUsesScopedOverride() = prompt(true)

    private fun prompt(cupertino: Boolean) = runComposeUiTest {
        var override by mutableStateOf(false)
        setContent {
            JsonForms(data = Json.parseToJsonElement("{}"), schema = schema,
                uischema = ControlElement(scope = "#/properties/value", i18n = "value"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = "en-US", translate = { key, fallback, _ ->
                    if (override && key == "value.selectType") "Host type prompt" else fallback
                }))
        }
        onNodeWithText("Select a type").assertExists()
        runOnIdle { override = true }
        onNodeWithText("Host type prompt").assertExists()
    }
}
