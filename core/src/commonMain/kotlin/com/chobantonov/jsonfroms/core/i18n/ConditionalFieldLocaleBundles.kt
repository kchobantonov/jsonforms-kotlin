package com.chobantonov.jsonfroms.core.i18n

import com.chobantonov.jsonfroms.core.mappers.ConditionalFieldDiagnostic
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private val conditionalFieldLocaleBundles = mapOf(
    "bg" to JsonObject(mapOf(
        "conditionalFields.discoveryDepth" to JsonPrimitive("Откриването на условни полета надхвърли поддържаната дълбочина."),
        "conditionalFields.conditionCompilationFailure" to JsonPrimitive("Условието за полетата не може да бъде оценено."),
    )),
)

fun translateConditionalFieldDiagnostic(
    i18n: JsonFormsI18nState,
    diagnostic: ConditionalFieldDiagnostic,
): String {
    val (key, default) = when (diagnostic) {
        ConditionalFieldDiagnostic.DiscoveryDepth ->
            "conditionalFields.discoveryDepth" to "Conditional field discovery exceeded the supported depth."
        ConditionalFieldDiagnostic.ConditionCompilationFailure ->
            "conditionalFields.conditionCompilationFailure" to "Unable to evaluate the conditional field schema."
    }
    return translateRendererText(i18n.translate, i18n.locale, key, default,
        conditionalFieldLocaleBundles)
}
