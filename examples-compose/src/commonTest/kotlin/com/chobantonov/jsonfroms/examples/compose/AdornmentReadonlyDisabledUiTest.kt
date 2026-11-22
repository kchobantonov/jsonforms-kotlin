package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.toUiSchemaElement
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class AdornmentReadonlyDisabledUiTest {
    @Test fun materialReadonlyKeepsInformationalAdornment() = informational(false, true)
    @Test fun cupertinoReadonlyKeepsInformationalAdornment() = informational(true, true)
    @Test fun materialDisabledKeepsInformationalAdornment() = informational(false, false)
    @Test fun cupertinoDisabledKeepsInformationalAdornment() = informational(true, false)

    private fun informational(cupertino: Boolean, readonly: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"string"},"lock":{"type":"boolean"}}
        }""").jsonObject)
        val ui = Json.parseToJsonElement("""{
          "type":"Control","scope":"#/properties/value",
          "options":{"startAdornment":{"kind":"text","text":"Unit","tooltip":"Unit explanation"}},
          "rule":{"effect":"DISABLE","condition":{"scope":"#/properties/lock","expectedValue":true}}
        }""").jsonObject.toUiSchemaElement()
        var writes = 0
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"value":"Ada","lock":${!readonly}}"""),
                schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                readonly = readonly, onChange = { writes++ })
        }
        if (readonly) assertEquals(0, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        else onNodeWithText("Ada").assertIsNotEnabled()
        val adornment = onNodeWithText("Unit")
        assertEquals("Unit explanation",
            adornment.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        adornment.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        waitForIdle()
        onNodeWithText("Unit explanation").assertExists()
        assertEquals(0, writes)
    }
}
