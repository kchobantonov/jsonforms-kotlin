package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.mappers.groupContainsData
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.util.getDetailUiSchema
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import com.chobantonov.jsonfroms.extended.renderers.cupertinoExtendedRenderers
import com.chobantonov.jsonfroms.extended.renderers.materialExtendedRenderers
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
    @Test fun materialRegionalControlsWriteSelectedArrayItem() = regionalDetail(false)
    @Test fun cupertinoRegionalControlsWriteSelectedArrayItem() = regionalDetail(true)
    @Test fun materialArrayItemRegionsDoNotEnterGroupIndicators() = regionalIndicators(false)
    @Test fun cupertinoArrayItemRegionsDoNotEnterGroupIndicators() = regionalIndicators(true)
    @Test fun materialArrayDetailHeaderNamesSelectedItem() = interpolatedItemHeader(false)
    @Test fun cupertinoArrayDetailHeaderNamesSelectedItem() = interpolatedItemHeader(true)

    private fun interpolatedItemHeader(cupertino: Boolean) = runComposeUiTest {
        val detail = Json.parseToJsonElement("""{
          "type":"Group","label":"Unused title",
          "options":{"collapsible":true,"collapsed":true},
          "header":[
            {"type":"Button","label":"Help"},
            {"type":"Label","text":"Record {person}","i18n":"rows.heading",
              "options":{"interpolate":true,"textParams":{"person":"{item.name}"}}}
          ],
          "elements":[{"type":"Control","scope":"#/properties/name"}]
        }""").jsonObject
        val ui = ControlElement(scope = "#/properties/rows", options = mapOf(
            "detail" to detail, "initCollapsed" to JsonPrimitive(false)))
        var current by mutableStateOf(Json.parseToJsonElement("""{
          "rows":[{"name":"Ava"},{"name":"Ada"}]
        }"""))
        var locale by mutableStateOf("en")
        var writes = 0
        setContent {
            JsonForms(data = current, schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf("jsonformsExtended" to mapOf("dynamicValues" to mapOf("enabled" to true))),
                i18n = JsonFormsI18nState(locale = locale, translate = { key, fallback, _ ->
                    if (locale == "bg" && key == "rows.heading.text") "Запис {person}" else fallback
                }),
                onChange = { writes++ })
        }
        onNodeWithContentDescription("Record Ava").assertExists()
        onNodeWithText("Ada").performClick()
        onNodeWithContentDescription("Record Ava").assertDoesNotExist()
        onNodeWithContentDescription("Record Ada").assertExists()
        runOnIdle {
            current = Json.parseToJsonElement("""{
              "rows":[{"name":"Ava"},{"name":"Mila"}]
            }""")
        }
        onNodeWithContentDescription("Record Mila").assertExists()
        runOnIdle { locale = "bg" }
        onNodeWithContentDescription("Запис Mila").assertExists()
        onNodeWithText("Ava").performClick()
        onNodeWithContentDescription("Запис Ava").assertExists()
        assertEquals(0, writes)
    }

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

    private fun regionalIndicators(cupertino: Boolean) = runComposeUiTest {
        val regionalSchema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"rows":{"type":"array","items":{
            "type":"object","required":["body"],"properties":{
              "name":{"type":"string"},
              "cover":{"type":"string","minLength":2},
              "header":{"type":"string","minLength":2},
              "body":{"type":"string","minLength":2},
              "footer":{"type":"string","minLength":2}}}}}
        }""").jsonObject)
        val regionalGroup = Json.parseToJsonElement("""{
          "type":"Group","options":{"collapsible":true,"collapsed":true,
            "showDataIndicator":true,"showValidationIndicator":true,
            "showValidationIndicatorCount":true},
          "cover":{"type":"Control","scope":"#/properties/cover"},
          "header":[{"type":"Label","text":"Heading"},
            {"type":"Control","scope":"#/properties/header"}],
          "elements":[{"type":"Control","scope":"#/properties/body"}],
          "footer":[{"type":"Control","scope":"#/properties/footer"}]
        }""").jsonObject
        val regionalUi = ControlElement(scope = "#/properties/rows", options = mapOf(
            "detail" to regionalGroup,
            "initCollapsed" to JsonPrimitive(false),
        ))
        var current by mutableStateOf(Json.parseToJsonElement("""{
          "rows":[{"name":"Ava","body":"ok"},
            {"name":"Ada","cover":"x","header":"x","footer":"x"}]
        }"""))
        var writes = 0
        setContent {
            JsonForms(data = current, schema = regionalSchema, uischema = regionalUi,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { writes++ })
        }
        onNodeWithText("Ada").performClick()
        fun heading() = onNodeWithText("Heading").fetchSemanticsNode().config
        assertEquals("1 error", heading()[SemanticsProperties.Error])
        onNodeWithContentDescription("Contains data").assertDoesNotExist()
        runOnIdle {
            current = Json.parseToJsonElement("""{
              "rows":[{"name":"Ava","body":"ok"},
                {"name":"Ada","cover":"x","header":"x","body":"ok","footer":"x"}]
            }""")
        }
        assertTrue(!heading().contains(SemanticsProperties.Error))
        onNodeWithContentDescription("Contains data").assertExists()
        runOnIdle {
            current = Json.parseToJsonElement("""{
              "rows":[{"name":"Ava","body":"ok"},
                {"name":"Ada","cover":"x","header":"x","body":"x","footer":"x"}]
            }""")
        }
        assertEquals("1 error", heading()[SemanticsProperties.Error])
        assertEquals(0, writes)
    }

    private fun regionalDetail(cupertino: Boolean) = runComposeUiTest {
        val regionalSchema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"rows":{"type":"array","items":{
            "type":"object","properties":{
              "name":{"type":"string"},"cover":{"type":"string"},
              "header":{"type":"string"},"body":{"type":"string"},
              "footer":{"type":"string"}}}}}
        }""").jsonObject)
        val regionalGroup = Json.parseToJsonElement("""{
          "type":"Group","label":"Item details",
          "cover":{"type":"Control","scope":"#/properties/cover"},
          "header":[{"type":"Control","scope":"#/properties/header"}],
          "elements":[{"type":"Control","scope":"#/properties/body"}],
          "footer":[{"type":"Control","scope":"#/properties/footer"}]
        }""").jsonObject
        val regionalUi = ControlElement(scope = "#/properties/rows", options = mapOf(
            "detail" to regionalGroup,
            "initCollapsed" to JsonPrimitive(false),
        ))
        var current by mutableStateOf(Json.parseToJsonElement("""{
          "rows":[
            {"name":"Ava","cover":"A cover","header":"A header","body":"A body","footer":"A footer"},
            {"name":"Ada","cover":"B cover","header":"B header","body":"B body","footer":"B footer"}
          ]
        }"""))
        var writes = 0
        setContent {
            JsonForms(data = current, schema = regionalSchema, uischema = regionalUi,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { current = it.data; writes++ })
        }
        onNodeWithText("Ada").performClick()
        assertEquals(4, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onAllNodes(hasSetTextAction())[0].performTextReplacement("New cover")
        onAllNodes(hasSetTextAction())[1].performTextReplacement("New header")
        onAllNodes(hasSetTextAction())[3].performTextReplacement("New footer")
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{
          "rows":[
            {"name":"Ava","cover":"A cover","header":"A header","body":"A body","footer":"A footer"},
            {"name":"Ada","cover":"New cover","header":"New header","body":"B body","footer":"New footer"}
          ]
        }"""), current)
        assertEquals(3, writes)
    }
}
