package com.chobantonov.jsonfroms.examples.compose

import com.chobantonov.jsonfroms.core.models.AndCondition
import com.chobantonov.jsonfroms.core.models.BooleanControlLabel
import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.Condition
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.DescriptionControlLabel
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.models.LabelElement
import com.chobantonov.jsonfroms.core.models.LeafCondition
import com.chobantonov.jsonfroms.core.models.ListWithDetailElement
import com.chobantonov.jsonfroms.core.models.OrCondition
import com.chobantonov.jsonfroms.core.models.Rule
import com.chobantonov.jsonfroms.core.models.SchemaBasedCondition
import com.chobantonov.jsonfroms.core.models.StringControlLabel
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.models.UnknownUiSchemaElement
import com.chobantonov.jsonfroms.core.models.ValidateFunctionCondition
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal fun UiSchemaElement.toJsonObject(): JsonObject = buildJsonObject {
    put("type", type)
    when (val element = this@toJsonObject) {
        is ControlElement -> {
            put("scope", element.scope)
            element.label?.let { label ->
                put("label", when (label) {
                    is StringControlLabel -> JsonPrimitive(label.value)
                    is BooleanControlLabel -> JsonPrimitive(label.value)
                    is DescriptionControlLabel -> buildJsonObject {
                        label.value.text?.let { put("text", it) }
                        label.value.show?.let { put("show", it) }
                    }
                })
            }
            element.i18n?.let { put("i18n", it) }
        }
        is ListWithDetailElement -> {
            put("scope", element.scope)
            element.i18n?.let { put("i18n", it) }
        }
        is LabelElement -> {
            put("text", element.text)
            element.i18n?.let { put("i18n", it) }
        }
        is GroupElement -> {
            element.label?.let { put("label", it) }
            element.i18n?.let { put("i18n", it) }
            put("elements", JsonArray(element.elements.map(UiSchemaElement::toJsonObject)))
        }
        is CategoryElement -> {
            put("label", element.label)
            element.i18n?.let { put("i18n", it) }
            put("elements", JsonArray(element.elements.map(UiSchemaElement::toJsonObject)))
        }
        is CategorizationElement -> {
            if (element.label.isNotEmpty()) put("label", element.label)
            element.i18n?.let { put("i18n", it) }
            put("elements", JsonArray(element.elements.map(UiSchemaElement::toJsonObject)))
        }
        is VerticalLayout -> put("elements", JsonArray(element.elements.map(UiSchemaElement::toJsonObject)))
        is HorizontalLayout -> put("elements", JsonArray(element.elements.map(UiSchemaElement::toJsonObject)))
        is UnknownUiSchemaElement -> {
            element.properties.forEach { (name, value) -> put(name, value) }
            if (element.elements.isNotEmpty()) {
                put("elements", JsonArray(element.elements.map(UiSchemaElement::toJsonObject)))
            }
        }
        else -> throw IllegalArgumentException("Unsupported UI schema element '${element.type}'")
    }
    rule?.let { put("rule", it.toJsonObject()) }
    if (options.isNotEmpty()) put("options", JsonObject(options))
}

private fun Rule.toJsonObject() = buildJsonObject {
    put("effect", effect.name)
    put("condition", condition.toJsonObject())
}

private fun Condition.toJsonObject(): JsonObject = when (this) {
    is AndCondition -> buildJsonObject {
        put("type", type)
        put("conditions", buildJsonArray { conditions.forEach { add(it.toJsonObject()) } })
    }
    is OrCondition -> buildJsonObject {
        put("type", type)
        put("conditions", buildJsonArray { conditions.forEach { add(it.toJsonObject()) } })
    }
    is LeafCondition -> buildJsonObject {
        put("type", type)
        scope?.let { put("scope", it) }
        expectedValue?.let { put("expectedValue", it) }
    }
    is SchemaBasedCondition -> buildJsonObject {
        type?.let { put("type", it) }
        put("scope", scope)
        put("schema", schema.toJsonObject())
        if (failWhenUndefined) put("failWhenUndefined", true)
    }
    is ValidateFunctionCondition -> throw IllegalArgumentException("Function-based rule conditions cannot be represented as JSON")
}
