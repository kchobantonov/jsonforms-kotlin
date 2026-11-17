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
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class CombinatorDetailModeUiTest {
    @Test fun materialOneOfDetailModeSelectsBranchForm() = scenario(false)
    @Test fun cupertinoOneOfDetailModeSelectsBranchForm() = scenario(true)
    @Test fun materialAllOfGenerateBypassesWholeSchemaRegistry() = allOf(false)
    @Test fun cupertinoAllOfGenerateBypassesWholeSchemaRegistry() = allOf(true)

    private fun scenario(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"choice":{"oneOf":[{"title":"First","type":"object","properties":{"name":{"type":"string"}},"required":["name"]},{"title":"Second","type":"object","properties":{"other":{"type":"string"}},"required":["other"]}]}}}"""
        ).jsonObject)
        val registry = listOf(JsonFormsUISchemaRegistryEntry(
            tester = { _, _, _ -> 10 }, uischema = LabelElement("Registered branch"),
        ))
        var mode by mutableStateOf("GENERATE")
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"choice":{"name":"Ada"}}"""),
                schema = schema,
                uischema = ControlElement("#/properties/choice", options = mapOf("detail" to JsonPrimitive(mode))),
                uischemas = registry,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
            )
        }
        onNodeWithText("Registered branch").assertDoesNotExist()
        assertTrue(onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty())
        runOnIdle { mode = "REGISTERED" }
        onNodeWithText("Registered branch").assertExists()
    }

    private fun allOf(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"choice":{"allOf":[{"type":"object","properties":{"name":{"type":"string"}}}]}}}"""
        ).jsonObject)
        val registry = listOf(JsonFormsUISchemaRegistryEntry(
            tester = { _, _, _ -> 10 }, uischema = LabelElement("Registered branch"),
        ))
        var mode by mutableStateOf("GENERATE")
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"choice":{"name":"Ada"}}"""),
                schema = schema,
                uischema = ControlElement("#/properties/choice", options = mapOf("detail" to JsonPrimitive(mode))),
                uischemas = registry,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
            )
        }
        onNodeWithText("Registered branch").assertDoesNotExist()
        assertTrue(onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty())
        runOnIdle { mode = "REGISTERED" }
        onNodeWithText("Registered branch").assertExists()
    }
}
