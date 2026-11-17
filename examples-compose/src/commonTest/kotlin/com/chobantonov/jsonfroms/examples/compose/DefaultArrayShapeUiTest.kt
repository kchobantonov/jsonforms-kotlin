package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DefaultArrayShapeUiTest {
    @Test fun materialPrimitiveRowsUseOneValueCell() = primitive(false)
    @Test fun cupertinoPrimitiveRowsUseOneValueCell() = primitive(true)
    @Test fun materialFlatObjectsUsePropertyColumns() = flat(false)
    @Test fun cupertinoFlatObjectsUsePropertyColumns() = flat(true)
    @Test fun materialNestedObjectsUseExpandableDetails() = nested(false)
    @Test fun cupertinoNestedObjectsUseExpandableDetails() = nested(true)
    @Test fun materialDefaultShapesRevalidateAfterMutation() = defaultMutations(false)
    @Test fun cupertinoDefaultShapesRevalidateAfterMutation() = defaultMutations(true)
    @Test fun materialDefaultShapesRepairRowErrors() = rowValidation(false)
    @Test fun cupertinoDefaultShapesRepairRowErrors() = rowValidation(true)
    @Test fun materialComposedArrayPropertyKeepsShapeAndPath() = composedProperty(false)
    @Test fun cupertinoComposedArrayPropertyKeepsShapeAndPath() = composedProperty(true)
    @Test fun materialAlternativeArrayScopesKeepTheirPaths() = alternativeArrayScopes(false)
    @Test fun cupertinoAlternativeArrayScopesKeepTheirPaths() = alternativeArrayScopes(true)

    private fun alternativeArrayScopes(cupertino: Boolean) = runComposeUiTest {
        val flat = """{"type":"array","items":{"type":"object","properties":{
            "name":{"type":"string","minLength":3}}}}"""
        val nested = """{"type":"array","items":{"type":"object","properties":{
            "address":{"type":"object","properties":{"city":{"type":"string","minLength":3}}}}}}"""
        var kind by mutableStateOf(0)
        var useNested by mutableStateOf(false)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"flatRows":[{"name":"x"}]}"""))
        var writes = 0
        var validity: CombinedValidityStatus? = null
        setContent {
            val schemaText = when (kind) {
                0, 1 -> """{"type":"object","${if (kind == 0) "anyOf" else "oneOf"}":[
                    {"properties":{"flatRows":$flat},"required":["flatRows"]},
                    {"properties":{"nestedRows":$nested},"required":["nestedRows"]}]}"""
                else -> """{"type":"object",
                    "if":{"properties":{"mode":{"const":"flat"}},"required":["mode"]},
                    "then":{"properties":{"flatRows":$flat},"required":["flatRows"]},
                    "else":{"properties":{"nestedRows":$nested},"required":["nestedRows"]}}"""
            }
            JsonForms(
                data = data,
                schema = JsonSchema(Json.parseToJsonElement(schemaText).jsonObject),
                uischema = ControlElement(
                    if (useNested) "#/properties/nestedRows" else "#/properties/flatRows"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; writes++ },
                onValidityChange = { validity = it.status },
            )
        }
        for (variant in 0..2) for (nestedRow in listOf(false, true)) {
            val row = if (nestedRow) """"nestedRows":[{"address":{"city":"x"}}]"""
                else """"flatRows":[{"name":"x"}]"""
            val fixedRow = if (nestedRow) """"nestedRows":[{"address":{"city":"Sofia"}}]"""
                else """"flatRows":[{"name":"Sofia"}]"""
            val mode = if (variant == 2) """"mode":"${if (nestedRow) "nested" else "flat"}",""" else ""
            runOnIdle {
                kind = variant
                useNested = nestedRow
                data = Json.parseToJsonElement("{$mode$row}")
            }
            waitForIdle()
            assertEquals(CombinedValidityStatus.Invalid, validity)
            onNodeWithText(if (nestedRow) "City" else "Name").assertExists()
            assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
            onAllNodes(hasSetTextAction())[0].performTextReplacement("Sofia")
            runOnIdle {
                assertEquals(Json.parseToJsonElement("{$mode$fixedRow}"), data)
                assertEquals(CombinedValidityStatus.Valid, validity)
                assertEquals(variant * 2 + (if (nestedRow) 2 else 1), writes)
            }
        }
    }

    private fun composedProperty(cupertino: Boolean) = runComposeUiTest {
        val items = listOf(
            """{"type":"object","properties":{"name":{"type":"string","minLength":3}}}""",
            """{"type":"object","properties":{"address":{"type":"object","properties":{"city":{"type":"string","minLength":3}}}}}""",
        )
        val invalid = listOf(
            """{"rows":[{"name":"x"}]}""",
            """{"rows":[{"address":{"city":"x"}}]}""",
        )
        val repaired = listOf(
            """{"rows":[{"name":"Sofia"}]}""",
            """{"rows":[{"address":{"city":"Sofia"}}]}""",
        )
        var shape by mutableStateOf(0)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement(invalid[0]))
        var writes = 0
        var validity: CombinedValidityStatus? = null
        setContent {
            JsonForms(
                data = data,
                schema = JsonSchema(Json.parseToJsonElement(
                    """{"type":"object","allOf":[{"properties":{"rows":{"type":"array","items":${items[shape]}}}}]}"""
                ).jsonObject),
                uischema = ControlElement("#/properties/rows"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; writes++ },
                onValidityChange = { validity = it.status },
            )
        }
        items.indices.forEach { index ->
            runOnIdle {
                shape = index
                data = Json.parseToJsonElement(invalid[index])
            }
            waitForIdle()
            assertEquals(CombinedValidityStatus.Invalid, validity)
            onNodeWithText(if (index == 0) "Name" else "City").assertExists()
            assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
            onAllNodes(hasSetTextAction())[0].performTextReplacement("Sofia")
            runOnIdle {
                assertEquals(Json.parseToJsonElement(repaired[index]), data)
                assertEquals(CombinedValidityStatus.Valid, validity)
                assertEquals(index + 1, writes)
            }
        }
    }

    private fun rowValidation(cupertino: Boolean) = runComposeUiTest {
        val items = listOf(
            """{"type":"string","minLength":3}""",
            """{"type":"object","properties":{"name":{"type":"string","minLength":3}}}""",
            """{"type":"object","properties":{"address":{"type":"object","properties":{"city":{"type":"string","minLength":3}}}}}""",
        )
        val invalid = listOf("""["x"]""", """[{"name":"x"}]""", """[{"address":{"city":"x"}}]""")
        val repaired = listOf("""["Sofia"]""", """[{"name":"Sofia"}]""", """[{"address":{"city":"Sofia"}}]""")
        var shape by mutableStateOf(0)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement(invalid[0]))
        var writes = 0
        var validity: CombinedValidityStatus? = null
        setContent {
            JsonForms(
                data = data,
                schema = JsonSchema(Json.parseToJsonElement(
                    """{"type":"array","items":${items[shape]}}"""
                ).jsonObject),
                uischema = ControlElement("#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; writes++ },
                onValidityChange = { validity = it.status },
            )
        }
        items.indices.forEach { index ->
            runOnIdle {
                shape = index
                data = Json.parseToJsonElement(invalid[index])
            }
            waitForIdle()
            assertEquals(CombinedValidityStatus.Invalid, validity)
            assertTrue(onAllNodes(hasContentDescription("errors", substring = true))
                .fetchSemanticsNodes().isNotEmpty())
            assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
            onAllNodes(hasSetTextAction())[0].performTextReplacement("Sofia")
            runOnIdle {
                assertEquals(Json.parseToJsonElement(repaired[index]), data)
                assertEquals(CombinedValidityStatus.Valid, validity)
                assertEquals(index + 1, writes)
            }
            assertEquals(0, onAllNodes(hasContentDescription("errors", substring = true))
                .fetchSemanticsNodes().size)
        }
    }

    private fun defaultMutations(cupertino: Boolean) = runComposeUiTest {
        val items = listOf(
            """{"type":"string"}""",
            """{"type":"object","properties":{"name":{"type":"string"},"age":{"type":"integer"}}}""",
            """{"type":"object","properties":{"name":{"type":"string"},"address":{"type":"object","properties":{"city":{"type":"string"}}}}}""",
        )
        val expected = listOf(
            """[""]""",
            """[{}]""",
            """[{}]""",
        )
        var shape by mutableStateOf(0)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("[]"))
        var writes = 0
        var validity: CombinedValidityStatus? = null
        setContent {
            JsonForms(
                data = data,
                schema = JsonSchema(Json.parseToJsonElement(
                    """{"type":"array","minItems":1,"items":${items[shape]}}"""
                ).jsonObject),
                uischema = ControlElement("#", options = mapOf("restrict" to JsonPrimitive(false))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; writes++ },
                onValidityChange = { validity = it.status },
            )
        }
        items.indices.forEach { index ->
            runOnIdle {
                shape = index
                data = Json.parseToJsonElement("[]")
            }
            waitForIdle()
            assertEquals(CombinedValidityStatus.Invalid, validity)
            onNodeWithContentDescription("Add item").performClick()
            runOnIdle {
                assertEquals(Json.parseToJsonElement(expected[index]), data)
                assertEquals(CombinedValidityStatus.Valid, validity)
                assertEquals(index * 2 + 1, writes)
            }
            onAllNodes(hasContentDescription("Delete item"))[0].performClick()
            runOnIdle {
                assertEquals(Json.parseToJsonElement("[]"), data)
                assertEquals(CombinedValidityStatus.Invalid, validity)
                assertEquals(index * 2 + 2, writes)
            }
        }
    }

    private fun primitive(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"array","items":{"type":"string"}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""["Ada"]"""))
        setContent {
            JsonForms(data = data, schema = schema, uischema = ControlElement("#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onNodeWithText("Item 1").assertDoesNotExist()
        onNodeWithText("Value").assertDoesNotExist()
        onAllNodes(hasSetTextAction())[0].performTextReplacement("Eve")
        runOnIdle { assertEquals(Json.parseToJsonElement("""["Eve"]"""), data) }
    }

    private fun flat(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"array","items":{"type":"object","properties":{
                "name":{"type":"string"},"age":{"type":"integer"}}}
        }""").jsonObject)
        setContent {
            JsonForms(data = Json.parseToJsonElement("""[{"name":"Ada","age":32}]"""),
                schema = schema, uischema = ControlElement("#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onNodeWithText("Name").assertExists()
        onNodeWithText("Age").assertExists()
        onNodeWithText("Item 1").assertDoesNotExist()
        assertEquals(2, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
    }

    private fun nested(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"array","items":{"type":"object","properties":{
                "name":{"type":"string"},
                "address":{"type":"object","properties":{"city":{"type":"string"}}}}}
        }""").jsonObject)
        setContent {
            JsonForms(data = Json.parseToJsonElement("""[{"name":"Ada","address":{"city":"Sofia"}}]"""),
                schema = schema, uischema = ControlElement("#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        assertEquals(2, onAllNodes(hasText("Ada")).fetchSemanticsNodes().size)
        onNodeWithText("City").assertExists()
    }
}
