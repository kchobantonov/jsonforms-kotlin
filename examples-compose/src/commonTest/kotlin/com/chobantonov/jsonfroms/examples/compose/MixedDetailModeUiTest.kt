package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.LabelElement
import com.chobantonov.jsonfroms.core.store.JsonFormsUISchemaRegistryEntry
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class MixedDetailModeUiTest {
    @Test fun materialObjectDetailOverridesGenericMode() = scenario(false)
    @Test fun cupertinoObjectDetailOverridesGenericMode() = scenario(true)

    private fun scenario(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"value":{"type":["object","string"],"properties":{"note":{"type":"string"}}}}}"""
        ).jsonObject)
        val inline = Json.parseToJsonElement("""{"type":"Label","text":"Object-specific detail"}""")
        var specific by mutableStateOf<JsonElement>(inline)
        val registry = listOf(JsonFormsUISchemaRegistryEntry(
            tester = { _, _, _ -> 10 }, uischema = LabelElement("Generic registered detail"),
        ))
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"value":{"note":"Keep"}}"""),
                schema = schema,
                uischema = ControlElement("#/properties/value", options = mapOf(
                    "detail" to JsonPrimitive("REGISTERED"), "object-detail" to specific,
                )),
                uischemas = registry,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
            )
        }
        onNodeWithText("Object-specific detail").assertExists()
        onNodeWithText("Generic registered detail").assertDoesNotExist()
        runOnIdle { specific = JsonPrimitive("GENERATE") }
        onNodeWithText("Object-specific detail").assertDoesNotExist()
        onNodeWithText("Generic registered detail").assertDoesNotExist()
        assertTrue(onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty())
    }
}
