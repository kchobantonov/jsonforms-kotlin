package com.chobantonov.jsonfroms.core.store

import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.UiSchemaTester
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import com.chobantonov.jsonfroms.core.validation.JsonSchemaValidator
import com.chobantonov.jsonfroms.core.validation.ValidateFunction
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

/**
 * Kotlin equivalent of the TypeScript `JsonFormsStore extends Store<JsonFormsState>`.
 * This stays intentionally type-focused; reducer wiring lives in a separate file.
 */
interface JsonFormsStore<TRenderer, TCell> : Store<JsonFormsState<TRenderer, TCell>>

/**
 * The state shape of JSON Forms.
 */
data class JsonFormsState<TRenderer, TCell>(
    val jsonforms: JsonFormsSubStates<TRenderer, TCell> = JsonFormsSubStates(),
)

/**
 * Represents JSON Form's sub-state.
 */
data class JsonFormsSubStates<TRenderer, TCell>(
    val core: JsonFormsCore = JsonFormsCoreState(),
    val config: Any? = emptyMap<String, Any?>(),
    val renderers: List<JsonFormsRendererRegistryEntry<TRenderer>> = emptyList(),
    val cells: List<JsonFormsCellRendererRegistryEntry<TCell>> = emptyList(),
    val i18n: JsonFormsI18nState = JsonFormsI18nState(),
    val uischemas: List<JsonFormsUISchemaRegistryEntry> = emptyList(),
    val readonly: Boolean = false,
    val additionalState: Map<String, Any?> = emptyMap(),
)

enum class ValidationMode {
    ValidateAndShow,
    ValidateAndHide,
    NoValidation,
}

interface JsonFormsCore {
    val data: JsonElement
    val dataProvided: Boolean
    val schema: JsonSchema
    val uischema: UiSchemaElement?
    val errors: List<ErrorObject>
    val additionalErrors: List<ErrorObject>
    val validator: ValidateFunction?
    val validatorEngine: JsonSchemaValidator
    val validationMode: ValidationMode
}

data class JsonFormsCoreState(
    override val data: JsonElement = JsonNull,
    override val schema: JsonSchema = buildJsonSchema { },
    override val uischema: UiSchemaElement? = null,
    override val errors: List<ErrorObject> = emptyList(),
    override val additionalErrors: List<ErrorObject> = emptyList(),
    override val validatorEngine: JsonSchemaValidator,
    override val validator: ValidateFunction?,
    override val validationMode: ValidationMode = ValidationMode.ValidateAndShow,
    override val dataProvided: Boolean = true,
) : JsonFormsCore {
    constructor() : this(
        validatorEngine = com.chobantonov.jsonfroms.core.validation.OptimumCodeJsonSchemaValidator(),
        validator = null,
    )
}

data class JsonFormsRendererRegistryEntry<T>(
    val tester: RankedTester,
    val renderer: T,
)

data class JsonFormsUISchemaRegistryEntry(
    val tester: UiSchemaTester,
    val uischema: UiSchemaElement,
)

data class JsonFormsCellRendererRegistryEntry<T>(
    val tester: RankedTester,
    val cell: T,
)

data class JsonFormsExtendedState<T>(
    val jsonforms: Map<String, T>,
)
