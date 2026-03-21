package com.chobantonov.jsonfroms.core.models

import kotlinx.serialization.json.JsonElement

enum class RuleEffect {
    SHOW,
    HIDE,
    ENABLE,
    DISABLE,
    READONLY,
    WRITABLE,
}

interface Scopable {
    val scope: String?
}

interface Scoped : Scopable {
    override val scope: String
}

interface Labelable<T> {
    val label: T?
}

interface Labeled<T> : Labelable<T> {
    override val label: T
}

interface Internationalizable {
    val i18n: String?
}

sealed interface Condition {
    val type: String?
}

data class LeafCondition(
    override val scope: String? = null,
    val expectedValue: JsonElement? = null,
    override val type: String = "LEAF",
) : Condition, Scopable

data class SchemaBasedCondition(
    override val scope: String,
    val schema: JsonSchema,
    val failWhenUndefined: Boolean = false,
    override val type: String? = null,
) : Condition, Scoped

data class ValidateFunctionContext(
    val data: JsonElement?,
    val fullData: JsonElement,
    val path: String? = null,
    val uischemaElement: UiSchemaElement,
    val config: Any? = null,
)

data class ValidateFunctionCondition(
    override val scope: String,
    val validate: (ValidateFunctionContext) -> Boolean,
    override val type: String? = null,
) : Condition, Scoped

typealias PredicateCondition = ValidateFunctionCondition

data class AndCondition(
    val conditions: List<Condition>,
    override val type: String = "AND",
) : Condition

data class OrCondition(
    val conditions: List<Condition>,
    override val type: String = "OR",
) : Condition

data class Rule(
    val effect: RuleEffect,
    val condition: Condition,
)

sealed interface UiSchemaElement : BaseUiSchemaElement

interface BaseUiSchemaElement {
    val type: String
    val rule: Rule?
    val options: JsonOptions
}

sealed interface ScopedUiSchemaElement : UiSchemaElement, Scoped

interface Layout : UiSchemaElement {
    val elements: List<UiSchemaElement>
}

interface GroupLayout : Layout, Labelable<String>, Internationalizable

interface Category : Layout, Labeled<String>, Internationalizable

interface Categorization : UiSchemaElement, Labeled<String>, Internationalizable {
    val elements: List<UiSchemaElement>
}

data class LabelDescription(
    val text: String? = null,
    val show: Boolean? = null,
)

sealed interface ControlLabel

data class StringControlLabel(
    val value: String,
) : ControlLabel

data class BooleanControlLabel(
    val value: Boolean,
) : ControlLabel

data class DescriptionControlLabel(
    val value: LabelDescription,
) : ControlLabel

data class VerticalLayout(
    override val rule: Rule? = null,
    override val options: JsonOptions = emptyMap(),
    override val elements: List<UiSchemaElement> = emptyList(),
) : Layout {
    override val type: String = "VerticalLayout"
}

data class HorizontalLayout(
    override val rule: Rule? = null,
    override val options: JsonOptions = emptyMap(),
    override val elements: List<UiSchemaElement> = emptyList(),
) : Layout {
    override val type: String = "HorizontalLayout"
}

data class GroupElement(
    override val label: String? = null,
    override val i18n: String? = null,
    override val rule: Rule? = null,
    override val options: JsonOptions = emptyMap(),
    override val elements: List<UiSchemaElement> = emptyList(),
) : GroupLayout {
    override val type: String = "Group"
}

data class LabelElement(
    val text: String,
    override val i18n: String? = null,
    override val rule: Rule? = null,
    override val options: JsonOptions = emptyMap(),
) : UiSchemaElement, Internationalizable {
    override val type: String = "Label"
}

data class ControlElement(
    override val scope: String,
    override val label: ControlLabel? = null,
    override val i18n: String? = null,
    override val rule: Rule? = null,
    override val options: JsonOptions = emptyMap(),
) : ScopedUiSchemaElement, Labelable<ControlLabel>, Internationalizable {
    override val type: String = "Control"
}

data class ListWithDetailElement(
    override val scope: String,
    override val i18n: String? = null,
    override val rule: Rule? = null,
    override val options: JsonOptions = emptyMap(),
) : ScopedUiSchemaElement, Internationalizable {
    override val type: String = "ListWithDetail"
}

data class CategoryElement(
    override val label: String,
    override val i18n: String? = null,
    override val rule: Rule? = null,
    override val options: JsonOptions = emptyMap(),
    override val elements: List<UiSchemaElement> = emptyList(),
) : Category {
    override val type: String = "Category"
}

data class CategorizationElement(
    override val label: String = "",
    override val i18n: String? = null,
    override val rule: Rule? = null,
    override val options: JsonOptions = emptyMap(),
    override val elements: List<UiSchemaElement> = emptyList(),
) : Categorization {
    override val type: String = "Categorization"
}

data class UnknownUiSchemaElement(
    override val type: String,
    override val rule: Rule? = null,
    override val options: JsonOptions = emptyMap(),
) : UiSchemaElement
