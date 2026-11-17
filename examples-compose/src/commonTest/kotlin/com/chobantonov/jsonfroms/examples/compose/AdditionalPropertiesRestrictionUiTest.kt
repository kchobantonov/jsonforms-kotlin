package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class AdditionalPropertiesRestrictionUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","minProperties":2,"maxProperties":2,
          "properties":{"fixed":{"type":"string"}},"additionalProperties":{"type":"string"}}"""
    ).jsonObject)

    @Test fun materialRenamePreservesCountAndClosesOnHostChange() = renameAtBoundary(false)
    @Test fun cupertinoRenamePreservesCountAndClosesOnHostChange() = renameAtBoundary(true)
    @Test fun materialAddRepairsMinimumAndLocalFalseAllowsMaximumViolation() = addAcrossCountBounds(false)
    @Test fun cupertinoAddRepairsMinimumAndLocalFalseAllowsMaximumViolation() = addAcrossCountBounds(true)

    private fun addAcrossCountBounds(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"fixed":"declared"}"""))
        var restrict by mutableStateOf(true)
        var validity: CombinedValidityStatus? = null
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ControlElement("#", options = mapOf("restrict" to JsonPrimitive(restrict))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data },
                onValidityChange = { validity = it.status },
            )
        }
        assertEquals(CombinedValidityStatus.Invalid, validity)
        onAllNodes(hasSetTextAction() and hasText("")).onFirst().performTextReplacement("extra")
        onNodeWithContentDescription("Add button").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"fixed":"declared","extra":""}"""), data)
        assertEquals(CombinedValidityStatus.Valid, validity)
        onNodeWithContentDescription("Add button").assertIsNotEnabled()

        runOnIdle { restrict = false }
        waitForIdle()
        onAllNodes(hasSetTextAction() and hasText("")).onFirst().performTextReplacement("third")
        onNodeWithContentDescription("Add button").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"fixed":"declared","extra":"","third":""}"""), data)
        assertEquals(CombinedValidityStatus.Invalid, validity)
    }

    private fun renameAtBoundary(cupertino: Boolean) = runComposeUiTest {
        val initial = Json.parseToJsonElement("""{"fixed":"declared","extra":"value"}""")
        var data by mutableStateOf(initial)
        var readonly by mutableStateOf(false)
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema, uischema = ControlElement("#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                readonly = readonly,
                onChange = { data = it.data; changes++ },
            )
        }
        onNodeWithContentDescription("Add button").assertIsNotEnabled()
        onNodeWithContentDescription("Delete button").assertIsNotEnabled()
        onNodeWithContentDescription("Rename button").performClick()
        onAllNodes(hasSetTextAction() and hasText("extra")).onFirst().performTextReplacement("renamed")
        runOnIdle { readonly = true }
        waitForIdle()
        onNodeWithText("New property name").assertDoesNotExist()
        assertEquals(initial, data)
        assertEquals(0, changes)

        runOnIdle { readonly = false }
        waitForIdle()
        onNodeWithContentDescription("Rename button").performClick()
        onAllNodes(hasSetTextAction() and hasText("extra")).onFirst().performTextReplacement("renamed")
        onNodeWithContentDescription("Save button").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"fixed":"declared","renamed":"value"}"""), data)
        assertEquals(2, data.jsonObject.size)
        assertEquals(1, changes)
        onNodeWithContentDescription("Add button").assertIsNotEnabled()
        onNodeWithContentDescription("Delete button").assertIsNotEnabled()

        onNodeWithContentDescription("Rename button").performClick()
        onAllNodes(hasSetTextAction() and hasText("renamed")).onFirst().performTextReplacement("next")
        val replacement = Json.parseToJsonElement("""{"fixed":"declared","renamed":"host"}""")
        runOnIdle { data = replacement }
        waitForIdle()
        onNodeWithText("New property name").assertDoesNotExist()
        assertEquals(replacement, data)
        assertEquals(1, changes)
    }
}
