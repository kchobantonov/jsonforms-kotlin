package com.chobantonov.jsonfroms.core

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.LabelElement
import com.chobantonov.jsonfroms.core.models.ListWithDetailElement
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import com.chobantonov.jsonfroms.core.testers.TesterContext
import com.chobantonov.jsonfroms.core.testers.Tester
import com.chobantonov.jsonfroms.core.testers.and
import com.chobantonov.jsonfroms.core.testers.formatIs
import com.chobantonov.jsonfroms.core.testers.hasOption
import com.chobantonov.jsonfroms.core.testers.isBooleanControl
import com.chobantonov.jsonfroms.core.testers.isDateControl
import com.chobantonov.jsonfroms.core.testers.isDateTimeControl
import com.chobantonov.jsonfroms.core.testers.isEnumControl
import com.chobantonov.jsonfroms.core.testers.isIntegerControl
import com.chobantonov.jsonfroms.core.testers.isMultiLineControl
import com.chobantonov.jsonfroms.core.testers.isMixedArrayControl
import com.chobantonov.jsonfroms.core.testers.isNumberControl
import com.chobantonov.jsonfroms.core.testers.isPasswordControl
import com.chobantonov.jsonfroms.core.testers.isObjectArray
import com.chobantonov.jsonfroms.core.testers.isObjectArrayControl
import com.chobantonov.jsonfroms.core.testers.isObjectArrayWithNesting
import com.chobantonov.jsonfroms.core.testers.isOneOfEnumControl
import com.chobantonov.jsonfroms.core.testers.isPrimitiveArrayControl
import com.chobantonov.jsonfroms.core.testers.isSingleScalarEditorCombinator
import com.chobantonov.jsonfroms.core.testers.isStringControl
import com.chobantonov.jsonfroms.core.testers.isTimeControl
import com.chobantonov.jsonfroms.core.testers.optionIs
import com.chobantonov.jsonfroms.core.testers.or
import com.chobantonov.jsonfroms.core.testers.schemaMatches
import com.chobantonov.jsonfroms.core.testers.schemaSubPathMatches
import com.chobantonov.jsonfroms.core.testers.schemaTypeIs
import com.chobantonov.jsonfroms.core.testers.scopeEndIs
import com.chobantonov.jsonfroms.core.testers.scopeEndsWith
import com.chobantonov.jsonfroms.core.testers.uiTypeIs
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class TestersTest {
    private fun testerContext(rootSchema: JsonSchema): TesterContext =
        TesterContext(rootSchema = rootSchema)

    @Test
    fun validationOnlyScalarAlternativesKeepTheOuterEditor() {
        val schema = buildJsonSchema {
            put("type", "object")
            put("properties", buildJsonObject {
                put("attendees", buildJsonObject {
                    put("type", "integer")
                    put("anyOf", buildJsonArray {
                        add(buildJsonObject { put("maximum", 12) })
                        add(buildJsonObject { put("minimum", 50) })
                    })
                })
                put("seats", buildJsonObject {
                    put("type", "integer")
                    put("oneOf", buildJsonArray {
                        add(buildJsonObject { put("multipleOf", 3) })
                        add(buildJsonObject { put("multipleOf", 5) })
                    })
                })
                put("choice", buildJsonObject {
                    put("type", "string")
                    put("oneOf", buildJsonArray {
                        add(buildJsonObject { put("const", "email") })
                        add(buildJsonObject { put("const", "phone") })
                    })
                })
                put("annotated", buildJsonObject {
                    put("type", "integer")
                    put("allOf", buildJsonArray {
                        add(buildJsonObject { put("minimum", 0) })
                        add(buildJsonObject { put("default", 0) })
                    })
                })
                put("formatted", buildJsonObject {
                    put("type", "string")
                    put("anyOf", buildJsonArray {
                        add(buildJsonObject { put("format", "date") })
                        add(buildJsonObject { put("maxLength", 10) })
                    })
                })
            })
        }
        val context = testerContext(schema)
        assertTrue(isSingleScalarEditorCombinator(ControlElement(scope = "#/properties/attendees"), schema, context))
        assertTrue(isSingleScalarEditorCombinator(ControlElement(scope = "#/properties/seats"), schema, context))
        assertTrue(isSingleScalarEditorCombinator(ControlElement(scope = "#/properties/annotated"), schema, context))
        assertFalse(isSingleScalarEditorCombinator(ControlElement(scope = "#/properties/choice"), schema, context))
        assertFalse(isSingleScalarEditorCombinator(ControlElement(scope = "#/properties/formatted"), schema, context))
    }

    @Test
    fun referencedAllOfCanEstablishOneScalarEditorWithoutMergingDifferentTypes() {
        val schema = buildJsonSchema {
            put("type", "object")
            put("definitions", buildJsonObject {
                put("nonNegativeInteger", buildJsonObject {
                    put("type", "integer")
                    put("minimum", 0)
                })
            })
            put("properties", buildJsonObject {
                put("minLength", buildJsonObject {
                    put("allOf", buildJsonArray {
                        add(buildJsonObject { put("\$ref", "#/definitions/nonNegativeInteger") })
                        add(buildJsonObject { put("default", 0) })
                    })
                })
                put("ambiguous", buildJsonObject {
                    put("allOf", buildJsonArray {
                        add(buildJsonObject { put("type", "integer") })
                        add(buildJsonObject { put("type", "string") })
                    })
                })
                put("typedInteger", buildJsonObject {
                    put("type", "integer")
                    put("allOf", buildJsonArray {
                        add(buildJsonObject { put("\$ref", "#/definitions/nonNegativeInteger") })
                        add(buildJsonObject { put("default", 0) })
                    })
                })
                put("conflicting", buildJsonObject {
                    put("type", "string")
                    put("allOf", buildJsonArray {
                        add(buildJsonObject { put("\$ref", "#/definitions/nonNegativeInteger") })
                    })
                })
            })
        }
        val context = testerContext(schema)
        val scalar = ControlElement(scope = "#/properties/minLength")
        assertTrue(isSingleScalarEditorCombinator(scalar, schema, context))
        assertTrue(isIntegerControl(scalar, schema, context))
        assertTrue(isSingleScalarEditorCombinator(ControlElement(scope = "#/properties/typedInteger"), schema, context))
        assertFalse(isSingleScalarEditorCombinator(ControlElement(scope = "#/properties/ambiguous"), schema, context))
        assertFalse(isSingleScalarEditorCombinator(ControlElement(scope = "#/properties/conflicting"), schema, context))
    }

    @Test
    fun schemaTypeChecksResolvedSubSchema() {
        val schema = buildJsonSchema {
            put("type", "object")
            put("properties", buildJsonObject {
                put("foo", buildJsonObject { put("type", "string") })
            })
        }
        val control = ControlElement(scope = "#/properties/foo")

        assertTrue(schemaTypeIs("string")(control, schema, testerContext(schema)))
        assertFalse(schemaTypeIs("integer")(control, schema, testerContext(schema)))
        assertFalse(schemaTypeIs("integer")(LabelElement("x"), schema, testerContext(schema)))
    }

    @Test
    fun formatChecksResolvedSubSchema() {
        val schema = buildJsonSchema {
            put("type", "object")
            put("properties", buildJsonObject {
                put("foo", buildJsonObject {
                    put("type", "string")
                    put("format", "date-time")
                })
            })
        }

        assertTrue(formatIs("date-time")(ControlElement(scope = "#/properties/foo"), schema, testerContext(schema)))
    }

    @Test
    fun uiTypeAndOptionsAndScopeHelpers() {
        val control = ControlElement(
            scope = "#/properties/bar",
            options = mapOf("answer" to JsonPrimitive(42), "multi" to JsonPrimitive(true)),
        )

        assertTrue(uiTypeIs("Control")(control, null, TesterContext()))
        assertTrue(optionIs("answer", 42)(control, null, TesterContext()))
        assertTrue(hasOption("answer")(control, null, TesterContext()))
        assertTrue(scopeEndsWith("properties/bar")(control, null, TesterContext()))
        assertTrue(scopeEndIs("bar")(control, null, TesterContext()))
    }

    @Test
    fun optionTesterKeepsJsonScalarTypesDistinct() {
        fun matches(value: JsonPrimitive, expected: Any?) = optionIs("value", expected)(
            ControlElement(scope = "#", options = mapOf("value" to value)),
            null, TesterContext(),
        )
        assertTrue(matches(JsonPrimitive(true), true))
        assertFalse(matches(JsonPrimitive("true"), true))
        assertTrue(matches(JsonPrimitive(42), 42))
        assertFalse(matches(JsonPrimitive("42"), 42))
        assertTrue(matches(JsonPrimitive(1.5), 1.5))
        assertFalse(matches(JsonPrimitive("1.5"), 1.5))
        assertTrue(matches(JsonPrimitive("true"), "true"))
        assertFalse(matches(JsonPrimitive(true), "true"))
    }

    @Test
    fun schemaMatchesAndCombinators() {
        val schema = buildJsonSchema {
            put("type", "object")
            put("properties", buildJsonObject {
                put("foo", buildJsonObject { put("type", "string") })
            })
        }
        val control = ControlElement(scope = "#/properties/foo")

        assertTrue(schemaMatches { it["type"] == JsonPrimitive("string") }(control, schema, testerContext(schema)))
        assertTrue(and(schemaTypeIs("string"), scopeEndIs("foo"))(control, schema, testerContext(schema)))
        assertTrue(or(schemaTypeIs("integer"), scopeEndIs("foo"))(control, schema, testerContext(schema)))
    }

    @Test
    fun primitiveAndObjectArrayTesters() {
        val control = ControlElement(scope = "#/properties/foo")

        val primitiveArraySchema = buildJsonSchema {
            put("type", "object")
            put("properties", buildJsonObject {
                put("foo", buildJsonObject {
                    put("type", "array")
                    put("items", buildJsonObject { put("type", "integer") })
                })
            })
        }
        assertTrue(isPrimitiveArrayControl(control, primitiveArraySchema, testerContext(primitiveArraySchema)))

        val objectArraySchema = buildJsonSchema {
            put("type", "object")
            put("properties", buildJsonObject {
                put("foo", buildJsonObject {
                    put("type", "array")
                    put("items", buildJsonObject {
                        put("type", "object")
                        put("properties", buildJsonObject {
                            put("x", buildJsonObject { put("type", "integer") })
                        })
                    })
                })
            })
        }
        assertTrue(isObjectArrayControl(control, objectArraySchema, testerContext(objectArraySchema)))
        assertTrue(
            isObjectArray(
                ListWithDetailElement(scope = "#/properties/foo"),
                objectArraySchema,
                testerContext(objectArraySchema),
            ),
        )

        val mixedArraySchema = buildJsonSchema {
            put("type", "array")
            put("items", buildJsonObject {
                put("type", buildJsonArray {
                    add(JsonPrimitive("array"))
                    add(JsonPrimitive("boolean"))
                    add(JsonPrimitive("integer"))
                    add(JsonPrimitive("null"))
                    add(JsonPrimitive("number"))
                    add(JsonPrimitive("object"))
                    add(JsonPrimitive("string"))
                })
            })
        }
        assertTrue(isMixedArrayControl(ControlElement(scope = ""), mixedArraySchema, testerContext(mixedArraySchema)))

        val combinatorArrayRoot = buildJsonSchema {
            put("type", "object")
            put("definitions", buildJsonObject {
                put("node", buildJsonObject {
                    put("oneOf", buildJsonArray {
                        add(buildJsonObject { put("type", "string") })
                        add(buildJsonObject { put("type", "object") })
                    })
                })
            })
            put("properties", buildJsonObject {
                put("children", buildJsonObject {
                    put("type", "array")
                    put("items", buildJsonObject { put("\$ref", "#/definitions/node") })
                })
            })
        }
        assertTrue(isMixedArrayControl(ControlElement(scope = "#/properties/children"),
            combinatorArrayRoot, testerContext(combinatorArrayRoot)))

        val closedArraySchema = buildJsonSchema {
            put("type", "array")
            put("items", false)
            put("maxItems", 0)
        }
        assertTrue(isMixedArrayControl(ControlElement(scope = ""), closedArraySchema,
            testerContext(closedArraySchema)))
    }

    @Test
    fun primitiveControlTesters() {
        val control = ControlElement(scope = "#/properties/foo")

        fun schemaFor(vararg entries: Pair<String, kotlinx.serialization.json.JsonElement>) = buildJsonSchema {
            put("type", "object")
            put("properties", buildJsonObject {
                put("foo", buildJsonObject {
                    entries.forEach { (key, value) -> put(key, value) }
                })
            })
        }

        assertTrue(isBooleanControl(control, schemaFor("type" to JsonPrimitive("boolean")), TesterContext()))
        assertTrue(isDateControl(control, schemaFor("type" to JsonPrimitive("string"), "format" to JsonPrimitive("date")), TesterContext()))
        assertTrue(
            isEnumControl(
                control,
                schemaFor(
                    "type" to JsonPrimitive("string"),
                    "enum" to buildJsonArray {
                        add(JsonPrimitive("a"))
                        add(JsonPrimitive("b"))
                    },
                ),
                TesterContext(),
            ),
        )
        assertTrue(isIntegerControl(control, schemaFor("type" to JsonPrimitive("integer")), TesterContext()))
        assertTrue(isNumberControl(control, schemaFor("type" to JsonPrimitive("number")), TesterContext()))
        assertTrue(isStringControl(control, schemaFor("type" to JsonPrimitive("string")), TesterContext()))
        assertTrue(isTimeControl(control, schemaFor("type" to JsonPrimitive("string"), "format" to JsonPrimitive("time")), TesterContext()))
        assertTrue(isMultiLineControl(control.copy(options = mapOf("multi" to JsonPrimitive(true))), schemaFor("type" to JsonPrimitive("string")), TesterContext()))
    }

    @Test
    fun passwordSelectionSupportsSchemaAndLocalUiFormatOnlyOnStrings() {
        val plainString = buildJsonSchema { put("type", "string") }
        val schemaPassword = buildJsonSchema {
            put("type", "string")
            put("format", "password")
        }
        val integer = buildJsonSchema { put("type", "integer") }
        val control = ControlElement(scope = "#")
        val uiPassword = control.copy(options = mapOf("format" to JsonPrimitive("password")))

        assertTrue(isPasswordControl(control, schemaPassword, TesterContext()))
        assertTrue(isPasswordControl(uiPassword, plainString, TesterContext()))
        assertFalse(isPasswordControl(control, plainString, TesterContext(config = mapOf("format" to "password"))))
        assertFalse(isPasswordControl(uiPassword, integer, TesterContext()))
    }

    @Test
    fun temporalSelectionRequiresStringSchemaAndLocalOrSchemaFormat() {
        val plainString = buildJsonSchema { put("type", "string") }
        val integer = buildJsonSchema { put("type", "integer") }
        val control = ControlElement(scope = "#")
        val temporalTesters: List<Pair<String, Tester>> = listOf(
            "date" to isDateControl,
            "time" to isTimeControl,
            "date-time" to isDateTimeControl,
        )

        temporalTesters.forEach { (format, tester) ->
            val formattedString = buildJsonSchema {
                put("type", "string")
                put("format", format)
            }
            val formattedInteger = buildJsonSchema {
                put("type", "integer")
                put("format", format)
            }
            val uiFormat = control.copy(options = mapOf("format" to JsonPrimitive(format)))
            assertTrue(tester(control, formattedString, TesterContext()), format)
            assertTrue(tester(uiFormat, plainString, TesterContext()), format)
            assertFalse(tester(control, plainString, TesterContext(config = mapOf("format" to format))), format)
            assertFalse(tester(uiFormat, integer, TesterContext()), format)
            assertFalse(tester(control, formattedInteger, TesterContext()), format)
        }
    }

    @Test
    fun objectArrayWithNestingAndSubPathMatcher() {
        val schema = buildJsonSchema {
            put("type", "array")
            put("items", buildJsonObject {
                put("type", "array")
                put("items", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("message", buildJsonObject { put("type", "string") })
                    })
                })
            })
        }
        val control = ControlElement(scope = "#")

        assertTrue(isObjectArrayWithNesting(control, schema, testerContext(schema)))
        assertTrue(schemaSubPathMatches("items") { items, _ -> items["type"] == JsonPrimitive("array") }(control, schema, testerContext(schema)))
    }

    @Test
    fun oneOfEnumControlSupportsRefs() {
        val oneOfSchema = buildJsonObject {
            put("type", "string")
            put("oneOf", buildJsonArray {
                add(buildJsonObject { put("const", JsonPrimitive("AU")); put("title", JsonPrimitive("Australia")) })
                add(buildJsonObject { put("const", JsonPrimitive("NZ")); put("title", JsonPrimitive("New Zealand")) })
            })
        }
        val schema = buildJsonSchema {
            put("definitions", buildJsonObject {
                put("country", oneOfSchema)
            })
            put("type", "object")
            put("properties", buildJsonObject {
                put("country", buildJsonObject { put("\$ref", "#/definitions/country") })
            })
        }

        assertTrue(isOneOfEnumControl(ControlElement(scope = "#/properties/country"), schema, testerContext(schema)))
    }
}
