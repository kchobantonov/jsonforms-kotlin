package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.actions.JsonFormsAction
import com.chobantonov.jsonfroms.core.actions.UpdateAction
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.LeafCondition
import com.chobantonov.jsonfroms.core.models.Rule
import com.chobantonov.jsonfroms.core.models.RuleEffect
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import com.chobantonov.jsonfroms.core.store.Dispatch
import com.chobantonov.jsonfroms.core.store.JsonFormsCoreState
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.store.JsonFormsState
import com.chobantonov.jsonfroms.core.store.JsonFormsSubStates
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class CellTest {
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
    fun `mapStateToCellProps - visible via ownProps `() {
        val uiSchema = coreUiSchema.copy(rule = hideRule)
        val props = mapStateToCellProps(
            createState(uiSchema),
            ownProps(uiSchema, visible = true),
        )

        assertTrue(props.visible)
    }

    @Test
    fun `mapStateToCellProps - hidden via ownProps `() {
        val uiSchema = coreUiSchema.copy(rule = hideRule)
        val props = mapStateToCellProps(
            createState(uiSchema),
            ownProps(uiSchema, visible = false),
        )

        assertFalse(props.visible)
    }

    @Ignore
    @Test
    fun `mapStateToCellProps - hidden via state `() {
        val uiSchema = coreUiSchema.copy(rule = hideRule)
        val props = mapStateToCellProps(
            createState(uiSchema),
            ownProps(uiSchema),
        )

        assertFalse(props.visible)
    }

    @Test
    fun `mapStateToCellProps - visible via state `() {
        val uiSchema = coreUiSchema.copy(rule = hideRule)
        val props = mapStateToCellProps(
            createState(uiSchema, firstName = "Lisa"),
            ownProps(uiSchema),
        )

        assertTrue(props.visible)
    }

    @Test
    fun `mapStateToCellProps - enabled via ownProps `() {
        val uiSchema = coreUiSchema.copy(rule = disableRule)
        val props = mapStateToCellProps(
            createState(uiSchema),
            ownProps(uiSchema, enabled = true),
        )

        assertTrue(props.enabled)
    }

    @Test
    fun `mapStateToCellProps - disabled via ownProps `() {
        val uiSchema = coreUiSchema.copy(rule = disableRule)
        val props = mapStateToCellProps(
            createState(uiSchema),
            ownProps(uiSchema, enabled = false),
        )

        assertFalse(props.enabled)
    }

    @Ignore
    @Test
    fun `mapStateToCellProps - disabled via state `() {
        val uiSchema = coreUiSchema.copy(rule = disableRule)
        val props = mapStateToCellProps(
            createState(uiSchema),
            ownProps(uiSchema),
        )

        assertFalse(props.enabled)
    }

    @Test
    fun `mapStateToCellProps - enabled via state `() {
        val uiSchema = coreUiSchema.copy(rule = disableRule)
        val props = mapStateToCellProps(
            createState(uiSchema, firstName = "Lisa"),
            ownProps(uiSchema),
        )

        assertTrue(props.enabled)
    }

    @Test
    fun `mapStateToCellProps - disabled via global readonly`() {
        val props = mapStateToCellProps(
            createState(coreUiSchema, readonly = true),
            ownProps(coreUiSchema),
        )

        assertFalse(props.enabled)
    }

    @Test
    fun `mapStateToCellProps - path`() {
        val props = mapStateToCellProps(
            createState(coreUiSchema),
            ownProps(coreUiSchema),
        )

        assertEquals("firstName", props.path)
    }

    @Test
    fun `mapStateToCellProps - data`() {
        val props = mapStateToCellProps(
            createState(coreUiSchema),
            ownProps(coreUiSchema),
        )

        assertEquals("Homer", props.data.toString().trim('"'))
    }

    @Test
    fun `mapStateToCellProps - id`() {
        val props = mapStateToCellProps(
            createState(coreUiSchema),
            OwnPropsOfCell(id = "my-id", controlUiSchema = coreUiSchema),
        )

        assertEquals("my-id", props.id)
    }

    @Ignore
    @Test
    fun `mapStateToCellProps - translated error`() {
        val props = mapStateToCellProps(
            createState(
                coreUiSchema,
                errors = listOf(ErrorObject(keyword = "custom", message = "raw")),
                i18n = JsonFormsI18nState(
                    translateError = com.chobantonov.jsonfroms.core.store.ErrorTranslator { _, _, _ -> "translated error" },
                ),
            ),
            ownProps(coreUiSchema),
        )

        assertEquals("translated error", props.errors)
        assertFalse(props.isValid)
    }

    @Test
    fun `mapStateToEnumCellProps - set default options for dropdown list`() {
        val schema = buildJsonSchema {
            put("type", "object")
            put("properties", buildJsonObject {
                put("nationality", buildJsonObject {
                    put("type", "string")
                    put("enum", buildJsonArray {
                        add(JsonPrimitive("DE"))
                        add(JsonPrimitive("IT"))
                        add(JsonPrimitive("JP"))
                        add(JsonPrimitive("US"))
                        add(JsonPrimitive("RU"))
                        add(JsonPrimitive("Other"))
                    })
                })
            })
        }

        val props = defaultMapStateToEnumCellProps(
            state(schema = schema),
            OwnPropsOfCell(controlUiSchema = ControlElement(scope = "#/properties/nationality")),
        )

        assertEquals(listOf("DE", "IT", "JP", "US", "RU", "Other"), props.options.map { it.label })
    }

    @Test
    fun `mapStateToOneOfEnumCellProps - set one of options for dropdown list`() {
        val schema = buildJsonSchema {
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

        val props = mapStateToOneOfEnumCellProps(
            state(schema = schema),
            OwnPropsOfCell(controlUiSchema = ControlElement(scope = "#/properties/country")),
        )

        assertEquals(listOf("Australia", "New Zealand"), props.options.map { it.label })
    }

    @Test
    fun `defaultMapDispatchToControlProps initialized with custom handleChange`() {
        val dispatched = mutableListOf<JsonFormsAction>()
        val handled = mutableListOf<Pair<String, String>>()
        val props = defaultMapDispatchToControlProps(
            dispatch = Dispatch { action ->
                dispatched += action
                action
            },
            handleChangeOverride = { path, value ->
                handled += path to value.toString().trim('"')
            },
        )

        props.handleChange("firstName", JsonPrimitive("Lisa"))

        assertEquals(listOf("firstName" to "Lisa"), handled)
        assertTrue(dispatched.isEmpty())
    }

    @Test
    fun `defaultMapDispatchToControlProps with default handleChange`() {
        val dispatched = mutableListOf<JsonFormsAction>()
        val props = defaultMapDispatchToControlProps(
            dispatch = Dispatch { action ->
                dispatched += action
                action
            },
        )

        props.handleChange("firstName", JsonPrimitive("Lisa"))

        val action = assertIs<UpdateAction>(dispatched.single())
        assertEquals("firstName", action.path)
        assertEquals("Lisa", action.updater(null).toString().trim('"'))
    }

    private fun ownProps(
        uiSchema: ControlElement,
        visible: Boolean? = null,
        enabled: Boolean? = null,
    ) = OwnPropsOfCell(
        controlUiSchema = uiSchema,
        own = OwnPropsOfRenderer(
            visible = visible,
            enabled = enabled,
        ),
    )

    private fun createState(
        uiSchema: ControlElement,
        firstName: String = "Homer",
        readonly: Boolean = false,
        errors: List<ErrorObject> = emptyList(),
        i18n: JsonFormsI18nState = JsonFormsI18nState(),
    ): JsonFormsState<Any, Any> = JsonFormsState(
        jsonforms = JsonFormsSubStates(
            core = JsonFormsCoreState(
                data = buildJsonObject { put("firstName", firstName) },
                schema = buildJsonSchema {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("firstName", buildJsonObject { put("type", "string") })
                        put("lastName", buildJsonObject { put("type", "string") })
                        put("nationality", buildJsonObject {
                            put("type", "string")
                            put("enum", buildJsonArray {
                                add(JsonPrimitive("DE"))
                                add(JsonPrimitive("IT"))
                                add(JsonPrimitive("JP"))
                                add(JsonPrimitive("US"))
                                add(JsonPrimitive("RU"))
                                add(JsonPrimitive("Other"))
                            })
                        })
                    })
                    put("required", buildJsonArray { add(JsonPrimitive("firstName")) })
                },
                uischema = uiSchema,
                errors = errors,
                validatorEngine = com.chobantonov.jsonfroms.core.validation.OptimumCodeJsonSchemaValidator(),
                validator = null,
            ),
            readonly = readonly,
            i18n = i18n,
        ),
    )

    private fun state(
        data: kotlinx.serialization.json.JsonElement = buildJsonObject { },
        schema: JsonSchema,
    ): JsonFormsState<Any, Any> = JsonFormsState(
        jsonforms = JsonFormsSubStates(
            core = JsonFormsCoreState(
                data = data,
                schema = schema,
                validatorEngine = com.chobantonov.jsonfroms.core.validation.OptimumCodeJsonSchemaValidator(),
                validator = null,
            ),
        ),
    )
}
