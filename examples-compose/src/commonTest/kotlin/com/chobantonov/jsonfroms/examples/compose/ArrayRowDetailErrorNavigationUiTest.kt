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
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.ValidationMode
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class ArrayRowDetailErrorNavigationUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"},"note":{"type":"string","minLength":2}}}}}}"""
    ).jsonObject)
    private val initial = Json.parseToJsonElement(
        """{"rows":[{"name":"Ada","note":"ok"},{"name":"Bob","note":"fine"},{"name":"Cyd","note":"x"}]}"""
    )
    private fun ui(presentation: String) = ControlElement("#/properties/rows", options = mapOf(
        "table" to JsonPrimitive(true),
        "columnDefs" to Json.parseToJsonElement("""[{"field":"name"}]"""),
        "pagination" to Json.parseToJsonElement("""{"pageSize":1}"""),
        "rowDetail" to Json.parseToJsonElement(
            """{"presentation":"$presentation","collapsed":true,"detail":{"type":"Control","scope":"#/properties/note"}}"""
        ),
    ))

    @Test fun materialFirstErrorOpensOffPageRowDetail() = dialog(false)
    @Test fun cupertinoFirstErrorOpensOffPageRowDetail() = dialog(true)

    private fun dialog(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(initial)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = ui("dialog"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Go to first error").performClick()
        onNodeWithText("Item 3 details").assertExists()
        onNodeWithText("Cancel").performClick()
        onNodeWithContentDescription("Item 3: 1 error").assertExists()
        onNodeWithText("Edit details").performClick()
        onAllNodes(hasSetTextAction() and hasText("x")).onFirst().performTextReplacement("fixed")
        onNodeWithText("Apply").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement(
            """{"rows":[{"name":"Ada","note":"ok"},{"name":"Bob","note":"fine"},{"name":"Cyd","note":"fixed"}]}"""
        ), data)
        assertEquals(1, changes)
        onNodeWithText("Go to first error").assertDoesNotExist()
        onNodeWithContentDescription("Item 3: 1 error").assertDoesNotExist()
    }

    @Test fun firstErrorRevealsCollapsedPanelAndSelectsSourceRow() = runComposeUiTest {
        setContent {
            JsonForms(data = initial, schema = schema, uischema = ui("panel"),
                renderers = materialRenderers)
        }
        onNodeWithText("Show details").assertExists()
        onNodeWithText("Go to first error").performClick()
        onNodeWithText("Hide details").assertExists()
        onNodeWithContentDescription("Item 3: 1 error").assertExists()
        onNodeWithText("x").assertExists()
    }

    @Test fun hiddenSchemaErrorsDoNotExposeFirstErrorNavigation() = runComposeUiTest {
        setContent {
            JsonForms(data = initial, schema = schema, uischema = ui("dialog"),
                validationMode = ValidationMode.ValidateAndHide,
                renderers = materialRenderers)
        }
        onNodeWithText("Go to first error").assertDoesNotExist()
        onNodeWithText("Page 3: 1 error").assertDoesNotExist()
    }
}
