package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonOptions
import com.chobantonov.jsonfroms.core.models.LabelElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.BooleanControlLabel
import com.chobantonov.jsonfroms.core.store.Translator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class ArrayTableCellOptionsTest {
    @Test fun parsesCellSpecificSummaryDetailAndDialog() {
        val options: JsonOptions = Json.parseToJsonElement("""{"cells":{"contact":{
          "summary":{"type":"Control","scope":"#/properties/city"},
          "detail":{"type":"Control","scope":"#"},"dialog":{"width":720}},
          "virtual":{"summary":{"type":"Label","text":"Preview"}}}}""").jsonObject
        val contact = assertNotNull(resolveArrayTableCellOptions(options, "contact"))
        assertEquals("#/properties/city", assertIs<ControlElement>(contact.summary).scope)
        assertIs<ControlElement>(contact.detail)
        assertEquals("720", contact.dialog?.get("width")?.toString())
        assertIs<LabelElement>(resolveArrayTableCellOptions(options, "virtual")?.summary)
        assertNull(resolveArrayTableCellOptions(options, "missing"))
    }

    @Test fun typeIndicatorUsesCellThenCollectionThenTopLevelConfig() {
        val trueOption = Json.parseToJsonElement("""{"showTypeIndicator":true}""").jsonObject
        val falseOption = Json.parseToJsonElement("""{"showTypeIndicator":false}""").jsonObject
        assertEquals(false, resolveArrayTableCellTypeIndicator(null, emptyMap(), emptyMap()))
        assertEquals(true, resolveArrayTableCellTypeIndicator(null, emptyMap(),
            mapOf("showTypeIndicator" to true)))
        assertEquals(false, resolveArrayTableCellTypeIndicator(falseOption, emptyMap(),
            mapOf("showTypeIndicator" to true)))
        assertEquals(true, resolveArrayTableCellTypeIndicator(trueOption,
            mapOf("showTypeIndicator" to Json.parseToJsonElement("false")), emptyMap()))
        assertEquals(false, resolveArrayTableCellTypeIndicator(null,
            mapOf("showTypeIndicator" to Json.parseToJsonElement("false")),
            mapOf("showTypeIndicator" to true)))
        assertEquals(false, resolveArrayTableCellTypeIndicator(null, emptyMap(),
            mapOf("showTypeIndicator" to "true")))
    }

    @Test fun objectSummaryUsesSelectedScalarAndGeneratedFallbacks() {
        val summary = ControlElement("#/properties/city")
        assertEquals("Boston", arrayTableCellPreview(Json.parseToJsonElement("""{"city":"Boston"}"""), summary))
        assertEquals("City not specified", arrayTableCellPreview(Json.parseToJsonElement("""{"city":{}}"""), summary))
        assertEquals("City not specified", arrayTableCellPreview(Json.parseToJsonElement("""{"phone":"555"}"""), summary))
        assertEquals("City not specified", arrayTableCellPreview(Json.parseToJsonElement("""{"city":"  ","phone":"555"}"""), summary))
        assertEquals("0", arrayTableCellPreview(Json.parseToJsonElement("""{"city":0}"""), summary))
        assertEquals("false", arrayTableCellPreview(Json.parseToJsonElement("""{"city":false}"""), summary))
        assertEquals("Empty object", arrayTableCellPreview(Json.parseToJsonElement("""{}"""), summary))
        assertEquals("Not set", arrayTableCellPreview(null, summary))
        assertEquals("Details", arrayTableCellPreview(Json.parseToJsonElement("""{"city":{}}""")))
        assertEquals("Details", arrayTableCellPreview(Json.parseToJsonElement("""{"city":{}}"""),
            summary.copy(label = BooleanControlLabel(false))))
    }

    @Test fun previewDistinguishesDataFromDescriptionsAndUsesLocalizedFieldLabel() {
        val summary = ControlElement("#/properties/city")
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"city":{"type":"string","title":"Town"}}}""").jsonObject)
        val translator = Translator { key, fallback, values ->
            when (key) {
                "Town" -> "Град"
                "composite.summary.unspecified" -> "{label} не е посочено"
                else -> fallback
            }
        }
        assertEquals(ArrayTableCellPreview("Boston", false), arrayTableCellPreviewResult(
            Json.parseToJsonElement("""{"city":"Boston"}"""), summary, translator, schema))
        assertEquals(ArrayTableCellPreview("Град не е посочено", true), arrayTableCellPreviewResult(
            Json.parseToJsonElement("""{"city":null,"phone":"555"}"""), summary, translator, schema))
        assertEquals(ArrayTableCellPreview("Empty object", true), arrayTableCellPreviewResult(
            Json.parseToJsonElement("{}"), summary, translator, schema))
        assertEquals(ArrayTableCellPreview("0 items", true), arrayTableCellPreviewResult(
            Json.parseToJsonElement("[]"), summary, translator, schema))
        assertEquals(ArrayTableCellPreview("0", false), arrayTableCellPreviewResult(
            Json.parseToJsonElement("0"), summary, translator, schema))
    }

    @Test fun arrayPreviewKeepsSkippedItemsInRemainingCountAndPreservesZeroFalse() {
        val values = Json.parseToJsonElement("""[{"name":""},{"name":"Ada"},{"name":0},{"name":false}]""")
        assertEquals("Ada, 0 (+2 more)", arrayTableCellPreview(values, ControlElement("#/properties/name")))
        assertEquals("4 items", arrayTableCellPreview(values))
        assertEquals("0 items", arrayTableCellPreview(Json.parseToJsonElement("[]")))
        assertEquals("555, 0 (+1 more)", arrayTableCellPreview(Json.parseToJsonElement(
            """["555",0,false]"""), ControlElement("#")))
    }
}
