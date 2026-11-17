package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class InputDescriptionUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"code":{"type":"string","minLength":3,"description":"Enter code."}}}"""
    ).jsonObject)

    @Test fun materialHelpRemainsBesideError() = helpAndError(false)
    @Test fun cupertinoHelpRemainsBesideError() = helpAndError(true)

    @Test fun materialUnfocusedDescriptionReadsTopLevelConfig() = topLevelConfig(false)
    @Test fun cupertinoUnfocusedDescriptionReadsTopLevelConfig() = topLevelConfig(true)

    private fun topLevelConfig(cupertino: Boolean) = runComposeUiTest {
        var show by mutableStateOf(true)
        var changes = 0
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"code":"abc"}"""), schema = schema,
                uischema = ControlElement("#/properties/code"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf(
                    "showUnfocusedDescription" to show,
                    "jsonformsExtended" to mapOf("showUnfocusedDescription" to !show),
                ),
                onChange = { changes++ },
            )
        }
        onNodeWithText("Enter code.").assertExists()
        runOnIdle { show = false }
        waitForIdle()
        onNodeWithText("Enter code.").assertDoesNotExist()
        assertEquals(0, changes)
    }

    private fun helpAndError(cupertino: Boolean) = runComposeUiTest {
        val data = Json.parseToJsonElement("""{"code":"a"}""")
        var showWhenUnfocused by mutableStateOf<JsonElement>(JsonPrimitive(true))
        var validity: CombinedValidityStatus? = null
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ControlElement("#/properties/code",
                    options = mapOf("showUnfocusedDescription" to showWhenUnfocused)),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf("showUnfocusedDescription" to true),
                onChange = { changes++ },
                onValidityChange = { validity = it.status },
            )
        }
        val input = onAllNodes(hasSetTextAction()).onFirst()
        onNodeWithText("Enter code.").assertExists()
        assertTrue(input.fetchSemanticsNode().config[SemanticsProperties.Error].isNotBlank())
        assertEquals("Enter code.", input.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        assertEquals(CombinedValidityStatus.Invalid, validity)
        runOnIdle { showWhenUnfocused = JsonPrimitive(false) }
        waitForIdle()
        onNodeWithText("Enter code.").assertDoesNotExist()
        runOnIdle { showWhenUnfocused = JsonPrimitive("true") }
        waitForIdle()
        onNodeWithText("Enter code.").assertDoesNotExist()
        input.performClick()
        input.assertIsFocused()
        onNodeWithText("Enter code.").assertExists()
        assertTrue(input.fetchSemanticsNode().config[SemanticsProperties.Error].isNotBlank())
        assertEquals("Enter code.", input.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        assertEquals(0, changes)
    }
}
