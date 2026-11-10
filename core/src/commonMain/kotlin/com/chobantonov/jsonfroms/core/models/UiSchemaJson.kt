package com.chobantonov.jsonfroms.core.models

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Parses the JSON Forms UI schema format used by browser integrations and editors. */
fun JsonObject.toUiSchemaElement(): UiSchemaElement {
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
        else -> UnknownUiSchemaElement(
            type = type,
            rule = rule,
            options = options,
            properties = JsonObject(filterKeys { it !in setOf("type", "rule", "options", "elements") }),
            elements = elements,
        )
    }
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
