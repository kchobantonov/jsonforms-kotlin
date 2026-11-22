package com.chobantonov.jsonfroms.examples.compose

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

enum class DemoBrowserRuntime(val label: String) {
    Js("Kotlin/JS"),
    Wasm("Kotlin/Wasm"),
}

/** The one-time form state passed between the two browser builds. */
data class DemoRuntimeHandoff(
    val target: DemoBrowserRuntime,
    val exampleName: String,
    val data: JsonElement,
    val dataProvided: Boolean,
)

fun DemoRuntimeHandoff.encode(): String = JsonObject(mapOf(
    "version" to JsonPrimitive(1),
    "target" to JsonPrimitive(target.name),
    "example" to JsonPrimitive(exampleName),
    "data" to data,
    "dataProvided" to JsonPrimitive(dataProvided),
)).toString()

fun decodeDemoRuntimeHandoff(value: String?): DemoRuntimeHandoff? = runCatching {
    val fields = Json.parseToJsonElement(value ?: return null) as? JsonObject ?: return null
    if ((fields["version"] as? JsonPrimitive)?.contentOrNull != "1") return null
    val target = DemoBrowserRuntime.entries.firstOrNull {
        it.name == (fields["target"] as? JsonPrimitive)?.contentOrNull
    } ?: return null
    val example = (fields["example"] as? JsonPrimitive)?.takeIf { it.isString }
        ?.contentOrNull?.takeIf(String::isNotBlank) ?: return null
    val data = fields["data"] ?: return null
    val provided = (fields["dataProvided"] as? JsonPrimitive)?.booleanOrNull ?: return null
    DemoRuntimeHandoff(target, example, data, provided)
}.getOrNull()

/** Preserve the GitHub Pages project prefix while switching between / and /wasm/. */
fun demoRuntimePath(pathname: String, current: DemoBrowserRuntime, target: DemoBrowserRuntime): String {
    val directory = pathname.removeSuffix("index.html").let { if (it.endsWith('/')) it else "$it/" }
    val base = if (current == DemoBrowserRuntime.Wasm && directory.endsWith("/wasm/"))
        directory.removeSuffix("wasm/") else directory
    return if (target == DemoBrowserRuntime.Wasm) "${base}wasm/" else base
}

private const val HandoffParameter = "runtime-handoff"

fun demoRuntimeHandoffToken(search: String): String? = search.removePrefix("?")
    .split('&').firstOrNull { it.substringBefore('=') == HandoffParameter }
    ?.substringAfter('=', "")?.takeIf { token -> token.isNotEmpty() && token.all { it.isLetterOrDigit() } }

fun withoutDemoRuntimeHandoffToken(search: String): String = search.removePrefix("?")
    .split('&').filter { it.isNotEmpty() && it.substringBefore('=') != HandoffParameter }
    .joinToString("&").let { if (it.isEmpty()) "" else "?$it" }

fun withDemoRuntimeHandoffToken(search: String, token: String): String {
    require(token.isNotEmpty() && token.all(Char::isLetterOrDigit))
    val clean = withoutDemoRuntimeHandoffToken(search)
    return clean + if (clean.isEmpty()) "?$HandoffParameter=$token" else "&$HandoffParameter=$token"
}

internal fun demoRuntimeDestination(
    pathname: String, search: String, hash: String,
    current: DemoBrowserRuntime, target: DemoBrowserRuntime,
    handoff: DemoRuntimeHandoff?, token: String?,
): String {
    val cleanSearch = withoutDemoRuntimeHandoffToken(search)
    val route = parseDemoUrl(cleanSearch, hash)
    val next = route.copy(exampleName = handoff?.exampleName ?: route.exampleName)
    val (nextSearch, nextHash) = writeDemoUrl(cleanSearch, hash, next)
    return demoRuntimePath(pathname, current, target) +
        (if (token == null) nextSearch else withDemoRuntimeHandoffToken(nextSearch, token)) + nextHash
}
