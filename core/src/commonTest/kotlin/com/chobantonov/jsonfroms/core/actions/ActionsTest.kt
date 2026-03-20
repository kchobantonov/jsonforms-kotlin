package com.chobantonov.jsonfroms.core.actions

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ActionsTest {
    @Test
    fun `Init Action generates schema when not provided`() {
        val schema = buildJsonSchema {
            put("type", "object")
            put("properties", buildJsonObject {
                put("name", buildJsonObject {
                    put("type", "string")
                })
            })
            put("additionalProperties", true)
            put("required", buildJsonArray {
                add(JsonPrimitive("name"))
            })
        }

        val init = Actions.init(buildJsonObject { put("name", "foobar") })

        assertEquals(schema, init.schema)
    }

    @Test
    fun `Init Action generates ui schema when not provided`() {
        val schema = buildJsonSchema {
            put("type", "object")
            put("properties", buildJsonObject {
                put("name", buildJsonObject {
                    put("type", "string")
                })
            })
        }
        val uischema = VerticalLayout(
            elements = listOf(
                ControlElement(scope = "#/properties/name"),
            ),
        )

        val init = Actions.init(buildJsonObject {}, schema)

        assertEquals(uischema, init.uiSchema)
    }

    @Test
    fun `Init Action generates ui schema when not valid`() {
        val schema = buildJsonSchema {
            put("type", "object")
            put("properties", buildJsonObject {
                put("name", buildJsonObject {
                    put("type", "string")
                })
            })
        }

        val init = Actions.init(buildJsonObject {}, schema, false)

        assertEquals(
            VerticalLayout(
                elements = listOf(
                    ControlElement(scope = "#/properties/name"),
                ),
            ),
            init.uiSchema,
        )
    }

    @Test
    fun `isUpdateArrayContext correctly identifies `() {
        assertFalse(Actions.isUpdateArrayContext(emptyMap<String, Any?>()))
        assertFalse(Actions.isUpdateArrayContext(mapOf("type" to ADD)))
        assertFalse(Actions.isUpdateArrayContext(mapOf("type" to ADD, "values" to emptyList<Any>())))
        assertTrue(
            Actions.isUpdateArrayContext(
                mapOf(
                    "type" to ADD,
                    "values" to listOf(0, ""),
                ),
            ),
        )

        assertFalse(Actions.isUpdateArrayContext(mapOf("type" to REMOVE)))
        assertFalse(Actions.isUpdateArrayContext(mapOf("type" to REMOVE, "indices" to emptyList<Any>())))
        assertFalse(Actions.isUpdateArrayContext(mapOf("type" to REMOVE, "indices" to listOf(0, ""))))
        assertTrue(Actions.isUpdateArrayContext(mapOf("type" to REMOVE, "indices" to listOf(0, 2))))

        assertFalse(Actions.isUpdateArrayContext(mapOf("type" to MOVE)))
        assertFalse(Actions.isUpdateArrayContext(mapOf("type" to MOVE, "moves" to emptyList<Any>())))
        assertFalse(Actions.isUpdateArrayContext(mapOf("type" to MOVE, "moves" to listOf(0))))
        assertFalse(Actions.isUpdateArrayContext(mapOf("type" to MOVE, "moves" to listOf(null))))
        assertFalse(
            Actions.isUpdateArrayContext(
                mapOf(
                    "type" to MOVE,
                    "moves" to listOf(
                        mapOf("from" to 0, "to" to 1),
                        mapOf("from" to 2),
                    ),
                ),
            ),
        )
        assertFalse(
            Actions.isUpdateArrayContext(
                mapOf(
                    "type" to MOVE,
                    "moves" to listOf(
                        mapOf("from" to 0, "to" to 1),
                        mapOf("to" to 0),
                    ),
                ),
            ),
        )
        assertFalse(
            Actions.isUpdateArrayContext(
                mapOf(
                    "type" to MOVE,
                    "moves" to listOf(
                        mapOf("from" to 0, "to" to ""),
                    ),
                ),
            ),
        )
        assertFalse(
            Actions.isUpdateArrayContext(
                mapOf(
                    "type" to MOVE,
                    "moves" to listOf(
                        mapOf("from" to 0, "to" to 1),
                        mapOf("from" to 0, "to" to ""),
                    ),
                ),
            ),
        )
        assertTrue(
            Actions.isUpdateArrayContext(
                mapOf(
                    "type" to MOVE,
                    "moves" to listOf(
                        mapOf("from" to 0, "to" to 1),
                    ),
                ),
            ),
        )
    }
}
