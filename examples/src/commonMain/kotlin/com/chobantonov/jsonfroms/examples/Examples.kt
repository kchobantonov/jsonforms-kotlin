package com.chobantonov.jsonfroms.examples

import com.chobantonov.jsonfroms.examples.upstream.behavior.additionalErrorsExample
import com.chobantonov.jsonfroms.examples.upstream.behavior.categorizationExample
import com.chobantonov.jsonfroms.examples.upstream.behavior.i18nExample
import com.chobantonov.jsonfroms.examples.upstream.behavior.listWithDetailRegisteredExample
import com.chobantonov.jsonfroms.examples.upstream.behavior.onChangeExample
import com.chobantonov.jsonfroms.examples.upstream.behavior.ruleExample
import com.chobantonov.jsonfroms.examples.upstream.behavior.ruleInheritanceExample

private val behaviorExamples = listOf(
    additionalErrorsExample,
    categorizationExample,
    i18nExample,
    listWithDetailRegisteredExample,
    onChangeExample,
    ruleExample,
    ruleInheritanceExample,
)

private val builtInCatalog by lazy {
    val behaviorByName = behaviorExamples.associateBy(ExampleDescription::name)
    upstreamExamples.map { upstream ->
        val behavior = behaviorByName[upstream.name]
        if (behavior == null) upstream else upstream.copy(
            actions = upstream.actions.ifEmpty { behavior.actions },
            additionalErrors = upstream.additionalErrors.ifEmpty { behavior.additionalErrors },
            middleware = upstream.middleware ?: behavior.middleware,
            i18n = upstream.i18n ?: behavior.i18n,
            uischemas = upstream.uischemas.ifEmpty { behavior.uischemas },
            uischema = if (upstream.name == "rule") behavior.uischema else upstream.uischema,
        )
    }.map { it.copy(featureGroup = builtInFeatureGroup(it.name)) } +
        specSnapshotExamples.map { it.copy(featureGroup = builtInFeatureGroup(it.name)) }
}

/** A curated demo taxonomy; IDs and source links remain independent of presentation groups. */
internal fun builtInFeatureGroup(name: String): ExampleFeatureGroup = when (name.removePrefix("spec-")) {
    "array-choices", "array-controls", "array-detail-modes", "additional-items-pagination",
    "collection-pagination", "property-pagination", "table-row-details", "tuple-control",
    "array", "array-i18n", "enumInArray", "stringArray", "array-with-custom-element-label",
    "array-with-defaults", "array-with-detail", "array-with-detail-and-rule",
    "array-with-sorting", "array-with-translated-custom-element-label", "nestedArray",
    "oneOfArray", "oneOf_1265_array", "list-with-detail", "list-with-detail-no-labelref",
    "list-with-detail-registered", "list-with-detail-primitive-number",
    "list-with-detail-primitive-string" -> ExampleFeatureGroup.Collections

    "boolean-controls", "choice-controls", "color-control", "cron-control", "file-control",
    "input-adornments", "mixed-control", "null-control", "numeric-controls", "object-control",
    "password-control", "string-controls", "temporal-controls", "control-options",
    "dates", "enum", "multi-enum", "multi-enum-with-label-and-desc", "numbers",
    "object", "rootObject", "radio-group", "string", "text", "login",
    "Readonly Fields", "special-property-names" -> ExampleFeatureGroup.Controls

    "categorization", "group-layout", "layout-sizing", "presentation", "split-layout",
    "layout-complex", "layout-group", "layout-horizontal", "layout-vertical",
    "nestedCategorization", "categorization_1713", "categorization_2343",
    "categorization-stepper-nav-buttons", "categorizationstepper",
    "prepend-append-slots" -> ExampleFeatureGroup.Layout

    "combinators", "conditional-fields", "recursive-tree", "tree-max-depth",
    "allOf", "anyOf", "anyOf-oneOf-allOf-resolve", "anyOf_simple", "ifthenelse",
    "oneOf", "oneOf_1273", "oneOf_1273_simple", "oneOf_1265_object",
    "oneOf_1265_simple", "oneOf-recursive", "conditional-schema-compositions",
    "rule", "rule-enable", "rule-inheritance" -> ExampleFeatureGroup.Composition

    "additional-errors", "container-validation-indicator", "destructive-confirmation",
    "pre-touch-errors", "validator-profile", "validation-nested-same-name" ->
        ExampleFeatureGroup.Validation

    "label-interpolation", "i18n", "enum-i18n" -> ExampleFeatureGroup.Internationalization

    "ag-grid", "button-actions", "code-editor", "draft-07-metaschema",
    "dynamic-playground", "editor-details", "json-editor", "json-inference",
    "job-application", "kitchen-sink", "markup-label", "template-layout", "template-slots",
    "additional-properties", "configCustom", "configDefault", "default", "dynamic",
    "generate", "generate-ui", "huge", "jsonschema", "mixed", "mixed-object",
    "onChange", "person", "scope", "1884", "1948_without", "1948_with" ->
        ExampleFeatureGroup.Advanced

    else -> ExampleFeatureGroup.Other
}

fun registerBuiltInExamples() {
    clearExamples()
    registerExamples(builtInCatalog)
}

fun getBuiltInExamples(): List<ExampleDescription> {
    registerBuiltInExamples()
    return getExamples()
}
