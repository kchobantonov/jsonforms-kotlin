package com.chobantonov.jsonfroms.compose

import com.chobantonov.jsonfroms.core.i18n.defaultErrorTranslator
import com.chobantonov.jsonfroms.core.i18n.defaultTranslator
import com.chobantonov.jsonfroms.core.i18n.getCombinedErrorMessage
import com.chobantonov.jsonfroms.core.mappers.TuplePositionScope
import com.chobantonov.jsonfroms.core.mappers.TupleSchema
import com.chobantonov.jsonfroms.core.mappers.ContainerErrorIndex
import com.chobantonov.jsonfroms.core.mappers.resolveTuplePositionScope
import com.chobantonov.jsonfroms.core.models.Categorization
import com.chobantonov.jsonfroms.core.models.Layout
import com.chobantonov.jsonfroms.core.models.ScopedUiSchemaElement
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.models.UnknownUiSchemaElement
import com.chobantonov.jsonfroms.core.util.composePaths
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.util.getControlPath
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

/**
 * A forbidden tuple tail is reported by the validator at the excess item path.
 * Present that structural error at the array control while preserving its original
 * ErrorObject for form validity and host callbacks.
 */
fun tupleForbiddenTailErrorText(
    props: ComposeJsonFormsRendererProps,
    arrayPath: String,
    schema: JsonSchema,
    fixedCount: Int,
    control: ControlElement,
    errorIndex: ContainerErrorIndex,
): String {
    val keyword = when {
        schema["items"] is JsonArray && schema["additionalItems"] == JsonPrimitive(false) -> "additionalItems"
        schema["prefixItems"] is JsonArray && schema["items"] == JsonPrimitive(false) -> "items"
        else -> return ""
    }
    val errors = errorIndex.errorsAtOrBelow(arrayPath).filter { error ->
        if (error.keyword != keyword) return@filter false
        val path = getControlPath(error)
        path.substringBeforeLast('.', missingDelimiterValue = "") == arrayPath &&
            (path.substringAfterLast('.').toIntOrNull() ?: -1) >= fixedCount
    }
    if (errors.isEmpty()) return ""
    val i18n = props.state.jsonforms.i18n
    return getCombinedErrorMessage(
        errors,
        i18n.translateError ?: defaultErrorTranslator,
        i18n.translate ?: defaultTranslator,
        schema,
        control,
        arrayPath,
    )
}

/** Show validation for fixed positions omitted by an authored tuple layout. */
fun tupleOmittedPositionErrorText(
    props: ComposeJsonFormsRendererProps,
    arrayPath: String,
    tuple: TupleSchema,
    layout: UiSchemaElement,
    errorIndex: ContainerErrorIndex,
): String {
    val represented = mutableSetOf<Int>()
    fun visit(element: UiSchemaElement) {
        if (element is ScopedUiSchemaElement) {
            when (val position = resolveTuplePositionScope(element.scope, tuple)) {
                is TuplePositionScope.Whole -> represented += position.index
                is TuplePositionScope.Nested -> represented += position.index
                else -> Unit
            }
        }
        when (element) {
            is Layout -> element.elements.forEach(::visit)
            is Categorization -> element.elements.forEach(::visit)
            is UnknownUiSchemaElement -> element.elements.forEach(::visit)
            else -> Unit
        }
    }
    visit(layout)

    if (tuple.prefix.indices.all(represented::contains)) return ""
    val pathPrefix = if (arrayPath.isEmpty()) "" else arrayPath + "."
    val errorsByIndex = errorIndex.errorsAtOrBelow(arrayPath)
        .mapNotNull { error ->
            val path = getControlPath(error)
            if (!path.startsWith(pathPrefix)) return@mapNotNull null
            val index = path.removePrefix(pathPrefix).substringBefore('.').toIntOrNull()
                ?: return@mapNotNull null
            if (index !in tuple.prefix.indices || index in represented) null else index to error
        }.groupBy({ it.first }, { it.second })

    val i18n = props.state.jsonforms.i18n
    val translator = i18n.translate ?: defaultTranslator
    val errorTranslator = i18n.translateError ?: defaultErrorTranslator
    return tuple.prefix.indices.filterNot(represented::contains).mapNotNull { index ->
        val schema = tuple.schemaAt(index) ?: return@mapNotNull null
        val path = composePaths(arrayPath, index.toString())
        val errors = errorsByIndex[index].orEmpty()
        if (errors.isEmpty()) return@mapNotNull null
        val fallbackLabel = schema.title ?: ("Item " + (index + 1))
        val labelKey = (schema["i18n"] as? JsonPrimitive)?.content?.let { it + ".label" }
        val label = labelKey?.let { translator.translate(it, fallbackLabel, emptyMap()) }
            ?: schema.title
            ?: translator.translate("tuple.position", fallbackLabel, mapOf("position" to index + 1))
            ?: fallbackLabel
        val message = getCombinedErrorMessage(errors, errorTranslator, translator, schema, null, path)
        translator.translate(
            "tuple.omittedPositionError",
            label + ": " + message,
            mapOf("position" to index + 1, "label" to label, "message" to message),
        ) ?: label + ": " + message
    }.joinToString("\n")
}
