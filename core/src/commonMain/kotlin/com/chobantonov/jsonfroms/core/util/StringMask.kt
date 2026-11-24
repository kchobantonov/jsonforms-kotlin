package com.chobantonov.jsonfroms.core.util

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

/** A portable input mask for string Controls. The stored value remains independent of its display. */
class StringMask private constructor(
    private val patterns: List<String>,
    private val tokens: Map<Char, String>,
    private val eager: Boolean,
    private val returnMaskedValue: Boolean,
) {
    data class Display(val text: String, val rawOffsets: List<Int>)

    fun display(stored: String): String {
        return visualize(stored).text
    }

    fun raw(stored: String): String {
        if (stored.isEmpty()) return stored
        val match = bestMatch(stored)
        return if (match.rejected > 0) stored else match.raw
    }

    fun visualize(input: String): Display {
        if (input.isEmpty()) return Display("", listOf(0))
        val match = bestMatch(input)
        // Imported values that the mask cannot represent must remain visible and editable.
        return if (match.rejected > 0) Display(input, (0..input.length).toList())
        else formatDisplay(match)
    }

    fun edit(input: String, previousStored: String? = null): String {
        if (input.isEmpty()) return input
        val match = bestMatch(input)
        // Let users repair an imported value a few characters at a time. Until
        // it fits the mask, retain their draft as data so no characters vanish.
        if (previousStored != null && bestMatch(previousStored).rejected > 0 && match.rejected > 0) {
            return input
        }
        return if (returnMaskedValue) formatDisplay(match).text else match.raw
    }

    private fun bestMatch(input: String): Match = patterns.map { parse(it, input) }
        .sortedWith(compareByDescending<Match> { jsonStringLength(it.raw) }
            .thenBy { it.rejected }.thenBy { it.capacity })
        .first()

    private fun parse(pattern: String, input: String): Match {
        val raw = StringBuilder()
        // A leading literal prefix is consumed as a unit. Otherwise a raw value
        // beginning with the same digit as the prefix would lose that digit.
        val prefix = pattern.takeWhile { !isToken(it) }
        var position = if (prefix.isNotEmpty() && input.startsWith(prefix)) prefix.length else 0
        var rejected = 0
        for (part in pattern.drop(prefix.length)) {
            if (!isToken(part)) {
                if (position < input.length && input[position] == part) position++
                continue
            }
            while (position < input.length) {
                val candidate = input.codePointText(position)
                if (accepts(part, candidate)) break
                position += candidate.length
                rejected++
            }
            if (position == input.length) break
            val accepted = input.codePointText(position)
            raw.append(accepted)
            position += accepted.length
        }
        rejected += jsonStringLength(input.substring(position))
        return Match(pattern, raw.toString(), rejected, pattern.count(::isToken))
    }

    private fun formatDisplay(match: Match): Display {
        if (match.raw.isEmpty()) return Display("", listOf(0))
        val result = StringBuilder()
        val offsets = MutableList(match.raw.length + 1) { 0 }
        var position = 0
        for ((index, part) in match.pattern.withIndex()) {
            if (isToken(part)) {
                if (position == match.raw.length) break
                val accepted = match.raw.codePointText(position)
                result.append(accepted)
                repeat(accepted.length) { offsets[++position] = result.length }
            } else if (position < match.raw.length || eager && position > 0 &&
                match.pattern.drop(index + 1).any(::isToken)) {
                result.append(part)
                offsets[position] = result.length
            }
        }
        return Display(result.toString(), offsets)
    }

    private fun isToken(char: Char) = tokens.containsKey(char)

    private fun accepts(token: Char, codePoint: String): Boolean {
        val expression = tokens[token] ?: return false
        return if (expression == "\\p{L}" && codePoint.length == 1) codePoint[0].isLetter()
        else runCatching { Regex(expression).matches(codePoint) }.getOrDefault(false)
    }

    private fun String.codePointText(index: Int): String {
        val first = this[index].code
        val length = if (first in 0xD800..0xDBFF && index + 1 < this.length &&
            this[index + 1].code in 0xDC00..0xDFFF) 2 else 1
        return substring(index, index + length)
    }

    private data class Match(val pattern: String, val raw: String, val rejected: Int, val capacity: Int)

    companion object {
        private val defaultTokens = mapOf('#' to "[0-9]", '@' to "[A-Za-z]", '*' to "[A-Za-z0-9]")

        fun fromOptions(options: Map<String, kotlinx.serialization.json.JsonElement>): StringMask? {
            val patterns = when (val mask = options["mask"]) {
                is JsonPrimitive -> listOfNotNull(mask.contentOrNull?.takeIf { mask.isString && it.isNotEmpty() })
                is JsonArray -> mask.mapNotNull { (it as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.contentOrNull?.takeIf(String::isNotEmpty) }
                else -> emptyList()
            }
            if (patterns.isEmpty()) return null
            val tokens = defaultTokens.toMutableMap()
            val replace = (options["tokensReplace"] as? JsonPrimitive)?.booleanOrNull != false
            (options["tokens"] as? JsonObject).orEmpty().forEach { (key, value) ->
                if (key.length != 1) return@forEach
                val token = key[0]
                val removesToken = when (value) {
                    JsonNull -> true
                    is JsonPrimitive -> if (value.isString) value.content.isEmpty()
                        else value.booleanOrNull == false || value.content.toDoubleOrNull() == 0.0
                    else -> false
                }
                if (removesToken) {
                    if (replace) tokens.remove(token)
                    return@forEach
                }
                val expression = when (value) {
                    is JsonPrimitive -> value.takeIf(JsonPrimitive::isString)?.contentOrNull
                    is JsonObject -> (value["pattern"] as? JsonPrimitive)
                        ?.takeIf(JsonPrimitive::isString)?.contentOrNull
                    else -> null
                }
                if (!expression.isNullOrEmpty() && runCatching { Regex(expression) }.isSuccess) {
                    tokens[token] = expression
                }
            }
            return StringMask(
                patterns,
                tokens,
                (options["eager"] as? JsonPrimitive)?.booleanOrNull == true,
                (options["returnMaskedValue"] as? JsonPrimitive)?.booleanOrNull == true,
            )
        }
    }
}
