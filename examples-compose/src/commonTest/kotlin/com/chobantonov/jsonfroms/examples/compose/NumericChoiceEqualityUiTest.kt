package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class NumericChoiceEqualityUiTest {
    @Test fun materialEnumDropdownRecognizesEquivalentNumericValue() = verify(false, false, false)
    @Test fun cupertinoEnumDropdownRecognizesEquivalentNumericValue() = verify(true, false, false)
    @Test fun materialOneOfDropdownRecognizesEquivalentNumericValue() = verify(false, true, false)
    @Test fun cupertinoOneOfDropdownRecognizesEquivalentNumericValue() = verify(true, true, false)
    @Test fun materialEnumRadioRecognizesEquivalentNumericValue() = verify(false, false, true)
    @Test fun cupertinoEnumRadioRecognizesEquivalentNumericValue() = verify(true, false, true)
    @Test fun materialOneOfRadioRecognizesEquivalentNumericValue() = verify(false, true, true)
    @Test fun cupertinoOneOfRadioRecognizesEquivalentNumericValue() = verify(true, true, true)
    @Test fun materialOneOfCheckboxArrayRecognizesEquivalentNumericValue() = checkboxArray(false)
    @Test fun cupertinoOneOfCheckboxArrayRecognizesEquivalentNumericValue() = checkboxArray(true)
    @Test fun materialOneOfDropdownRecognizesNestedNumericValue() = composite(false, true, false)
    @Test fun cupertinoOneOfDropdownRecognizesNestedNumericValue() = composite(true, true, false)
    @Test fun materialOneOfRadioRecognizesNestedNumericValue() = composite(false, true, true)
    @Test fun cupertinoOneOfRadioRecognizesNestedNumericValue() = composite(true, true, true)
    @Test fun materialEnumRadioRecognizesNestedNumericValue() = composite(false, false, true)
    @Test fun cupertinoEnumRadioRecognizesNestedNumericValue() = composite(true, false, true)

    private fun composite(cupertino: Boolean, oneOf: Boolean, radio: Boolean) = runComposeUiTest {
        val choices = if (oneOf) """"oneOf":[
            {"const":{"x":1},"title":"Object choice"},
            {"const":[1,2],"title":"Array choice"}]"""
        else """"enum":[{"x":1},[1,2]]"""
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"choice":{$choices}}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement(
            """{"choice":{"x":1.0}}"""))
        var writes = 0
        val options = if (radio) mapOf("format" to JsonPrimitive("radio")) else emptyMap()
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/choice", options = options),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; writes++ })
        }
        val radios = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
        if (radio) onAllNodes(radios)[0].assertIsSelected()
        else onNodeWithText("Object choice").assertExists()
        onNodeWithText("Current value outside choices", substring = true).assertDoesNotExist()
        runOnIdle {
            assertEquals(0, writes)
            data = Json.parseToJsonElement("""{"choice":[1.0,2e0]}""")
        }
        if (radio) onAllNodes(radios)[1].assertIsSelected()
        else onNodeWithText("Array choice").assertExists()
        onNodeWithText("Current value outside choices", substring = true).assertDoesNotExist()
        runOnIdle { assertEquals(0, writes) }
    }

    private fun checkboxArray(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"choices":{"type":"array","uniqueItems":true,
                "items":{"oneOf":[{"const":1},{"const":2}]}}}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"choices":[1.0]}"""))
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = ControlElement("#/properties/choices"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; writes++ })
        }
        val checkboxes = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)
        onAllNodes(checkboxes)[0].assertIsOn()
        onAllNodes(checkboxes)[1].assertIsOff()
        onNodeWithText("Current value outside choices", substring = true).assertDoesNotExist()
        runOnIdle { assertEquals(0, writes) }
        onAllNodes(checkboxes)[0].performClick()
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{"choices":[]}"""), data)
            assertEquals(1, writes)
            data = Json.parseToJsonElement("""{"choices":[2e0]}""")
        }
        onAllNodes(checkboxes)[1].assertIsOn()
        onNodeWithText("Current value outside choices", substring = true).assertDoesNotExist()
        runOnIdle { assertEquals(1, writes) }
    }

    private fun verify(cupertino: Boolean, oneOf: Boolean, radio: Boolean) = runComposeUiTest {
        val choices = if (oneOf) """"oneOf":[{"const":1,"title":"One"},
            {"const":2,"title":"Two"}]""" else """"enum":[1,2]"""
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"value":{$choices}}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"value":1.0}"""))
        var writes = 0
        val options = if (radio) mapOf("format" to JsonPrimitive("radio")) else emptyMap()
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value", options = options),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; writes++ })
        }
        val firstLabel = if (oneOf) "One" else "1"
        val secondLabel = if (oneOf) "Two" else "2"
        val radios = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
        if (radio) onAllNodes(radios)[0].assertIsSelected()
        else onNodeWithText(firstLabel).assertExists()
        onNodeWithText("Current value outside choices", substring = true).assertDoesNotExist()
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{"value":1.0}"""), data)
            assertEquals(0, writes)
            data = Json.parseToJsonElement("""{"value":2e0}""")
        }
        if (radio) onAllNodes(radios)[1].assertIsSelected()
        else onNodeWithText(secondLabel).assertExists()
        onNodeWithText("Current value outside choices", substring = true).assertDoesNotExist()
        runOnIdle { assertEquals(0, writes) }
        if (radio) {
            onAllNodes(radios)[0].performClick()
            runOnIdle {
                assertEquals(Json.parseToJsonElement("""{"value":1}"""), data)
                assertEquals(1, writes)
            }
        }
    }
}
