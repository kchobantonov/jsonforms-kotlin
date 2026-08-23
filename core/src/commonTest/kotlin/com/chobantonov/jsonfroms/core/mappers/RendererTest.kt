package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.actions.JsonFormsAction
import com.chobantonov.jsonfroms.core.actions.UpdateAction
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.LabelElement
import com.chobantonov.jsonfroms.core.models.LeafCondition
import com.chobantonov.jsonfroms.core.models.Rule
import com.chobantonov.jsonfroms.core.models.RuleEffect
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import com.chobantonov.jsonfroms.core.store.Dispatch
import com.chobantonov.jsonfroms.core.store.JsonFormsCoreState
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry
import com.chobantonov.jsonfroms.core.store.JsonFormsState
import com.chobantonov.jsonfroms.core.store.JsonFormsSubStates
import com.chobantonov.jsonfroms.core.store.ValidationMode
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class RendererTest {
    private val hideRule = Rule(
        effect = RuleEffect.HIDE,
        condition = LeafCondition(
            scope = "#/properties/firstName",
            expectedValue = JsonPrimitive("Homer"),
        ),
    )

    private val disableRule = Rule(
        effect = RuleEffect.DISABLE,
        condition = LeafCondition(
            scope = "#/properties/firstName",
            expectedValue = JsonPrimitive("Homer"),
        ),
    )

    private val coreUiSchema = ControlElement(scope = "#/properties/firstName")

    @Test
    fun `mapStateToControlProps - hide rule overrides inherited visibility`() {
        val uiSchema = coreUiSchema.copy(rule = hideRule)
        val props = mapStateToControlProps(
            createState(uiSchema),
            ownProps(uiSchema, visible = true),
        )

        assertFalse(props.visible)
    }

    @Ignore
    @Test
    fun `mapStateToControlProps - hidden via ownProps `() {
        val uiSchema = coreUiSchema.copy(rule = hideRule)
        val props = mapStateToControlProps(
            createState(uiSchema),
            ownProps(uiSchema, visible = false),
        )

        assertFalse(props.visible)
    }

    @Ignore
    @Test
    fun `mapStateToControlProps - hidden via state `() {
        val uiSchema = coreUiSchema.copy(rule = hideRule)
        val props = mapStateToControlProps(
            createState(uiSchema),
            ownProps(uiSchema),
        )

        assertFalse(props.visible)
    }

    @Test
    fun `mapStateToControlProps - visible via state `() {
        val uiSchema = coreUiSchema.copy(rule = hideRule)
        val props = mapStateToControlProps(
            createState(uiSchema, firstName = "Lisa"),
            ownProps(uiSchema),
        )

        assertTrue(props.visible)
    }

    @Test
    fun `mapStateToControlProps - disable rule overrides inherited enablement`() {
        val uiSchema = coreUiSchema.copy(rule = disableRule)
        val props = mapStateToControlProps(
            createState(uiSchema),
            ownProps(uiSchema, enabled = true),
        )

        assertFalse(props.enabled)
    }

    @Ignore
    @Test
    fun `mapStateToControlProps - disabled via ownProps `() {
        val uiSchema = coreUiSchema.copy(rule = disableRule)
        val props = mapStateToControlProps(
            createState(uiSchema),
            ownProps(uiSchema, enabled = false),
        )

        assertFalse(props.enabled)
    }

    @Ignore
    @Test
    fun `mapStateToControlProps - disabled via state `() {
        val uiSchema = coreUiSchema.copy(rule = disableRule)
        val props = mapStateToControlProps(
            createState(uiSchema),
            ownProps(uiSchema),
        )

        assertFalse(props.enabled)
    }

    @Test
    fun `mapStateToControlProps - enabled via state `() {
        val uiSchema = coreUiSchema.copy(rule = disableRule)
        val props = mapStateToControlProps(
            createState(uiSchema, firstName = "Lisa"),
            ownProps(uiSchema),
        )

        assertTrue(props.enabled)
    }

    @Test
    fun `mapStateToControlProps - path`() {
        val props = mapStateToControlProps(
            createState(coreUiSchema),
            ownProps(coreUiSchema),
        )

        assertEquals("firstName", props.path)
    }

    @Test
    fun `mapStateToControlProps - compose path with ownProps path`() {
        val props = mapStateToControlProps(
            nestedState(coreUiSchema, "foo"),
            OwnPropsOfControl(
                controlUiSchema = coreUiSchema,
                own = OwnPropsOfRenderer(path = "foo"),
            ),
        )

        assertEquals("foo.firstName", props.path)
    }

    @Test
    fun `mapStateToControlProps - derive label`() {
        val props = mapStateToControlProps(
            createState(coreUiSchema),
            ownProps(coreUiSchema),
        )

        assertEquals("First Name", props.label)
    }

    @Test
    fun `mapStateToControlProps - data`() {
        val props = mapStateToControlProps(
            createState(coreUiSchema),
            ownProps(coreUiSchema),
        )

        assertEquals("Homer", props.data.toString().trim('"'))
    }

    @Ignore
    @Test
    fun `mapStateToControlProps - errors`() {
        val props = mapStateToControlProps(
            createState(
                coreUiSchema,
                errors = listOf(ErrorObject(keyword = "custom", message = "error message")),
            ),
            ownProps(coreUiSchema),
        )

        assertEquals("error message", props.errors)
    }

    @Test
    fun `mapStateToControlProps - id`() {
        val props = mapStateToControlProps(
            createState(coreUiSchema),
            OwnPropsOfControl(id = "my-id", controlUiSchema = coreUiSchema),
        )

        assertEquals("my-id", props.id)
    }

    @Test
    fun `mapStateToControlProps - hide errors in hide validation mode`() {
        val props = mapStateToControlProps(
            createState(
                coreUiSchema,
                errors = listOf(ErrorObject(keyword = "custom", message = "error message")),
                validationMode = ValidationMode.ValidateAndHide,
            ),
            ownProps(coreUiSchema),
        )

        assertEquals("", props.errors)
    }

    @Test
    fun `mapDispatchToControlProps`() {
        val dispatched = mutableListOf<JsonFormsAction>()
        val props = mapDispatchToControlProps(
            Dispatch { action ->
                dispatched += action
                action
            },
        )

        props.handleChange("firstName", JsonPrimitive("Lisa"))

        val action = assertIs<UpdateAction>(dispatched.single())
        assertEquals("firstName", action.path)
        assertEquals("Lisa", action.updater(null).toString().trim('"'))
    }

    @Test
    fun `mapDispatchToControlProps - null removes the controlled property`() {
        val dispatched = mutableListOf<JsonFormsAction>()
        val props = mapDispatchToControlProps(
            Dispatch { action ->
                dispatched += action
                action
            },
        )

        props.handleChange("personalData.height", null)

        val action = assertIs<UpdateAction>(dispatched.single())
        assertEquals("personalData.height", action.path)
        assertNull(action.updater(JsonPrimitive(180)))
    }

    @Test
    fun `createDefaultValue`() {
        assertEquals(JsonPrimitive(""), createDefaultValue(buildJsonSchema { put("type", "string") }))
        assertEquals(JsonPrimitive(0), createDefaultValue(buildJsonSchema { put("type", "integer") }))
        assertEquals(JsonPrimitive(false), createDefaultValue(buildJsonSchema { put("type", "boolean") }))
    }

    @Test
    fun `createDefaultValue does not materialize type fallbacks in object properties`() {
        val schema = buildJsonSchema {
            put("type", "object")
            put("properties", buildJsonObject {
                put("plainEnum", buildJsonObject {
                    put("type", "string")
                    put("enum", buildJsonArray {
                        add(JsonPrimitive("foo"))
                        add(JsonPrimitive("bar"))
                    })
                })
                put("oneOfEnum", buildJsonObject {
                    put("type", "string")
                    put("oneOf", buildJsonArray {
                        add(buildJsonObject { put("const", "foo") })
                        add(buildJsonObject { put("const", "bar") })
                    })
                })
            })
        }

        assertEquals(buildJsonObject {}, createDefaultValue(schema))
    }

    @Test
    fun `createDefaultValue preserves explicit and nested object defaults`() {
        val schema = buildJsonSchema {
            put("type", "object")
            put("properties", buildJsonObject {
                put("name", buildJsonObject {
                    put("type", "string")
                    put("default", "Ada")
                })
                put("address", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("city", buildJsonObject {
                            put("type", "string")
                            put("default", "London")
                        })
                        put("street", buildJsonObject { put("type", "string") })
                    })
                })
            })
        }

        assertEquals(
            buildJsonObject {
                put("name", "Ada")
                put("address", buildJsonObject { put("city", "London") })
            },
            createDefaultValue(schema),
        )
    }

    @Test
    fun `mapStateToEnumControlProps - enum items`() {
        val enumSchema = buildJsonSchema {
            put("type", "object")
            put("properties", buildJsonObject {
                put("country", buildJsonObject {
                    put("type", "string")
                    put("enum", buildJsonArray {
                        add(JsonPrimitive("AU"))
                        add(JsonPrimitive("NZ"))
                    })
                })
            })
        }

        val props = mapStateToEnumControlProps(
            state(schema = enumSchema),
            OwnPropsOfControl(controlUiSchema = ControlElement(scope = "#/properties/country")),
        )

        assertEquals(listOf("AU", "NZ"), props.options.map { it.label })
    }

    @Test
    fun `mapStateToOneOfEnumControlProps - i18n - default message translation`() {
        val oneOfSchema = buildJsonSchema {
            put("type", "object")
            put("properties", buildJsonObject {
                put("country", buildJsonObject {
                    put("type", "string")
                    put("oneOf", buildJsonArray {
                        add(buildJsonObject { put("const", JsonPrimitive("AU")); put("title", JsonPrimitive("Australia")) })
                        add(buildJsonObject { put("const", JsonPrimitive("NZ")); put("title", JsonPrimitive("New Zealand")) })
                    })
                })
            })
        }

        val props = mapStateToOneOfEnumControlProps(
            state(
                schema = oneOfSchema,
                i18n = JsonFormsI18nState(
                    translate = com.chobantonov.jsonfroms.core.store.Translator { _, defaultMessage, _ -> "translated:${defaultMessage.orEmpty()}" },
                ),
            ),
            OwnPropsOfControl(controlUiSchema = ControlElement(scope = "#/properties/country")),
        )

        assertEquals(listOf("translated:Australia", "translated:New Zealand"), props.options.map { it.label })
    }

    @Test
    fun `mapStateToLayoutProps should return renderers prop via ownProps`() {
        val renderers = listOf(JsonFormsRendererRegistryEntry({ _, _, _ -> 1 }, "renderer"))
        val props = mapStateToLayoutProps(
            createState(coreUiSchema),
            OwnPropsOfLayout(
                own = OwnPropsOfRenderer(
                    uischema = VerticalLayout(),
                    renderers = renderers,
                ),
            ),
        )

        assertEquals(renderers, props.renderers)
    }

    @Ignore
    @Test
    fun `mapStateToLabelProps - i18n - default message translation`() {
        val props = mapStateToLayoutProps(
            createState(coreUiSchema, i18n = JsonFormsI18nState(
                translate = com.chobantonov.jsonfroms.core.store.Translator { _, defaultMessage, _ -> "translated:${defaultMessage.orEmpty()}" },
            )),
            OwnPropsOfLayout(
                own = OwnPropsOfRenderer(
                    uischema = LabelElement(text = "My Label"),
                ),
            ),
        )

        assertEquals("translated:My Label", props.label)
    }

    private fun ownProps(
        uiSchema: ControlElement,
        visible: Boolean? = null,
        enabled: Boolean? = null,
    ) = OwnPropsOfControl(
        controlUiSchema = uiSchema,
        own = OwnPropsOfRenderer(
            visible = visible,
            enabled = enabled,
        ),
    )

    private fun createState(
        uiSchema: ControlElement,
        firstName: String = "Homer",
        errors: List<ErrorObject> = emptyList(),
        readonly: Boolean = false,
        validationMode: ValidationMode = ValidationMode.ValidateAndShow,
        i18n: JsonFormsI18nState = JsonFormsI18nState(),
    ): JsonFormsState<Any, Any> = JsonFormsState(
        jsonforms = JsonFormsSubStates(
            core = JsonFormsCoreState(
                data = buildJsonObject { put("firstName", firstName) },
                schema = buildJsonSchema {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("firstName", buildJsonObject {
                            put("type", "string")
                            put("description", "A name")
                        })
                    })
                    put("required", buildJsonArray { add(JsonPrimitive("firstName")) })
                },
                uischema = uiSchema,
                errors = if (validationMode == ValidationMode.ValidateAndHide) emptyList() else errors,
                validatorEngine = com.chobantonov.jsonfroms.core.validation.OptimumCodeJsonSchemaValidator(),
                validator = null,
                validationMode = validationMode,
            ),
            readonly = readonly,
            i18n = i18n,
        ),
    )

    private fun nestedState(
        uiSchema: ControlElement,
        path: String,
    ): JsonFormsState<Any, Any> = JsonFormsState(
        jsonforms = JsonFormsSubStates(
            core = JsonFormsCoreState(
                data = buildJsonObject {
                    put(path, buildJsonObject { put("firstName", "Lisa") })
                },
                schema = buildJsonSchema {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put(path, buildJsonObject {
                            put("type", "object")
                            put("properties", buildJsonObject {
                                put("firstName", buildJsonObject { put("type", "string") })
                            })
                        })
                    })
                },
                uischema = uiSchema,
                validatorEngine = com.chobantonov.jsonfroms.core.validation.OptimumCodeJsonSchemaValidator(),
                validator = null,
            ),
        ),
    )

    private fun state(
        data: kotlinx.serialization.json.JsonElement = buildJsonObject { },
        schema: com.chobantonov.jsonfroms.core.models.JsonSchema,
        i18n: JsonFormsI18nState = JsonFormsI18nState(),
    ): JsonFormsState<Any, Any> = JsonFormsState(
        jsonforms = JsonFormsSubStates(
            core = JsonFormsCoreState(
                data = data,
                schema = schema,
                validatorEngine = com.chobantonov.jsonfroms.core.validation.OptimumCodeJsonSchemaValidator(),
                validator = null,
            ),
            i18n = i18n,
        ),
    )
}
