package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.LeafCondition
import com.chobantonov.jsonfroms.core.models.Rule
import com.chobantonov.jsonfroms.core.models.RuleEffect
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class FormatConflictMountedUiTest {
    @Test fun materialUiColorWinsOverSchemaPasswordInFullRegistry() = conflict(false)
    @Test fun cupertinoUiColorWinsOverSchemaPasswordInFullRegistry() = conflict(true)
    @Test fun materialConflictingColorStaysHiddenUntilRuleMatchesAbsentValue() = hiddenAbsent(false)
    @Test fun cupertinoConflictingColorStaysHiddenUntilRuleMatchesAbsentValue() = hiddenAbsent(true)
    @Test fun materialColorPresentationKeepsSchemaValidation() = presentationKeepsValidation(false)
    @Test fun cupertinoColorPresentationKeepsSchemaValidation() = presentationKeepsValidation(true)

    private fun presentationKeepsValidation(cupertino: Boolean) = runComposeUiTest {
        val schemaJson = Json.parseToJsonElement("""{
            "type":"object","properties":{
                "shade":{"type":"string","minLength":7},
                "other":{"type":"string"}
            }
        }""").jsonObject
        val schema = JsonSchema(schemaJson)
        var data by mutableStateOf(Json.parseToJsonElement("""{"shade":"#fff","other":"kept"}"""))
        var validity by mutableStateOf<CombinedValidityStatus?>(null)
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = VerticalLayout(elements = listOf(
                    ControlElement("#/properties/shade", options = mapOf(
                        "format" to JsonPrimitive("color"))),
                    ControlElement("#/properties/other"),
                )),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; writes++ },
                onValidityChange = { validity = it.status })
        }
        waitForIdle()
        assertEquals(CombinedValidityStatus.Invalid, validity)
        assertEquals(0, writes)
        onNodeWithContentDescription("Choose color").performClick()
        onNodeWithContentDescription("Color #ef4444").performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("#ef4444"), data.jsonObject["shade"])
        assertEquals(JsonPrimitive("kept"), data.jsonObject["other"])
        assertEquals(CombinedValidityStatus.Valid, validity)
        assertEquals(1, writes)
        assertEquals(schemaJson, schema.value)
    }

    private fun hiddenAbsent(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{
                "show":{"type":"boolean"},
                "shade":{"type":"string","format":"password"},
                "other":{"type":"string"}
            }
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"show":false,"other":"kept"}"""))
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = VerticalLayout(elements = listOf(
                    ControlElement("#/properties/shade", options = mapOf(
                        "format" to JsonPrimitive("color")),
                        rule = Rule(RuleEffect.SHOW,
                            LeafCondition("#/show", JsonPrimitive(true)))),
                    ControlElement("#/properties/other"),
                )),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; writes++ })
        }
        onNodeWithContentDescription("Choose color").assertDoesNotExist()
        assertEquals(null, data.jsonObject["shade"])
        assertEquals(0, writes)

        runOnIdle { data = Json.parseToJsonElement("""{"show":true,"other":"kept"}""") }
        onNodeWithContentDescription("Choose color").performClick()
        onNodeWithContentDescription("Color #ef4444").performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("#ef4444"), data.jsonObject["shade"])
        assertEquals(JsonPrimitive("kept"), data.jsonObject["other"])
        assertEquals(1, writes)

        runOnIdle {
            data = Json.parseToJsonElement("""{"show":false,"shade":"#ef4444","other":"host"}""")
        }
        onNodeWithContentDescription("Choose color").assertDoesNotExist()
        assertEquals(1, writes)
        runOnIdle {
            data = Json.parseToJsonElement("""{"show":true,"shade":"#00ff00","other":"host"}""")
        }
        onAllNodesWithText("#00ff00").onFirst().assertExists()
        assertEquals(JsonPrimitive("host"), data.jsonObject["other"])
        assertEquals(1, writes)
    }

    private fun conflict(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{
                "shade":{"type":"string","format":"password"},
                "other":{"type":"string"}
            }
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"shade":"#ff0000","other":"kept"}"""))
        var readonly by mutableStateOf(false)
        var locale by mutableStateOf("en")
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema, readonly = readonly,
                uischema = VerticalLayout(elements = listOf(
                    ControlElement("#/properties/shade", options = mapOf(
                        "format" to JsonPrimitive("color"))),
                    ControlElement("#/properties/other"),
                )),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { data = it.data; writes++ })
        }
        onNodeWithContentDescription("Choose color").assertExists()
        onAllNodesWithText("#ff0000").onFirst().performTextReplacement("rgb(0, 255")
        onNodeWithText("Invalid color.").assertExists()
        assertEquals(JsonPrimitive("#ff0000"), data.jsonObject["shade"])
        assertEquals(0, writes)
        runOnIdle { readonly = true }
        onNodeWithContentDescription("Choose color").assertDoesNotExist()
        assertEquals(JsonPrimitive("kept"), data.jsonObject["other"])
        runOnIdle { readonly = false; locale = "bg-BG" }
        onNodeWithContentDescription("Избери цвят").performClick()
        onNodeWithContentDescription("Цвят #ef4444").performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("#ef4444"), data.jsonObject["shade"])
        assertEquals(JsonPrimitive("kept"), data.jsonObject["other"])
        assertEquals(1, writes)
        runOnIdle { data = Json.parseToJsonElement("""{"shade":"#00ff00","other":"host"}""") }
        onAllNodesWithText("#00ff00").onFirst().assertExists()
        assertEquals(JsonPrimitive("host"), data.jsonObject["other"])
        assertEquals(1, writes)
    }
}
