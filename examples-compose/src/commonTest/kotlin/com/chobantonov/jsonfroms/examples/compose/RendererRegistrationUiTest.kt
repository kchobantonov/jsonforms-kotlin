package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry
import com.chobantonov.jsonfroms.core.testers.isStringControl
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class RendererRegistrationUiTest {
    @Test fun materialHigherRankHostRendererWinsWhenAppended() = hostRendererWins(false, 1001, false)
    @Test fun cupertinoHigherRankHostRendererWinsWhenAppended() = hostRendererWins(true, 1001, false)
    @Test fun materialEqualRankHostRendererWinsWhenPrepended() = hostRendererWins(false, 1, true)
    @Test fun cupertinoEqualRankHostRendererWinsWhenPrepended() = hostRendererWins(true, 101, true)
    @Test fun materialEqualRankHostRendererLosesWhenAppended() = equalRankHostRendererLoses(false, 1)
    @Test fun cupertinoEqualRankHostRendererLosesWhenAppended() = equalRankHostRendererLoses(true, 101)

    private fun hostRendererWins(cupertino: Boolean, rank: Int, prepend: Boolean) = runComposeUiTest {
        var writes = 0
        val host = JsonFormsRendererRegistryEntry(
            rankWith(rank, isStringControl),
            ComposeJsonFormsRenderer { BasicText("Host renderer") },
        )
        val base = if (cupertino) cupertinoRenderers else materialRenderers
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"name":"Alice"}"""),
                schema = JsonSchema(Json.parseToJsonElement(
                    """{"type":"object","properties":{"name":{"type":"string"}}}"""
                ).jsonObject),
                uischema = ControlElement("#/properties/name"),
                renderers = if (prepend) listOf(host) + base else base + host,
                onChange = { writes++ },
            )
        }
        onAllNodes(hasText("Host renderer")).onFirst().assertExists()
        onAllNodes(hasSetTextAction()).assertCountEquals(0)
        assertEquals(0, writes)
    }

    private fun equalRankHostRendererLoses(cupertino: Boolean, rank: Int) = runComposeUiTest {
        var writes = 0
        val host = JsonFormsRendererRegistryEntry(
            rankWith(rank, isStringControl),
            ComposeJsonFormsRenderer { BasicText("Host renderer") },
        )
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"name":"Alice"}"""),
                schema = JsonSchema(Json.parseToJsonElement(
                    """{"type":"object","properties":{"name":{"type":"string"}}}"""
                ).jsonObject),
                uischema = ControlElement("#/properties/name"),
                renderers = (if (cupertino) cupertinoRenderers else materialRenderers) + host,
                onChange = { writes++ },
            )
        }
        onAllNodes(hasText("Host renderer")).assertCountEquals(0)
        onAllNodes(hasSetTextAction()).onFirst().assertExists()
        assertEquals(0, writes)
    }
}
