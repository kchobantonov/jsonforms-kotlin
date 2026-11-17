package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.validation.ErrorObject
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
class TupleTailPaginationUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"array","items":[{"type":"string","title":"Code"}],"additionalItems":{"type":"string"}}"""
    ).jsonObject)
    private val initial = Json.parseToJsonElement(
        """["fixed","v0","v1","v2","v3","v4","v5","v6"]"""
    )

    @Test fun materialForbiddenTailCanBeRemovedFromLaterPage() = forbiddenTail(false)
    @Test fun cupertinoForbiddenTailCanBeRemovedFromLaterPage() = forbiddenTail(true)

    private fun forbiddenTail(cupertino: Boolean) = runComposeUiTest {
        val forbiddenSchema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","prefixItems":[{"type":"string","title":"Code"}],"items":false}"""
        ).jsonObject)
        var data by mutableStateOf(initial)
        setContent {
            JsonForms(
                data = data, schema = forbiddenSchema, uischema = ControlElement("#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data },
            )
        }
        onNodeWithText("Add item").assertDoesNotExist()
        onNodeWithText("Delete Item 8").assertDoesNotExist()
        onNodeWithContentDescription("Next page").performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithText("Code").assertExists()
        onNodeWithText("Delete Item 8").performClick()
        assertEquals(7, (data as JsonArray).size)
        onNodeWithText("Delete Item 8").assertDoesNotExist()
    }

    @Test fun materialPagesOnlyTailAndUsesAbsoluteIndices() = pagesTail(false)
    @Test fun cupertinoPagesOnlyTailAndUsesAbsoluteIndices() = pagesTail(true)

    @Test fun materialSizeChangeRetainsFirstVisibleTailItem() = changesSize(false)
    @Test fun cupertinoSizeChangeRetainsFirstVisibleTailItem() = changesSize(true)

    private fun changesSize(cupertino: Boolean) = runComposeUiTest {
        val options = mapOf("additionalItems" to Json.parseToJsonElement(
            """{"pagination":{"pageSize":2,"pageSizeOptions":[2,3]}}"""
        ))
        setContent {
            JsonForms(
                data = initial, schema = schema, uischema = ControlElement("#", options = options),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
            )
        }
        onNodeWithText("Delete Item 3").assertExists()
        onNodeWithText("Delete Item 4").assertDoesNotExist()
        onNodeWithContentDescription("Next page").performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithText("Delete Item 4").assertExists()
        onNodeWithContentDescription("Page size").performSemanticsAction(SemanticsActions.OnClick)
        onAllNodes(hasClickAction() and hasText("3")).onFirst().performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithText("Delete Item 4").assertExists()
        onNodeWithText("Delete Item 5").assertDoesNotExist()
        onNodeWithText("Code").assertExists()
    }

    private fun pagesTail(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(initial)
        setContent {
            JsonForms(
                data = data, schema = schema, uischema = ControlElement("#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data },
            )
        }
        onNodeWithText("Code").assertExists()
        onNodeWithText("Delete Item 6").assertExists()
        onNodeWithText("Delete Item 7").assertDoesNotExist()
        waitForIdle()
        onNodeWithContentDescription("Next page").performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithText("Code").assertExists()
        onNodeWithText("Delete Item 7").assertExists()
        onAllNodes(hasSetTextAction() and hasText("v5")).onFirst().performTextReplacement("edited")
        waitForIdle()
        assertEquals(JsonPrimitive("edited"), (data as JsonArray)[6])

        onNodeWithText("Add item").performClick()
        onNodeWithText("Delete Item 9").assertExists()
        onNodeWithText("Delete Item 2").assertDoesNotExist()
        onNodeWithText("Delete Item 9").performClick()
        onNodeWithText("Delete Item 8").performClick()
        onNodeWithText("Delete Item 7").performClick()
        onNodeWithText("Delete Item 2").assertExists()
        onNodeWithContentDescription("Next page").assertDoesNotExist()
        assertEquals(JsonPrimitive("fixed"), (data as JsonArray)[0])
    }

    @Test fun materialLocalSettingsAndErrorPageLink() = settingsAndError(false)
    @Test fun cupertinoLocalSettingsAndErrorPageLink() = settingsAndError(true)

    private fun settingsAndError(cupertino: Boolean) = runComposeUiTest {
        var pagination by mutableStateOf<JsonElement>(JsonPrimitive(false))
        setContent {
            JsonForms(
                data = initial, schema = schema,
                uischema = ControlElement("#", options = mapOf(
                    "additionalItems" to Json.parseToJsonElement("""{"pagination":$pagination}"""),
                )),
                config = mapOf("jsonformsExtended" to mapOf(
                    "additionalItems" to mapOf("pagination" to true),
                )),
                additionalErrors = listOf(ErrorObject(instancePath = "/7", message = "server")),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
            )
        }
        onNodeWithText("Delete Item 8").assertExists()
        onNodeWithContentDescription("Next page").assertDoesNotExist()
        runOnIdle { pagination = Json.parseToJsonElement("""{"pageSize":2}""") }
        onNodeWithText("Delete Item 8").assertDoesNotExist()
        onNodeWithText("Delete Item 3").assertExists()
        onNodeWithText("Page 4: 1 error").performClick()
        onNodeWithText("Delete Item 8").assertExists()
        onNodeWithText("Code").assertExists()
    }
}
