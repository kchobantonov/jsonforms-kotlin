package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.actions.JsonFormsAction
import com.chobantonov.jsonfroms.core.i18n.defaultErrorTranslator
import com.chobantonov.jsonfroms.core.i18n.defaultTranslator
import com.chobantonov.jsonfroms.core.i18n.getCombinedErrorMessage
import com.chobantonov.jsonfroms.core.i18n.getI18nKeyPrefix
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.asJsonSchemaOrNull
import com.chobantonov.jsonfroms.core.store.Dispatch
import com.chobantonov.jsonfroms.core.store.ErrorTranslator
import com.chobantonov.jsonfroms.core.store.JsonFormsCellRendererRegistryEntry
import com.chobantonov.jsonfroms.core.store.JsonFormsState
import com.chobantonov.jsonfroms.core.store.Translator
import com.chobantonov.jsonfroms.core.store.getConfig
import com.chobantonov.jsonfroms.core.store.getData
import com.chobantonov.jsonfroms.core.store.getErrorAt
import com.chobantonov.jsonfroms.core.store.getSchema
import com.chobantonov.jsonfroms.core.store.getValidator
import com.chobantonov.jsonfroms.core.util.Resolve
import com.chobantonov.jsonfroms.core.util.composeWithUi
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

typealias DispatchPropsOfCell = DispatchPropsOfControl

data class OwnPropsOfCell(
    val id: String? = null,
    val controlUiSchema: ControlElement? = null,
    val own: OwnPropsOfRenderer = OwnPropsOfRenderer(),
    val data: JsonElement? = null,
)

data class StatePropsOfCell(
    val config: Map<String, Any?> = emptyMap(),
    val uischema: ControlElement,
    val schema: JsonSchema,
    val data: JsonElement,
    val enabled: Boolean,
    val readonly: Boolean = false,
    val visible: Boolean,
    val path: String,
    val renderers: List<com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry<*>> = emptyList(),
    val cells: List<JsonFormsCellRendererRegistryEntry<*>> = emptyList(),
    val errors: String,
    val rootSchema: JsonSchema,
    val id: String,
    val isValid: Boolean,
)

data class StatePropsOfEnumCell(
    val config: Map<String, Any?> = emptyMap(),
    val uischema: ControlElement,
    val schema: JsonSchema,
    val data: JsonElement,
    val enabled: Boolean,
    val readonly: Boolean = false,
    val visible: Boolean,
    val path: String,
    val renderers: List<com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry<*>> = emptyList(),
    val cells: List<JsonFormsCellRendererRegistryEntry<*>> = emptyList(),
    val errors: String,
    val rootSchema: JsonSchema,
    val id: String,
    val isValid: Boolean,
    val options: List<EnumOption> = emptyList(),
)

data class CellProps(
    val config: Map<String, Any?> = emptyMap(),
    val uischema: ControlElement,
    val schema: JsonSchema,
    val data: JsonElement,
    val enabled: Boolean,
    val readonly: Boolean = false,
    val visible: Boolean,
    val path: String,
    val renderers: List<com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry<*>> = emptyList(),
    val cells: List<JsonFormsCellRendererRegistryEntry<*>> = emptyList(),
    val errors: String,
    val rootSchema: JsonSchema,
    val id: String,
    val isValid: Boolean,
    val handleChange: (String, JsonElement?) -> Unit,
)

data class EnumCellProps(
    val config: Map<String, Any?> = emptyMap(),
    val uischema: ControlElement,
    val schema: JsonSchema,
    val data: JsonElement,
    val enabled: Boolean,
    val readonly: Boolean = false,
    val visible: Boolean,
    val path: String,
    val renderers: List<com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry<*>> = emptyList(),
    val cells: List<JsonFormsCellRendererRegistryEntry<*>> = emptyList(),
    val errors: String,
    val rootSchema: JsonSchema,
    val id: String,
    val isValid: Boolean,
    val options: List<EnumOption> = emptyList(),
    val handleChange: (String, JsonElement?) -> Unit,
)

data class DispatchCellStateProps(
    val config: Map<String, Any?> = emptyMap(),
    val uischema: ControlElement,
    val schema: JsonSchema,
    val data: JsonElement,
    val enabled: Boolean,
    val readonly: Boolean = false,
    val visible: Boolean,
    val path: String,
    val renderers: List<com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry<*>> = emptyList(),
    val cells: List<JsonFormsCellRendererRegistryEntry<*>> = emptyList(),
    val errors: String,
    val rootSchema: JsonSchema,
    val id: String,
    val isValid: Boolean,
)

fun mapStateToCellProps(
    state: JsonFormsState<*, *>,
    ownProps: OwnPropsOfCell,
): StatePropsOfCell {
    val uischema = ownProps.controlUiSchema ?: ownProps.own.uischema as? ControlElement
        ?: error("Cell uischema is required")
    val rootData = getData(state)
    val config = getConfig(state)
    val path = composeWithUi(uischema, ownProps.own.path)
    val visible = ownProps.own.visible
        ?: com.chobantonov.jsonfroms.core.util.Runtime.isVisible(uischema, rootData, getValidator(state))

    val rootSchema = getSchema(state)
    val schema = com.chobantonov.jsonfroms.core.util.Resolve.schema(
        ownProps.own.schema ?: rootSchema,
        uischema.scope,
        rootSchema,
    ) ?: (ownProps.own.schema ?: rootSchema)

    val enabled = when {
        config["separateReadonlyFromDisabled"] != true && state.jsonforms.readonly -> false
        ownProps.own.enabled != null -> ownProps.own.enabled
        else -> isInherentlyEnabled(state, mapOf("enabled" to ownProps.own.enabled), uischema, schema, rootData, config)
    } ?: true

    val readonly = ownProps.own.readonly
        ?: isInherentlyReadonly(state, mapOf("readonly" to ownProps.own.readonly), uischema, schema, rootData, config)

    val translator = state.jsonforms.i18n.translate ?: defaultTranslator
    val errorTranslator = state.jsonforms.i18n.translateError ?: defaultErrorTranslator
    val errors = getCombinedErrorMessage(
        getErrorAt(path, schema)(state),
        errorTranslator,
        translator,
        schema,
        uischema,
        path,
    )

    return StatePropsOfCell(
        config = config,
        uischema = uischema,
        schema = schema,
        data = ownProps.data ?: Resolve.data(rootData, path) ?: JsonNull,
        enabled = enabled,
        readonly = readonly,
        visible = visible,
        path = path,
        renderers = ownProps.own.renderers.orEmpty(),
        cells = ownProps.own.cells.orEmpty(),
        errors = errors,
        rootSchema = rootSchema,
        id = ownProps.id ?: path,
        isValid = errors.isEmpty(),
    )
}

fun mapStateToDispatchCellProps(
    state: JsonFormsState<*, *>,
    ownProps: OwnPropsOfCell,
): DispatchCellStateProps {
    val props = mapStateToCellProps(state, ownProps)
    return DispatchCellStateProps(
        config = props.config,
        uischema = props.uischema,
        schema = props.schema,
        data = props.data,
        enabled = props.enabled,
        readonly = props.readonly,
        visible = props.visible,
        path = props.path,
        renderers = props.renderers,
        cells = ownProps.own.cells ?: state.jsonforms.cells,
        errors = props.errors,
        rootSchema = props.rootSchema,
        id = props.id,
        isValid = props.isValid,
    )
}

fun defaultMapStateToEnumCellProps(
    state: JsonFormsState<*, *>,
    ownProps: OwnPropsOfCell,
    enumProps: OwnPropsOfEnum = OwnPropsOfEnum(),
): StatePropsOfEnumCell {
    val props = mapStateToCellProps(state, ownProps)
    val options = enumProps.options
        ?: (props.schema["enum"] as? JsonArray)?.map {
            enumToEnumOptionMapper(it, state.jsonforms.i18n.translate, getI18nKeyPrefix(props.schema, props.uischema, props.path))
        }
        ?: props.schema["const"]?.let {
            listOf(enumToEnumOptionMapper(it, state.jsonforms.i18n.translate, getI18nKeyPrefix(props.schema, props.uischema, props.path)))
        }
        ?: emptyList()
    return StatePropsOfEnumCell(
        config = props.config,
        uischema = props.uischema,
        schema = props.schema,
        data = props.data,
        enabled = props.enabled,
        readonly = props.readonly,
        visible = props.visible,
        path = props.path,
        renderers = props.renderers,
        cells = props.cells,
        errors = props.errors,
        rootSchema = props.rootSchema,
        id = props.id,
        isValid = props.isValid,
        options = options,
    )
}

fun mapStateToOneOfEnumCellProps(
    state: JsonFormsState<*, *>,
    ownProps: OwnPropsOfCell,
    enumProps: OwnPropsOfEnum = OwnPropsOfEnum(),
): StatePropsOfEnumCell {
    val props = mapStateToCellProps(state, ownProps)
    val options = enumProps.options
        ?: (props.schema["oneOf"] as? JsonArray)?.mapNotNull(JsonElement::asJsonSchemaOrNull)?.map {
            oneOfToEnumOptionMapper(it, state.jsonforms.i18n.translate, getI18nKeyPrefix(props.schema, props.uischema, props.path))
        }
        ?: emptyList()
    return StatePropsOfEnumCell(
        config = props.config,
        uischema = props.uischema,
        schema = props.schema,
        data = props.data,
        enabled = props.enabled,
        readonly = props.readonly,
        visible = props.visible,
        path = props.path,
        renderers = props.renderers,
        cells = props.cells,
        errors = props.errors,
        rootSchema = props.rootSchema,
        id = props.id,
        isValid = props.isValid,
        options = options,
    )
}

fun mapDispatchToCellProps(
    dispatch: Dispatch<JsonFormsAction>,
): DispatchPropsOfCell = mapDispatchToControlProps(dispatch)

fun defaultMapDispatchToControlProps(
    dispatch: Dispatch<JsonFormsAction>,
    handleChangeOverride: ((String, JsonElement?) -> Unit)? = null,
): DispatchPropsOfControl {
    val delegate = mapDispatchToCellProps(dispatch)
    return DispatchPropsOfControl { path, value ->
        handleChangeOverride?.invoke(path, value) ?: delegate.handleChange(path, value)
    }
}
