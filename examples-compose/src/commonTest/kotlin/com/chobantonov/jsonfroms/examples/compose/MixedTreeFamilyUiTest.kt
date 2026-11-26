package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class MixedTreeFamilyUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"value":{"type":["object","array"]}}}""",
    ).jsonObject)
    private val original = Json.parseToJsonElement("""{"value":{"child":{"name":"Ada"},"other":{"name":"Bob"}}}""")

    @Test fun materialTreeSearchAndRenameKeepDataContract() = verify(false)
    @Test fun cupertinoTreeSearchAndRenameKeepDataContract() = verify(true)

    private fun verify(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(original)
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement(scope = "#/properties/value"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; writes++ })
        }
        onNodeWithTag("mixed-tree-row-value.other").assertExists()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("child")
        onNodeWithTag("mixed-tree-row-value.other").assertDoesNotExist()
        runOnIdle { assertEquals(original, data); assertEquals(0, writes) }
        onNodeWithContentDescription("Clear search").performClick()
        onNodeWithTag("mixed-tree-row-value.other").assertExists()
        onAllNodesWithContentDescription("Rename").onFirst().performClick()
        onAllNodes(hasSetTextAction()).get(1).performTextReplacement("renamed")
        onNodeWithContentDescription("Confirm").performClick()
        runOnIdle {
            assertEquals(Json.parseToJsonElement(
                """{"value":{"renamed":{"name":"Ada"},"other":{"name":"Bob"}}}"""), data)
            assertEquals(1, writes)
        }
    }
}
