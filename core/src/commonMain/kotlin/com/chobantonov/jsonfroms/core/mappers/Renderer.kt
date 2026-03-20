package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.generators.Generate
import com.chobantonov.jsonfroms.core.i18n.defaultErrorTranslator
import com.chobantonov.jsonfroms.core.i18n.defaultTranslator
import com.chobantonov.jsonfroms.core.i18n.deriveLabelForUISchemaElement
import com.chobantonov.jsonfroms.core.i18n.getCombinedErrorMessage
import com.chobantonov.jsonfroms.core.i18n.getI18nKey
import com.chobantonov.jsonfroms.core.i18n.getI18nKeyPrefix
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.LabelElement
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.models.asJsonSchemaOrNull
import com.chobantonov.jsonfroms.core.store.ErrorTranslator
import com.chobantonov.jsonfroms.core.util.convertDateToString
import com.chobantonov.jsonfroms.core.util.createLabelDescriptionFrom
import com.chobantonov.jsonfroms.core.util.composeWithUi
import com.chobantonov.jsonfroms.core.util.hasType
import com.chobantonov.jsonfroms.core.util.hasShowRule
import com.chobantonov.jsonfroms.core.util.isLabelable
import com.chobantonov.jsonfroms.core.util.resolveSchema
import com.chobantonov.jsonfroms.core.store.Dispatch
import com.chobantonov.jsonfroms.core.store.JsonFormsCellRendererRegistryEntry
import com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry
import com.chobantonov.jsonfroms.core.store.JsonFormsState
import com.chobantonov.jsonfroms.core.store.JsonFormsUISchemaRegistryEntry
import com.chobantonov.jsonfroms.core.store.Translator
import com.chobantonov.jsonfroms.core.store.getCells
import com.chobantonov.jsonfroms.core.store.getConfig
import com.chobantonov.jsonfroms.core.store.getData
import com.chobantonov.jsonfroms.core.store.getErrorAt
import com.chobantonov.jsonfroms.core.store.getRenderers
import com.chobantonov.jsonfroms.core.store.getSchema
import com.chobantonov.jsonfroms.core.store.getSubErrorsAt
import com.chobantonov.jsonfroms.core.store.getValidator
import com.chobantonov.jsonfroms.core.actions.JsonFormsAction
import com.chobantonov.jsonfroms.core.actions.UpdateAction
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class EnumOption(
    val label: String,
    val value: JsonElement,
)

data class WithClassname(
    val className: String? = null,
)

data class OwnPropsOfRenderer(
    val uischema: UiSchemaElement? = null,
    val schema: JsonSchema? = null,
    val enabled: Boolean? = null,
    val readonly: Boolean? = null,
    val visible: Boolean? = null,
    val path: String? = null,
    val renderers: List<JsonFormsRendererRegistryEntry<*>>? = null,
    val cells: List<JsonFormsCellRendererRegistryEntry<*>>? = null,
    val uischemas: List<JsonFormsUISchemaRegistryEntry>? = null,
)

data class OwnPropsOfControl(
    val id: String? = null,
    val controlUiSchema: ControlElement? = null,
    val own: OwnPropsOfRenderer = OwnPropsOfRenderer(),
)

data class OwnPropsOfLabel(
    val labelUiSchema: LabelElement? = null,
    val own: OwnPropsOfRenderer = OwnPropsOfRenderer(),
)

data class OwnPropsOfEnum(
    val options: List<EnumOption>? = null,
)

data class OwnPropsOfLayout(
    val direction: String? = null,
    val own: OwnPropsOfRenderer = OwnPropsOfRenderer(),
)

data class StatePropsOfRenderer(
    val config: Map<String, Any?> = emptyMap(),
    val uischema: UiSchemaElement,
    val schema: JsonSchema,
    val data: JsonElement? = null,
    val enabled: Boolean,
    val readonly: Boolean = false,
    val visible: Boolean,
    val path: String,
    val renderers: List<JsonFormsRendererRegistryEntry<*>> = emptyList(),
    val cells: List<JsonFormsCellRendererRegistryEntry<*>> = emptyList(),
)

data class StatePropsOfScopedRenderer(
    val config: Map<String, Any?> = emptyMap(),
    val uischema: ControlElement,
    val schema: JsonSchema,
    val data: JsonElement,
    val enabled: Boolean,
    val readonly: Boolean = false,
    val visible: Boolean,
    val path: String,
    val renderers: List<JsonFormsRendererRegistryEntry<*>> = emptyList(),
    val cells: List<JsonFormsCellRendererRegistryEntry<*>> = emptyList(),
    val errors: String,
    val rootSchema: JsonSchema,
    val id: String,
)

data class StatePropsOfControl(
    val config: Map<String, Any?> = emptyMap(),
    val uischema: ControlElement,
    val schema: JsonSchema,
    val data: JsonElement,
    val enabled: Boolean,
    val readonly: Boolean = false,
    val visible: Boolean,
    val path: String,
    val renderers: List<JsonFormsRendererRegistryEntry<*>> = emptyList(),
    val cells: List<JsonFormsCellRendererRegistryEntry<*>> = emptyList(),
    val errors: String,
    val rootSchema: JsonSchema,
    val id: String,
    val label: String,
    val description: String? = null,
    val required: Boolean = false,
    val i18nKeyPrefix: String? = null,
)

fun interface DispatchPropsOfControl {
    fun handleChange(path: String, value: JsonElement)
}

data class ControlProps(
    val config: Map<String, Any?> = emptyMap(),
    val uischema: ControlElement,
    val schema: JsonSchema,
    val data: JsonElement,
    val enabled: Boolean,
    val readonly: Boolean = false,
    val visible: Boolean,
    val path: String,
    val renderers: List<JsonFormsRendererRegistryEntry<*>> = emptyList(),
    val cells: List<JsonFormsCellRendererRegistryEntry<*>> = emptyList(),
    val errors: String,
    val rootSchema: JsonSchema,
    val id: String,
    val label: String,
    val description: String? = null,
    val required: Boolean = false,
    val i18nKeyPrefix: String? = null,
    val handleChange: (String, JsonElement) -> Unit,
)

data class StatePropsOfLayout(
    val config: Map<String, Any?> = emptyMap(),
    val uischema: UiSchemaElement,
    val schema: JsonSchema,
    val data: JsonElement? = null,
    val enabled: Boolean,
    val readonly: Boolean = false,
    val visible: Boolean,
    val path: String,
    val renderers: List<JsonFormsRendererRegistryEntry<*>> = emptyList(),
    val cells: List<JsonFormsCellRendererRegistryEntry<*>> = emptyList(),
    val direction: String,
    val label: String? = null,
)

data class LayoutProps(
    val config: Map<String, Any?> = emptyMap(),
    val uischema: UiSchemaElement,
    val schema: JsonSchema,
    val data: JsonElement? = null,
    val enabled: Boolean,
    val readonly: Boolean = false,
    val visible: Boolean,
    val path: String,
    val renderers: List<JsonFormsRendererRegistryEntry<*>> = emptyList(),
    val cells: List<JsonFormsCellRendererRegistryEntry<*>> = emptyList(),
    val direction: String,
    val label: String? = null,
)

data class ControlState(
    val value: JsonElement? = null,
    val isFocused: Boolean = false,
)

data class StatePropsOfControlWithDetail(
    val config: Map<String, Any?> = emptyMap(),
    val uischema: ControlElement,
    val schema: JsonSchema,
    val data: JsonElement,
    val enabled: Boolean,
    val readonly: Boolean = false,
    val visible: Boolean,
    val path: String,
    val renderers: List<JsonFormsRendererRegistryEntry<*>> = emptyList(),
    val cells: List<JsonFormsCellRendererRegistryEntry<*>> = emptyList(),
    val errors: String,
    val rootSchema: JsonSchema,
    val id: String,
    val label: String,
    val description: String? = null,
    val required: Boolean = false,
    val i18nKeyPrefix: String? = null,
    val uischemas: List<JsonFormsUISchemaRegistryEntry> = emptyList(),
)

data class StatePropsOfArrayControl(
    val config: Map<String, Any?> = emptyMap(),
    val uischema: ControlElement,
    val schema: JsonSchema,
    val data: JsonElement,
    val enabled: Boolean,
    val readonly: Boolean = false,
    val visible: Boolean,
    val path: String,
    val renderers: List<JsonFormsRendererRegistryEntry<*>> = emptyList(),
    val cells: List<JsonFormsCellRendererRegistryEntry<*>> = emptyList(),
    val errors: String,
    val rootSchema: JsonSchema,
    val id: String,
    val label: String,
    val description: String? = null,
    val required: Boolean = false,
    val i18nKeyPrefix: String? = null,
    val uischemas: List<JsonFormsUISchemaRegistryEntry> = emptyList(),
    val arraySchema: JsonSchema,
    val childErrors: List<com.chobantonov.jsonfroms.core.validation.ErrorObject> = emptyList(),
)

data class StatePropsOfControlWithOptions(
    val config: Map<String, Any?> = emptyMap(),
    val uischema: ControlElement,
    val schema: JsonSchema,
    val data: JsonElement,
    val enabled: Boolean,
    val readonly: Boolean = false,
    val visible: Boolean,
    val path: String,
    val renderers: List<JsonFormsRendererRegistryEntry<*>> = emptyList(),
    val cells: List<JsonFormsCellRendererRegistryEntry<*>> = emptyList(),
    val errors: String,
    val rootSchema: JsonSchema,
    val id: String,
    val label: String,
    val description: String? = null,
    val required: Boolean = false,
    val i18nKeyPrefix: String? = null,
    val options: List<EnumOption> = emptyList(),
)

data class DispatchPropsOfArrayControl(
    val addItem: (String, JsonElement) -> (() -> Unit),
    val removeItems: ((String, List<Int>) -> (() -> Unit))? = null,
    val moveUp: ((String, Int) -> (() -> Unit))? = null,
    val moveDown: ((String, Int) -> (() -> Unit))? = null,
)

fun moveUp(
    array: List<JsonElement>,
    toMove: Int,
): List<JsonElement> {
    if (toMove <= 0 || toMove >= array.size) {
        return array
    }
    val copy = array.toMutableList()
    val element = copy.removeAt(toMove)
    copy.add(toMove - 1, element)
    return copy
}

fun moveDown(
    array: List<JsonElement>,
    toMove: Int,
): List<JsonElement> {
    if (toMove < 0 || toMove >= array.lastIndex) {
        return array
    }
    val copy = array.toMutableList()
    val element = copy.removeAt(toMove)
    copy.add(toMove + 1, element)
    return copy
}

fun computeLabel(
    label: String?,
    required: Boolean,
    hideRequiredAsterisk: Boolean = false,
): String = "${label.orEmpty()}${if (required && !hideRequiredAsterisk) "*" else ""}"

fun showAsRequired(
    required: Boolean,
    hideRequiredAsterisk: Boolean = false,
): Boolean = required && !hideRequiredAsterisk

fun isDescriptionHidden(
    visible: Boolean,
    description: String?,
    isFocused: Boolean = false,
    showUnfocusedDescription: Boolean = false,
): Boolean = description == null || !visible || (!showUnfocusedDescription && !isFocused)

fun enumToEnumOptionMapper(
    value: JsonElement,
    translate: Translator? = null,
    i18nKeyPrefix: String? = null,
): EnumOption {
    val fallback = value.toString().trim('"')
    val translated = if (translate != null && i18nKeyPrefix != null) {
        translate.translate("$i18nKeyPrefix.$fallback", fallback, emptyMap())
    } else if (translate != null) {
        translate.translate(fallback, fallback, emptyMap())
    } else {
        null
    }
    return EnumOption(label = translated ?: fallback, value = value)
}

fun oneOfToEnumOptionMapper(
    oneOfSubSchema: JsonSchema,
    translate: Translator? = null,
    i18nKeyPrefix: String? = null,
): EnumOption {
    val constValue = oneOfSubSchema["const"] ?: JsonNull
    val title = (oneOfSubSchema["title"] as? JsonPrimitive)?.content ?: constValue.toString().trim('"')
    val translated = if (translate != null && i18nKeyPrefix != null) {
        translate.translate("$i18nKeyPrefix.$title", title, mapOf("schema" to oneOfSubSchema))
    } else if (translate != null) {
        translate.translate(title, title, mapOf("schema" to oneOfSubSchema))
    } else {
        null
    }
    return EnumOption(label = translated ?: title, value = constValue)
}

fun createDefaultValue(
    schema: JsonSchema,
    rootSchema: JsonSchema = schema,
): JsonElement = doCreateDefaultValue(schema, rootSchema) ?: buildJsonObject { }

@OptIn(ExperimentalTime::class)
fun doCreateDefaultValue(
    schema: JsonSchema,
    rootSchema: JsonSchema,
): JsonElement? {
    val resolvedSchema = ((schema["\$ref"] as? JsonPrimitive)?.content?.let {
        resolveSchema(rootSchema, it, rootSchema)
    } ?: schema)

    resolvedSchema["default"]?.let { return extractDefaults(resolvedSchema, rootSchema) }

    return when {
        hasType(resolvedSchema, "string") -> {
            when ((resolvedSchema["format"] as? JsonPrimitive)?.content) {
                "date-time" -> JsonPrimitive(convertDateToString(Clock.System.now(), "date-time"))
                "date" -> JsonPrimitive(convertDateToString(Clock.System.now(), "date"))
                "time" -> JsonPrimitive(convertDateToString(Clock.System.now(), "time"))
                else -> JsonPrimitive("")
            }
        }

        hasType(resolvedSchema, "integer") || hasType(resolvedSchema, "number") -> JsonPrimitive(0)
        hasType(resolvedSchema, "boolean") -> JsonPrimitive(false)
        hasType(resolvedSchema, "array") -> buildJsonArray { }
        hasType(resolvedSchema, "object") -> extractDefaults(resolvedSchema, rootSchema)
        hasType(resolvedSchema, "null") -> JsonNull
        else -> {
            listOf("oneOf", "anyOf", "allOf").forEach { combinator ->
                val schemas = resolvedSchema[combinator] as? JsonArray ?: return@forEach
                schemas.mapNotNull(JsonElement::asJsonSchemaOrNull).forEach { subSchema ->
                    val defaultValue = doCreateDefaultValue(subSchema, rootSchema)
                    if (defaultValue != null) {
                        return defaultValue
                    }
                }
            }
            null
        }
    }
}

fun extractDefaults(
    schema: JsonSchema,
    rootSchema: JsonSchema,
): JsonElement {
    schema["default"]?.let { return it }
    val properties = schema["properties"] as? JsonObject ?: return buildJsonObject { }
    return buildJsonObject {
        properties.forEach { (key, value) ->
            val propertySchema = value.asJsonSchemaOrNull() ?: return@forEach
            val defaultValue = doCreateDefaultValue(propertySchema, rootSchema)
            if (defaultValue != null) {
                put(key, defaultValue)
            }
        }
    }
}

private fun isRequired(
    schema: JsonSchema,
    schemaPath: String,
    rootSchema: JsonSchema,
): Boolean {
    val pathSegments = schemaPath.split('/')
    val lastSegment = com.chobantonov.jsonfroms.core.util.JsonPointer.decode(pathSegments.last())
    val nextHigherSchemaPath = pathSegments.dropLast(2).joinToString("/")
    val nextHigherSchema = com.chobantonov.jsonfroms.core.util.Resolve.schema(
        schema,
        nextHigherSchemaPath,
        rootSchema,
    )
    val required = nextHigherSchema?.get("required") as? JsonArray ?: return false
    return required.any { (it as? JsonPrimitive)?.content == lastSegment }
}

fun mapStateToControlProps(
    state: JsonFormsState<*, *>,
    ownProps: OwnPropsOfControl,
): StatePropsOfControl {
    val uischema = ownProps.controlUiSchema ?: ownProps.own.uischema as? ControlElement
        ?: error("Control uischema is required")
    val rootData = getData(state)
    val path = composeWithUi(uischema, ownProps.own.path)
    val config = getConfig(state)
    val visible = if (ownProps.own.visible == null || hasShowRule(uischema)) {
        com.chobantonov.jsonfroms.core.util.Runtime.isVisible(uischema, rootData, getValidator(state))
    } else {
        ownProps.own.visible
    } ?: true
    val rootSchema = getSchema(state)
    val required = isRequired(ownProps.own.schema ?: rootSchema, uischema.scope, rootSchema)
    val resolvedSchema = com.chobantonov.jsonfroms.core.util.Resolve.schema(
        ownProps.own.schema ?: rootSchema,
        uischema.scope,
        rootSchema,
    ) ?: rootSchema
    val errors = getErrorAt(path, resolvedSchema)(state)
    val description = (resolvedSchema["description"] as? JsonPrimitive)?.content ?: ""
    val data = com.chobantonov.jsonfroms.core.util.Resolve.data(rootData, path)
    val labelDesc = createLabelDescriptionFrom(uischema, resolvedSchema)
    val label = if (labelDesc.show == true) labelDesc.text.orEmpty() else ""
    val enabled = isInherentlyEnabled(state, mapOf("enabled" to ownProps.own.enabled), uischema, resolvedSchema, rootData, config)
    val readonly = isInherentlyReadonly(state, mapOf("readonly" to ownProps.own.readonly), uischema, resolvedSchema, rootData, config)
    val translator = state.jsonforms.i18n.translate ?: defaultTranslator
    val errorTranslator = state.jsonforms.i18n.translateError ?: defaultErrorTranslator
    val i18nKeyPrefix = getI18nKeyPrefix(resolvedSchema, uischema, path)
    val i18nLabel = translator.translate(getI18nKey(resolvedSchema, uischema, path, "label"), label, emptyMap()) ?: label
    val i18nDescription = translator.translate(getI18nKey(resolvedSchema, uischema, path, "description"), description, emptyMap()) ?: description
    val i18nErrorMessage = getCombinedErrorMessage(errors, errorTranslator, translator, resolvedSchema, uischema, path)

    return StatePropsOfControl(
        config = config,
        uischema = uischema,
        schema = resolvedSchema,
        data = data ?: JsonNull,
        enabled = enabled,
        readonly = readonly,
        visible = visible,
        path = path,
        renderers = ownProps.own.renderers ?: getRenderers(state),
        cells = ownProps.own.cells ?: getCells(state),
        errors = i18nErrorMessage,
        rootSchema = rootSchema,
        id = ownProps.id ?: path,
        label = i18nLabel ?: label,
        description = i18nDescription,
        required = required,
        i18nKeyPrefix = i18nKeyPrefix,
    )
}

fun mapDispatchToControlProps(
    dispatch: Dispatch<JsonFormsAction>,
): DispatchPropsOfControl = DispatchPropsOfControl { path, value ->
    dispatch.dispatch(
        UpdateAction(
            path = path,
            updater = { value },
        ),
    )
}

fun mapStateToEnumControlProps(
    state: JsonFormsState<*, *>,
    ownProps: OwnPropsOfControl,
    enumProps: OwnPropsOfEnum = OwnPropsOfEnum(),
): StatePropsOfControlWithOptions {
    val props = mapStateToControlProps(state, ownProps)
    val schema = props.schema
    val options = enumProps.options
        ?: (schema["enum"] as? JsonArray)?.map { enumToEnumOptionMapper(it, state.jsonforms.i18n.translate, props.i18nKeyPrefix) }
        ?: schema["const"]?.let { listOf(enumToEnumOptionMapper(it, state.jsonforms.i18n.translate, props.i18nKeyPrefix)) }
        ?: emptyList()
    return StatePropsOfControlWithOptions(
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
        label = props.label,
        description = props.description,
        required = props.required,
        i18nKeyPrefix = props.i18nKeyPrefix,
        options = options,
    )
}

fun mapStateToOneOfEnumControlProps(
    state: JsonFormsState<*, *>,
    ownProps: OwnPropsOfControl,
    enumProps: OwnPropsOfEnum = OwnPropsOfEnum(),
): StatePropsOfControlWithOptions {
    val props = mapStateToControlProps(state, ownProps)
    val oneOf = props.schema["oneOf"] as? JsonArray
    val options = enumProps.options
        ?: oneOf?.mapNotNull(JsonElement::asJsonSchemaOrNull)?.map {
            oneOfToEnumOptionMapper(it, state.jsonforms.i18n.translate, props.i18nKeyPrefix)
        }
        ?: emptyList()
    return StatePropsOfControlWithOptions(
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
        label = props.label,
        description = props.description,
        required = props.required,
        i18nKeyPrefix = props.i18nKeyPrefix,
        options = options,
    )
}

fun mapStateToMultiEnumControlProps(
    state: JsonFormsState<*, *>,
    ownProps: OwnPropsOfControl,
    enumProps: OwnPropsOfEnum = OwnPropsOfEnum(),
): StatePropsOfControlWithOptions {
    val props = mapStateToControlProps(state, ownProps)
    var items = props.schema["items"].asJsonSchemaOrNull()
    items = items?.get("\$ref")?.let { ref ->
        (ref as? JsonPrimitive)?.content?.let { resolveSchema(props.rootSchema, it, props.rootSchema) }
    } ?: items
    val options = enumProps.options
        ?: (items?.get("oneOf") as? JsonArray)?.mapNotNull(JsonElement::asJsonSchemaOrNull)?.map {
            oneOfToEnumOptionMapper(it, state.jsonforms.i18n.translate, props.i18nKeyPrefix)
        }
        ?: (items?.get("enum") as? JsonArray)?.map {
            enumToEnumOptionMapper(it, state.jsonforms.i18n.translate, props.i18nKeyPrefix)
        }
        ?: emptyList()
    return StatePropsOfControlWithOptions(
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
        label = props.label,
        description = props.description,
        required = props.required,
        i18nKeyPrefix = props.i18nKeyPrefix,
        options = options,
    )
}

fun mapStateToControlWithDetailProps(
    state: JsonFormsState<*, *>,
    ownProps: OwnPropsOfControl,
): StatePropsOfControlWithDetail {
    val props = mapStateToControlProps(state, ownProps)
    return StatePropsOfControlWithDetail(
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
        label = props.label,
        description = props.description,
        required = props.required,
        i18nKeyPrefix = props.i18nKeyPrefix,
        uischemas = ownProps.own.uischemas ?: state.jsonforms.uischemas,
    )
}

fun mapStateToArrayControlProps(
    state: JsonFormsState<*, *>,
    ownProps: OwnPropsOfControl,
): StatePropsOfArrayControl {
    val props = mapStateToControlWithDetailProps(state, ownProps)
    val resolvedSchema = com.chobantonov.jsonfroms.core.util.Resolve.schema(
        props.schema,
        "items",
        props.rootSchema,
    ) ?: props.schema
    val childErrors = getSubErrorsAt(props.path, resolvedSchema)(state)
    return StatePropsOfArrayControl(
        config = props.config,
        uischema = props.uischema,
        schema = resolvedSchema,
        data = props.data,
        enabled = props.enabled,
        readonly = props.readonly,
        visible = props.visible,
        path = props.path,
        renderers = ownProps.own.renderers ?: getRenderers(state),
        cells = ownProps.own.cells ?: getCells(state),
        errors = props.errors,
        rootSchema = props.rootSchema,
        id = props.id,
        label = props.label,
        description = props.description,
        required = props.required,
        i18nKeyPrefix = props.i18nKeyPrefix,
        uischemas = props.uischemas,
        arraySchema = props.schema,
        childErrors = childErrors,
    )
}

fun mapDispatchToArrayControlProps(
    dispatch: Dispatch<JsonFormsAction>,
): DispatchPropsOfArrayControl = DispatchPropsOfArrayControl(
    addItem = { path, value ->
        {
            dispatch.dispatch(
                UpdateAction(
                    path = path,
                    updater = { current ->
                        val array = (current as? JsonArray)?.toMutableList() ?: mutableListOf()
                        array += value
                        JsonArray(array)
                    },
                ),
            )
        }
    },
    removeItems = { path, toDelete ->
        {
            dispatch.dispatch(
                UpdateAction(
                    path = path,
                    updater = { current ->
                        val array = (current as? JsonArray)?.toMutableList() ?: mutableListOf()
                        toDelete.sortedDescending().forEach { index ->
                            if (index in array.indices) {
                                array.removeAt(index)
                            }
                        }
                        JsonArray(array)
                    },
                ),
            )
        }
    },
    moveUp = { path, toMove ->
        {
            dispatch.dispatch(
                UpdateAction(
                    path = path,
                    updater = { current ->
                        val array = (current as? JsonArray)?.toList().orEmpty()
                        JsonArray(moveUp(array, toMove))
                    },
                ),
            )
        }
    },
    moveDown = { path, toMove ->
        {
            dispatch.dispatch(
                UpdateAction(
                    path = path,
                    updater = { current ->
                        val array = (current as? JsonArray)?.toList().orEmpty()
                        JsonArray(moveDown(array, toMove))
                    },
                ),
            )
        }
    },
)

fun mapStateToLayoutProps(
    state: JsonFormsState<*, *>,
    ownProps: OwnPropsOfLayout,
): StatePropsOfLayout {
    val uischema = ownProps.own.uischema ?: error("Layout uischema is required")
    val rootData = getData(state)
    val visible = if (ownProps.own.visible == null || hasShowRule(uischema)) {
        com.chobantonov.jsonfroms.core.util.Runtime.isVisible(uischema, rootData, getValidator(state))
    } else {
        ownProps.own.visible
    } ?: true
    val config = getConfig(state)
    val enabled = isInherentlyEnabled(state, mapOf("enabled" to ownProps.own.enabled), uischema, null, rootData, config)
    val readonly = isInherentlyReadonly(state, mapOf("readonly" to ownProps.own.readonly), uischema, null, rootData, config)
    val label = if (isLabelable(uischema)) {
        deriveLabelForUISchemaElement(uischema, state.jsonforms.i18n.translate ?: defaultTranslator)
    } else {
        null
    }
    return StatePropsOfLayout(
        config = config,
        uischema = uischema,
        schema = ownProps.own.schema ?: getSchema(state),
        data = com.chobantonov.jsonfroms.core.util.Resolve.data(rootData, ownProps.own.path ?: ""),
        enabled = enabled,
        readonly = readonly,
        visible = visible,
        path = ownProps.own.path ?: "",
        renderers = ownProps.own.renderers ?: getRenderers(state),
        cells = ownProps.own.cells ?: getCells(state),
        direction = ownProps.direction ?: when (uischema.type) {
            "HorizontalLayout" -> "row"
            else -> "column"
        },
        label = label,
    )
}
