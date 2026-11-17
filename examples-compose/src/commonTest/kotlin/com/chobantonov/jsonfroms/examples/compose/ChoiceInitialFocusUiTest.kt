package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ChoiceInitialFocusUiTest {
    @Test fun materialEnumSelectFocusesOnce() = focus(false, false, false)
    @Test fun cupertinoEnumSelectFocusesOnce() = focus(true, false, false)
    @Test fun materialOneOfSelectFocusesOnce() = focus(false, true, false)
    @Test fun cupertinoOneOfSelectFocusesOnce() = focus(true, true, false)
    @Test fun materialSearchableEnumFocusesInputOnce() = focus(false, false, true)
    @Test fun cupertinoSearchableEnumFocusesInputOnce() = focus(true, false, true)

    private fun focus(cupertino: Boolean, oneOf: Boolean, searchable: Boolean) = runComposeUiTest {
        val choices = if (oneOf) """"oneOf":[
            {"const":"red","title":"Red"},{"const":"blue","title":"Blue"}
        ]""" else """"type":"string","enum":["Red","Blue"]"""
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{
                "choice":{$choices},"other":{"type":"string"}
            }
        }""").jsonObject)
        val selected = if (oneOf) "blue" else "Blue"
        var data by mutableStateOf(Json.parseToJsonElement("""{"choice":"$selected","other":"X"}"""))
        var request by mutableStateOf(true)
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = VerticalLayout(elements = listOf(
                    ControlElement("#/properties/choice", options = mapOf(
                        "focus" to JsonPrimitive(request),
                        "autocomplete" to JsonPrimitive(searchable))),
                    ControlElement("#/properties/other"),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { data = it.data; changes++ })
        }
        waitForIdle()
        val choice = if (searchable) onAllNodes(hasSetTextAction()).onFirst()
            else onNodeWithText("Blue")
        choice.assertIsFocused()
        onNodeWithText("Red").assertDoesNotExist()
        val other = onAllNodes(hasSetTextAction()).onLast()
        other.performClick()
        other.assertIsFocused()
        runOnIdle {
            data = Json.parseToJsonElement("""{"choice":"$selected","other":"Changed"}""")
            request = false
            locale = "bg"
        }
        waitForIdle()
        runOnIdle { request = true }
        waitForIdle()
        choice.assertIsNotFocused()
        other.assertIsFocused()
        assertEquals(0, changes)
    }
}
