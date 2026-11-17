package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class CombinatorBranchConfirmationUiTest {
    private val original = Json.parseToJsonElement("""{"choice":{"name":"Ada"}}""")
    private val replacement = Json.parseToJsonElement("""{"choice":{"name":"Host"}}""")
    private val second = Json.parseToJsonElement("""{"choice":{"other":"new"}}""")

    private fun schema(oneOf: Boolean) = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"choice":{"${if (oneOf) "oneOf" else "anyOf"}":[
          {"title":"First","type":"object","properties":{"name":{"type":"string"}},"required":["name"]},
          {"title":"Second","type":"object","default":{"other":"new"},"properties":{"other":{"type":"string"}},"required":["other"]}
        ]}}}""",
    ).jsonObject)

    @Test fun materialOneOfConfirmsAndCancels() = confirm(false, true)
    @Test fun cupertinoOneOfConfirmsAndCancels() = confirm(true, true)
    @Test fun materialAnyOfTabsPreserveAndEditSameValue() = anyOfTabs(false)
    @Test fun cupertinoAnyOfTabsPreserveAndEditSameValue() = anyOfTabs(true)
    @Test fun materialAnyOfMissingValueDoesNotInitialize() = anyOfMissing(false)
    @Test fun cupertinoAnyOfMissingValueDoesNotInitialize() = anyOfMissing(true)
    @Test fun materialAnyOfReadonlyNavigation() = anyOfReadonly(false)
    @Test fun cupertinoAnyOfReadonlyNavigation() = anyOfReadonly(true)
    @Test fun cupertinoAnyOfTabsTrackHostBranchReplacementWithoutWriting() = runComposeUiTest {
        var data by mutableStateOf<JsonElement>(original)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema(false),
                uischema = ControlElement("#/properties/choice"),
                renderers = cupertinoRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("First").assertIsSelected()
        onNodeWithText("Second").performClick().assertIsSelected()
        runOnIdle {
            assertEquals(0, changes)
            data = Json.parseToJsonElement("""{"choice":{"other":"host"}}""")
        }
        onNodeWithText("Second").assertIsSelected()
        runOnIdle { data = original }
        onNodeWithText("First").assertIsSelected()
        runOnIdle { assertEquals(0, changes) }
    }
    @Test fun materialOneOfHostReplacementCancels() = hostReplacement(false, true)
    @Test fun cupertinoOneOfHostReplacementCancels() = hostReplacement(true, true)

    private fun anyOfTabs(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf<JsonElement>(original)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema(false), uischema = ControlElement("#/properties/choice"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Second").performClick()
        onNodeWithText("Change this branch?").assertDoesNotExist()
        runOnIdle { assertEquals(original, data); assertEquals(0, changes) }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("new")
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{"choice":{"name":"Ada","other":"new"}}"""), data)
            assertEquals(1, changes)
        }
    }

    private fun anyOfMissing(cupertino: Boolean) = runComposeUiTest {
        val empty = Json.parseToJsonElement("{}")
        var data by mutableStateOf<JsonElement>(empty)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema(false), uischema = ControlElement("#/properties/choice"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Second").performClick()
        onNodeWithText("Second").assertIsSelected()
        onNodeWithText("Change this branch?").assertDoesNotExist()
        runOnIdle { assertEquals(empty, data); assertEquals(0, changes) }
    }

    private fun anyOfReadonly(cupertino: Boolean) = runComposeUiTest {
        var changes = 0
        setContent {
            JsonForms(data = original, schema = schema(false), uischema = ControlElement("#/properties/choice"),
                readonly = true, renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++ })
        }
        onNodeWithText("Second").performClick()
        onNodeWithText("Change this branch?").assertDoesNotExist()
        onNodeWithText("Second").assertIsSelected()
        runOnIdle { assertEquals(0, changes) }
    }

    private fun confirm(cupertino: Boolean, oneOf: Boolean) = runComposeUiTest {
        var data by mutableStateOf(original)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema(oneOf), uischema = ControlElement("#/properties/choice"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        if (oneOf) onNodeWithText("First").performClick()
        onNodeWithText("Second").performClick()
        onNodeWithText("Change this branch?").assertExists()
        runOnIdle { assertEquals(original, data); assertEquals(0, changes) }
        onNodeWithText("No").performClick()
        runOnIdle { assertEquals(original, data); assertEquals(0, changes) }
        if (oneOf) onNodeWithText("First").performClick()
        onNodeWithText("Second").performClick()
        onNodeWithText("Yes").performClick()
        runOnIdle { assertEquals(second, data); assertEquals(1, changes) }
    }

    private fun hostReplacement(cupertino: Boolean, oneOf: Boolean) = runComposeUiTest {
        var data by mutableStateOf<JsonElement>(original)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema(oneOf), uischema = ControlElement("#/properties/choice"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        if (oneOf) onNodeWithText("First").performClick()
        onNodeWithText("Second").performClick()
        onNodeWithText("Change this branch?").assertExists()
        runOnIdle { data = replacement }
        onNodeWithText("Change this branch?").assertDoesNotExist()
        runOnIdle { assertEquals(replacement, data); assertEquals(0, changes) }
    }

}
