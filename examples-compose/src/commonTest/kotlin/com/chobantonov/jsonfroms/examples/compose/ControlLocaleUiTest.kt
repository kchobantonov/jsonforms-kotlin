package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.store.ValidationMode
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ControlLocaleUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement("""{
        "type":"object","properties":{
            "count":{"type":"integer"},
            "ratio":{"type":"number"}
        }
    }""").jsonObject)
    private val ui = VerticalLayout(elements = listOf(
        ControlElement(scope = "#/properties/count"),
        ControlElement(scope = "#/properties/ratio"),
    ))

    @Test fun materialNumericErrorsAndClearActionFollowLocale() = numericLocale(false)
    @Test fun cupertinoNumericErrorsAndClearActionFollowLocale() = numericLocale(true)

    private fun numericLocale(cupertino: Boolean) = runComposeUiTest {
        var locale by mutableStateOf("en-US")
        var overrideClear by mutableStateOf(false)
        var preferScoped by mutableStateOf(false)
        var data by mutableStateOf(Json.parseToJsonElement("""{"count":1,"ratio":2}"""))
        setContent {
            JsonForms(
                data = data,
                schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                validationMode = ValidationMode.NoValidation,
                i18n = JsonFormsI18nState(locale = locale, translate = { key, fallback, _ ->
                    when {
                        key == "control.clear" && preferScoped -> "Scoped clear"
                        key == "clear" && overrideClear -> "Custom clear"
                        else -> fallback
                    }
                }),
                onChange = { data = it.data },
            )
        }
        onAllNodesWithContentDescription("Clear value").assertCountEquals(2)
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("bad")
        onAllNodes(hasSetTextAction()).onLast().performTextReplacement("bad")
        onNodeWithText("Invalid integer.").assertExists()
        onNodeWithText("Invalid number.").assertExists()
        runOnIdle { locale = "bg-BG" }
        onAllNodesWithContentDescription("Изчисти стойността").assertCountEquals(2)
        onNodeWithText("Невалидно цяло число.").assertExists()
        onNodeWithText("Невалидно число.").assertExists()
        runOnIdle { overrideClear = true }
        onAllNodesWithContentDescription("Custom clear").assertCountEquals(2)
        runOnIdle { preferScoped = true }
        onAllNodesWithContentDescription("Scoped clear").assertCountEquals(2)
    }

    @Test fun materialEnumClearUsesLocaleDefault() = enumClear(false, false)
    @Test fun cupertinoEnumClearUsesLocaleDefault() = enumClear(true, false)
    @Test fun materialOneOfClearUsesLocaleDefault() = enumClear(false, true)
    @Test fun cupertinoOneOfClearUsesLocaleDefault() = enumClear(true, true)

    private fun enumClear(cupertino: Boolean, oneOf: Boolean) = runComposeUiTest {
        val property = if (oneOf)
            """{"oneOf":[{"const":"yes","title":"Yes"},{"const":"no","title":"No"}]}"""
        else """{"type":"string","enum":["red","blue"]}"""
        val choiceSchema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"choice":$property}}""").jsonObject)
        var locale by mutableStateOf("en-US")
        var data by mutableStateOf(Json.parseToJsonElement(if (oneOf) """{"choice":"yes"}""" else """{"choice":"red"}"""))
        setContent {
            JsonForms(data = data, schema = choiceSchema,
                uischema = ControlElement(scope = "#/properties/choice"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { data = it.data })
        }
        onAllNodesWithContentDescription("Clear value").assertCountEquals(1)
        runOnIdle { locale = "bg-BG" }
        onAllNodesWithContentDescription("Изчисти стойността").assertCountEquals(1)
    }

    @Test fun materialEnumInvalidQueryUsesLocaleDefault() = runComposeUiTest {
        val choiceSchema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"choice":{"type":"string","enum":["red","blue"]}}}"""
        ).jsonObject)
        setContent {
            JsonForms(data = Json.parseToJsonElement("{}"), schema = choiceSchema,
                uischema = ControlElement(scope = "#/properties/choice",
                    options = mapOf("autocomplete" to JsonPrimitive(true))),
                renderers = materialRenderers, validationMode = ValidationMode.NoValidation,
                i18n = JsonFormsI18nState(locale = "en-US"))
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("unknown")
        onNodeWithText("Select a valid option.").assertExists()
    }
}
