package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.mappers.groupContainsData
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.util.getDetailUiSchema
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class GroupArrayItemContextUiTest {
    @Test fun materialGroupResolvesCurrentItemAndNestedRootScope() = scenario(false)
    @Test fun cupertinoGroupResolvesCurrentItemAndNestedRootScope() = scenario(true)

    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"}}}}}}"""
    ).jsonObject)
    private val data = Json.parseToJsonElement(
        """{"rows":[{"name":" "},{"name":"Ada"}]}"""
    )
    private val detail = Json.parseToJsonElement(
        """{"type":"Group","label":"Item details","options":{"collapsible":true,"collapsed":true,"showDataIndicator":true,"showValidationIndicator":true,"showValidationIndicatorCount":true},"elements":[{"type":"Control","scope":"#"}]}"""
    ).jsonObject
    private val ui = ControlElement(
        scope = "#/properties/rows",
        options = mapOf("detail" to detail, "initCollapsed" to JsonPrimitive(false)),
    )

    private fun scenario(cupertino: Boolean) = runComposeUiTest {
        val parsedDetail = getDetailUiSchema(ui) as GroupElement
        assertTrue(parsedDetail.options.containsKey("showDataIndicator"))
        assertTrue(parsedDetail.options.containsKey("collapsed"))
        assertTrue(groupContainsData(parsedDetail, data, "rows.1"))
        setContent {
            JsonForms(data = data, schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                additionalErrors = listOf(ErrorObject(
                    instancePath = "/rows/1/name", message = "server",
                )))
        }
        onNodeWithText("Item details").assertExists()
        onNodeWithContentDescription("Contains data").assertDoesNotExist()
        val firstErrorIndicators = onAllNodes(hasContentDescription("1 error"))
            .fetchSemanticsNodes().size
        onNodeWithText("Ada").performClick()
        onNodeWithText("Item details").assertExists()
        onNodeWithContentDescription("Contains data").assertExists()
        val secondErrorIndicators = onAllNodes(hasContentDescription("1 error"))
            .fetchSemanticsNodes().size
        assertEquals(firstErrorIndicators + 1, secondErrorIndicators)
        onNodeWithText("Item 1").performClick()
        onNodeWithContentDescription("Contains data").assertDoesNotExist()
        assertEquals(firstErrorIndicators, onAllNodes(hasContentDescription("1 error"))
            .fetchSemanticsNodes().size)
    }
}
