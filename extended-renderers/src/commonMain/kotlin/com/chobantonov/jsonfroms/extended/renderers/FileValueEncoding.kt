package com.chobantonov.jsonfroms.extended.renderers

import com.chobantonov.jsonfroms.core.models.JsonSchema
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Converts an already-read local file into the project's portable file-control storage convention.
 * The caller is responsible for reading bytes, checking their original size, and base64 encoding.
 */
fun encodedFileValue(schema: JsonSchema, base64: String, mimeType: String?, filename: String): JsonPrimitive {
    if (schema.format != "binary") {
        val encoding = (schema["contentEncoding"] as? JsonPrimitive)?.content
        if (encoding == "base64" || schema.format == "byte") return JsonPrimitive(base64)
    }
    val mime = mimeType?.takeIf(String::isNotBlank) ?: "application/octet-stream"
    return JsonPrimitive("data:$mime;filename=${encodeFileName(filename)};base64,$base64")
}

/** Decodes the binary data-URL filename for bounded, accessible file labels. */
fun fileNameFromStoredValue(value: JsonElement?): String? {
    val stored = (value as? JsonPrimitive)?.contentOrNull ?: return null
    val encoded = stored.substringAfter(";filename=", "").substringBefore(';').takeIf(String::isNotEmpty)
        ?: return null
    val raw = encoded.encodeToByteArray()
    val decoded = ArrayList<Byte>(raw.size)
    var index = 0
    while (index < raw.size) {
        if (raw[index] == '%'.code.toByte() && index + 2 < raw.size) {
            val high = raw[index + 1].toInt().toChar().digitToIntOrNull(16)
            val low = raw[index + 2].toInt().toChar().digitToIntOrNull(16)
            if (high != null && low != null) {
                decoded += ((high shl 4) or low).toByte()
                index += 3
                continue
            }
        }
        decoded += raw[index]
        index++
    }
    return decoded.toByteArray().decodeToString()
}

private fun encodeFileName(value: String): String = buildString {
    val hex = "0123456789ABCDEF"
    value.encodeToByteArray().forEach { byte ->
        val number = byte.toInt() and 0xFF
        val safe = number in 'a'.code..'z'.code || number in 'A'.code..'Z'.code ||
            number in '0'.code..'9'.code || number == '-'.code || number == '_'.code ||
            number == '.'.code || number == '~'.code
        if (safe) append(number.toChar())
        else {
            append('%')
            append(hex[number ushr 4])
            append(hex[number and 15])
        }
    }
}
