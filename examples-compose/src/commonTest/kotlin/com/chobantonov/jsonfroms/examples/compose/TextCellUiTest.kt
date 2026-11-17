package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoCells
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialCells
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class TextCellUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement("""{
      "type":"array","items":{"type":"object","properties":{
        "text":{"type":"string","maxLength":4}
      }}
    }""").jsonObject)

    @Test fun materialPlainCellPreservesInvalidImportAndRestrictsEdits() = plain(false)
    @Test fun cupertinoPlainCellPreservesInvalidImportAndRestrictsEdits() = plain(true)
    @Test fun materialMultilineCellStoresNewline() = multiline(false)
    @Test fun cupertinoMultilineCellStoresNewline() = multiline(true)

    private fun plain(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""[{"text":null}]"""))
        var locale by mutableStateOf("en")
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#", options = mapOf("table" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                cells = if (cupertino) cupertinoCells else materialCells,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { data = it.data; writes++ })
        }
        onNodeWithTag(if (cupertino) "cupertino-text-cell" else "material-text-cell").assertExists()
        onNodeWithText("Invalid value: null").assertDoesNotExist()
        assertTrue(onAllNodes(hasSetTextAction()).onFirst().fetchSemanticsNode()
            .config[SemanticsProperties.Error].contains("Invalid value: null"))
        onNodeWithContentDescription("Text contains errors", substring = true).assertExists()
        runOnIdle { assertEquals(0, writes); locale = "bg-BG" }
        onNodeWithText("Невалидна стойност: null").assertDoesNotExist()
        runOnIdle { assertEquals(0, writes) }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("okay")
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""[{"text":"okay"}]"""), data)
            assertEquals(1, writes)
        }
        onNodeWithText("Невалидна стойност: null").assertDoesNotExist()
        onNodeWithContentDescription("Text contains errors", substring = true).assertDoesNotExist()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("longer")
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""[{"text":"okay"}]"""), data)
            assertEquals(1, writes)
            data = Json.parseToJsonElement("""[{"text":"host"}]""")
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("new")
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""[{"text":"new"}]"""), data)
            assertEquals(2, writes)
        }
    }

    private fun multiline(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""[{"text":"old"}]"""))
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#", options = mapOf(
                    "table" to JsonPrimitive(true),
                    "cells" to Json.parseToJsonElement("""{"text":{"multi":true}}"""),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                cells = if (cupertino) cupertinoCells else materialCells,
                onChange = { data = it.data; writes++ })
        }
        onNodeWithTag(if (cupertino) "cupertino-text-cell" else "material-text-cell").assertExists()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("a\nb")
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""[{"text":"a\nb"}]"""), data)
            assertEquals(1, writes)
        }
    }
}
