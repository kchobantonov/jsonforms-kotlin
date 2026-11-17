package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.LabelElement
import com.chobantonov.jsonfroms.core.models.ListWithDetailElement
import com.chobantonov.jsonfroms.core.store.JsonFormsUISchemaRegistryEntry
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class DetailModeUiTest {
    @Test fun materialObjectGenerateBypassesRegistry() = objectDetail(false)
    @Test fun cupertinoObjectGenerateBypassesRegistry() = objectDetail(true)
    @Test fun materialArrayGenerateBypassesRegistry() = arrayDetail(false)
    @Test fun cupertinoArrayGenerateBypassesRegistry() = arrayDetail(true)
    @Test fun materialListGenerateBypassesRegistry() = listDetail(false)
    @Test fun cupertinoListGenerateBypassesRegistry() = listDetail(true)
    @Test fun materialExplicitTableOutranksNestedDetail() = explicitTable(false)
    @Test fun cupertinoExplicitTableOutranksNestedDetail() = explicitTable(true)
    @Test fun materialInlineArrayDetailWins() = inlineArray(false)
    @Test fun cupertinoInlineArrayDetailWins() = inlineArray(true)
    @Test fun materialGeneratedMissingObjectDoesNotInitializeOnMount() = missingObject(false)
    @Test fun cupertinoGeneratedMissingObjectDoesNotInitializeOnMount() = missingObject(true)

    private val registry = listOf(JsonFormsUISchemaRegistryEntry(
        tester = { _, _, _ -> 10 },
        uischema = LabelElement("Registered detail"),
    ))

    private fun missingObject(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"person":{"type":"object","properties":{"name":{"type":"string"}}}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("{}"))
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ControlElement("#/properties/person",
                    options = mapOf("detail" to JsonPrimitive("GENERATE"))),
                uischemas = registry,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ },
            )
        }
        onNodeWithText("Registered detail").assertDoesNotExist()
        assertTrue(onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty())
        waitForIdle()
        assertEquals(Json.parseToJsonElement("{}"), data)
        assertEquals(0, changes)
        onAllNodes(hasSetTextAction())[0].performTextReplacement("Ada")
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"person":{"name":"Ada"}}"""), data)
        assertEquals(1, changes)
    }

    private fun objectDetail(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"person":{"type":"object","properties":{"name":{"type":"string"}}}}}"""
        ).jsonObject)
        var mode by mutableStateOf("GENERATE")
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"person":{"name":"Ada"}}"""),
                schema = schema,
                uischema = ControlElement("#/properties/person", options = mapOf("detail" to JsonPrimitive(mode))),
                uischemas = registry,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
            )
        }
        onNodeWithText("Registered detail").assertDoesNotExist()
        assertTrue(onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty())
        runOnIdle { mode = "REGISTERED" }
        onNodeWithText("Registered detail").assertExists()
        runOnIdle { mode = "gEnErAtE" }
        onNodeWithText("Registered detail").assertDoesNotExist()
    }

    private fun arrayDetail(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"}}}}}}"""
        ).jsonObject)
        var mode by mutableStateOf("GENERATE")
        val arrayRegistry = listOf(JsonFormsUISchemaRegistryEntry(
            tester = { _, scope, _ -> if (scope == "#/properties/rows") 10 else -1 },
            uischema = LabelElement("Registered detail"),
        ))
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"rows":[{"name":"Ada"}]}"""),
                schema = schema,
                uischema = ControlElement("#/properties/rows", options = mapOf("detail" to JsonPrimitive(mode))),
                uischemas = arrayRegistry,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
            )
        }
        onNodeWithText("Registered detail").assertDoesNotExist()
        assertTrue(onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty())
        runOnIdle { mode = "REGISTERED" }
        onNodeWithText("Registered detail").assertExists()
    }

    private fun listDetail(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"}}}}}}"""
        ).jsonObject)
        var mode by mutableStateOf("GENERATE")
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"rows":[{"name":"Ada"}]}"""),
                schema = schema,
                uischema = ListWithDetailElement("#/properties/rows",
                    options = mapOf("detail" to JsonPrimitive(mode),
                        "elementLabelProp" to JsonPrimitive("name"))),
                uischemas = registry,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
            )
        }
        onNodeWithText("Ada").performClick()
        onNodeWithText("Registered detail").assertDoesNotExist()
        assertTrue(onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty())
        runOnIdle { mode = "REGISTERED" }
        onNodeWithText("Registered detail").assertExists()
    }

    private fun explicitTable(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"}}}}}}"""
        ).jsonObject)
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"rows":[{"name":"Ada"}]}"""),
                schema = schema,
                uischema = ControlElement("#/properties/rows", options = mapOf(
                    "detail" to JsonPrimitive("GENERATE"), "table" to JsonPrimitive(true))),
                uischemas = registry,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
            )
        }
        onNodeWithContentDescription("Expand item").assertDoesNotExist()
        onNodeWithText("Registered detail").assertDoesNotExist()
        assertTrue(onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty())
    }

    private fun inlineArray(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"}}}}}}"""
        ).jsonObject)
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"rows":[{"name":"Ada"}]}"""),
                schema = schema,
                uischema = ControlElement("#/properties/rows", options = mapOf(
                    "detail" to Json.parseToJsonElement("""{"type":"Label","text":"Inline detail"}"""))),
                uischemas = registry,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
            )
        }
        onNodeWithText("Inline detail").assertExists()
        onNodeWithText("Registered detail").assertDoesNotExist()
    }
}
