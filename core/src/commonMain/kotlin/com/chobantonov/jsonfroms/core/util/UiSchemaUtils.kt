package com.chobantonov.jsonfroms.core.util

import com.chobantonov.jsonfroms.core.models.BooleanControlLabel
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.DescriptionControlLabel
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.models.Internationalizable
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.LabelElement
import com.chobantonov.jsonfroms.core.models.LabelDescription
import com.chobantonov.jsonfroms.core.models.Labelable
import com.chobantonov.jsonfroms.core.models.Labeled
import com.chobantonov.jsonfroms.core.models.Scopable
import com.chobantonov.jsonfroms.core.models.ScopedUiSchemaElement
import com.chobantonov.jsonfroms.core.models.StringControlLabel
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

typealias IterateCallback = (UiSchemaElement) -> Unit

fun hasShowRule(uiSchema: UiSchemaElement): Boolean =
    uiSchema.rule?.effect in setOf(
        com.chobantonov.jsonfroms.core.models.RuleEffect.SHOW,
        com.chobantonov.jsonfroms.core.models.RuleEffect.HIDE,
    )

fun hasEnableRule(uiSchema: UiSchemaElement): Boolean =
    uiSchema.rule?.effect in setOf(
        com.chobantonov.jsonfroms.core.models.RuleEffect.ENABLE,
        com.chobantonov.jsonfroms.core.models.RuleEffect.DISABLE,
    )

fun hasReadonlyRule(uiSchema: UiSchemaElement): Boolean =
    uiSchema.rule?.effect in setOf(
        com.chobantonov.jsonfroms.core.models.RuleEffect.READONLY,
        com.chobantonov.jsonfroms.core.models.RuleEffect.WRITABLE,
    )

fun iterateSchema(
    uiSchema: UiSchemaElement,
    toApply: IterateCallback,
) {
    if (isLayout(uiSchema)) {
        uiSchemaElements(uiSchema).forEach { child -> iterateSchema(child, toApply) }
        return
    }
    toApply(uiSchema)
}

fun setReadonly(uiSchema: UiSchemaElement): UiSchemaElement =
    mapUiSchema(uiSchema) { element ->
        when (element) {
            is VerticalLayout -> element
            is HorizontalLayout -> element
            is GroupElement -> element
            is com.chobantonov.jsonfroms.core.models.CategoryElement -> element
            is com.chobantonov.jsonfroms.core.models.CategorizationElement -> element
            else -> withReadonlyOption(element, true)
        }
    }

fun unsetReadonly(uiSchema: UiSchemaElement): UiSchemaElement =
    mapUiSchema(uiSchema) { element ->
        when (element) {
            is VerticalLayout -> element
            is HorizontalLayout -> element
            is GroupElement -> element
            is com.chobantonov.jsonfroms.core.models.CategoryElement -> element
            is com.chobantonov.jsonfroms.core.models.CategorizationElement -> element
            else -> withReadonlyOption(element, false)
        }
    }

fun composeWithUi(
    uiSchema: UiSchemaElement,
    path: String?,
): String {
    val scoped = uiSchema as? ControlElement ?: return path.orEmpty()
    val segments = toDataPathSegments(scoped.scope)
    return if (segments.isEmpty()) {
        path.orEmpty()
    } else {
        composePaths(path.orEmpty(), segments.joinToString("."))
    }
}

fun isLabelable(obj: UiSchemaElement): Boolean = obj is Labelable<*>

fun isInternationalized(element: Any?): Boolean = (element as? Internationalizable)?.i18n != null

fun isGroup(layout: UiSchemaElement): Boolean = layout.type == "Group"

fun isLayout(uiSchema: UiSchemaElement): Boolean =
    uiSchema is VerticalLayout ||
        uiSchema is HorizontalLayout ||
        uiSchema is GroupElement ||
        uiSchema is com.chobantonov.jsonfroms.core.models.CategoryElement ||
        uiSchema is com.chobantonov.jsonfroms.core.models.CategorizationElement

fun isScopable(obj: Any?): Boolean = obj is Scopable

fun isScoped(obj: Any?): Boolean = obj is ScopedUiSchemaElement

fun isLabeled(obj: Any?): Boolean = obj is Labeled<*>

fun isControlElement(uiSchema: UiSchemaElement): Boolean = uiSchema.type == "Control"

fun createLabelDescriptionFrom(
    controlElement: ControlElement,
    schema: JsonSchema? = null,
): LabelDescription {
    val labelProperty = controlElement.label
    return when {
        labelProperty is BooleanControlLabel && !labelProperty.value ->
            LabelDescription(deriveLabel(controlElement, schema), false)
        labelProperty is StringControlLabel ->
            LabelDescription(labelProperty.value, true)
        labelProperty is DescriptionControlLabel ->
            LabelDescription(
                text = labelProperty.value.text ?: deriveLabel(controlElement, schema),
                show = labelProperty.value.show ?: true,
            )
        else -> LabelDescription(deriveLabel(controlElement, schema), true)
    }
}

fun getPropPath(path: String): String =
    "/properties/${path.split('.').joinToString("/properties/") { JsonPointer.encode(it) }}"

fun findUiControl(
    uiSchema: UiSchemaElement,
    path: String,
): UiSchemaElement? {
    val scoped = uiSchema as? ScopedUiSchemaElement
    if (isControlElement(uiSchema)) {
        if (scoped != null && scoped.scope.endsWith(getPropPath(path))) {
            return scoped
        }
        val detail = (uiSchema.options["detail"] as? JsonObject)?.let(::jsonObjectToUiSchema)
        if (detail != null) {
            return findUiControl(detail, path)
        }
    }

    val elements = uiSchemaElements(uiSchema)
    elements.forEach { child ->
        val found = findUiControl(child, path)
        if (found != null) return found
    }
    return null
}

fun getFirstPrimitiveProp(schema: JsonSchema?): String? {
    val properties = schema?.get("properties") as? JsonObject ?: return null
    return properties.entries.firstOrNull { (_, value) ->
        val property = value as? JsonObject ?: return@firstOrNull false
        val type = (property["type"] as? JsonPrimitive)?.content
        type in setOf("string", "number", "integer")
    }?.key
}

fun getDetailUiSchema(uiSchema: UiSchemaElement): UiSchemaElement? =
    (uiSchema.options["detail"] as? JsonObject)?.let(::jsonObjectToUiSchema)

private fun uiSchemaElements(uiSchema: UiSchemaElement): List<UiSchemaElement> =
    when (uiSchema) {
        is VerticalLayout -> uiSchema.elements
        is HorizontalLayout -> uiSchema.elements
        is GroupElement -> uiSchema.elements
        is com.chobantonov.jsonfroms.core.models.CategoryElement -> uiSchema.elements
        is com.chobantonov.jsonfroms.core.models.CategorizationElement -> uiSchema.elements
        else -> emptyList()
    }

private fun withReadonlyOption(
    uiSchema: UiSchemaElement,
    value: Boolean,
): UiSchemaElement {
    val updatedOptions = uiSchema.options + ("readonly" to JsonPrimitive(value))
    return when (uiSchema) {
        is ControlElement -> uiSchema.copy(options = updatedOptions)
        is LabelElement -> uiSchema.copy(options = updatedOptions)
        is com.chobantonov.jsonfroms.core.models.UnknownUiSchemaElement -> uiSchema.copy(options = updatedOptions)
        else -> uiSchema
    }
}

private fun mapUiSchema(
    uiSchema: UiSchemaElement,
    transformLeaf: (UiSchemaElement) -> UiSchemaElement,
): UiSchemaElement =
    when (uiSchema) {
        is VerticalLayout -> uiSchema.copy(elements = uiSchema.elements.map { mapUiSchema(it, transformLeaf) })
        is HorizontalLayout -> uiSchema.copy(elements = uiSchema.elements.map { mapUiSchema(it, transformLeaf) })
        is GroupElement -> uiSchema.copy(elements = uiSchema.elements.map { mapUiSchema(it, transformLeaf) })
        is com.chobantonov.jsonfroms.core.models.CategoryElement ->
            uiSchema.copy(elements = uiSchema.elements.map { mapUiSchema(it, transformLeaf) })
        is com.chobantonov.jsonfroms.core.models.CategorizationElement ->
            uiSchema.copy(elements = uiSchema.elements.map { mapUiSchema(it, transformLeaf) })
        is com.chobantonov.jsonfroms.core.models.UnknownUiSchemaElement ->
            if (uiSchema.elements.isEmpty()) transformLeaf(uiSchema) else uiSchema.copy(
                elements = uiSchema.elements.map { mapUiSchema(it, transformLeaf) },
            )
        else -> transformLeaf(uiSchema)
    }

private fun jsonObjectToUiSchema(element: JsonObject): UiSchemaElement? {
    val type = (element["type"] as? JsonPrimitive)?.content ?: return null
    return when (type) {
        "Control" -> {
            val scope = (element["scope"] as? JsonPrimitive)?.content ?: return null
            ControlElement(
                scope = scope,
                options = (element["options"] as? JsonObject).orEmpty(),
            )
        }
        "Label" -> {
            val text = (element["text"] as? JsonPrimitive)?.content ?: return null
            LabelElement(text = text)
        }
        "VerticalLayout" -> VerticalLayout(elements = element.uiSchemaElements())
        "HorizontalLayout" -> HorizontalLayout(elements = element.uiSchemaElements())
        "Group" -> GroupElement(
            label = (element["label"] as? JsonPrimitive)?.content,
            elements = element.uiSchemaElements(),
        )
        else -> com.chobantonov.jsonfroms.core.models.UnknownUiSchemaElement(
            type = type,
            options = (element["options"] as? JsonObject).orEmpty(),
            properties = JsonObject(element.filterKeys { it !in setOf("type", "options", "elements") }),
            elements = element.uiSchemaElements(),
        )
    }
}

private fun JsonObject.uiSchemaElements(): List<UiSchemaElement> =
    (this["elements"] as? JsonArray)
        ?.mapNotNull { child -> (child as? JsonObject)?.let(::jsonObjectToUiSchema) }
        .orEmpty()

private fun deriveLabel(
    controlElement: ControlElement,
    schema: JsonSchema?,
): String {
    val title = (schema?.get("title") as? JsonPrimitive)?.content
    if (title != null) {
        return title
    }
    return createCleanLabel(controlElement.scope.substringAfterLast('/').let(JsonPointer::decode))
}
