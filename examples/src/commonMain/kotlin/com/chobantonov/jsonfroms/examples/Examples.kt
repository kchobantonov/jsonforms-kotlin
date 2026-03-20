package com.chobantonov.jsonfroms.examples

import com.chobantonov.jsonfroms.examples.examples.layoutExamples
import com.chobantonov.jsonfroms.examples.examples.personExample
import com.chobantonov.jsonfroms.examples.examples.textExample

private val builtInExamples = listOf(textExample) + layoutExamples + listOf(personExample)

fun registerBuiltInExamples() {
    clearExamples()
    registerExamples(builtInExamples)
}

fun getBuiltInExamples(): List<ExampleDescription> {
    registerBuiltInExamples()
    return getExamples()
}
