package com.chobantonov.jsonfroms.extended.renderers

import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.ListWithDetailElement
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry
import com.chobantonov.jsonfroms.core.testers.TesterContext
import com.chobantonov.jsonfroms.cupertino.renderers.complex.CupertinoArrayControlRenderer
import com.chobantonov.jsonfroms.cupertino.renderers.complex.CupertinoChipsRenderer
import com.chobantonov.jsonfroms.cupertino.renderers.complex.CupertinoEnumArrayRenderer
import com.chobantonov.jsonfroms.cupertino.renderers.complex.CupertinoMultiSelectRenderer
import com.chobantonov.jsonfroms.cupertino.renderers.complex.CupertinoTupleRenderer
import com.chobantonov.jsonfroms.cupertino.renderers.layouts.CupertinoCategorizationAccordionLayout
import com.chobantonov.jsonfroms.cupertino.renderers.layouts.CupertinoCategorizationLayout
import com.chobantonov.jsonfroms.cupertino.renderers.layouts.CupertinoCategorizationStepperLayout
import com.chobantonov.jsonfroms.cupertino.renderers.additional.CupertinoListWithDetailRenderer
import com.chobantonov.jsonfroms.cupertino.renderers.additional.cupertinoListWithDetailTester
import com.chobantonov.jsonfroms.cupertino.renderers.layouts.CupertinoArrayLayoutRenderer
import com.chobantonov.jsonfroms.material.renderers.complex.MaterialArrayControlRenderer
import com.chobantonov.jsonfroms.material.renderers.complex.MaterialChipsRenderer
import com.chobantonov.jsonfroms.material.renderers.complex.MaterialEnumArrayRenderer
import com.chobantonov.jsonfroms.material.renderers.complex.MaterialMultiSelectRenderer
import com.chobantonov.jsonfroms.material.renderers.complex.MaterialTupleRenderer
import com.chobantonov.jsonfroms.material.renderers.layouts.MaterialCategorizationAccordionLayout
import com.chobantonov.jsonfroms.material.renderers.layouts.MaterialCategorizationLayout
import com.chobantonov.jsonfroms.material.renderers.layouts.MaterialCategorizationStepperLayout
import com.chobantonov.jsonfroms.material.renderers.additional.MaterialListWithDetailRenderer
import com.chobantonov.jsonfroms.material.renderers.additional.materialListWithDetailTester
import com.chobantonov.jsonfroms.material.renderers.layouts.MaterialArrayLayoutRenderer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ArraySelectionTest {
    private val finite = JsonSchema(Json.parseToJsonElement("""{
        "type":"array","uniqueItems":true,
        "items":{"type":"string","enum":["Email","SMS"]}
    }""").jsonObject)
    private val ordinary = JsonSchema(Json.parseToJsonElement("""{
        "type":"array","items":{"type":"string"}
    }""").jsonObject)

    private fun winner(
        entries: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
        schema: JsonSchema,
        options: Map<String, JsonPrimitive> = emptyMap(),
        config: Map<String, Any?> = emptyMap(),
        scope: String = "#",
    ): Pair<ComposeJsonFormsRenderer, Int> = winnerForUi(
        entries, schema, ControlElement(scope, options = options), config)

    private fun winnerForUi(
        entries: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
        schema: JsonSchema,
        ui: UiSchemaElement,
        config: Map<String, Any?> = emptyMap(),
    ): Pair<ComposeJsonFormsRenderer, Int> {
        val context = TesterContext(rootSchema = schema, config = config)
        var best: Pair<ComposeJsonFormsRenderer, Int>? = null
        for (entry in entries) {
            val rank = entry.tester(ui, schema, context)
            if (best == null || rank > best.second) best = entry.renderer to rank
        }
        return requireNotNull(best)
    }

    @Test fun explicitArrayPresentationsBeatAutomaticCheckboxes() {
        val families = listOf(
            Triple(materialExtendedRenderers, MaterialEnumArrayRenderer, MaterialArrayControlRenderer),
            Triple(cupertinoExtendedRenderers, CupertinoEnumArrayRenderer, CupertinoArrayControlRenderer),
        )
        for ((entries, checkboxes, table) in families) {
            val (automatic, automaticRank) = winner(entries, finite)
            assertSame(checkboxes, automatic)
            assertEquals(4, automaticRank)
            assertSame(table, winner(entries, finite, mapOf("table" to JsonPrimitive(true))).first)
            assertSame(table, winner(entries, finite, mapOf("variant" to JsonPrimitive("ag-grid"))).first)
        }
    }

    @Test fun configurationCannotSelectArrayPresentation() {
        val config = mapOf<String, Any?>("table" to true, "variant" to "ag-grid")
        assertSame(MaterialEnumArrayRenderer, winner(materialExtendedRenderers, finite, config = config).first)
        assertSame(CupertinoEnumArrayRenderer, winner(cupertinoExtendedRenderers, finite, config = config).first)
        assertSame(MaterialArrayControlRenderer, winner(materialExtendedRenderers, ordinary, config = config).first)
        assertSame(CupertinoArrayControlRenderer, winner(cupertinoExtendedRenderers, ordinary, config = config).first)
        for (variant in listOf("chips", "multi-select")) {
            val variantConfig = mapOf<String, Any?>("variant" to variant)
            assertSame(MaterialEnumArrayRenderer,
                winner(materialExtendedRenderers, finite, config = variantConfig).first)
            assertSame(CupertinoEnumArrayRenderer,
                winner(cupertinoExtendedRenderers, finite, config = variantConfig).first)
        }
    }

    @Test fun chipsAndMultiSelectRemainExplicitChoices() {
        for ((entries, chips, multiple) in listOf(
            Triple(materialExtendedRenderers, MaterialChipsRenderer, MaterialMultiSelectRenderer),
            Triple(cupertinoExtendedRenderers, CupertinoChipsRenderer, CupertinoMultiSelectRenderer),
        )) {
            assertSame(chips, winner(entries, finite, mapOf("variant" to JsonPrimitive("chips"))).first)
            assertSame(multiple, winner(entries, finite, mapOf("variant" to JsonPrimitive("multi-select"))).first)
        }
    }

    @Test fun canonicalArrayVariantsFallBackWhenInapplicable() {
        val numeric = JsonSchema(Json.parseToJsonElement("""{
            "type":"array","items":{"type":"integer"}
        }""").jsonObject)
        for ((entries, chips, table) in listOf(
            Triple(materialExtendedRenderers, MaterialChipsRenderer, MaterialArrayControlRenderer),
            Triple(cupertinoExtendedRenderers, CupertinoChipsRenderer, CupertinoArrayControlRenderer),
        )) {
            assertSame(chips, winner(entries, ordinary,
                mapOf("variant" to JsonPrimitive("chips"))).first)
            assertSame(table, winner(entries, ordinary,
                mapOf("variant" to JsonPrimitive("multi-select"))).first)
            assertSame(table, winner(entries, numeric,
                mapOf("variant" to JsonPrimitive("chips"))).first)
        }
    }

    @Test fun canonicalCategorizationVariantsSelectAndFallBack() {
        val categories = listOf(CategoryElement(label = "First"), CategoryElement(label = "Second"))
        data class Case(
            val entries: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
            val default: ComposeJsonFormsRenderer,
            val stepper: ComposeJsonFormsRenderer,
            val accordion: ComposeJsonFormsRenderer,
        )
        for ((entries, default, stepper, accordion) in listOf(
            Case(materialExtendedRenderers, MaterialCategorizationLayout,
                MaterialCategorizationStepperLayout, MaterialCategorizationAccordionLayout),
            Case(cupertinoExtendedRenderers, CupertinoCategorizationLayout,
                CupertinoCategorizationStepperLayout, CupertinoCategorizationAccordionLayout),
        )) {
            fun selected(variant: String) = winnerForUi(entries,
                JsonSchema(), CategorizationElement(
                    options = mapOf("variant" to JsonPrimitive(variant)), elements = categories)).first
            assertSame(stepper, selected("stepper"))
            assertSame(accordion, selected("accordion"))
            assertSame(default, selected("chips"))
        }
    }

    @Test fun dynamicVariantDescriptorDoesNotSelectStaticPresentation() {
        val descriptor = Json.parseToJsonElement("""{
            "${'$'}dynamic":{"path":"#/presentation"}
        }""")
        for ((entries, automatic) in listOf(
            materialExtendedRenderers to MaterialEnumArrayRenderer,
            cupertinoExtendedRenderers to CupertinoEnumArrayRenderer,
        )) {
            assertSame(automatic, winnerForUi(entries, finite,
                ControlElement("#", options = mapOf("variant" to descriptor))).first)
        }
    }

    @Test fun referencedFiniteArrayKeepsAutomaticAndExplicitSelection() {
        val root = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"choices":{"${'$'}ref":"#/${'$'}defs/choices"}},
            "${'$'}defs":{"choices":{"type":"array","uniqueItems":true,
                "items":{"${'$'}ref":"#/${'$'}defs/choice"}},
                "choice":{"type":"string","enum":["Email","SMS"]}}
        }""").jsonObject)
        for ((entries, automatic, table) in listOf(
            Triple(materialExtendedRenderers, MaterialEnumArrayRenderer, MaterialArrayControlRenderer),
            Triple(cupertinoExtendedRenderers, CupertinoEnumArrayRenderer, CupertinoArrayControlRenderer),
        )) {
            assertSame(automatic, winner(entries, root, scope = "#/properties/choices").first)
            assertSame(table, winner(entries, root, mapOf("table" to JsonPrimitive(true)),
                scope = "#/properties/choices").first)
        }
    }

    @Test fun positionalTupleAndReferencedListDetailSelectTheirPresentations() {
        val tuple = JsonSchema(Json.parseToJsonElement("""{
            "type":"array","prefixItems":[{"type":"string"},{"type":"number"}],"items":false
        }""").jsonObject)
        val root = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"rows":{"${'$'}ref":"#/${'$'}defs/rows"}},
            "${'$'}defs":{"rows":{"type":"array","items":{"type":"object",
                "properties":{"name":{"type":"string"}}}}}
        }""").jsonObject)
        for ((entries, tupleRenderer, detailRenderer) in listOf(
            Triple(materialExtendedRenderers, MaterialTupleRenderer, MaterialListWithDetailRenderer),
            Triple(cupertinoExtendedRenderers, CupertinoTupleRenderer, CupertinoListWithDetailRenderer),
        )) {
            assertSame(tupleRenderer, winner(entries, tuple).first)
            assertSame(tupleRenderer, winner(entries, tuple,
                mapOf("variant" to JsonPrimitive("tuple"))).first)
            assertSame(detailRenderer, winnerForUi(entries, root,
                ListWithDetailElement("#/properties/rows")).first)
        }
    }

    @Test fun listDetailRejectsNonObjectArrays() {
        val primitive = JsonSchema(Json.parseToJsonElement("""{
            "type":"array","items":{"type":"string"}
        }""").jsonObject)
        val positional = JsonSchema(Json.parseToJsonElement("""{
            "type":"array","prefixItems":[{"type":"string"}],"items":false
        }""").jsonObject)
        for (tester in listOf(materialListWithDetailTester, cupertinoListWithDetailTester)) {
            for (schema in listOf(primitive, positional)) {
                assertEquals(-1, tester(ListWithDetailElement("#"), schema,
                    TesterContext(rootSchema = schema)))
            }
        }
    }

    @Test fun defaultArrayShapeSelectsTableOrExpandableDetail() {
        val flat = JsonSchema(Json.parseToJsonElement("""{
            "type":"array","items":{"type":"object","properties":{
                "name":{"type":"string"},"age":{"type":"integer"}}}
        }""").jsonObject)
        val nested = JsonSchema(Json.parseToJsonElement("""{
            "type":"array","items":{"type":"object","properties":{
                "name":{"type":"string"},
                "address":{"type":"object","properties":{"city":{"type":"string"}}}}}
        }""").jsonObject)
        for ((entries, table, expandable) in listOf(
            Triple(materialExtendedRenderers, MaterialArrayControlRenderer, MaterialArrayLayoutRenderer),
            Triple(cupertinoExtendedRenderers, CupertinoArrayControlRenderer, CupertinoArrayLayoutRenderer),
        )) {
            for (schema in listOf(ordinary, flat)) {
                assertSame(table, winner(entries, schema).first)
            }
            assertSame(expandable, winner(entries, nested).first)
            assertSame(table, winner(entries, nested, mapOf("table" to JsonPrimitive(true))).first)
        }
    }

    @Test fun alternativeItemBranchesUseTableOnlyForSharedFlatFields() {
        val shared = JsonSchema(Json.parseToJsonElement("""{
            "type":"array","items":{"type":"object","oneOf":[
                {"properties":{"value":{"type":"string"}}},
                {"properties":{"value":{"type":"integer"}}}
            ]}
        }""").jsonObject)
        val nested = JsonSchema(Json.parseToJsonElement("""{
            "type":"array","items":{"type":"object","anyOf":[
                {"properties":{"value":{"type":"object"}}},
                {"properties":{"value":{"type":"object"}}}
            ]}
        }""").jsonObject)
        val distinct = JsonSchema(Json.parseToJsonElement("""{
            "type":"array","items":{"type":"object","oneOf":[
                {"properties":{"name":{"type":"string"}}},
                {"properties":{"count":{"type":"integer"}}}
            ]}
        }""").jsonObject)
        for ((entries, table, expandable) in listOf(
            Triple(materialExtendedRenderers, MaterialArrayControlRenderer, MaterialArrayLayoutRenderer),
            Triple(cupertinoExtendedRenderers, CupertinoArrayControlRenderer, CupertinoArrayLayoutRenderer),
        )) {
            assertSame(table, winner(entries, shared).first)
            assertSame(expandable, winner(entries, nested).first)
            assertSame(expandable, winner(entries, distinct).first)
        }
    }

    @Test fun arrayPropertiesInCompositionSelectDefaultPresentation() {
        val flat = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","allOf":[{"properties":{"rows":{"type":"array",
                "items":{"type":"object","properties":{
                    "name":{"type":"string"},"age":{"type":"integer"}}}}}}]
        }""").jsonObject)
        val nested = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","allOf":[{"properties":{"rows":{"type":"array",
                "items":{"type":"object","properties":{
                    "name":{"type":"string"},
                    "address":{"type":"object","properties":{"city":{"type":"string"}}}}}}}}]
        }""").jsonObject)
        for ((entries, table, expandable) in listOf(
            Triple(materialExtendedRenderers, MaterialArrayControlRenderer, MaterialArrayLayoutRenderer),
            Triple(cupertinoExtendedRenderers, CupertinoArrayControlRenderer, CupertinoArrayLayoutRenderer),
        )) {
            assertSame(table, winner(entries, flat, scope = "#/properties/rows").first)
            assertSame(expandable, winner(entries, nested, scope = "#/properties/rows").first)
            assertSame(table, winner(entries, nested, mapOf("table" to JsonPrimitive(true)),
                scope = "#/properties/rows").first)
        }
    }

    @Test fun distinctAlternativeAndConditionalArrayScopesKeepTheirShapes() {
        val flat = """{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"}}}}"""
        val nested = """{"type":"array","items":{"type":"object","properties":{
            "address":{"type":"object","properties":{"city":{"type":"string"}}}}}}"""
        val alternatives = listOf("anyOf", "oneOf").map { keyword ->
            JsonSchema(Json.parseToJsonElement("""{
                "type":"object","$keyword":[
                    {"properties":{"flatRows":$flat},"required":["flatRows"]},
                    {"properties":{"nestedRows":$nested},"required":["nestedRows"]}
                ]
            }""").jsonObject)
        }
        val conditional = JsonSchema(Json.parseToJsonElement("""{
            "type":"object",
            "if":{"properties":{"mode":{"const":"flat"}},"required":["mode"]},
            "then":{"properties":{"flatRows":$flat}},
            "else":{"properties":{"nestedRows":$nested}}
        }""").jsonObject)
        for ((entries, table, expandable) in listOf(
            Triple(materialExtendedRenderers, MaterialArrayControlRenderer, MaterialArrayLayoutRenderer),
            Triple(cupertinoExtendedRenderers, CupertinoArrayControlRenderer, CupertinoArrayLayoutRenderer),
        )) {
            for (schema in alternatives + conditional) {
                for (registry in listOf(entries, entries.reversed())) {
                    assertSame(table, winner(registry, schema, scope = "#/properties/flatRows").first)
                    assertSame(expandable, winner(registry, schema, scope = "#/properties/nestedRows").first)
                    assertSame(table, winner(registry, schema,
                        mapOf("table" to JsonPrimitive(true)), scope = "#/properties/nestedRows").first)
                }
            }
        }
    }
}
