package com.chobantonov.jsonfroms.core.util

import kotlinx.serialization.json.JsonArray
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
        .sortedWith(compareByDescending<Match> { it.raw.length }.thenBy { it.rejected }.thenBy { it.capacity })
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
            while (position < input.length && !accepts(part, input[position])) {
                position++
                rejected++
            }
            if (position == input.length) break
            raw.append(input[position++])
        }
        rejected += input.length - position
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
                result.append(match.raw[position++])
                offsets[position] = result.length
            } else if (position < match.raw.length || eager && position > 0 &&
                match.pattern.drop(index + 1).any(::isToken)) {
                result.append(part)
                offsets[position] = result.length
            }
        }
        return Display(result.toString(), offsets)
    }

    private fun isToken(char: Char) = char == '#' || tokens.containsKey(char)

    private fun accepts(token: Char, char: Char): Boolean = when (token) {
        '#' -> char in '0'..'9'
        else -> {
            val expression = tokens[token] ?: return false
            if (expression == "\\p{L}") char.isLetter()
            else runCatching { Regex(expression).matches(char.toString()) }.getOrDefault(false)
        }
    }

    private data class Match(val pattern: String, val raw: String, val rejected: Int, val capacity: Int)

    companion object {
        fun fromOptions(options: Map<String, kotlinx.serialization.json.JsonElement>): StringMask? {
            val patterns = when (val mask = options["mask"]) {
                is JsonPrimitive -> listOfNotNull(mask.contentOrNull?.takeIf { mask.isString && it.isNotEmpty() })
                is JsonArray -> mask.mapNotNull { (it as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.contentOrNull?.takeIf(String::isNotEmpty) }
                else -> emptyList()
            }
            if (patterns.isEmpty()) return null
            val tokens = (options["tokens"] as? JsonObject).orEmpty().mapNotNull { (key, value) ->
                val expression = (value as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.contentOrNull
                if (key.length == 1 && !expression.isNullOrEmpty()) key[0] to expression else null
            }.toMap()
            return StringMask(
                patterns,
                tokens,
                (options["eager"] as? JsonPrimitive)?.booleanOrNull == true,
                (options["returnMaskedValue"] as? JsonPrimitive)?.booleanOrNull == true,
            )
        }
    }
}
