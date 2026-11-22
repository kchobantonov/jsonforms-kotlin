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
        "VerticalLayout" -> VerticalLayout(rule, options, elements, optionalString("name"))
        "HorizontalLayout" -> HorizontalLayout(rule, options, elements, optionalString("name"))
        "Group" -> GroupElement(
            label = optionalString("label"), i18n = optionalString("i18n"), rule = rule,
            options = options, elements = elements, name = optionalString("name"),
            cover = this["cover"]?.requiredObject("cover")?.toUiSchemaElement(),
            header = optionalArray("header")?.mapIndexed { index, child ->
                child.requiredObject("header[$index]").toUiSchemaElement()
            }.orEmpty(),
            footer = optionalArray("footer")?.mapIndexed { index, child ->
                child.requiredObject("footer[$index]").toUiSchemaElement()
            }.orEmpty(),
        )
        "Category" -> CategoryElement(requiredString("label"), optionalString("i18n"), rule, options, elements, optionalString("name"))
        "Categorization" -> CategorizationElement(optionalString("label").orEmpty(), optionalString("i18n"), rule, options, elements, optionalString("name"))
        "Label" -> LabelElement(requiredString("text"), optionalString("i18n"), rule, options, optionalString("name"))
        "Spacer" -> SpacerElement(this["size"], rule, options, optionalString("name"))
        "Separator" -> SeparatorElement(rule, options, optionalString("name"))
        "Link" -> LinkElement(requiredString("href"), optionalString("label"), optionalString("target"),
            optionalString("rel"), optionalString("i18n"), rule, options, optionalString("name"))
        "ImageView" -> ImageViewElement(
            src = optionalImageField("src"),
            scope = optionalImageField("scope"),
            alt = optionalImageField("alt"),
            i18n = optionalString("i18n"),
            rule = rule,
            options = options,
            name = optionalString("name"),
        )
        "Control" -> ControlElement(requiredString("scope"), controlLabel(), optionalString("i18n"), rule, options, optionalString("name"))
        "ListWithDetail" -> ListWithDetailElement(requiredString("scope"), optionalString("i18n"), rule, options, optionalString("name"))
        else -> UnknownUiSchemaElement(
            type = type,
            rule = rule,
            options = options,
            properties = JsonObject(filterKeys { it !in setOf("type", "rule", "options", "elements") }),
            elements = elements,
            elementsPresent = "elements" in this,
            optionsPresent = "options" in this,
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

private fun JsonObject.optionalImageField(name: String): String? {
    if (name !in this) return null
    return optionalString(name) ?: throw IllegalArgumentException("$name must be a string")
}

private fun JsonObject.optionalString(name: String): String? =
    (this[name] as? JsonPrimitive)?.takeIf { it.isString }?.content

private fun JsonObject.optionalBoolean(name: String): Boolean? =
    (this[name] as? JsonPrimitive)?.takeUnless { it.isString }?.let {
        when (it.content) {
            "true" -> true
            "false" -> false
            else -> null
        }
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
