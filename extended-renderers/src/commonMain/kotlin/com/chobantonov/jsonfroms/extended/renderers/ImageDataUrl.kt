package com.chobantonov.jsonfroms.extended.renderers

/** Detects malformed base64 data URLs before handing them to platform image loaders. */
internal fun malformedBase64ImageDataUrl(url: String): Boolean {
    if (!url.startsWith("data:", ignoreCase = true)) return false
    val comma = url.indexOf(',')
    if (comma < 0) return true
    val metadata = url.substring(5, comma)
    if (!metadata.split(';').any { it.equals("base64", ignoreCase = true) }) return false
    val encoded = url.substring(comma + 1)
    if (encoded.isEmpty()) return true
    val payload = StringBuilder(encoded.length)
    var index = 0
    while (index < encoded.length) {
        val char = encoded[index]
        if (char == '%') {
            if (index + 2 >= encoded.length) return true
            val high = encoded[index + 1].digitToIntOrNull(16) ?: return true
            val low = encoded[index + 2].digitToIntOrNull(16) ?: return true
            payload.append((high * 16 + low).toChar())
            index += 3
        } else {
            payload.append(char)
            index++
        }
    }
    val value = payload.toString()
    val padding = value.takeLastWhile { it == '=' }.length
    if (padding > 2 || (padding > 0 && value.length % 4 != 0) || value.length % 4 == 1) return true
    return value.dropLast(padding).any { it !in 'A'..'Z' && it !in 'a'..'z' && it !in '0'..'9' && it != '+' && it != '/' }
}
