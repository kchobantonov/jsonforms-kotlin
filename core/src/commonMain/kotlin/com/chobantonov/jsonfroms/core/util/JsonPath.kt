package com.chobantonov.jsonfroms.core.util

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object JsonPath {
    fun get(root: JsonElement?, path: String): JsonElement? {
        if (root == null || path.isBlank()) {
            return root
        }

        var current: JsonElement? = root
        parse(path).forEach { segment ->
            current = when {
                current == null -> null
                segment is PathSegment.Property && current is JsonObject -> (current as JsonObject)[segment.name]
                segment is PathSegment.Index && current is JsonArray -> (current as JsonArray).getOrNull(segment.index)
                else -> null
            }
        }
        return current
    }

    fun set(
        root: JsonElement?,
        path: String,
        value: JsonElement?,
    ): JsonElement {
        val segments = parse(path)
        if (segments.isEmpty()) {
            return value ?: JsonNull
        }

        return setAt(root ?: JsonObject(emptyMap()), segments, value)
    }

    private fun setAt(
        current: JsonElement?,
        segments: List<PathSegment>,
        value: JsonElement?,
    ): JsonElement {
        val segment = segments.first()
        val remaining = segments.drop(1)

        return when (segment) {
            is PathSegment.Property -> {
                val source = current as? JsonObject ?: JsonObject(emptyMap())
                val updatedValue = if (remaining.isEmpty()) {
                    value
                } else {
                    setAt(source[segment.name], remaining, value)
                }

                buildJsonObject {
                    source.forEach { (key, existing) ->
                        if (key != segment.name) {
                            put(key, existing)
                        }
                    }
                    if (updatedValue != null) {
                        put(segment.name, updatedValue)
                    }
                }
            }

            is PathSegment.Index -> {
                val source = current as? JsonArray ?: JsonArray(emptyList())
                val mutable = source.toMutableList()
                while (mutable.size <= segment.index) {
                    mutable += JsonNull
                }

                if (remaining.isEmpty()) {
                    if (value == null) {
                        if (segment.index in mutable.indices) {
                            mutable.removeAt(segment.index)
                        }
                    } else {
                        mutable[segment.index] = value
                    }
                } else {
                    mutable[segment.index] = setAt(mutable.getOrNull(segment.index), remaining, value)
                }

                buildJsonArray {
                    mutable.forEach(::add)
                }
            }
        }
    }

    internal fun parse(path: String): List<PathSegment> {
        if (path.isBlank()) {
            return emptyList()
        }

        val segments = mutableListOf<PathSegment>()
        val token = StringBuilder()
        var index = 0
        while (index < path.length) {
            when (val char = path[index]) {
                '.' -> {
                    flushProperty(token, segments)
                    index++
                }

                '[' -> {
                    flushProperty(token, segments)
                    val closing = path.indexOf(']', startIndex = index)
                    require(closing > index) { "Invalid array segment in path: $path" }
                    val value = path.substring(index + 1, closing).toInt()
                    segments += PathSegment.Index(value)
                    index = closing + 1
                }

                else -> {
                    token.append(char)
                    index++
                }
            }
        }

        flushProperty(token, segments)
        return segments
    }

    private fun flushProperty(
        token: StringBuilder,
        segments: MutableList<PathSegment>,
    ) {
        if (token.isNotEmpty()) {
            segments += PathSegment.Property(token.toString())
            token.clear()
        }
    }
}

sealed interface PathSegment {
    data class Property(
        val name: String,
    ) : PathSegment

    data class Index(
        val index: Int,
    ) : PathSegment
}
