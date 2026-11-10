package com.chobantonov.jsonfroms.extended.renderers

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.UnknownUiSchemaElement
import com.chobantonov.jsonfroms.core.testers.TesterContext
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertNull

class ExtendedRenderersTest {
    private val context = TesterContext()

    @Test
    fun registriesContainBaseAndSevenExtendedEntries() {
        assertEquals(materialExtendedRendererEntries.size, 7)
        assertEquals(cupertinoExtendedRendererEntries.size, 7)
        assertEquals(materialExtendedRenderers.size, materialExtendedRendererEntries.size + 32)
        assertEquals(cupertinoExtendedRenderers.size, cupertinoExtendedRendererEntries.size + 32)
    }

    @Test
    fun controlTestersMatchExtendedFormats() {
        val control = ControlElement("#/properties/value")
        assertEquals(3, colorControlTester(control, rootSchema(schema("string", "color")), context))
        assertEquals(3, durationControlTester(control, rootSchema(schema("string", "duration")), context))
        assertEquals(2, fileControlTester(control, rootSchema(schema("string", "binary")), context))
        assertEquals(2, nullControlTester(control, rootSchema(schema("null")), context))
        assertEquals(
            1,
            buttonTester(UnknownUiSchemaElement("Button"), JsonSchema(), context),
        )
    }

    @Test
    fun extendedRenderersWinSelectionInBothThemes() {
        val root = rootSchema(schema("string", "color"))
        val control = ControlElement("#/properties/value")
        val cupertinoBest = cupertinoExtendedRenderers.maxBy {
            it.tester(control, root, context)
        }
        val materialBest = materialExtendedRenderers.maxBy {
            it.tester(control, root, context)
        }
        assertSame(cupertinoExtendedRendererEntries[1].renderer, cupertinoBest.renderer)
        assertSame(materialExtendedRendererEntries[1].renderer, materialBest.renderer)

        val split = HorizontalLayout(options = mapOf("variant" to JsonPrimitive("splitter")))
        val cupertinoSplit = cupertinoExtendedRenderers.maxBy { it.tester(split, root, context) }
        assertSame(cupertinoExtendedRendererEntries[5].renderer, cupertinoSplit.renderer)
    }

    @Test
    fun fileArrayControlHasPriorityOverArrayRenderer() {
        val item = schema("string", "binary")
        val fileArray = JsonSchema(buildJsonObject {
            put("type", "array")
            put("items", item.toJsonObject())
        })
        val root = rootSchema(fileArray)
        val control = ControlElement("#/properties/value")
        assertEquals(7, fileControlTester(control, root, context))
        val best = materialExtendedRenderers.maxBy { it.tester(control, root, context) }
        assertSame(materialExtendedRendererEntries[3].renderer, best.renderer)
    }

    @Test
    fun fileArrayTesterResolvesReferencedItems() {
        val root = JsonSchema(buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("value", buildJsonObject {
                    put("type", "array")
                    put("items", buildJsonObject { put("\$ref", "#/definitions/file") })
                })
            })
            put("definitions", buildJsonObject { put("file", schema("string", "binary").toJsonObject()) })
        })
        assertEquals(7, fileControlTester(ControlElement("#/properties/value"), root, context))
    }

    @Test
    fun durationPartsRoundTripWithoutDroppingTime() {
        val parts = parseSimpleDuration("P3DT2H15M")
        assertEquals(listOf(3, 2, 15), parts)
        assertEquals("P3DT2H15M", formatSimpleDuration(parts!!))
        assertNull(parseSimpleDuration("P1W"))
    }

    @Test
    fun splitLayoutTesterRequiresItsVariant() {
        val split = HorizontalLayout(options = mapOf("variant" to JsonPrimitive("splitter")))
        assertEquals(5, splitLayoutTester(split, JsonSchema(), context))
    }

    @Test
    fun slotTesterMatchesExtendedSlotElements() {
        assertEquals(2, slotTester(UnknownUiSchemaElement("Slot"), null, context))
    }

    private fun schema(type: String, format: String? = null) = JsonSchema(
        buildJsonObject {
            put("type", type)
            format?.let { put("format", it) }
        },
    )

    private fun rootSchema(property: JsonSchema, name: String = "value") = JsonSchema(
        buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject { put(name, property.toJsonObject()) })
        },
    )
}
