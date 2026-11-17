package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
class MixedTreeDeleteUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"value":{"type":["object","array"]}}}""",
    ).jsonObject)
    private val original = Json.parseToJsonElement("""{"value":{"child":{"name":"Ada"}}}""")

    @Test fun materialDeleteConfirmsAndRespectsReadOnly() = verify(false)
    @Test fun cupertinoDeleteConfirmsAndRespectsReadOnly() = verify(true)

    private fun verify(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(original)
        var readonly by mutableStateOf(false)
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema, uischema = ControlElement(scope = "#/properties/value"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                readonly = readonly,
                onChange = { data = it.data; changes++ },
            )
        }
        onNodeWithContentDescription("Delete").performClick()
        onNodeWithText("Delete this value?").assertExists()
        onNodeWithText("No").performClick()
        runOnIdle { assertEquals(original, data); assertEquals(0, changes) }

        onNodeWithContentDescription("Delete").performClick()
        runOnIdle { readonly = true }
        onNodeWithText("Delete this value?").assertDoesNotExist()
        onNodeWithContentDescription("Delete").assertDoesNotExist()
        runOnIdle { assertEquals(original, data); assertEquals(0, changes) }

        runOnIdle { readonly = false }
        onNodeWithContentDescription("Delete").performClick()
        onNodeWithText("Yes").performClick()
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{"value":{}}"""), data)
            assertEquals(1, changes)
        }
    }
}
