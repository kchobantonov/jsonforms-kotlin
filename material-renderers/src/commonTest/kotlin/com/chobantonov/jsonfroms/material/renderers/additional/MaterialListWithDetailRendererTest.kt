package com.chobantonov.jsonfroms.material.renderers.additional

import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.ListWithDetailElement
import com.chobantonov.jsonfroms.core.testers.TesterContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class MaterialListWithDetailRendererTest {
    @Test
    fun matchesScopedObjectArrayWithReferencedAllOfItems() {
        val schema = JsonSchema(buildJsonObject {
            put("type", "object")
            put("definitions", buildJsonObject {
                put("person", buildJsonObject {
                    put("allOf", buildJsonArray {
                        add(buildJsonObject {
                            put("properties", buildJsonObject {
                                put("id", buildJsonObject { put("type", "string") })
                            })
                        })
                    })
                })
            })
            put("properties", buildJsonObject {
                put("persons", buildJsonObject {
                    put("type", "array")
                    put("items", buildJsonObject { put("\$ref", "#/definitions/person") })
                })
            })
        })

        assertEquals(
            4,
            materialListWithDetailTester(
                ListWithDetailElement(scope = "#/properties/persons"),
                schema,
                TesterContext(rootSchema = schema),
            ),
        )
    }

    @Test
    fun usesConfiguredPrimitivePropertyAsItemLabel() {
        val item = buildJsonObject {
            put("id", "S-1")
            put("description", "A much longer description")
        }

        assertEquals("S-1", computeItemLabel(item, 0, "id"))
    }

    @Test
    fun doesNotShowFallbackLabelWhenConfiguredPropertyIsMissing() {
        assertEquals("", computeItemLabel(buildJsonObject {}, 1, "id"))
        assertEquals("", computeItemLabel(JsonNull, 2, "id"))
        assertEquals("", computeItemLabel(buildJsonObject { put("id", "") }, 3, "id"))
    }

    @Test
    fun usesOneBasedPositionWhenThereIsNoLabelProperty() {
        assertEquals("2", computeItemLabel(buildJsonObject {}, 1, null))
    }
}
