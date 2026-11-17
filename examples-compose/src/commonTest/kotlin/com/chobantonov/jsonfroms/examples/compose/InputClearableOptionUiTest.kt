package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalTestApi::class)
class InputClearableOptionUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement("""{
      "type":"object","required":["name"],"properties":{
        "name":{"type":"string","minLength":2},
        "age":{"type":"integer","minimum":0}
      }
    }""").jsonObject)
    private val initial = Json.parseToJsonElement("""{"name":"Ada","age":36}""")

    @Test fun materialRequiredTextClearableOption() = requiredText(false)
    @Test fun cupertinoRequiredTextClearableOption() = requiredText(true)
    @Test fun materialNumericClearableOption() = numeric(false)
    @Test fun cupertinoNumericClearableOption() = numeric(true)

    private fun requiredText(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(initial)
        var clearable by mutableStateOf(false)
        var readonly by mutableStateOf(false)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/name",
                    options = mapOf("clearable" to JsonPrimitive(clearable))),
                readonly = readonly,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        runOnIdle { clearable = true }
        onNodeWithContentDescription("Clear value").assertExists()
        runOnIdle { readonly = true }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        runOnIdle { readonly = false }
        onNodeWithContentDescription("Clear value").performClick()
        runOnIdle {
            assertFalse("name" in data.jsonObject)
            assertEquals(1, changes)
        }
    }

    private fun numeric(cupertino: Boolean) = runComposeUiTest {
        var clearable by mutableStateOf(false)
        setContent {
            JsonForms(data = initial, schema = schema,
                uischema = ControlElement("#/properties/age",
                    options = mapOf("clearable" to JsonPrimitive(clearable))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        runOnIdle { clearable = true }
        onNodeWithContentDescription("Clear value").assertExists()
    }
}
