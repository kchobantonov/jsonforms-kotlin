package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.ListWithDetailElement
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class ArrayActionPolicyUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"}}}}}}"""
    ).jsonObject)
    private val first = Json.parseToJsonElement("""{"rows":[{"name":"Ada"},{"name":"Bob"}]}""")
    private val swapped = Json.parseToJsonElement("""{"rows":[{"name":"Bob"},{"name":"Ada"}]}""")

    @Test fun materialTableHonorsLocalAndGlobalActionPolicy() = policy(false, 0)
    @Test fun cupertinoTableHonorsLocalAndGlobalActionPolicy() = policy(true, 0)
    @Test fun materialExpandableArrayHonorsLocalAndGlobalActionPolicy() = policy(false, 1)
    @Test fun cupertinoExpandableArrayHonorsLocalAndGlobalActionPolicy() = policy(true, 1)
    @Test fun materialListDetailHonorsLocalAndGlobalActionPolicy() = policy(false, 2)
    @Test fun cupertinoListDetailHonorsLocalAndGlobalActionPolicy() = policy(true, 2)
    @Test fun materialTableRevokesDeleteDuringConfirmation() = revoke(false, 0)
    @Test fun cupertinoTableRevokesDeleteDuringConfirmation() = revoke(true, 0)
    @Test fun materialExpandableArrayRevokesDeleteDuringConfirmation() = revoke(false, 1)
    @Test fun cupertinoExpandableArrayRevokesDeleteDuringConfirmation() = revoke(true, 1)
    @Test fun materialListDetailRevokesDeleteDuringConfirmation() = revoke(false, 2)
    @Test fun cupertinoListDetailRevokesDeleteDuringConfirmation() = revoke(true, 2)

    private fun policy(cupertino: Boolean, presentation: Int) = runComposeUiTest {
        var data by mutableStateOf(first)
        var localDisable by mutableStateOf<Boolean?>(null)
        var globalDisable by mutableStateOf(true)
        var changes = 0
        setContent {
            val options: Map<String, JsonElement> = mapOf(
                "showSortButtons" to JsonPrimitive(true),
                "confirmation" to Json.parseToJsonElement("""{"delete":"never"}"""),
            ) +
                (localDisable?.let { mapOf(
                    "disableAdd" to JsonPrimitive(it),
                    "disableRemove" to JsonPrimitive(it),
                ) } ?: emptyMap())
            val ui: UiSchemaElement = when (presentation) {
                0 -> ControlElement("#/properties/rows", options = options + ("table" to JsonPrimitive(true)))
                1 -> ControlElement("#/properties/rows", options = options + (
                    "detail" to Json.parseToJsonElement("""{"type":"Control","scope":"#/properties/name"}""")
                ))
                else -> ListWithDetailElement("#/properties/rows", options = options + (
                    "elementLabelProp" to JsonPrimitive("name")
                ))
            }
            JsonForms(
                data = data, schema = schema, uischema = ui,
                config = mapOf("disableAdd" to globalDisable, "disableRemove" to globalDisable),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ },
            )
        }
        onNodeWithContentDescription("Add item").assertIsNotEnabled()
        onAllNodes(hasContentDescription("Delete item")).onFirst().assertIsNotEnabled()
        onAllNodes(hasContentDescription("Move up"))[1].assertIsEnabled()
        onAllNodes(hasContentDescription("Move up"))[1].performClick()
        waitForIdle()
        assertEquals(swapped, data)
        assertEquals(1, changes)

        runOnIdle { localDisable = false }
        onNodeWithContentDescription("Add item").assertIsEnabled()
        onAllNodes(hasContentDescription("Delete item")).onFirst().assertIsEnabled()
        onNodeWithContentDescription("Add item").performClick()
        waitForIdle()
        assertEquals(3, (data.jsonObject["rows"] as JsonArray).size)
        onAllNodes(hasContentDescription("Delete item")).onFirst().performClick()
        waitForIdle()
        assertEquals(2, (data.jsonObject["rows"] as JsonArray).size)
        assertEquals(3, changes)
        val afterActions = data
        runOnIdle { globalDisable = false; localDisable = true }
        onNodeWithContentDescription("Add item").assertIsNotEnabled()
        onAllNodes(hasContentDescription("Delete item")).onFirst().assertIsNotEnabled()
        assertEquals(afterActions, data)
        assertEquals(3, changes)
    }

    private fun revoke(cupertino: Boolean, presentation: Int) = runComposeUiTest {
        var disableRemove by mutableStateOf(false)
        var data by mutableStateOf(first)
        var changes = 0
        setContent {
            val options: Map<String, JsonElement> = mapOf(
                "confirmation" to Json.parseToJsonElement("""{"delete":"always"}"""),
            )
            val ui: UiSchemaElement = when (presentation) {
                0 -> ControlElement("#/properties/rows", options = options + ("table" to JsonPrimitive(true)))
                1 -> ControlElement("#/properties/rows", options = options + (
                    "detail" to Json.parseToJsonElement("""{"type":"Control","scope":"#/properties/name"}""")
                ))
                else -> ListWithDetailElement("#/properties/rows", options = options + (
                    "elementLabelProp" to JsonPrimitive("name")
                ))
            }
            JsonForms(
                data = data, schema = schema, uischema = ui,
                config = mapOf("disableRemove" to disableRemove),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ },
            )
        }
        onAllNodes(hasContentDescription("Delete item")).onFirst().performClick()
        onNodeWithText("Delete this value?").assertExists()
        runOnIdle { disableRemove = true }
        onNodeWithText("Delete this value?").assertDoesNotExist()
        onAllNodes(hasContentDescription("Delete item")).onFirst().assertIsNotEnabled()
        assertEquals(first, data)
        assertEquals(0, changes)
    }
}
