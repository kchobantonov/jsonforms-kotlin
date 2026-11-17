package com.chobantonov.jsonfroms.core.confirmation

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfirmationPolicyTest {
    private fun value(text: String): JsonElement = Json.parseToJsonElement(text)
    private fun decision(
        operation: ConfirmationOperation = ConfirmationOperation.Delete,
        catalogId: String = "arrayTable",
        values: List<JsonElement?>,
        options: Map<String, JsonElement> = emptyMap(),
        config: Map<String, Any?> = emptyMap(),
    ) = resolveConfirmationDecision(operation, catalogId, values, options, config)

    @Test
    fun documentedFallbackUsesOldValueAndDistinguishesAbsentFromNull() {
        assertEquals(ConfirmationMode.Complex, decision(values = listOf(value("{}"))).mode)
        assertFalse(decision(values = listOf(value("{}"))).required)
        assertTrue(decision(values = listOf(value("{\"child\":null}"))).required)
        assertFalse(decision(values = listOf(JsonNull)).required)
        assertEquals(ConfirmationMode.Always,
            decision(ConfirmationOperation.BranchChange, "oneOf", listOf(JsonNull)).mode)
        assertTrue(decision(ConfirmationOperation.BranchChange, "oneOf", listOf(JsonNull)).required)
        assertFalse(decision(ConfirmationOperation.BranchChange, "oneOf", listOf(null)).required)
        assertEquals(ConfirmationMode.Complex,
            decision(ConfirmationOperation.TypeChange, "mixed", listOf(value("[]"))).mode)
        assertFalse(decision(ConfirmationOperation.TypeChange, "mixed", listOf(value("[]"))).required)
    }

    @Test
    fun elementThenRendererThenGlobalThenFallbackAndInvalidValuesFallThrough() {
        val config = mapOf("jsonformsExtended" to mapOf("confirmation" to mapOf(
            "default" to "always",
            "renderers" to mapOf("arrayTable" to mapOf("delete" to "never")),
        )))
        assertEquals(ConfirmationMode.Never, decision(values = listOf(value("1")), config = config).mode)
        val element = mapOf("confirmation" to value("""{"delete":"complex"}"""))
        assertEquals(ConfirmationMode.Complex,
            decision(values = listOf(value("{\"x\":1}")), options = element, config = config).mode)
        val typo = mapOf("confirmation" to value("""{"delete":"alwayz"}"""))
        assertEquals(ConfirmationMode.Never,
            decision(values = listOf(value("1")), options = typo, config = config).mode)
        assertEquals(ConfirmationMode.Always,
            decision(values = listOf(value("1")), config = mapOf("jsonformsExtended" to mapOf(
                "confirmation" to mapOf("default" to "always"),
            ))).mode)
    }

    @Test
    fun serializedNamespacedConfirmationUsesLiteralWireKey() {
        val settings = Json.parseToJsonElement("""{
            "confirmation":{"default":"always","renderers":{"arrayTable":{"delete":"never"}}}
        }""").jsonObject
        val discarded = listOf(value("1"))
        val namespaced = mapOf("jsonformsExtended" to settings)
        assertEquals(ConfirmationMode.Never, decision(values = discarded, config = namespaced).mode)
        assertEquals(ConfirmationMode.Always,
            decision(catalogId = "other", values = discarded, config = namespaced).mode)
        assertEquals(ConfirmationMode.Complex, decision(values = discarded,
            config = mapOf("confirmation" to settings["confirmation"])).mode)
        assertEquals(ConfirmationMode.Complex, decision(values = discarded,
            config = mapOf("jsonFormsExtended" to settings)).mode)
        assertEquals(ConfirmationMode.Always, decision(values = discarded,
            options = mapOf("confirmation" to value("""{"delete":"always"}""")),
            config = namespaced).mode)
    }

    @Test
    fun alwaysCountsFalsyAndEmptyValuesButComplexChecksAnyDiscardedContainer() {
        val always = mapOf("confirmation" to value("""{"delete":"always"}"""))
        listOf("false", "0", "\"\"", "{}", "[]", "null").forEach { old ->
            assertTrue(decision(values = listOf(value(old)), options = always).required, old)
        }
        assertTrue(decision(values = listOf(value("0"), value("[]"), value("[1]"))).required)
        assertFalse(decision(values = listOf(value("0"), value("[]"), value("{}"))).required)
        assertFalse(decision(values = emptyList(), options = always).required)
    }
}
