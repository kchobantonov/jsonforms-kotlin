package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.Rule
import com.chobantonov.jsonfroms.core.models.RuleEffect
import com.chobantonov.jsonfroms.core.models.SchemaBasedCondition
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

@OptIn(ExperimentalTestApi::class)
class GroupDataPresenceUiTest {
    @Test fun materialDataPresenceFollowsBoundValuesAndLocale() = presence(false)

    @Test fun cupertinoDataPresenceFollowsBoundValuesAndLocale() = presence(true)
    @Test fun materialFallbackLabelAndDataIndicatorUseLocaleBundle() = noCatalog(false)
    @Test fun cupertinoFallbackLabelAndDataIndicatorUseLocaleBundle() = noCatalog(true)

    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"flag":{"type":["boolean","null"]},"count":{"type":["integer","null"]},"other":{"type":"string"},"show":{"type":"boolean"}}}"""
    ).jsonObject)

    private fun group(indicator: Boolean) = GroupElement(
        label = "Details",
        options = mapOf(
            "collapsible" to JsonPrimitive(true),
            "collapsed" to JsonPrimitive(true),
            "showDataIndicator" to JsonPrimitive(indicator),
        ),
        elements = listOf(
            ControlElement(
                scope = "#/properties/flag",
                rule = Rule(
                    effect = RuleEffect.SHOW,
                    condition = SchemaBasedCondition(
                        scope = "#/properties/show",
                        schema = buildJsonSchema { put("const", true) },
                    ),
                ),
            ),
            ControlElement(scope = "#/properties/count"),
        ),
    )

    private fun data(flag: String, count: String) = Json.parseToJsonElement(
        """{"flag":$flag,"count":$count,"other":"unrelated","show":false}"""
    )

    private fun presence(cupertino: Boolean) = runComposeUiTest {
        var value by mutableStateOf(data("null", "null"))
        var ui by mutableStateOf(group(true))
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(data = value, schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf("jsonformsExtended" to mapOf("showDataIndicator" to true)),
                i18n = JsonFormsI18nState(locale = locale, translate = { key, fallback, _ ->
                    if (key == "group.dataIndicator" && locale == "bg") "Съдържа данни" else fallback
                }),
                onChange = { changes++ })
        }
        fun english() = onNodeWithContentDescription("Contains data")
        fun bulgarian() = onNodeWithContentDescription("Съдържа данни")
        english().assertDoesNotExist()
        assertEquals(0, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        runOnIdle { value = data("false", "null") }
        english().assertExists()
        runOnIdle { locale = "bg" }
        english().assertDoesNotExist()
        bulgarian().assertExists()
        runOnIdle { value = data("null", "0") }
        bulgarian().assertExists()
        runOnIdle { ui = group(false) }
        bulgarian().assertDoesNotExist()
        runOnIdle { ui = group(true) }
        bulgarian().assertExists()
        runOnIdle { value = data("null", "null") }
        bulgarian().assertDoesNotExist()
        assertEquals(0, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        assertEquals(0, changes)
    }

    private fun noCatalog(cupertino: Boolean) = runComposeUiTest {
        var locale by mutableStateOf("en-US")
        setContent {
            JsonForms(data = data("false", "null"), schema = schema,
                uischema = group(true).copy(label = null),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale))
        }
        onNodeWithText("Group").assertExists()
        onNodeWithContentDescription("Contains data").assertExists()
        runOnIdle { locale = "bg-BG" }
        onNodeWithText("Група").assertExists()
        onNodeWithContentDescription("Съдържа данни").assertExists()
    }
}
