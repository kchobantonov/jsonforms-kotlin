package com.chobantonov.jsonfroms.examples.compose

private const val ExampleRoutePrefix = "/examples/"

internal fun exampleNameFromHash(hash: String): String? {
    val route = hash.removePrefix("#").substringBefore('?')
    if (!route.startsWith(ExampleRoutePrefix)) return null
    return route.removePrefix(ExampleRoutePrefix).takeIf { it.isNotBlank() && '/' !in it }
}

internal fun exampleHash(name: String): String = "#$ExampleRoutePrefix$name"
