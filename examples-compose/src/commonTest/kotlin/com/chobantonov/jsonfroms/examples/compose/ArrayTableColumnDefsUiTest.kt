package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class ArrayTableColumnDefsUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"},"email":{"type":"string"},"hidden":{"type":"string"}}}}}}"""
    ).jsonObject)
    private val initial = Json.parseToJsonElement(
        """{"rows":[{"name":"Ada","email":"ada@example.com","hidden":"kept"}]}"""
    )

    @Test fun materialPortableDefinitionsOrderAndEditOriginalField() = portable(false)
    @Test fun cupertinoPortableDefinitionsOrderAndEditOriginalField() = portable(true)

    private fun portable(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(initial)
        val options = mapOf<String, JsonElement>(
            "table" to JsonPrimitive(true),
            "columnDefs" to Json.parseToJsonElement(
                """[{"field":"email","width":400,"minWidth":160,"maxWidth":190},{"field":"unknown"},{"field":"name"},{"field":"email"}]"""
            ),
        )
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ControlElement("#/properties/rows", options = options),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data },
            )
        }
        val email = onNodeWithText("Email").fetchSemanticsNode().boundsInRoot
        val name = onNodeWithText("Name").fetchSemanticsNode().boundsInRoot
        assertTrue(email.left < name.left)
        assertTrue(email.width < name.width / 2)
        onNodeWithText("Hidden").assertDoesNotExist()
        assertEquals(2, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onAllNodes(hasSetTextAction() and hasText("ada@example.com")).onFirst()
            .performTextReplacement("new@example.com")
        waitForIdle()
        assertEquals(Json.parseToJsonElement(
            """{"rows":[{"name":"Ada","email":"new@example.com","hidden":"kept"}]}"""
        ), data)
    }

    @Test fun materialGridNativeListReplacesPortableList() = native(false)
    @Test fun cupertinoGridNativeListReplacesPortableList() = native(true)

    private fun native(cupertino: Boolean) = runComposeUiTest {
        val options = mapOf<String, JsonElement>(
            "variant" to JsonPrimitive("ag-grid"),
            "columnDefs" to Json.parseToJsonElement("""[{"field":"name"}]"""),
            "agGridOptions" to Json.parseToJsonElement(
                """{"columnDefs":[{"field":"email","headerName":"Mail"}]}"""
            ),
        )
        setContent {
            JsonForms(
                data = initial, schema = schema,
                uischema = ControlElement("#/properties/rows", options = options),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
            )
        }
        onNodeWithText("Mail").assertExists()
        onNodeWithText("Name").assertDoesNotExist()
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
    }

    @Test fun emptyPortableListKeepsActionsAndData() = runComposeUiTest {
        setContent {
            JsonForms(
                data = initial, schema = schema,
                uischema = ControlElement("#/properties/rows", options = mapOf(
                    "table" to JsonPrimitive(true),
                    "columnDefs" to Json.parseToJsonElement("[]"),
                )),
                renderers = materialRenderers,
            )
        }
        assertEquals(0, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onNodeWithContentDescription("Delete item").assertExists()
    }

    @Test fun wholeRowScopeEditsOriginalItem() = runComposeUiTest {
        var data by mutableStateOf(initial)
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ControlElement("#/properties/rows", options = mapOf(
                    "table" to JsonPrimitive(true),
                    "columnDefs" to Json.parseToJsonElement(
                        """[{"field":"summary","scope":"#","headerName":"Summary"}]"""
                    ),
                )),
                renderers = materialRenderers,
                onChange = { data = it.data },
            )
        }
        onNodeWithText("Summary").assertExists()
        onAllNodes(hasSetTextAction() and hasText("Ada")).onFirst()
            .performTextReplacement("Grace")
        waitForIdle()
        assertEquals(Json.parseToJsonElement(
            """{"rows":[{"name":"Grace","email":"ada@example.com","hidden":"kept"}]}"""
        ), data)
    }
}
