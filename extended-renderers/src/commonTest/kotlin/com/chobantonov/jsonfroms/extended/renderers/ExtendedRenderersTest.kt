package com.chobantonov.jsonfroms.extended.renderers

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.models.LabelElement
import com.chobantonov.jsonfroms.core.store.JsonFormsUISchemaRegistryEntry
import com.chobantonov.jsonfroms.core.models.UnknownUiSchemaElement
import com.chobantonov.jsonfroms.core.testers.TesterContext
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoCells
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialCells
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
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
    fun registriesContainBaseAndFifteenExtendedEntries() {
        assertEquals(15, materialExtendedRendererEntries.size)
        assertEquals(15, cupertinoExtendedRendererEntries.size)
        assertEquals(materialExtendedRenderers.size, materialExtendedRendererEntries.size + materialRenderers.size)
        assertEquals(cupertinoExtendedRenderers.size, cupertinoExtendedRendererEntries.size + cupertinoRenderers.size)
        assertEquals(materialCells.size + 5, materialExtendedCells.size)
        assertEquals(cupertinoCells.size + 5, cupertinoExtendedCells.size)
        val nullSchema = rootSchema(schema("null"))
        val fileSchema = rootSchema(schema("string", "binary"))
        val control = ControlElement("#/properties/value")
        assertEquals(2, materialExtendedCells.single { it.tester(control, nullSchema, context) >= 0 }
            .tester(control, nullSchema, context))
        assertEquals(102, cupertinoExtendedCells.single { it.tester(control, nullSchema, context) >= 0 }
            .tester(control, nullSchema, context))
        assertEquals(2, materialExtendedCells.single { it.tester(control, fileSchema, context) >= 0 }
            .tester(control, fileSchema, context))
        assertEquals(102, cupertinoExtendedCells.single { it.tester(control, fileSchema, context) >= 0 }
            .tester(control, fileSchema, context))
        val templateLayout = UnknownUiSchemaElement("TemplateLayout")
        assertEquals(2, materialExtendedRendererEntries.single {
            it.tester(templateLayout, JsonSchema(), context) >= 0
        }.tester(templateLayout, JsonSchema(), context))
        assertEquals(102, cupertinoExtendedRendererEntries.single {
            it.tester(templateLayout, JsonSchema(), context) >= 0
        }.tester(templateLayout, JsonSchema(), context))
        val code = ControlElement("#/properties/value", options = mapOf("format" to JsonPrimitive("code")))
        assertEquals(30, materialExtendedRendererEntries.single {
            it.tester(code, fileSchema, context) == 30
        }.tester(code, fileSchema, context))
        assertEquals(130, cupertinoExtendedRendererEntries.single {
            it.tester(code, fileSchema, context) == 130
        }.tester(code, fileSchema, context))
    }

    @Test
    fun controlTestersMatchExtendedFormats() {
        val control = ControlElement("#/properties/value")
        assertEquals(3, colorControlTester(control, rootSchema(schema("string", "color")), context))
        assertEquals(3, durationControlTester(control, rootSchema(schema("string", "duration")), context))
        assertEquals(3, cronControlTester(control, rootSchema(schema("string", "cron")), context))
        assertEquals(3, cronControlTester(control.copy(options = mapOf("format" to JsonPrimitive("cron"))),
            rootSchema(schema("string")), context))
        assertEquals(2, fileControlTester(control, rootSchema(schema("string", "binary")), context))
        assertEquals(2, nullControlTester(control, rootSchema(schema("null")), context))
        val nullableString = JsonSchema(buildJsonObject {
            put("type", kotlinx.serialization.json.buildJsonArray {
                add(JsonPrimitive("null"))
                add(JsonPrimitive("string"))
            })
        })
        assertEquals(-1, nullControlTester(control, rootSchema(nullableString), context))
        assertEquals(
            1,
            buttonTester(UnknownUiSchemaElement("Button"), JsonSchema(), context),
        )
    }

    @Test
    fun buttonSemanticColorsKeepAllPortableNames() {
        assertEquals(ButtonSemanticStyle.Primary, buttonSemanticStyle(null))
        assertEquals(ButtonSemanticStyle.Primary, buttonSemanticStyle("primary"))
        assertEquals(ButtonSemanticStyle.Secondary, buttonSemanticStyle("secondary"))
        assertEquals(ButtonSemanticStyle.Alternative, buttonSemanticStyle("alternative"))
        assertEquals(ButtonSemanticStyle.Success, buttonSemanticStyle("success"))
        assertEquals(ButtonSemanticStyle.Warning, buttonSemanticStyle("warning"))
        assertEquals(ButtonSemanticStyle.Error, buttonSemanticStyle("error"))
        assertEquals(ButtonSemanticStyle.Primary, buttonSemanticStyle("#ff0000"))
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
        assertSame(cupertinoExtendedRendererEntries[8].renderer, cupertinoSplit.renderer)
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
        assertSame(materialExtendedRendererEntries[4].renderer, best.renderer)
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
        val parts = parseDuration("P3DT2H15M")
        assertEquals(DurationParts(days = 3, hours = 2, minutes = 15), parts)
        assertEquals("P3DT2H15M", formatDuration(parts!!))
        assertEquals(DurationParts(weeks = 1), parseDuration("P1W"))
    }

    @Test
    fun durationSupportsUnboundedComponentsAndWeeksOnly() {
        val large = "P23Y51M724DT92H150M3600S"
        assertEquals(large, formatDuration(parseDuration(large)!!))
        assertEquals("P9007199254740991W", formatDuration(parseDuration("P9007199254740991W")!!))
        assertNull(parseDuration("P9007199254740992W"))
        assertNull(parseDuration("P1W2D"))
        assertNull(parseDuration("P1.5D"))
        assertNull(parseDuration("-P1D"))
        assertNull(parseDuration("PT"))
        assertNull(parseDuration("P"))
    }

    @Test
    fun durationFormattingOnlyCanonicalizesAnExplicitEdit() {
        assertEquals(DurationParts(minutes = 90), parseDuration("PT090M"))
        assertEquals("PT90M", formatDuration(DurationParts(minutes = 90)))
        assertEquals(DurationParts(hours = 1, minutes = 30), parseDuration("PT1H30M"))
        assertEquals(DurationParts(weeks = 0), parseDuration("P0W"))
        assertEquals("P0D", formatDuration(DurationParts(weeks = 0)))
        assertEquals("P0D", formatDuration(DurationParts()))
    }

    @Test
    fun splitLayoutTesterRequiresItsVariant() {
        val split = HorizontalLayout(options = mapOf("variant" to JsonPrimitive("splitter")))
        assertEquals(5, splitLayoutTester(split, JsonSchema(), context))
        assertEquals(5, splitLayoutTester(VerticalLayout(options = split.options), JsonSchema(), context))
        assertEquals(-1, splitLayoutTester(split.copy(options = mapOf(
            "variant" to JsonPrimitive("SPLITTER"))), JsonSchema(), context))
        assertEquals(-1, splitLayoutTester(split.copy(options = mapOf(
            "variant" to JsonPrimitive("filled"))), JsonSchema(), context))
    }

    @Test
    fun namedTemplateLookupRejectsAmbiguousExactNameWithoutUsingTesterRank() {
        val first = GroupElement(name = "card")
        val second = GroupElement(name = "card")
        val registry = listOf(
            JsonFormsUISchemaRegistryEntry({ _, _, _ -> error("Name lookup must not call tester") }, first),
            JsonFormsUISchemaRegistryEntry({ _, _, _ -> 999 }, second),
        )
        assertNull(resolveNamedTemplate("card", registry).template)
        assertEquals(TemplateDiagnosticCode.DuplicateName, resolveNamedTemplate("card", registry).diagnostic)
        assertEquals(TemplateDiagnosticCode.MissingTemplate, resolveNamedTemplate("Card", registry).diagnostic)
        assertEquals(TemplateDiagnosticCode.MissingName, resolveNamedTemplate(null, registry).diagnostic)
        assertEquals(TemplateDiagnosticCode.RecursiveTemplate, resolveNamedTemplate("card", registry, setOf("card")).diagnostic)
    }

    @Test
    fun namedSlotContentsOverrideInheritedAndLaterChildrenWin() {
        val inherited = LabelElement("Inherited", name = "heading")
        val earlier = LabelElement("Earlier", name = "heading")
        val latest = LabelElement("Latest", name = "heading")
        val unnamed = LabelElement("Unnamed")
        val unique = mergeNamedSlots(mapOf("heading" to inherited), listOf(earlier, unnamed))
        assertSame(earlier, unique["heading"])
        val replaced = mergeNamedSlots(mapOf("heading" to inherited), listOf(earlier, unnamed, latest))
        assertSame(latest, replaced["heading"])
        assertEquals(setOf("heading"), replaced.keys)
    }

    @Test
    fun slotTesterMatchesExtendedSlotElements() {
        assertEquals(2, slotTester(UnknownUiSchemaElement("Slot"), null, context))
        assertEquals(2, templateTester(UnknownUiSchemaElement("Template"), null, context))
        assertEquals(2, imageViewTester(UnknownUiSchemaElement("ImageView"), null, context))
    }

    @Test
    fun imageDimensionsAcceptPositivePixelsOnly() {
        assertEquals(640f, imageDimensionPixels(JsonPrimitive(640)))
        assertEquals(320f, imageDimensionPixels(JsonPrimitive("320px")))
        assertNull(imageDimensionPixels(JsonPrimitive("50%")))
        assertNull(imageDimensionPixels(JsonPrimitive(-1)))
        assertEquals(1f, imageWidthFraction(JsonPrimitive("100%")))
        assertEquals(0.5f, imageWidthFraction(JsonPrimitive("50%")))
        assertNull(imageWidthFraction(JsonPrimitive("101%")))
        assertNull(imageWidthFraction(JsonPrimitive("0%")))
        assertNull(imageWidthFraction(JsonPrimitive("50px")))
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
