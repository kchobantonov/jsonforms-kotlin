package com.chobantonov.jsonfroms.examples.compose

private const val LegacyExampleRoutePrefix = "/examples/"
private val SupportedQueryKeys = setOf("read-only", "form-only", "active-tab", "use-webcomponent", "drawer")

data class DemoUrlViewState(
    val readonly: Boolean = false,
    val formOnly: Boolean = false,
    val activePane: DemoPane = DemoPane.Demo,
    val webComponent: Boolean = false,
    val drawer: Boolean? = null,
)

internal data class DemoUrlState(val exampleName: String?, val view: DemoUrlViewState = DemoUrlViewState())

internal fun exampleNameFromHash(hash: String): String? {
    val route = hash.removePrefix("#").substringBefore('?')
    val encoded = when {
        route.startsWith(LegacyExampleRoutePrefix) -> route.removePrefix(LegacyExampleRoutePrefix)
        route.startsWith('/') -> route.removePrefix("/")
        else -> route
    }
    if (encoded.isBlank() || '/' in encoded) return null
    return decodeUrlComponent(encoded).takeIf(String::isNotBlank)
}

internal fun exampleHash(name: String): String = "#${encodeUrlComponent(name)}"

internal fun parseDemoUrl(search: String, hash: String): DemoUrlState {
    val normal = parseQuery(search.removePrefix("?"))
    val hashQuery = parseQuery(hash.substringAfter('?', ""))
    fun parameter(key: String): String? = hashQuery.firstOrNull { it.first == key }?.second
        ?: normal.firstOrNull { it.first == key }?.second
    val activePane = when (parameter("active-tab")) {
        "data" -> DemoPane.Data
        "schema" -> DemoPane.Schema
        "uiSchema" -> DemoPane.UiSchema
        "uiSchemas" -> DemoPane.UiSchemas
        "internationalization" -> DemoPane.Internationalization
        "config" -> DemoPane.Config
        else -> DemoPane.Demo
    }
    return DemoUrlState(exampleNameFromHash(hash), DemoUrlViewState(
        readonly = parameter("read-only") == "true",
        formOnly = parameter("form-only") == "true",
        activePane = activePane,
        webComponent = parameter("use-webcomponent") == "true",
        drawer = parameter("drawer")?.let { it == "true" },
    ))
}

/** Returns the normal query and hash, keeping unrelated parameters in their original location. */
internal fun writeDemoUrl(search: String, hash: String, state: DemoUrlState): Pair<String, String> {
    val normal = parseQuery(search.removePrefix("?")).filterNot { it.first in SupportedQueryKeys }
    val hashOther = parseQuery(hash.substringAfter('?', "")).filterNot { it.first in SupportedQueryKeys }
    val view = state.view
    val supported = buildList {
        if (view.readonly) add("read-only" to "true")
        if (view.formOnly) add("form-only" to "true")
        if (view.activePane != DemoPane.Demo) add("active-tab" to when (view.activePane) {
            DemoPane.Data -> "data"
            DemoPane.Schema -> "schema"
            DemoPane.UiSchema -> "uiSchema"
            DemoPane.UiSchemas -> "uiSchemas"
            DemoPane.Internationalization -> "internationalization"
            DemoPane.Config -> "config"
            DemoPane.Demo -> error("The default tab is omitted")
        })
        if (view.webComponent) add("use-webcomponent" to "true")
        view.drawer?.let { add("drawer" to it.toString()) }
    }
    val query = (hashOther + supported).toQueryString()
    val route = state.exampleName?.let(::exampleHash) ?: if (query.isEmpty()) "" else "#"
    return normal.toQueryString().let { if (it.isEmpty()) "" else "?$it" } to
        (route + if (query.isEmpty()) "" else "?$query")
}

private fun parseQuery(query: String): List<Pair<String, String>> = query.split('&').mapNotNull { part ->
    if (part.isEmpty()) null else decodeUrlComponent(part.substringBefore('='), plusAsSpace = true) to
        decodeUrlComponent(part.substringAfter('=', ""), plusAsSpace = true)
}

private fun List<Pair<String, String>>.toQueryString(): String =
    joinToString("&") { (key, value) -> "${encodeUrlComponent(key)}=${encodeUrlComponent(value)}" }

private val HexDigits = "0123456789ABCDEF"

private fun encodeUrlComponent(value: String): String = buildString {
    value.encodeToByteArray().forEach { byte ->
        val octet = byte.toInt() and 0xff
        if (octet in 65..90 || octet in 97..122 || octet in 48..57 || octet == 45 ||
            octet == 46 || octet == 95 || octet == 126) append(octet.toChar())
        else {
            append('%')
            append(HexDigits[octet shr 4])
            append(HexDigits[octet and 15])
        }
    }
}

private fun decodeUrlComponent(value: String, plusAsSpace: Boolean = false): String = buildString {
    var index = 0
    while (index < value.length) {
        if (value[index] == '%' && index + 2 < value.length) {
            val bytes = mutableListOf<Byte>()
            while (index + 2 < value.length && value[index] == '%') {
                val high = value[index + 1].digitToIntOrNull(16) ?: break
                val low = value[index + 2].digitToIntOrNull(16) ?: break
                bytes += ((high shl 4) or low).toByte()
                index += 3
            }
            if (bytes.isNotEmpty()) {
                append(bytes.toByteArray().decodeToString())
                continue
            }
        }
        append(if (plusAsSpace && value[index] == '+') ' ' else value[index])
        index++
    }
}
