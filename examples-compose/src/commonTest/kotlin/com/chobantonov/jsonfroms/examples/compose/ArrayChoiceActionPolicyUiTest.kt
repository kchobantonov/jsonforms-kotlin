package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class ArrayChoiceActionPolicyUiTest {
    private val finiteSchema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"teams":{"type":"array","uniqueItems":true,"items":{"type":"string","enum":["ops","fin","qa"]}}}}"""
    ).jsonObject)
    private val freeSchema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"teams":{"type":"array","items":{"type":"string"}}}}"""
    ).jsonObject)
    private val initial = Json.parseToJsonElement("""{"teams":["ops"]}""")
    private val added = Json.parseToJsonElement("""{"teams":["ops","fin"]}""")

    @Test fun materialCheckboxPolicy() = policy(false, "checkbox")
    @Test fun cupertinoCheckboxPolicy() = policy(true, "checkbox")
    @Test fun materialMultiSelectPolicy() = policy(false, "multi-select")
    @Test fun cupertinoMultiSelectPolicy() = policy(true, "multi-select")
    @Test fun materialFiniteChipsPolicy() = policy(false, "chips")
    @Test fun cupertinoFiniteChipsPolicy() = policy(true, "chips")
    @Test fun materialFreeChipsPolicy() = policy(false, "free-chips")
    @Test fun cupertinoFreeChipsPolicy() = policy(true, "free-chips")

    private fun policy(cupertino: Boolean, variant: String) = runComposeUiTest {
        var data by mutableStateOf(initial)
        var localDisable by mutableStateOf<Boolean?>(null)
        var globalDisable by mutableStateOf(true)
        var changes = 0
        val checkboxes = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)
        setContent {
            val options = (if (variant == "checkbox") emptyMap() else mapOf(
                "variant" to JsonPrimitive(if (variant == "free-chips") "chips" else variant)
            )) + (localDisable?.let { mapOf(
                "disableAdd" to JsonPrimitive(it), "disableRemove" to JsonPrimitive(it)
            ) } ?: emptyMap())
            JsonForms(
                data = data,
                schema = if (variant == "free-chips") freeSchema else finiteSchema,
                uischema = ControlElement("#/properties/teams", options = options),
                config = mapOf("disableAdd" to globalDisable, "disableRemove" to globalDisable),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ },
            )
        }
        if (variant == "checkbox") {
            onAllNodes(checkboxes)[0].assertIsNotEnabled()
            onAllNodes(checkboxes)[1].assertIsNotEnabled()
        } else {
            onNodeWithText("Remove choice: ops").assertIsNotEnabled()
            onNodeWithText("Add choice").assertIsNotEnabled()
        }
        runOnIdle { localDisable = false }
        if (variant == "checkbox") {
            onAllNodes(checkboxes)[0].assertIsEnabled()
            onAllNodes(checkboxes)[1].performClick()
        } else if (variant == "free-chips") {
            onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("fin")
            onNodeWithText("Add choice").performClick()
            onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("qa")
        } else {
            onNodeWithText("Add choice").performClick()
            onNodeWithText("fin").performClick()
        }
        waitForIdle()
        assertEquals(added, data)
        assertEquals(1, changes)
        runOnIdle { globalDisable = false; localDisable = true }
        if (variant == "checkbox") {
            onAllNodes(checkboxes)[0].assertIsNotEnabled()
            onAllNodes(checkboxes)[2].assertIsNotEnabled()
        } else {
            onNodeWithText("Remove choice: ops").assertIsNotEnabled()
            onNodeWithText("Add choice").assertIsNotEnabled()
        }
        assertEquals(added, data)
        assertEquals(1, changes)
    }
}
