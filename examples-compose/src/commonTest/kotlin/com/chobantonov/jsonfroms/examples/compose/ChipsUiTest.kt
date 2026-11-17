package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ChipsUiTest {
    private val ui = ControlElement("#/properties/tags", options = mapOf("variant" to JsonPrimitive("chips")))

    @Test fun materialFreeDraftAndPositionalDuplicateRemoval() = free(false)
    @Test fun cupertinoFreeDraftAndPositionalDuplicateRemoval() = free(true)
    @Test fun materialFiniteChoicesStoreValues() = finite(false)
    @Test fun cupertinoFiniteChoicesStoreValues() = finite(true)
    @Test fun materialFreeChipsRespectRestrictedCounts() = limits(false)
    @Test fun cupertinoFreeChipsRespectRestrictedCounts() = limits(true)
    @Test fun materialReadonlyFreeChipsDoNotMutate() = readonly(false)
    @Test fun cupertinoReadonlyFreeChipsDoNotMutate() = readonly(true)
    @Test fun materialFiniteChipSearchUsesLabels() = search(false)
    @Test fun cupertinoFiniteChipSearchUsesLabels() = search(true)
    @Test fun materialChipsSearchFollowsLiveLocale() = localeSwitch(false, "chips")
    @Test fun cupertinoChipsSearchFollowsLiveLocale() = localeSwitch(true, "chips")
    @Test fun materialMultiSelectSearchFollowsLiveLocale() = localeSwitch(false, "multi-select")
    @Test fun cupertinoMultiSelectSearchFollowsLiveLocale() = localeSwitch(true, "multi-select")
    @Test fun materialFiniteChipsMarkUnknownImportedValue() = unknownFinite(false)
    @Test fun cupertinoFiniteChipsMarkUnknownImportedValue() = unknownFinite(true)

    private fun unknownFinite(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"tags":{"type":"array","uniqueItems":true,
              "items":{"type":"string","enum":["ops","fin"]}}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"tags":[42]}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Current value outside choices: 42").assertExists()
        assertEquals(0, changes)
        onNodeWithText("Remove choice: 42").performClick()
        assertEquals(Json.parseToJsonElement("""{"tags":[]}"""), data)
        assertEquals(1, changes)
        onNodeWithText("Current value outside choices: 42").assertDoesNotExist()
    }

    private fun free(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"tags":{"type":"array","items":{"type":"string","minLength":2}}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"tags":["a","b","a"]}"""))
        setContent {
            JsonForms(data = data, schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        onNodeWithContentDescription("Remove choice: a, 1").assertExists()
        onNodeWithContentDescription("Remove choice: a, 3").performClick()
        assertEquals(Json.parseToJsonElement("""{"tags":["a","b"]}"""), data)
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("a")
        assertEquals(Json.parseToJsonElement("""{"tags":["a","b"]}"""), data)
        onNodeWithText("Add choice").performClick()
        assertEquals(Json.parseToJsonElement("""{"tags":["a","b","a"]}"""), data)
    }

    private fun finite(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"tags":{"type":"array","uniqueItems":true,"items":{"oneOf":[{"const":"a","title":"Team"},{"const":"b","title":"Team"}]}}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"tags":[]}"""))
        setContent {
            JsonForms(data = data, schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        onNodeWithText("Add choice").performClick()
        onNodeWithContentDescription("Team (b)").performClick()
        assertEquals(Json.parseToJsonElement("""{"tags":["b"]}"""), data)
        onNodeWithText("Add choice").performClick()
        onNodeWithContentDescription("Team (a)").performClick()
        assertEquals(Json.parseToJsonElement("""{"tags":["b","a"]}"""), data)
    }

    private fun limits(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"tags":{"type":"array","minItems":1,"maxItems":2,"items":{"type":"string"}}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"tags":["a"]}"""))
        setContent {
            JsonForms(data = data, schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        onNodeWithText("Remove choice: a").assertIsNotEnabled()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("b")
        onNodeWithText("Add choice").performClick()
        assertEquals(Json.parseToJsonElement("""{"tags":["a","b"]}"""), data)
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("c")
        onNodeWithText("Add choice").assertIsNotEnabled()
        onNodeWithText("Remove choice: a").performClick()
        assertEquals(Json.parseToJsonElement("""{"tags":["b"]}"""), data)
    }

    private fun readonly(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"tags":{"type":"array","items":{"type":"string"}}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"tags":["a"]}"""))
        setContent {
            JsonForms(data = data, schema = schema, uischema = ui, readonly = true,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        onNodeWithText("Remove choice: a").assertIsNotEnabled()
        onNodeWithText("Add choice").assertIsNotEnabled()
        onAllNodes(hasSetTextAction()).assertCountEquals(0)
        assertEquals(Json.parseToJsonElement("""{"tags":["a"]}"""), data)
    }

    private fun search(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"tags":{"type":"array","uniqueItems":true,"items":{"oneOf":[{"const":"ops","title":"Operations"},{"const":"fin","title":"Finance"}]}}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"tags":[]}"""))
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/tags", options = mapOf(
                    "variant" to JsonPrimitive("chips"), "autocomplete" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        onNodeWithText("Add choice").performClick()
        onAllNodes(hasSetTextAction()).assertCountEquals(1)
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("ops")
        onNodeWithText("No matches").assertExists()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("oper")
        onNodeWithText("Operations").performClick()
        assertEquals(Json.parseToJsonElement("""{"tags":["ops"]}"""), data)
    }

    private fun localeSwitch(cupertino: Boolean, variant: String) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"tags":{"type":"array","uniqueItems":true,"items":{"oneOf":[{"const":"ops","title":"Operations"},{"const":"fin","title":"Finance"}]}}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"tags":[]}"""))
        var locale by mutableStateOf("en")
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/tags", options = mapOf(
                    "variant" to JsonPrimitive(variant), "autocomplete" to JsonPrimitive(true))),
                i18n = JsonFormsI18nState(locale = locale, translate = { key, fallback, _ ->
                    if (locale == "bg" && key == "tags.ops") "Операции" else fallback
                }),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        onNodeWithText("Add choice").performClick()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("oper")
        onNodeWithText("Operations").assertExists()
        runOnIdle { locale = "bg" }
        onNodeWithText("Няма съвпадения").assertExists()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("опер")
        onNodeWithText("Операции").performClick()
        assertEquals(Json.parseToJsonElement("""{"tags":["ops"]}"""), data)
        onNodeWithText("Операции").assertExists()
    }
}
