package com.chobantonov.jsonfroms.extended.renderers

import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry
import com.chobantonov.jsonfroms.core.testers.TesterContext
import com.chobantonov.jsonfroms.cupertino.renderers.controls.CupertinoAnyOfStringOrEnumControl
import com.chobantonov.jsonfroms.cupertino.renderers.controls.CupertinoEnumControl
import com.chobantonov.jsonfroms.cupertino.renderers.controls.CupertinoIntegerControl
import com.chobantonov.jsonfroms.cupertino.renderers.controls.CupertinoOneOfEnumControl
import com.chobantonov.jsonfroms.cupertino.renderers.controls.CupertinoPasswordControl
import com.chobantonov.jsonfroms.cupertino.renderers.controls.CupertinoSliderControl
import com.chobantonov.jsonfroms.cupertino.renderers.complex.CupertinoAnyOfRenderer
import com.chobantonov.jsonfroms.cupertino.renderers.complex.CupertinoOneOfRenderer
import com.chobantonov.jsonfroms.material.renderers.controls.MaterialAnyOfStringOrEnumControl
import com.chobantonov.jsonfroms.material.renderers.controls.MaterialEnumControl
import com.chobantonov.jsonfroms.material.renderers.controls.MaterialIntegerControl
import com.chobantonov.jsonfroms.material.renderers.controls.MaterialOneOfEnumControl
import com.chobantonov.jsonfroms.material.renderers.controls.MaterialPasswordControl
import com.chobantonov.jsonfroms.material.renderers.controls.MaterialSliderControl
import com.chobantonov.jsonfroms.material.renderers.complex.MaterialAnyOfRenderer
import com.chobantonov.jsonfroms.material.renderers.complex.MaterialOneOfRenderer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SpecializedSelectionTest {
    private data class Case(
        val name: String,
        val schema: JsonSchema,
        val options: Map<String, JsonPrimitive> = emptyMap(),
        val material: ComposeJsonFormsRenderer,
        val cupertino: ComposeJsonFormsRenderer,
    )

    private fun schema(json: String) = JsonSchema(Json.parseToJsonElement(json).jsonObject)

    private val cases = listOf(
        Case("suggested string",
            schema("""{"anyOf":[{"type":"string","enum":["Red","Blue"]},{"type":"string"}]}"""),
            material = MaterialAnyOfStringOrEnumControl,
            cupertino = CupertinoAnyOfStringOrEnumControl),
        Case("titled finite oneOf",
            schema("""{"oneOf":[{"const":"a","title":"A"},{"const":"b","title":"B"}]}"""),
            material = MaterialOneOfEnumControl, cupertino = CupertinoOneOfEnumControl),
        Case("explicit enum",
            schema("""{"type":"string","enum":["a","b"]}"""),
            material = MaterialEnumControl, cupertino = CupertinoEnumControl),
        Case("password",
            schema("""{"type":"string","format":"password"}"""),
            material = MaterialPasswordControl, cupertino = CupertinoPasswordControl),
        Case("bounded slider",
            schema("""{"type":"integer","minimum":0,"maximum":10,"default":5}"""),
            options = mapOf("slider" to JsonPrimitive(true)),
            material = MaterialSliderControl, cupertino = CupertinoSliderControl),
    )

    private val incompatible = listOf(
        Case("finite-only anyOf is not suggested text",
            schema("""{"anyOf":[{"type":"string","enum":["Red"]},{"type":"string","enum":["Blue"]}]}"""),
            material = MaterialAnyOfRenderer, cupertino = CupertinoAnyOfRenderer),
        Case("object oneOf is not a finite scalar choice",
            schema("""{"oneOf":[{"type":"object","properties":{"a":{"type":"string"}}},{"type":"object","properties":{"b":{"type":"string"}}}]}"""),
            material = MaterialOneOfRenderer, cupertino = CupertinoOneOfRenderer),
        Case("password option requires a string",
            schema("""{"type":"integer"}"""),
            options = mapOf("format" to JsonPrimitive("password")),
            material = MaterialIntegerControl, cupertino = CupertinoIntegerControl),
        Case("slider requires qualified bounds",
            schema("""{"type":"integer","minimum":0,"maximum":10}"""),
            options = mapOf("slider" to JsonPrimitive(true)),
            material = MaterialIntegerControl, cupertino = CupertinoIntegerControl),
    )

    private fun winner(
        entries: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
        case: Case,
    ): ComposeJsonFormsRenderer {
        val ui = ControlElement("#", options = case.options)
        val context = TesterContext(rootSchema = case.schema)
        val ranked = entries.map { it.renderer to it.tester(ui, case.schema, context) }
        val max = ranked.maxOf { it.second }
        assertTrue(max >= 0, case.name)
        return ranked.first { it.second == max }.first
    }

    @Test fun specializedControlsBeatGenericFallbacksInEitherRegistryOrder() {
        for (case in cases + incompatible) {
            assertSame(case.material, winner(materialExtendedRenderers, case), case.name)
            assertSame(case.material, winner(materialExtendedRenderers.reversed(), case), case.name)
            assertSame(case.cupertino, winner(cupertinoExtendedRenderers, case), case.name)
            assertSame(case.cupertino, winner(cupertinoExtendedRenderers.reversed(), case), case.name)
        }
    }
}
