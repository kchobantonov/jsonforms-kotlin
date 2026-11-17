package com.chobantonov.jsonfroms.extended.renderers

import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry
import com.chobantonov.jsonfroms.core.testers.TesterContext
import com.chobantonov.jsonfroms.cupertino.renderers.controls.CupertinoDateControl
import com.chobantonov.jsonfroms.cupertino.renderers.controls.CupertinoDateTimeControl
import com.chobantonov.jsonfroms.cupertino.renderers.controls.CupertinoPasswordControl
import com.chobantonov.jsonfroms.cupertino.renderers.controls.CupertinoTextControl
import com.chobantonov.jsonfroms.material.renderers.controls.MaterialDateControl
import com.chobantonov.jsonfroms.material.renderers.controls.MaterialDateTimeControl
import com.chobantonov.jsonfroms.material.renderers.controls.MaterialPasswordControl
import com.chobantonov.jsonfroms.material.renderers.controls.MaterialTextControl
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class FormatConflictSelectionTest {
    private val codeIndex = materialExtendedRendererEntries.indexOfFirst {
        it.tester === codeProfileFallbackTester
    }
    private fun winner(
        entries: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
        schemaFormat: String,
        uiFormat: String?,
        config: Map<String, Any?> = emptyMap(),
    ): Pair<ComposeJsonFormsRenderer, Int> {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"string","format":"$schemaFormat"}""").jsonObject)
        val control = ControlElement("#", options = uiFormat?.let { mapOf("format" to JsonPrimitive(it)) }
            ?: emptyMap())
        val context = TesterContext(rootSchema = schema, config = config)
        var best: Pair<ComposeJsonFormsRenderer, Int>? = null
        for (entry in entries) {
            val rank = entry.tester(control, schema, context)
            if (best == null || rank > best.second) best = entry.renderer to rank
        }
        return requireNotNull(best)
    }

    @Test fun configPresentationValuesDoNotSelectRenderers() {
        val config = mapOf<String, Any?>("format" to "password", "multi" to true,
            "variant" to "chips")
        assertSame(MaterialTextControl,
            winner(materialExtendedRenderers, "unknown", null, config).first)
        assertSame(CupertinoTextControl,
            winner(cupertinoExtendedRenderers, "unknown", null, config).first)
        assertSame(MaterialDateControl,
            winner(materialExtendedRenderers, "date", null, config).first)
        assertSame(CupertinoDateControl,
            winner(cupertinoExtendedRenderers, "date", null, config).first)
        assertSame(MaterialPasswordControl,
            winner(materialExtendedRenderers, "unknown", "password",
                mapOf("format" to "date")).first)
        assertSame(CupertinoPasswordControl,
            winner(cupertinoExtendedRenderers, "unknown", "password",
                mapOf("format" to "date")).first)
    }

    @Test fun materialConflictRanksAndFallbackAreStable() {
        val cases = listOf(
            Triple("date" to "time", MaterialDateControl, 4),
            Triple("time" to "date", MaterialDateControl, 4),
            Triple("date-time" to "date", MaterialDateControl, 4),
            Triple("date" to "password", MaterialDateControl, 4),
            Triple("date-time" to "password", MaterialDateTimeControl, 2),
            Triple("password" to "date-time", MaterialDateTimeControl, 2),
            Triple("password" to "color", materialExtendedControls.color, 3),
            Triple("color" to "date", MaterialDateControl, 4),
            Triple("duration" to "color", materialExtendedControls.color, 3),
            Triple("color" to "duration", materialExtendedControls.color, 3),
            Triple("cron" to "duration", materialExtendedControls.duration, 3),
            Triple("duration" to "cron", materialExtendedControls.duration, 3),
            Triple("date-time" to "cron", materialExtendedControls.cron, 3),
            Triple("binary" to "date", MaterialDateControl, 4),
            Triple("byte" to "password", MaterialPasswordControl, 2),
            Triple("binary" to "unknown", materialExtendedControls.file, 2),
            Triple("date" to "code", materialExtendedRendererEntries[codeIndex].renderer, 30),
            Triple("unknown" to "unknown", MaterialTextControl, 1),
        )
        for ((formats, renderer, rank) in cases) {
            val (actualRenderer, actualRank) = winner(materialExtendedRenderers, formats.first, formats.second)
            assertSame(renderer, actualRenderer, "${formats.first} / ${formats.second}")
            assertEquals(rank, actualRank, "${formats.first} / ${formats.second}")
        }
        assertSame(MaterialPasswordControl, winner(materialExtendedRenderers, "password", "unknown").first)
    }

    @Test fun cupertinoConflictRanksAndFallbackAreStable() {
        val cases = listOf(
            Triple("date" to "time", CupertinoDateControl, 104),
            Triple("time" to "date", CupertinoDateControl, 104),
            Triple("date-time" to "date", CupertinoDateControl, 104),
            Triple("date" to "password", CupertinoDateControl, 104),
            Triple("date-time" to "password", CupertinoDateTimeControl, 102),
            Triple("password" to "date-time", CupertinoDateTimeControl, 102),
            Triple("password" to "color", cupertinoExtendedControls.color, 103),
            Triple("color" to "date", CupertinoDateControl, 104),
            Triple("duration" to "color", cupertinoExtendedControls.color, 103),
            Triple("color" to "duration", cupertinoExtendedControls.color, 103),
            Triple("cron" to "duration", cupertinoExtendedControls.duration, 103),
            Triple("duration" to "cron", cupertinoExtendedControls.duration, 103),
            Triple("date-time" to "cron", cupertinoExtendedControls.cron, 103),
            Triple("binary" to "date", CupertinoDateControl, 104),
            Triple("byte" to "password", CupertinoPasswordControl, 102),
            Triple("binary" to "unknown", cupertinoExtendedControls.file, 102),
            Triple("date" to "code", cupertinoExtendedRendererEntries[codeIndex].renderer, 130),
            Triple("unknown" to "unknown", CupertinoTextControl, 101),
        )
        for ((formats, renderer, rank) in cases) {
            val (actualRenderer, actualRank) = winner(cupertinoExtendedRenderers, formats.first, formats.second)
            assertSame(renderer, actualRenderer, "${formats.first} / ${formats.second}")
            assertEquals(rank, actualRank, "${formats.first} / ${formats.second}")
        }
        assertSame(CupertinoPasswordControl, winner(cupertinoExtendedRenderers, "password", "unknown").first)
    }
}
