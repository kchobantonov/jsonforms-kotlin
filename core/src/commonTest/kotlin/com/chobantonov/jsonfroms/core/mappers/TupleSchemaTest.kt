package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.JsonSchemaTypeValue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TupleSchemaTest {
    private fun schema(text: String) = JsonSchema(Json.parseToJsonElement(text).jsonObject)

    @Test
    fun draftSevenKeepsFixedPositionsAndTypedTail() {
        val tuple = assertNotNull(resolveTupleSchema(schema(
            """{"type":"array","items":[{"type":"string","title":"Code"},{"type":"integer","title":"Count"}],"additionalItems":{"type":"boolean"}}"""
        )))
        assertEquals(2, tuple.fixedCount)
        assertEquals("Code", tuple.schemaAt(0)?.title)
        assertEquals("Count", tuple.schemaAt(1)?.title)
        assertIs<TupleTail.Typed>(tuple.tail)
        assertEquals("boolean", tuple.schemaAt(2)?.get("type")?.toString()?.trim('"'))
        assertFalse(tuple.canRemoveTrailing(1, 3))
        assertTrue(tuple.canRemoveTrailing(2, 3))
        assertFalse(tuple.canRemoveTrailing(3, 3))
    }

    @Test
    fun modernPrefixUsesItemsForTailAndPreservesBooleanSlots() {
        val tuple = assertNotNull(resolveTupleSchema(schema(
            """{"type":"array","prefixItems":[true,false,{"type":"number"}],"items":false}"""
        )))
        assertEquals(3, tuple.fixedCount)
        assertEquals(null, tuple.schemaAt(0)?.get("not"))
        assertNotNull(tuple.schemaAt(1)?.get("not"))
        assertEquals("number", tuple.schemaAt(2)?.get("type")?.toString()?.trim('"'))
        assertEquals(TupleTail.Forbidden, tuple.tail)
        assertNull(tuple.schemaAt(3))
        assertTrue(tuple.canRemoveTrailing(3, 4))
        assertFalse(tuple.isFixed(3))
    }

    @Test
    fun uniformEqualBoundsRequireExplicitTupleVariant() {
        val uniform = schema("""{"type":"array","items":{"type":"number"},"minItems":3,"maxItems":3}""")
        assertNull(resolveTupleSchema(uniform))
        val tuple = assertNotNull(resolveTupleSchema(uniform, explicitVariant = true))
        assertEquals(3, tuple.fixedCount)
        assertEquals(TupleTail.Forbidden, tuple.tail)
        assertEquals("number", tuple.schemaAt(2)?.get("type")?.toString()?.trim('"'))
        assertTrue(tuple.canRemoveTrailing(3, 4))
        assertNull(resolveTupleSchema(schema("""{"type":"array","items":{"type":"number"},"minItems":2,"maxItems":3}"""), true))
        assertNull(resolveTupleSchema(schema("""{"type":"array","items":{"type":"number"}}"""), true))
        assertNull(resolveTupleSchema(schema("""{"type":"string","minItems":2,"maxItems":2}"""), true))
    }

    @Test
    fun prefixInitializationIsConservativeAndRespectsDefaults() {
        val root = schema("""{"type":"object"}""")
        assertEquals(JsonPrimitive(""), tupleInitialValue(schema("""{"type":"string"}"""), root))
        assertEquals(JsonPrimitive(0), tupleInitialValue(schema("""{"type":"integer"}"""), root))
        assertEquals(JsonPrimitive("saved"), tupleInitialValue(
            schema("""{"type":["string","number"],"default":"saved"}"""), root,
        ))
        assertNull(tupleInitialValue(schema("""{"type":["string","number"]}"""), root))
        assertNull(tupleInitialValue(schema("""{"oneOf":[{"type":"string"},{"type":"number"}]}"""), root))
    }

    @Test
    fun authoredPositionScopesAcceptBothDraftSpellingsAndRejectUndeclaredIndices() {
        val tuple = assertNotNull(resolveTupleSchema(schema(
            """{"type":"array","items":[{"type":"object"},{"type":"string"}]}"""
        )))
        assertEquals(TuplePositionScope.Whole(1), resolveTuplePositionScope("#/items/1", tuple))
        assertEquals(TuplePositionScope.Whole(1), resolveTuplePositionScope("#/prefixItems/1", tuple))
        assertEquals(TuplePositionScope.Nested(0), resolveTuplePositionScope("#/items/0/properties/city", tuple))
        assertEquals(TuplePositionScope.Invalid, resolveTuplePositionScope("#/items/2", tuple))
        assertEquals(TuplePositionScope.Invalid, resolveTuplePositionScope("#/items/-1", tuple))
        assertEquals(TuplePositionScope.Other, resolveTuplePositionScope("#/properties/city", tuple))
    }

    @Test
    fun presentationOptionsRespectTupleNamespaceAndExplicitFalse() {
        assertEquals(TuplePresentation(false, true), resolveTuplePresentation(emptyMap(), emptyMap()))
        val config = mapOf(
            "vertical" to true,
            "jsonformsExtended" to mapOf(
                "vertical" to true,
                "tuple" to mapOf("vertical" to true, "showBorder" to false),
            ),
        )
        assertEquals(TuplePresentation(true, false), resolveTuplePresentation(emptyMap(), config))
        assertEquals(TuplePresentation(false, true), resolveTuplePresentation(
            mapOf("vertical" to JsonPrimitive(false), "showBorder" to JsonPrimitive(true)), config,
        ))
        assertEquals(TuplePresentation(false, true), resolveTuplePresentation(
            emptyMap(), mapOf("vertical" to true, "jsonformsExtended" to mapOf("vertical" to true)),
        ))
        assertEquals(TuplePresentation(true, false), resolveTuplePresentation(
            mapOf("vertical" to JsonPrimitive("false"), "showBorder" to JsonPrimitive("true")), config,
        ))
    }

    @Test
    fun quotedSchemaBooleansDoNotBecomeTupleSchemaFlags() {
        val tuple = assertNotNull(resolveTupleSchema(schema(
            """{"type":"array","prefixItems":["false"],"items":"false"}"""
        )))
        assertNull(tuple.schemaAt(0))
        assertEquals(TupleTail.Unconstrained, tuple.tail)
    }

    @Test
    fun missingTailKeywordIsUnconstrainedAndUniformItemsAreNotPositional() {
        val tuple = assertNotNull(resolveTupleSchema(schema(
            """{"type":"array","items":[{"type":"string"}]}"""
        )))
        assertEquals(TupleTail.Unconstrained, tuple.tail)
        assertIs<JsonSchemaTypeValue.Multiple>(assertNotNull(tuple.schemaAt(9)).typeValue)
        assertNull(resolveTupleSchema(schema(
            """{"type":"array","items":{"type":"number"},"minItems":2,"maxItems":2}"""
        )))
    }
}
