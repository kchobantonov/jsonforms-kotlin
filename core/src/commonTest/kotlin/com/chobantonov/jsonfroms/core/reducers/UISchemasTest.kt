package com.chobantonov.jsonfroms.core.reducers

import com.chobantonov.jsonfroms.core.actions.Actions
import com.chobantonov.jsonfroms.core.actions.RemoveUISchemaAction
import com.chobantonov.jsonfroms.core.generators.Generate
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsUISchemaRegistryEntry
import com.chobantonov.jsonfroms.core.testers.UiSchemaTester
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonPrimitive

class UISchemasTest {
    @Test
    fun `init state empty`() {
        val dummyAction = RemoveUISchemaAction(
            tester = { _, _, _ -> 0 },
        )

        assertEquals(emptyList(), uischemaRegistryReducer(state = emptyList(), action = dummyAction))
    }

    @Test
    fun `add ui schema`() {
        val tester: UiSchemaTester = { _, _, _ -> 1 }
        val control = ControlElement(scope = "#/definitions/foo")

        val after = uischemaRegistryReducer(
            state = null,
            action = Actions.registerUISchema(tester, control),
        )

        assertEquals(1, after.size)
    }

    @Test
    fun `remove ui schema`() {
        val tester: UiSchemaTester = { _, _, _ -> 1 }
        val control = ControlElement(scope = "#/definitions/foo")

        val after = uischemaRegistryReducer(
            state = listOf(
                JsonFormsUISchemaRegistryEntry(
                    tester = tester,
                    uischema = control,
                ),
            ),
            action = Actions.unregisterUISchema(tester),
        )

        assertEquals(0, after.size)
    }

    @Test
    fun `findMatchingUISchema`() {
        val testerA: UiSchemaTester = { _, schemaPath, _ -> if (schemaPath.endsWith("foo")) 1 else 0 }
        val testerB: UiSchemaTester = { _, schemaPath, _ -> if (schemaPath.endsWith("bar")) 1 else 0 }
        val controlA = ControlElement(scope = "#/definitions/foo")
        val controlB = ControlElement(scope = "#/definitions/bar")
        val before = listOf(
            JsonFormsUISchemaRegistryEntry(testerA, controlA),
            JsonFormsUISchemaRegistryEntry(testerB, controlB),
        )

        assertEquals(controlA, findMatchingUISchema(before)(null, "#/defintions/foo", ""))
        assertEquals(controlB, findMatchingUISchema(before)(null, "#/defintions/bar", ""))
    }

    @Test
    fun `findUISchema returns generated UI schema if no match has been found`() {
        val schema = buildJsonSchema {
            put("definitions", kotlinx.serialization.json.buildJsonObject {
                put("baz", kotlinx.serialization.json.buildJsonObject {
                    put("type", JsonPrimitive("number"))
                })
            })
        }

        assertEquals(
            Generate.uiSchema(schema),
            findUISchema(
                emptyList(),
                schema,
                "#/definitions/baz",
                "",
            ),
        )
    }

    @Test
    fun `findMatchingUISchema with highest priority`() {
        val testerA: UiSchemaTester = { _, schemaPath, _ -> if (schemaPath.endsWith("foo")) 2 else 0 }
        val testerB: UiSchemaTester = { _, schemaPath, _ -> if (schemaPath.endsWith("foo")) 1 else 0 }
        val controlA = ControlElement(scope = "#/definitions/foo")
        val controlB = ControlElement(scope = "#/definitions/foo")
        val before = listOf(
            JsonFormsUISchemaRegistryEntry(testerA, controlA),
            JsonFormsUISchemaRegistryEntry(testerB, controlB),
        )

        assertEquals(controlA, findMatchingUISchema(before)(null, "#/definitions/foo", ""))
    }
}
