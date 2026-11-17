package com.chobantonov.jsonfroms.extended.renderers

import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry
import com.chobantonov.jsonfroms.core.testers.TesterContext
import com.chobantonov.jsonfroms.cupertino.renderers.controls.CupertinoIntegerControl
import com.chobantonov.jsonfroms.cupertino.renderers.controls.CupertinoTextControl
import com.chobantonov.jsonfroms.material.renderers.controls.MaterialIntegerControl
import com.chobantonov.jsonfroms.material.renderers.controls.MaterialTextControl
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ComposedScopeSelectionTest {
    private val composedRoot = JsonSchema(Json.parseToJsonElement("""{
        "type":"object","allOf":[{"${'$'}ref":"#/${'$'}defs/fields"}],
        "${'$'}defs":{"fields":{"properties":{
            "name":{"type":"string"},
            "count":{"${'$'}ref":"#/${'$'}defs/count"}
        }},"count":{"type":"integer"}}
    }""").jsonObject)
    private val scalarComposition = JsonSchema(Json.parseToJsonElement("""{
        "allOf":[{"type":"string"}]
    }""").jsonObject)

    private fun winner(
        entries: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
        schema: JsonSchema,
        scope: String,
    ): ComposeJsonFormsRenderer {
        val ui = ControlElement(scope)
        val context = TesterContext(rootSchema = schema)
        val ranked = entries.map { it.renderer to it.tester(ui, schema, context) }
        val rank = ranked.maxOf { it.second }
        assertTrue(rank >= 0, "No renderer matched $scope")
        return ranked.first { it.second == rank }.first
    }

    @Test fun composedUntypedBranchKeepsTypedPropertyRenderers() {
        for ((entries, text, integer) in listOf(
            Triple(materialExtendedRenderers, MaterialTextControl, MaterialIntegerControl),
            Triple(cupertinoExtendedRenderers, CupertinoTextControl, CupertinoIntegerControl),
        )) {
            assertSame(text, winner(entries, composedRoot, "#/properties/name"))
            assertSame(integer, winner(entries, composedRoot, "#/properties/count"))
            assertSame(text, winner(entries.reversed(), composedRoot, "#/properties/name"))
            assertSame(integer, winner(entries.reversed(), composedRoot, "#/properties/count"))
        }
    }

    @Test fun scalarAllOfUsesResolvedStringControl() {
        assertSame(MaterialTextControl, winner(materialExtendedRenderers, scalarComposition, "#"))
        assertSame(CupertinoTextControl, winner(cupertinoExtendedRenderers, scalarComposition, "#"))
    }

    @Test fun alternativeBranchesKeepDistinctTypedProperties() {
        for (keyword in listOf("anyOf", "oneOf")) {
            val root = JsonSchema(Json.parseToJsonElement("""{
                "type":"object","$keyword":[
                    {"properties":{"name":{"type":"string"}}},
                    {"properties":{"count":{"type":"integer"}}}
                ]
            }""").jsonObject)
            for ((entries, text, integer) in listOf(
                Triple(materialExtendedRenderers, MaterialTextControl, MaterialIntegerControl),
                Triple(cupertinoExtendedRenderers, CupertinoTextControl, CupertinoIntegerControl),
            )) {
                assertSame(text, winner(entries, root, "#/properties/name"), keyword)
                assertSame(integer, winner(entries, root, "#/properties/count"), keyword)
                assertSame(text, winner(entries.reversed(), root, "#/properties/name"), keyword)
                assertSame(integer, winner(entries.reversed(), root, "#/properties/count"), keyword)
            }
        }
    }

    @Test fun conditionalBranchesKeepDistinctTypedProperties() {
        val root = JsonSchema(Json.parseToJsonElement("""{
            "type":"object",
            "if":{"properties":{"mode":{"const":"email"}},"required":["mode"]},
            "then":{"properties":{"email":{"type":"string"}},"required":["email"]},
            "else":{"properties":{"count":{"type":"integer"}},"required":["count"]}
        }""").jsonObject)
        for ((entries, text, integer) in listOf(
            Triple(materialExtendedRenderers, MaterialTextControl, MaterialIntegerControl),
            Triple(cupertinoExtendedRenderers, CupertinoTextControl, CupertinoIntegerControl),
        )) {
            assertSame(text, winner(entries, root, "#/properties/email"))
            assertSame(integer, winner(entries, root, "#/properties/count"))
            assertSame(text, winner(entries.reversed(), root, "#/properties/email"))
            assertSame(integer, winner(entries.reversed(), root, "#/properties/count"))
        }
    }
}
