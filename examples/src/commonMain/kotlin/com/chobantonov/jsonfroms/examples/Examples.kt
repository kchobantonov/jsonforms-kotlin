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
    } + specSnapshotExamples
}

fun registerBuiltInExamples() {
    clearExamples()
    registerExamples(builtInCatalog)
}

fun getBuiltInExamples(): List<ExampleDescription> {
    registerBuiltInExamples()
    return getExamples()
}
