package com.chobantonov.jsonfroms.extended.renderers

import com.chobantonov.jsonfroms.compose.ComposeJsonFormsCellRenderer
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsCellRendererRegistryEntry
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.TesterContext
import com.chobantonov.jsonfroms.core.testers.plainConstCellTester
import com.chobantonov.jsonfroms.core.testers.plainEnumCellTester
import com.chobantonov.jsonfroms.core.testers.plainNumericCellTester
import com.chobantonov.jsonfroms.core.testers.plainOneOfEnumCellTester
import com.chobantonov.jsonfroms.core.testers.plainPasswordCellTester
import com.chobantonov.jsonfroms.core.testers.plainStringCellTester
import com.chobantonov.jsonfroms.core.testers.plainTemporalCellTester
import com.chobantonov.jsonfroms.core.testers.radioChoiceCellTester
import com.chobantonov.jsonfroms.core.testers.sliderCellTester
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CellSelectionTest {
    private data class Case(
        val name: String,
        val schema: JsonSchema,
        val options: Map<String, JsonPrimitive> = emptyMap(),
        val tester: RankedTester,
    )

    private fun schema(json: String) = JsonSchema(Json.parseToJsonElement(json).jsonObject)

    private val supported = listOf(
        Case("plain string", schema("""{"type":"string"}"""), tester = plainStringCellTester),
        Case("plain integer", schema("""{"type":"integer"}"""), tester = plainNumericCellTester),
        Case("plain enum", schema("""{"type":"string","enum":["a","b"]}"""),
            tester = plainEnumCellTester),
        Case("const oneOf", schema("""{"oneOf":[{"const":"a","title":"A"},{"const":"b","title":"B"}]}"""),
            tester = plainOneOfEnumCellTester),
        Case("scalar const", schema("""{"const":"ready"}"""), tester = plainConstCellTester),
        Case("radio enum", schema("""{"type":"string","enum":["a","b"]}"""),
            mapOf("format" to JsonPrimitive("radio")), radioChoiceCellTester),
        Case("bounded slider", schema("""{"type":"integer","minimum":0,"maximum":10,"default":5}"""),
            mapOf("slider" to JsonPrimitive(true)), sliderCellTester),
        Case("date", schema("""{"type":"string","format":"date"}"""), tester = plainTemporalCellTester),
        Case("password", schema("""{"type":"string","format":"password"}"""),
            tester = plainPasswordCellTester),
    )

    private fun winner(
        entries: List<JsonFormsCellRendererRegistryEntry<ComposeJsonFormsCellRenderer>>,
        schema: JsonSchema,
        options: Map<String, JsonPrimitive> = emptyMap(),
    ): JsonFormsCellRendererRegistryEntry<ComposeJsonFormsCellRenderer>? {
        val ui = ControlElement("", options = options)
        val context = TesterContext(rootSchema = schema)
        val ranked = entries.map { it to it.tester(ui, schema, context) }
        val max = ranked.maxOf { it.second }
        return ranked.firstOrNull { it.second == max }?.first?.takeIf { max >= 0 }
    }

    @Test fun compactSpecialtiesWinInBothCellRegistries() {
        for (case in supported) {
            for (entries in listOf(materialExtendedCells, materialExtendedCells.reversed(),
                cupertinoExtendedCells, cupertinoExtendedCells.reversed())) {
                val selected = winner(entries, case.schema, case.options)
                assertSame(case.tester, selected?.tester, case.name)
            }
        }
    }

    @Test fun extendedNullAndFileCellsOwnTheirFeedback() {
        val nullSchema = schema("""{"type":"null"}""")
        val fileSchema = schema("""{"type":"string","format":"binary"}""")
        for (original in listOf(materialExtendedCells, cupertinoExtendedCells)) {
            val nullEntry = original[original.lastIndex - 1]
            val fileEntry = original.last()
            for (entries in listOf(original, original.reversed())) {
                assertSame(nullEntry, winner(entries, nullSchema))
                assertSame(fileEntry, winner(entries, fileSchema))
                assertTrue(winner(entries, nullSchema)?.ownsValidationFeedback == true)
                assertTrue(winner(entries, fileSchema)?.ownsValidationFeedback == true)
            }
        }
    }

    @Test fun incompatibleCellPresentationsRemainUnclaimed() {
        val cases = listOf(
            schema("""{"type":"string","format":"uuid"}""") to emptyMap(),
            schema("""{"type":"string","format":"date"}""") to
                mapOf("format" to JsonPrimitive("password")),
            schema("""{"oneOf":[{"type":"object"},{"type":"array"}]}""") to emptyMap(),
        )
        for ((schema, options) in cases) {
            assertNull(winner(materialExtendedCells, schema, options))
            assertNull(winner(cupertinoExtendedCells, schema, options))
        }
    }
}
