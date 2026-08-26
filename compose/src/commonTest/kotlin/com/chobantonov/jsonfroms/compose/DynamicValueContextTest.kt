package com.chobantonov.jsonfroms.compose

import com.chobantonov.jsonfroms.core.actions.InitAction
import com.chobantonov.jsonfroms.core.actions.SetConfigAction
import com.chobantonov.jsonfroms.core.actions.UpdateAction
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DynamicValueContextTest {
    @Test
    fun `clear keeps a dynamic property present with its empty value`() {
        var dispatched: Any? = null
        val dispatch = preserveDynamicValueOnClear(
            dispatch = { dispatched = it },
            path = "dynamic",
            emptyValue = JsonPrimitive(""),
        )

        dispatch(UpdateAction("dynamic", updater = { null }))

        val update = dispatched as UpdateAction
        assertEquals(JsonPrimitive(""), update.updater(JsonPrimitive("value")))
    }

    @Test
    fun `guard does not alter other updates or actions`() {
        val dispatched = mutableListOf<Any>()
        val dispatch = preserveDynamicValueOnClear(
            dispatch = { dispatched += it },
            path = "dynamic",
            emptyValue = JsonPrimitive(""),
        )
        val siblingUpdate = UpdateAction("sibling", updater = { null })
        val configAction = SetConfigAction(emptyMap())

        dispatch(siblingUpdate)
        dispatch(configAction)

        assertEquals(siblingUpdate, dispatched[0])
        assertEquals(configAction, dispatched[1])
    }

    @Test
    fun `new dynamic property survives its first text edit and clear`() {
        val controller = JsonFormsComposeController()
        controller.dispatch(
            InitAction(
                data = buildJsonObject { put("dynamic", "") },
                schema = buildJsonSchema {
                    put("type", "object")
                    put("additionalProperties", buildJsonObject { put("type", "string") })
                },
            ),
        )
        val dispatch = preserveDynamicValueOnClear(
            dispatch = controller::dispatch,
            path = "dynamic",
            emptyValue = JsonPrimitive(""),
        )

        dispatch(UpdateAction("dynamic", updater = { JsonPrimitive("first value") }))
        assertEquals(
            JsonPrimitive("first value"),
            (controller.state.jsonforms.core.data as JsonObject)["dynamic"],
        )

        dispatch(UpdateAction("dynamic", updater = { null }))
        val cleared = controller.state.jsonforms.core.data as JsonObject
        assertTrue("dynamic" in cleared)
        assertEquals(JsonPrimitive(""), cleared["dynamic"])

        dispatch(UpdateAction("dynamic", updater = { JsonPrimitive("typed after clear") }))
        assertEquals(
            JsonPrimitive("typed after clear"),
            (controller.state.jsonforms.core.data as JsonObject)["dynamic"],
        )
    }

    @Test
    fun `Additional Properties example keeps the edited dynamic control in place on type and clear`() {
        val data = Json.parseToJsonElement(
            """{"propertiesString":"data","string":"string value","number":10.2,"integer":11,"object":{"prop1":"prop 1 value"},"boolean":true,"stringArray":["value1","value2"],"numberArray":[12.2],"integerArray":[33],"objectArray":[{"prop1":"prop1 val"},{}],"booleanArray":[false,true]}""",
        ).jsonObject
        val schema = JsonSchema(
            Json.parseToJsonElement(
                """{"type":"object","properties":{"propertiesString":{"type":"string"}},"patternProperties":{"^string${'$'}":{"type":"string"},"^number${'$'}":{"type":"number"},"^integer${'$'}":{"type":"integer"},"^object${'$'}":{"type":"object"},"^boolean${'$'}":{"type":"boolean"},"^stringArray${'$'}":{"type":"array","items":{"type":"string"}},"^numberArray${'$'}":{"type":"array","items":{"type":"number"}},"^integerArray${'$'}":{"type":"array","items":{"type":"integer"}},"^objectArray${'$'}":{"type":"array","items":{"type":"object"}},"^booleanArray${'$'}":{"type":"array","items":{"type":"boolean"}}},"additionalProperties":{"type":"string"},"maxProperties":15}""",
            ).jsonObject,
        )
        val originalOrder = data.keys.toList()
        val controller = JsonFormsComposeController()
        controller.dispatch(InitAction(data = data, schema = schema))
        val dispatch = preserveDynamicValueOnClear(
            dispatch = controller::dispatch,
            path = "string",
            emptyValue = JsonPrimitive(""),
        )

        dispatch(UpdateAction("string", updater = { JsonPrimitive("typed") }))
        val typed = controller.state.jsonforms.core.data.jsonObject
        assertEquals(originalOrder, typed.keys.toList())
        assertEquals(JsonPrimitive("typed"), typed["string"])

        dispatch(UpdateAction("string", updater = { null }))
        val cleared = controller.state.jsonforms.core.data.jsonObject
        assertEquals(originalOrder, cleared.keys.toList())
        assertTrue("string" in cleared)
        assertEquals(JsonPrimitive(""), cleared["string"])
    }
}
