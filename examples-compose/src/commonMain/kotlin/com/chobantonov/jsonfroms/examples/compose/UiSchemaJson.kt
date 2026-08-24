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
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.LabelDescription
import com.chobantonov.jsonfroms.core.models.LabelElement
import com.chobantonov.jsonfroms.core.models.LeafCondition
import com.chobantonov.jsonfroms.core.models.ListWithDetailElement
import com.chobantonov.jsonfroms.core.models.OrCondition
import com.chobantonov.jsonfroms.core.models.Rule
import com.chobantonov.jsonfroms.core.models.RuleEffect
import com.chobantonov.jsonfroms.core.models.SchemaBasedCondition
import com.chobantonov.jsonfroms.core.models.StringControlLabel
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.models.UnknownUiSchemaElement
import com.chobantonov.jsonfroms.core.models.ValidateFunctionCondition
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal fun JsonObject.toUiSchemaElement(): UiSchemaElement {
    val type = requiredString("type")
    val rule = this["rule"]?.let { it.requiredObject("rule").toRule() }
    val options = optionalObject("options").orEmpty()
    val elements by lazy { optionalArray("elements")?.mapIndexed { index, child ->
        child.requiredObject("elements[$index]").toUiSchemaElement()
    }.orEmpty() }

    return when (type) {
        "VerticalLayout" -> VerticalLayout(rule, options, elements)
        "HorizontalLayout" -> HorizontalLayout(rule, options, elements)
        "Group" -> GroupElement(optionalString("label"), optionalString("i18n"), rule, options, elements)
        "Category" -> CategoryElement(requiredString("label"), optionalString("i18n"), rule, options, elements)
        "Categorization" -> CategorizationElement(optionalString("label").orEmpty(), optionalString("i18n"), rule, options, elements)
        "Label" -> LabelElement(requiredString("text"), optionalString("i18n"), rule, options)
        "Control" -> ControlElement(requiredString("scope"), controlLabel(), optionalString("i18n"), rule, options)
        "ListWithDetail" -> ListWithDetailElement(requiredString("scope"), optionalString("i18n"), rule, options)
        else -> UnknownUiSchemaElement(type, rule, options)
    }
}

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
        is UnknownUiSchemaElement -> Unit
        else -> throw IllegalArgumentException("Unsupported UI schema element '${element.type}'")
    }
    rule?.let { put("rule", it.toJsonObject()) }
    if (options.isNotEmpty()) put("options", JsonObject(options))
}

private fun JsonObject.controlLabel() = when (val label = this["label"]) {
    null, JsonNull -> null
    is JsonPrimitive -> if (label.isString) StringControlLabel(label.content) else when (label.content) {
        "true" -> BooleanControlLabel(true)
        "false" -> BooleanControlLabel(false)
        else -> throw IllegalArgumentException("label must be a string, boolean, or object")
    }
    is JsonObject -> DescriptionControlLabel(
        LabelDescription(label.optionalString("text"), label.optionalBoolean("show")),
    )
    else -> throw IllegalArgumentException("label must be a string, boolean, or object")
}

private fun JsonObject.toRule(): Rule {
    val effect = requiredString("effect")
    val parsedEffect = RuleEffect.entries.firstOrNull { it.name == effect.uppercase() }
        ?: throw IllegalArgumentException("Unknown rule effect '$effect'")
    return Rule(parsedEffect, this["condition"].requiredObject("condition").toCondition())
}

private fun JsonObject.toCondition(): Condition {
    val type = optionalString("type")
    return when {
        type == "AND" -> AndCondition(requiredConditions())
        type == "OR" -> OrCondition(requiredConditions())
        this["schema"] != null -> SchemaBasedCondition(
            scope = requiredString("scope"),
            schema = JsonSchema(this["schema"].requiredObject("schema")),
            failWhenUndefined = optionalBoolean("failWhenUndefined") ?: false,
            type = type,
        )
        type == null || type == "LEAF" -> LeafCondition(optionalString("scope"), this["expectedValue"], type ?: "LEAF")
        else -> throw IllegalArgumentException("Unsupported rule condition type '$type'")
    }
}

private fun JsonObject.requiredConditions(): List<Condition> =
    (this["conditions"] as? JsonArray)?.mapIndexed { index, condition ->
        condition.requiredObject("conditions[$index]").toCondition()
    } ?: throw IllegalArgumentException("conditions must be an array")

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

private fun JsonObject.requiredString(name: String): String =
    optionalString(name) ?: throw IllegalArgumentException("$name must be a string")

private fun JsonObject.optionalString(name: String): String? =
    (this[name] as? JsonPrimitive)?.takeIf { it.isString }?.content

private fun JsonObject.optionalBoolean(name: String): Boolean? = when ((this[name] as? JsonPrimitive)?.content) {
    "true" -> true
    "false" -> false
    else -> null
}

private fun JsonObject.optionalObject(name: String): JsonObject? = when (val value = this[name]) {
    null, JsonNull -> null
    is JsonObject -> value
    else -> throw IllegalArgumentException("$name must be an object")
}

private fun JsonObject.optionalArray(name: String): JsonArray? = when (val value = this[name]) {
    null, JsonNull -> null
    is JsonArray -> value
    else -> throw IllegalArgumentException("$name must be an array")
}

private fun JsonElement?.requiredObject(name: String): JsonObject =
    this as? JsonObject ?: throw IllegalArgumentException("$name must be an object")
