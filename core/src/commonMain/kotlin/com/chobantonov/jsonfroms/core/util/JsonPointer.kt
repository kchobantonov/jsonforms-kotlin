package com.chobantonov.jsonfroms.core.util

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

object JsonPointer {
    fun resolve(root: JsonElement?, pointer: String?): JsonElement? {
        if (root == null || pointer.isNullOrBlank() || pointer == "#") {
            return root
        }

        val normalized = pointer.removePrefix("#")
        if (normalized.isBlank() || normalized == "/") {
            return root
        }

        var current: JsonElement? = root
        normalized
            .split('/')
            .filter { it.isNotEmpty() }
            .map(::decode)
            .forEach { segment ->
                current = when (val value = current) {
                    is JsonObject -> value[segment]
                    is JsonArray -> segment.toIntOrNull()?.let(value::getOrNull)
                    else -> null
                }
            }

        return current
    }

    fun decode(segment: String): String =
        segment
            .replace("~1", "/")
            .replace("~0", "~")

    fun encode(segment: String): String =
        segment
            .replace("~", "~0")
            .replace("/", "~1")
}
