package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
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
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class AdditionalPropertiesPaginationUiTest {
    @Test fun materialHostReplacementCancelsPagedRenameDraft() = hostReplacementCancelsRename(false)
    @Test fun cupertinoHostReplacementCancelsPagedRenameDraft() = hostReplacementCancelsRename(true)

    private fun hostReplacementCancelsRename(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(initial)
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = ControlElement("#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; writes++ })
        }
        onNodeWithContentDescription("Next page").performClick()
        onNodeWithText("p5").assertExists()
        onAllNodes(hasContentDescription("Rename button")).onFirst().performClick()
        onAllNodes(hasSetTextAction() and hasText("p5")).onFirst().performTextReplacement("renamed")
        onNodeWithContentDescription("Previous page").assertIsNotEnabled()
        runOnIdle {
            data = Json.parseToJsonElement(
                """{"fixed":"declared","p0":"v0","p1":"v1","p2":"v2","p3":"v3","p4":"v4","p5":"server","p6":"v6"}"""
            )
        }
        onNodeWithContentDescription("Save button").assertDoesNotExist()
        onNodeWithContentDescription("Previous page").assertIsEnabled()
        onAllNodes(hasSetTextAction() and hasText("server")).onFirst().assertExists()
        assertEquals(null, data.jsonObject["renamed"])
        assertEquals(0, writes)
    }

    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"fixed":{"type":"string"}},"additionalProperties":{"type":"string"}}"""
    ).jsonObject)
    private val initial = Json.parseToJsonElement(
        """{"fixed":"declared","p0":"v0","p1":"v1","p2":"v2","p3":"v3","p4":"v4","p5":"v5","p6":"v6"}"""
    )

    @Test fun materialPagesOnlyDynamicFieldsAndEditsOriginalKey() = scenario(false)
    @Test fun cupertinoPagesOnlyDynamicFieldsAndEditsOriginalKey() = scenario(true)

    @Test fun materialOffPageDuplicateNameIsRejected() = offPageDuplicate(false)
    @Test fun cupertinoOffPageDuplicateNameIsRejected() = offPageDuplicate(true)

    private fun offPageDuplicate(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(initial)
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = ControlElement("#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; writes++ })
        }
        onNodeWithText("p6").assertDoesNotExist()
        onAllNodes(hasSetTextAction() and hasText("")).onFirst().performTextReplacement("p6")
        onNodeWithContentDescription("Add button").performClick()
        onNodeWithText("A property with this name already exists").assertExists()
        assertEquals(JsonPrimitive("v6"), data.jsonObject["p6"])
        assertEquals(0, writes)
    }

    @Test fun materialOffPageHostErrorRemainsInSummary() = offPageHostError(false)
    @Test fun cupertinoOffPageHostErrorRemainsInSummary() = offPageHostError(true)

    private fun offPageHostError(cupertino: Boolean) = runComposeUiTest {
        setContent {
            JsonForms(data = initial, schema = schema, uischema = ControlElement("#"),
                additionalErrors = listOf(ErrorObject(instancePath = "/p6", message = "server")),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onNodeWithText("p6").assertDoesNotExist()
        onNodeWithText("Page 2: 1 error").assertExists()
        onNodeWithText("Page 2: 1 error").performClick()
        onNodeWithText("p6").assertExists()
    }

    @Test fun materialAddRevealsNewDynamicKeyAndDeleteClampsPage() = addAndDelete(false)
    @Test fun cupertinoAddRevealsNewDynamicKeyAndDeleteClampsPage() = addAndDelete(true)

    @Test fun materialRenameKeepsValueAndFocusOnItsPage() = renameFocus(false)
    @Test fun cupertinoRenameKeepsValueAndFocusOnItsPage() = renameFocus(true)

    private fun renameFocus(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(initial)
        setContent {
            JsonForms(
                data = data, schema = schema, uischema = ControlElement("#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data },
            )
        }
        onNodeWithContentDescription("Next page").performClick()
        onNodeWithText("p5").assertExists()
        onAllNodes(hasContentDescription("Rename button")).onFirst().performClick()
        onAllNodes(hasSetTextAction() and hasText("p5")).onFirst().performTextReplacement("renamed")
        onNodeWithContentDescription("Save button").performClick()
        waitForIdle()
        onNodeWithText("p5").assertDoesNotExist()
        onNodeWithText("renamed").assertExists()
        onNodeWithText("p0").assertDoesNotExist()
        assertEquals(JsonPrimitive("v5"), data.jsonObject["renamed"])
        onAllNodes(hasSetTextAction() and hasText("v5")).onFirst().assertIsFocused()
        onAllNodes(hasSetTextAction() and hasText("v5")).onFirst().performTextReplacement("after")
        waitForIdle()
        assertEquals(JsonPrimitive("after"), data.jsonObject["renamed"])
    }

    private fun addAndDelete(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(initial)
        setContent {
            JsonForms(
                data = data, schema = schema, uischema = ControlElement("#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data },
            )
        }
        onAllNodes(hasSetTextAction() and hasText("")).onFirst().performTextReplacement("p7")
        onNodeWithContentDescription("Add button").performClick()
        onNodeWithText("p7").assertExists()
        onNodeWithText("p0").assertDoesNotExist()
        assertEquals(JsonPrimitive(""), data.jsonObject["p7"])

        // Remove all dynamic entries on the last page; the pager returns to page one.
        repeat(3) {
            onAllNodes(hasContentDescription("Delete button")).onLast().performClick()
        }
        onNodeWithText("p0").assertExists()
        onNodeWithContentDescription("Next page").assertDoesNotExist()
        assertEquals(6, data.jsonObject.size) // declared field plus five dynamic keys
    }

    private fun scenario(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(initial)
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema, uischema = ControlElement("#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ },
            )
        }
        onNodeWithText("Fixed").assertExists()
        onNodeWithText("p0").assertExists()
        onNodeWithText("p5").assertDoesNotExist()
        onNodeWithContentDescription("Next page").performClick()
        onNodeWithText("Fixed").assertExists()
        onNodeWithText("p5").assertExists()
        onAllNodes(hasSetTextAction() and hasText("v5")).onFirst().performTextReplacement("updated")
        waitForIdle()
        assertEquals(JsonPrimitive("updated"), data.jsonObject["p5"])
        assertEquals(JsonPrimitive("declared"), data.jsonObject["fixed"])
        assertEquals(1, changes)
    }

    @Test fun nestedLocalFalseOverridesScopedTrueAndLocalObjectReenables() = runComposeUiTest {
        var pagination by mutableStateOf<kotlinx.serialization.json.JsonElement>(JsonPrimitive(false))
        setContent {
            JsonForms(
                data = initial, schema = schema,
                uischema = ControlElement("#", options = mapOf(
                    "additionalProperties" to Json.parseToJsonElement(
                        """{"pagination":${pagination}}"""
                    ),
                )),
                config = mapOf("jsonformsExtended" to mapOf(
                    "additionalProperties" to mapOf("pagination" to true),
                )),
                renderers = materialRenderers,
            )
        }
        onNodeWithText("p6").assertExists()
        onNodeWithContentDescription("Next page").assertDoesNotExist()
        runOnIdle { pagination = Json.parseToJsonElement("""{"pageSize":2}""") }
        onNodeWithText("p6").assertDoesNotExist()
        onNodeWithContentDescription("Next page").assertExists()
    }

    @Test fun errorPageLinkAndRenameDraftBlockNavigation() = runComposeUiTest {
        setContent {
            JsonForms(
                data = initial, schema = schema, uischema = ControlElement("#"),
                additionalErrors = listOf(ErrorObject(instancePath = "/p6", message = "server")),
                renderers = materialRenderers,
            )
        }
        onAllNodes(hasContentDescription("Rename button")).onFirst().performClick()
        onNodeWithText("New property name").assertExists()
        onNodeWithContentDescription("Next page").assertIsNotEnabled()
        onNodeWithContentDescription("Cancel button").performClick()
        waitForIdle()
        onNodeWithContentDescription("Next page").assertIsEnabled()
        onNodeWithText("Page 2: 1 error").performClick()
        onNodeWithText("p6").assertExists()
    }
}
