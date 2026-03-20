package com.chobantonov.jsonfroms.core.reducers

import com.chobantonov.jsonfroms.core.actions.InitAction
import com.chobantonov.jsonfroms.core.actions.InitActionOptions
import com.chobantonov.jsonfroms.core.actions.SetSchemaAction
import com.chobantonov.jsonfroms.core.actions.SetValidationModeAction
import com.chobantonov.jsonfroms.core.actions.UpdateCoreAction
import com.chobantonov.jsonfroms.core.actions.UpdateAction
import com.chobantonov.jsonfroms.core.actions.UpdateErrorsAction
import com.chobantonov.jsonfroms.core.models.LabelElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsCoreState
import com.chobantonov.jsonfroms.core.store.JsonFormsState
import com.chobantonov.jsonfroms.core.store.JsonFormsSubStates
import com.chobantonov.jsonfroms.core.store.ValidationMode
import com.chobantonov.jsonfroms.core.store.getErrorAt
import com.chobantonov.jsonfroms.core.store.getSubErrorsAt
import com.chobantonov.jsonfroms.core.util.getControlPath
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import com.chobantonov.jsonfroms.core.validation.JsonSchemaValidator
import com.chobantonov.jsonfroms.core.validation.ValidateFunction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

class CoreTest {
    @Test
    fun `core reducer should support v7`() {
        val schema = buildJsonSchema {
            put("type", "object")
            put("properties", buildJsonObject {
                put("foo", buildJsonObject {
                    put("type", "string")
                    put("const", "bar")
                })
            })
        }

        val after = coreReducer(
            state = null,
            action = InitAction(
                data = buildJsonObject { put("foo", "baz") },
                schema = schema,
                options = InitActionOptions(validator = ConstValidator),
            ),
        )

        assertEquals(1, after.errors.size)
    }

    @Test
    fun `core reducer - no previous state - init without options should create new ajv`() {
        val schema = constSchema()

        val after = coreReducer(
            state = null,
            action = InitAction(
                data = buildJsonObject {},
                schema = schema,
            ),
        )

        assertNotNull(after.validatorEngine)
    }

    @Test
    fun `core reducer - previous state - init without options should keep previous objects`() {
        val previousValidator = AlwaysInvalidValidator
        val additionalErrors = listOf(
            ErrorObject(
                keyword = "required",
                message = "missing required properties: [foo]",
                params = mapOf("missingProperty" to "foo"),
            ),
        )

        val before = JsonFormsCoreState(
            data = buildJsonObject {},
            schema = buildJsonSchema {},
            uischema = LabelElement(text = "Label"),
            additionalErrors = additionalErrors,
            validatorEngine = previousValidator,
            validator = null,
        )

        val after = coreReducer(
            before,
            InitAction(
                data = buildJsonObject {},
                schema = constSchema(),
            ),
        )

        assertEquals(previousValidator, after.validatorEngine)
        assertEquals(additionalErrors, after.additionalErrors)
    }

    @Test
    fun `core reducer - previous state - init with additionalErrors option object should overwrite additionalErrors`() {
        val previous = listOf(ErrorObject(message = "prev"))
        val current = listOf(ErrorObject(message = "current"))

        val before = JsonFormsCoreState(
            data = buildJsonObject {},
            schema = buildJsonSchema {},
            uischema = LabelElement(text = "Label"),
            additionalErrors = previous,
            validatorEngine = AlwaysInvalidValidator,
            validator = null,
        )

        val after = coreReducer(
            before,
            InitAction(
                data = buildJsonObject {},
                schema = constSchema(),
                options = InitActionOptions(additionalErrors = current),
            ),
        )

        assertEquals(current, after.additionalErrors)
    }

    @Test
    fun `core reducer - previous state - init schema with id`() {
        val schema = buildJsonSchema {
            put("\$id", "https://www.jsonforms.io/example.json")
            put("type", "object")
            put("properties", buildJsonObject {
                put("animal", buildJsonObject {
                    put("type", "string")
                })
            })
        }
        val updatedSchema = buildJsonSchema {
            put("\$id", "https://www.jsonforms.io/example.json")
            put("type", "object")
            put("properties", buildJsonObject {
                put("animal", buildJsonObject {
                    put("type", "string")
                    put("minLength", 5)
                })
            })
        }

        val before = coreReducer(
            state = null,
            action = InitAction(
                data = buildJsonObject {},
                schema = schema,
            ),
        )

        val after = coreReducer(
            before,
            InitAction(
                data = buildJsonObject {},
                schema = updatedSchema,
                uiSchema = before.uischema,
            ),
        )

        assertEquals("5", after.schema.toJsonObject()["properties"]!!.jsonObject["animal"]!!.jsonObject["minLength"].toString())
    }

    @Test
    fun `core reducer - update - empty path should update root state`() {
        val schema = buildJsonSchema {
            put("type", "object")
            put("properties", buildJsonObject {
                put("foo", buildJsonObject { put("type", "string") })
            })
        }
        val before = JsonFormsCoreState(
            data = buildJsonObject {
                put("foo", "bar")
                put("baz", buildJsonObject { put("bar", "bar") })
            },
            schema = schema,
            uischema = LabelElement(text = "Label"),
            errors = emptyList(),
            validatorEngine = PassThroughValidator,
            validator = PassThroughValidator.compile(schema),
        )

        val after = coreReducer(
            before,
            UpdateAction(
                path = "",
                updater = { buildJsonObject { put("foo", "xyz") } },
            ),
        )

        assertEquals(buildJsonObject { put("foo", "xyz") }, after.data)
    }

    @Test
    fun `core reducer - update - providing a path should update data only belonging to path`() {
        val schema = buildJsonSchema {
            put("type", "object")
            put("properties", buildJsonObject {
                put("animal", buildJsonObject { put("type", "string") })
                put("color", buildJsonObject { put("type", "string") })
            })
        }
        val before = JsonFormsCoreState(
            data = buildJsonObject {
                put("animal", "Sloth")
                put("color", "Blue")
                put("baz", buildJsonObject { put("bar", "bar") })
            },
            schema = schema,
            uischema = LabelElement(text = "Label"),
            errors = emptyList(),
            validatorEngine = PassThroughValidator,
            validator = PassThroughValidator.compile(schema),
        )

        val after = coreReducer(
            before,
            UpdateAction(
                path = "color",
                updater = { JsonPrimitive("Green") },
            ),
        )

        assertEquals("Green", (after.data as JsonObject)["color"].toString().trim('"'))
        assertEquals("\"bar\"", (after.data as JsonObject)["baz"]!!.jsonObject["bar"].toString())
    }

    @Test
    fun `core reducer - update - setting a state slice as undefined should remove the slice`() {
        val schema = buildJsonSchema {
            put("type", "object")
            put("properties", buildJsonObject {
                put("foo", buildJsonObject { put("type", "string") })
                put("fizz", buildJsonObject { put("type", "string") })
            })
        }
        val before = JsonFormsCoreState(
            data = buildJsonObject {
                put("foo", "bar")
                put("fizz", "42")
            },
            schema = schema,
            uischema = LabelElement(text = "Label"),
            errors = emptyList(),
            validatorEngine = PassThroughValidator,
            validator = PassThroughValidator.compile(schema),
        )

        val after = coreReducer(
            before,
            UpdateAction(
                path = "foo",
                updater = { null },
            ),
        )

        assertEquals(listOf("fizz"), (after.data as JsonObject).keys.toList())
    }

    @Test
    fun `core reducer - updateErrors - should update errors with empty list`() {
        val before = JsonFormsCoreState(
            data = buildJsonObject {},
            schema = buildJsonSchema {},
            validatorEngine = PassThroughValidator,
            validator = null,
        )

        val after = coreReducer(before, UpdateErrorsAction(emptyList()))

        assertEquals(emptyList(), after.errors)
    }

    @Test
    fun `core reducer - updateErrors - should update errors with error`() {
        val before = JsonFormsCoreState(
            data = buildJsonObject {},
            schema = buildJsonSchema {},
            errors = emptyList(),
            validatorEngine = PassThroughValidator,
            validator = null,
        )
        val error = ErrorObject(
            instancePath = "/color",
            keyword = "enum",
            message = "should be equal to one of the allowed values",
        )

        val after = coreReducer(before, UpdateErrorsAction(listOf(error)))

        assertEquals(listOf(error), after.errors)
    }

    @Test
    fun `errorAt contains additionalErrors`() {
        val schema = buildJsonSchema {
            put("type", "object")
            put("properties", buildJsonObject {
                put("foo", buildJsonObject { put("type", "string") })
            })
        }
        val additional = ErrorObject(
            instancePath = "/foo",
            keyword = "custom",
            message = "from additional",
        )
        val state = JsonFormsState<Any, Any>(
            jsonforms = JsonFormsSubStates(
                core = JsonFormsCoreState(
                    data = buildJsonObject { put("foo", "bar") },
                    schema = schema,
                    additionalErrors = listOf(additional),
                    validatorEngine = PassThroughValidator,
                    validator = null,
                ),
            ),
        )

        val filtered = getErrorAt("foo", JsonSchema(schema.toJsonObject()["properties"]!!.jsonObject["foo"]!!.jsonObject))(state)

        assertEquals(listOf(additional), filtered)
    }

    @Test
    fun `errorAt respects hide validation mode`() {
        val schema = buildJsonSchema {
            put("type", "object")
            put("properties", buildJsonObject {
                put("foo", buildJsonObject { put("type", "string") })
            })
        }
        val validationError = ErrorObject(instancePath = "/foo", keyword = "enum", message = "validator")
        val additionalError = ErrorObject(instancePath = "/foo", keyword = "custom", message = "additional")
        val state = JsonFormsState<Any, Any>(
            jsonforms = JsonFormsSubStates(
                core = JsonFormsCoreState(
                    data = buildJsonObject { put("foo", "bar") },
                    schema = schema,
                    errors = listOf(validationError),
                    additionalErrors = listOf(additionalError),
                    validationMode = ValidationMode.ValidateAndHide,
                    validatorEngine = PassThroughValidator,
                    validator = null,
                ),
            ),
        )

        val filtered = getErrorAt("foo", JsonSchema(schema.toJsonObject()["properties"]!!.jsonObject["foo"]!!.jsonObject))(state)

        assertEquals(listOf(additionalError), filtered)
    }

    @Test
    fun `subErrorsAt only returning suberrors`() {
        val schema = buildJsonSchema {
            put("type", "object")
            put("properties", buildJsonObject {
                put("parentArray", buildJsonObject {
                    put("type", "array")
                    put("items", buildJsonObject {
                        put("type", "object")
                        put("properties", buildJsonObject {
                            put("child", buildJsonObject { put("type", "string") })
                        })
                    })
                })
            })
        }
        val state = JsonFormsState<Any, Any>(
            jsonforms = JsonFormsSubStates(
                core = JsonFormsCoreState(
                    data = buildJsonObject {},
                    schema = schema,
                    errors = listOf(
                        ErrorObject(instancePath = "/parentArray/0/child", keyword = "enum", message = "child"),
                        ErrorObject(instancePath = "/other", keyword = "enum", message = "other"),
                    ),
                    validatorEngine = PassThroughValidator,
                    validator = null,
                ),
            ),
        )

        val filtered = getSubErrorsAt("parentArray", JsonSchema(schema.toJsonObject()["properties"]!!.jsonObject["parentArray"]!!.jsonObject))(state)

        assertEquals(1, filtered.size)
        assertEquals("child", filtered.single().message)
    }

    @Test
    fun `core reducer - setValidationMode - No validation should remove errors`() {
        val schema = constSchema()
        val initial = coreReducer(
            state = null,
            action = InitAction(
                data = buildJsonObject { put("foo", "baz") },
                schema = schema,
                options = InitActionOptions(validator = ConstValidator),
            ),
        )

        val after = coreReducer(initial, SetValidationModeAction(ValidationMode.NoValidation))

        assertNull(after.validator)
        assertEquals(emptyList(), after.errors)
    }

    @Test
    fun `core reducer - init - ValidateAndShow should be default validationMode`() {
        val after = coreReducer(
            state = null,
            action = InitAction(
                data = buildJsonObject {},
                schema = buildJsonSchema {},
            ),
        )

        assertEquals(ValidationMode.ValidateAndShow, after.validationMode)
    }

    @Test
    fun `core reducer - setSchema - schema with id`() {
        val before = coreReducer(
            state = null,
            action = InitAction(
                data = buildJsonObject {},
                schema = buildJsonSchema {
                    put("type", "object")
                },
            ),
        )
        val schemaWithId = buildJsonSchema {
            put("\$id", "https://www.jsonforms.io/example.json")
            put("type", "object")
        }

        val after = coreReducer(before, SetSchemaAction(schemaWithId))

        assertEquals("https://www.jsonforms.io/example.json", after.schema.toJsonObject()["\$id"].toString().trim('"'))
    }

    @Test
    fun `core reducer - update core - additionalErrors should update`() {
        val before = coreReducer(
            state = null,
            action = InitAction(
                data = buildJsonObject {},
                schema = buildJsonSchema {},
                options = InitActionOptions(additionalErrors = listOf(ErrorObject(message = "before"))),
            ),
        )

        val after = coreReducer(
            before,
            UpdateCoreAction(
                data = buildJsonObject {},
                schema = buildJsonSchema {},
                options = InitActionOptions(additionalErrors = listOf(ErrorObject(message = "after"))),
            ),
        )

        assertEquals(listOf(ErrorObject(message = "after")), after.additionalErrors)
    }

    @Test
    fun `core reducer helpers - getControlPath - converts JSON Pointer notation to dot notation`() {
        assertEquals(
            "person.name",
            getControlPath(ErrorObject(instancePath = "/person/name", keyword = "enum", message = "invalid")),
        )
    }

    @Test
    fun `core reducer helpers - getControlPath - fallback to AJV legacy errors`() {
        assertEquals(
            "person.name",
            getControlPath(ErrorObject(dataPath = ".person.name", keyword = "enum", message = "invalid")),
        )
    }

    @Test
    fun `core reducer helpers - getControlPath - fallback to AJV legacy errors does not crash for empty paths`() {
        assertEquals(
            "",
            getControlPath(ErrorObject(dataPath = "", keyword = "enum", message = "invalid")),
        )
    }

    @Test
    fun `core reducer helpers - getControlPath - decodes JSON Pointer escape sequences`() {
        assertEquals(
            "foo~bar.baz/qux",
            getControlPath(ErrorObject(instancePath = "/foo~0bar/baz~1qux", keyword = "enum", message = "invalid")),
        )
    }
}

private fun constSchema(): JsonSchema = buildJsonSchema {
    put("type", "object")
    put("properties", buildJsonObject {
        put("foo", buildJsonObject {
            put("type", "string")
            put("const", "bar")
        })
    })
}

private object PassThroughValidator : JsonSchemaValidator {
    override fun compile(schema: JsonSchema): ValidateFunction =
        object : ValidateFunction {
            override val schema: JsonSchema = schema

            override fun validate(data: JsonElement): List<ErrorObject> = emptyList()
        }
}

private object AlwaysInvalidValidator : JsonSchemaValidator {
    override fun compile(schema: JsonSchema): ValidateFunction =
        object : ValidateFunction {
            override val schema: JsonSchema = schema

            override fun validate(data: JsonElement): List<ErrorObject> =
                listOf(ErrorObject(message = "invalid"))
        }
}

private object ConstValidator : JsonSchemaValidator {
    override fun compile(schema: JsonSchema): ValidateFunction =
        object : ValidateFunction {
            override val schema: JsonSchema = schema

            override fun validate(data: JsonElement): List<ErrorObject> {
                val foo = (data as? JsonObject)?.get("foo")?.toString()?.trim('"')
                return if (foo == "bar") {
                    emptyList()
                } else {
                    listOf(
                        ErrorObject(
                            instancePath = "/foo",
                            keyword = "const",
                            message = "must be equal to constant",
                        ),
                    )
                }
            }
        }
}
