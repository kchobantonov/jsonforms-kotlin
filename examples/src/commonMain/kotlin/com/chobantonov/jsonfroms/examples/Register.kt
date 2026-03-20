package com.chobantonov.jsonfroms.examples

private val knownExamples = linkedMapOf<String, ExampleDescription>()

fun registerExamples(examples: List<ExampleDescription>) {
    examples.forEach { example ->
        knownExamples[example.name] = example
    }
}

fun clearExamples() {
    knownExamples.clear()
}

fun getExamples(): List<ExampleDescription> = knownExamples.values.sortedBy(ExampleDescription::label)
