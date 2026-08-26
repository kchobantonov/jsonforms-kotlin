package com.chobantonov.jsonfroms.examples.compose

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.examples.StateProps
import com.chobantonov.jsonfroms.examples.examples.ruleUiSchema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class EditorDraftTest {
    private val originalSchema = JsonSchema(buildJsonObject { put("type", "object") })
    private val originalUiSchema = VerticalLayout(
        elements = listOf(ControlElement(scope = "#/properties/name")),
    )
    private val state = StateProps(
        data = buildJsonObject { put("name", "Ada") },
        schema = originalSchema,
        uischema = originalUiSchema,
    )

    @Test
    fun `invalid schema draft does not produce replacement state`() {
        val result = applyEditorDraft(DemoPane.Schema, "{ invalid", state)

        assertTrue(result.isFailure)
        assertEquals(originalSchema, state.schema)
    }

    @Test
    fun `data draft replaces data only after valid JSON parses`() {
        val invalid = applyEditorDraft(DemoPane.Data, "{ invalid", state)
        val valid = applyEditorDraft(DemoPane.Data, """{"name":"Grace"}""", state)

        assertTrue(invalid.isFailure)
        assertEquals("Ada", state.data.jsonObject["name"]?.jsonPrimitive?.content)
        assertEquals("Grace", valid.getOrThrow().data.jsonObject["name"]?.jsonPrimitive?.content)
    }

    @Test
    fun `blank data draft represents absent data while null remains explicit`() {
        val absent = applyEditorDraft(DemoPane.Data, "", state).getOrThrow()
        val explicitNull = applyEditorDraft(DemoPane.Data, "null", state).getOrThrow()

        assertEquals(JsonNull, absent.data)
        assertFalse(absent.dataProvided)
        assertEquals(JsonNull, explicitNull.data)
        assertTrue(explicitNull.dataProvided)
    }

    @Test
    fun `ui schema must parse and convert before replacement state is produced`() {
        val invalid = applyEditorDraft(DemoPane.UiSchema, """{"type":"Control"}""", state)
        val valid = applyEditorDraft(
            DemoPane.UiSchema,
            """{"type":"Control","scope":"#/properties/name"}""",
            state,
        )

        assertTrue(invalid.isFailure)
        assertEquals(originalUiSchema, state.uischema)
        assertIs<ControlElement>(valid.getOrThrow().uischema)
    }

    @Test
    fun `rule example UI schema can be serialized and applied by the editor`() {
        val ruleState = state.copy(uischema = ruleUiSchema)
        val text = Json.encodeToString(ruleUiSchema.toJsonObject())

        val result = applyEditorDraft(DemoPane.UiSchema, text, ruleState)

        assertTrue(result.isSuccess)
        assertEquals(ruleUiSchema.toJsonObject(), result.getOrThrow().uischema?.toJsonObject())
    }

    @Test
    fun `config draft converts JSON primitives to renderer values`() {
        val result = applyEditorDraft(
            DemoPane.Config,
            """{"restrict":false,"nested":{"count":2}}""",
            state,
        ).getOrThrow()

        assertEquals(false, result.config["restrict"])
        assertEquals(mapOf("count" to 2L), result.config["nested"])
    }
}
