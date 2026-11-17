package com.chobantonov.jsonfroms.extended.renderers

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal sealed interface TextSegment {
    data class Literal(val text: String) : TextSegment
    data class Expression(val source: String) : TextSegment
}

/** Returns the last character of a CEL string delimiter, or -1 for an unfinished literal. */
private fun quotedLiteralEnd(source: String, opening: Int): Int {
    val quote = source[opening]
    val width = if (source.startsWith("$quote$quote$quote", opening)) 3 else 1
    var index = opening + width
    while (index < source.length) {
        if (source[index] == '\\') { index += 2; continue }
        if (width == 1 && source[index] == quote) return index
        if (width == 3 && source.startsWith("$quote$quote$quote", index)) return index + 2
        index++
    }
    return -1
}

/** A quote-aware scanner for the spec's single-brace expression grammar. */
internal fun splitTextTemplate(text: String): List<TextSegment> {
    val segments = mutableListOf<TextSegment>()
    val literal = StringBuilder()
    fun flush() {
        if (literal.isNotEmpty()) {
            segments += TextSegment.Literal(literal.toString())
            literal.clear()
        }
    }
    var index = 0
    while (index < text.length) {
        when {
            text.startsWith("{{", index) -> { literal.append('{'); index += 2 }
            text.startsWith("}}", index) -> { literal.append('}'); index += 2 }
            text[index] != '{' -> { literal.append(text[index]); index++ }
            else -> {
                var end = index + 1
                var nestedBraces = 0
                while (end < text.length) {
                    val char = text[end]
                    if (char == '\'' || char == '"') {
                        val closing = quotedLiteralEnd(text, end)
                        if (closing < 0) { end = text.length; break }
                        end = closing + 1
                        continue
                    } else if (char == '{') nestedBraces++
                    else if (char == '}') {
                        if (nestedBraces == 0) break
                        nestedBraces--
                    }
                    end++
                }
                if (end == text.length) {
                    literal.append(text.substring(index))
                    break
                }
                flush()
                segments += TextSegment.Expression(text.substring(index + 1, end))
                index = end + 1
            }
        }
    }
    flush()
    return segments
}

internal data class InterpolatedLabel(
    val text: String,
    val failures: List<String>,
    val warnings: List<String> = emptyList(),
)

private val escapedDynamicPath = Regex("\\{\\{\\s*(?:data|item|config|context)(?:\\.[A-Za-z_][A-Za-z_0-9]*|\\[[^\\]]+\\])+(?:\\s*)\\}\\}")

internal fun looksLikeEscapedDynamicExpression(text: String): Boolean = escapedDynamicPath.containsMatchIn(text)

private open class ExpressionFailure(message: String) : Exception(message)
private class ExpressionLimitFailure(message: String) : ExpressionFailure(message)

private class ExpressionBudget {
    private var remaining = 100_000
    private var depth = 0

    fun enter() {
        if (remaining <= 0 || depth >= 128) throw ExpressionLimitFailure("expression evaluation limit exceeded")
        remaining--
        depth++
    }

    fun leave() { depth-- }
}

private fun display(value: JsonElement?): String = when (value) {
    null, JsonNull -> ""
    is JsonPrimitive -> {
        val content = value.content
        val number = if (!value.isString && content.any { it == '.' || it == 'e' || it == 'E' })
            content.toDoubleOrNull() else null
        if (number != null && number.isFinite() &&
            number in -9_007_199_254_740_991.0..9_007_199_254_740_991.0 &&
            number == number.toLong().toDouble()) number.toLong().toString() else content
    }
    is JsonArray -> throw ExpressionFailure("resolved to a list, which has no text form")
    else -> throw ExpressionFailure("resolved to a structure, which has no text form")
}

/** CEL size(string) counts Unicode code points, not UTF-16 code units. */
private fun stringCodePointCount(value: String): Int {
    var count = 0
    var index = 0
    while (index < value.length) {
        index += codePointWidth(value, index)
        count++
    }
    return count
}

private fun codePointWidth(value: String, index: Int): Int =
    if (value[index].isHighSurrogate() && index + 1 < value.length &&
        value[index + 1].isLowSurrogate()) 2 else 1

private fun codePointAt(value: String, index: Int): Int {
    val first = value[index].code
    if (codePointWidth(value, index) == 1) return first
    val second = value[index + 1].code
    return 0x10000 + ((first - 0xd800) shl 10) + (second - 0xdc00)
}

/** CEL orders strings by Unicode code points; UTF-16 comparison reverses some pairs. */
private fun compareStringsByCodePoint(left: String, right: String): Int {
    var leftIndex = 0
    var rightIndex = 0
    while (leftIndex < left.length && rightIndex < right.length) {
        val order = codePointAt(left, leftIndex).compareTo(codePointAt(right, rightIndex))
        if (order != 0) return order
        leftIndex += codePointWidth(left, leftIndex)
        rightIndex += codePointWidth(right, rightIndex)
    }
    return when {
        leftIndex < left.length -> 1
        rightIndex < right.length -> -1
        else -> 0
    }
}

/** CEL's string(double) uses a short, locale-independent decimal representation. */
private fun celDoubleString(value: Double): String {
    val source = value.toString()
    val negative = source.startsWith('-') || value == 0.0 && 1.0 / value < 0.0
    val unsigned = if (negative) source.drop(1) else source
    val exponentAt = unsigned.indexOfFirst { it == 'e' || it == 'E' }
    val mantissa = if (exponentAt < 0) unsigned else unsigned.substring(0, exponentAt)
    val exponent = if (exponentAt < 0) 0 else unsigned.substring(exponentAt + 1).toInt()
    val decimalAt = mantissa.indexOf('.')
    val integerDigits = if (decimalAt < 0) mantissa.length else decimalAt
    val digits = mantissa.filter { it != '.' }
    val first = digits.indexOfFirst { it != '0' }
    if (first < 0) return if (negative) "-0" else "0"
    var significant = digits.substring(first).trimEnd('0')
    var power = exponent + integerDigits - first - 1
    val magnitude = if (negative) -value else value
    for (precision in 1 until significant.length) {
        val rounded = significant.take(precision).toCharArray()
        var roundedPower = power
        if (significant[precision] >= '5') {
            var at = rounded.lastIndex
            while (at >= 0 && rounded[at] == '9') { rounded[at] = '0'; at-- }
            if (at < 0) roundedPower++ else rounded[at]++
        }
        val candidateDigits = if (roundedPower != power) "1" else rounded.concatToString().trimEnd('0')
        if (("${candidateDigits}e$roundedPower").toDoubleOrNull() == magnitude) {
            significant = candidateDigits
            power = roundedPower
            break
        }
    }
    val body = if (power < -4 || power >= 6) {
        val fraction = significant.drop(1)
        significant.first() + (if (fraction.isEmpty()) "" else ".$fraction") +
            "e" + (if (power < 0) "-" else "+") + power.toString().trimStart('-').padStart(2, '0')
    } else {
        val point = power + 1
        when {
            point <= 0 -> "0." + "0".repeat(-point) + significant
            point >= significant.length -> significant + "0".repeat(point - significant.length)
            else -> significant.substring(0, point) + "." + significant.substring(point)
        }
    }
    return if (negative) "-$body" else body
}

/** Escapes only substituted values, leaving the author's Markdown literal intact. */
internal fun escapeMarkdownValue(value: String): String = buildString {
    for (char in value) {
        if (char in "\\`*_{}[]<>()#+-.!|~") append('\\')
        append(char)
    }
}

/** URL values use UTF-8 component encoding; parentheses also need escaping in Markdown targets. */
internal fun encodeMarkdownUrlComponent(value: String): String = buildString {
    val hex = "0123456789ABCDEF"
    for (byte in value.encodeToByteArray()) {
        val code = byte.toInt() and 0xff
        val safe = code in 'A'.code..'Z'.code || code in 'a'.code..'z'.code ||
            code in '0'.code..'9'.code || code.toChar() in "-_.!~*'"
        if (safe) append(code.toChar()) else {
            append('%')
            append(hex[code shr 4])
            append(hex[code and 0x0f])
        }
    }
}

/** Tracks the authored Markdown link target, never the substituted value. */
private class MarkdownLinkTarget {
    private var bracketDepth = 0
    private var afterLabel = false
    private var definitionLabel = false
    private var afterDefinitionLabel = false
    private var definitionAwaitingTarget = false
    private var definitionTarget = false
    private var definitionAngleTarget = false
    private var lineStart = true
    private var lineIndent = 0
    private var destinationDepth = 0
    private var inDestination = false
    private var codeRun = 0

    fun inUrl(): Boolean = inDestination || definitionAwaitingTarget || definitionTarget

    fun consumedExpression() {
        if (definitionAwaitingTarget) {
            definitionAwaitingTarget = false
            definitionTarget = true
        }
    }

    fun accept(literal: String) {
        var index = 0
        while (index < literal.length) {
            val char = literal[index++]
            if (char == '\n' || char == '\r') {
                lineStart = true
                lineIndent = 0
                definitionLabel = false
                afterDefinitionLabel = false
                definitionAwaitingTarget = false
                definitionTarget = false
                definitionAngleTarget = false
                afterLabel = false
                continue
            }
            if (lineStart) {
                if (char == ' ') { lineIndent++; continue }
                definitionLabel = char == '[' && lineIndent <= 3
                lineStart = false
            }
            if (char == '\\' && index < literal.length) { index++; continue }
            if (definitionTarget) {
                if (definitionAngleTarget && char == '>' ||
                    !definitionAngleTarget && char.isWhitespace()) {
                    definitionTarget = false
                    definitionAngleTarget = false
                }
                continue
            }
            if (definitionAwaitingTarget) {
                if (char.isWhitespace()) continue
                definitionAwaitingTarget = false
                definitionTarget = true
                definitionAngleTarget = char == '<'
                continue
            }
            if (destinationDepth > 0) {
                when {
                    char == ')' -> {
                        destinationDepth--
                        if (destinationDepth == 0) inDestination = false
                    }
                    char == '(' -> destinationDepth++
                    char.isWhitespace() && destinationDepth == 1 -> inDestination = false
                }
                continue
            }
            if (char == '`') {
                var width = 1
                while (index < literal.length && literal[index] == '`') { index++; width++ }
                if (codeRun == 0) codeRun = width else if (codeRun == width) codeRun = 0
                continue
            }
            if (codeRun > 0) continue
            if (afterLabel) {
                afterLabel = false
                if (char == '(') {
                    destinationDepth = 1
                    inDestination = true
                    afterDefinitionLabel = false
                    definitionLabel = false
                    continue
                }
                if (char == ':' && afterDefinitionLabel) {
                    definitionAwaitingTarget = true
                    afterDefinitionLabel = false
                    definitionLabel = false
                    continue
                }
                afterDefinitionLabel = false
                definitionLabel = false
            }
            when (char) {
                '[' -> bracketDepth++
                ']' -> if (bracketDepth > 0) {
                    bracketDepth--
                    afterLabel = bracketDepth == 0
                    afterDefinitionLabel = afterLabel && definitionLabel
                }
            }
        }
    }
}

private val identifier = Regex("[A-Za-z_][A-Za-z_0-9]*")

private fun parseCelStringLiteral(source: String, raw: Boolean = false): JsonPrimitive {
    val quote = source.firstOrNull()
    if (source.length < 2 || quote !in listOf('\'', '"') || source.last() != quote)
        throw ExpressionFailure("invalid string literal")
    val width = if (source.startsWith("$quote$quote$quote")) 3 else 1
    if (source.length < width * 2 || !source.endsWith("$quote".repeat(width)))
        throw ExpressionFailure("invalid string literal")
    val end = source.length - width
    val result = StringBuilder()
    var index = width
    fun appendCodePoint(value: Int) {
        if (value > 0x10ffff || value in 0xd800..0xdfff)
            throw ExpressionFailure("invalid Unicode escape")
        if (value < 0x10000) result.append(value.toChar())
        else {
            val pair = value - 0x10000
            result.append((0xd800 + (pair shr 10)).toChar())
            result.append((0xdc00 + (pair and 0x3ff)).toChar())
        }
    }
    fun readHex(digits: Int): Int {
        if (index + digits > end) throw ExpressionFailure("invalid Unicode escape")
        var value = 0
        repeat(digits) {
            val digit = source[index++].digitToIntOrNull(16)
                ?: throw ExpressionFailure("invalid Unicode escape")
            value = value * 16 + digit
            if (value > 0x10ffff) throw ExpressionFailure("invalid Unicode escape")
        }
        return value
    }
    while (index < end) {
        val char = source[index++]
        if (width == 1 && (char == quote || char == '\n' || char == '\r') ||
            width == 3 && char == quote && source.startsWith("$quote$quote", index))
            throw ExpressionFailure("invalid string literal")
        if (char != '\\') {
            result.append(char)
            continue
        }
        if (index >= end) throw ExpressionFailure("invalid string escape")
        if (raw) {
            result.append('\\')
            result.append(source[index++])
            continue
        }
        when (val escaped = source[index++]) {
            '\\', '\'', '"', '`', '?' -> result.append(escaped)
            'a' -> result.append('\u0007')
            'b' -> result.append('\b')
            'f' -> result.append('\u000c')
            'n' -> result.append('\n')
            'r' -> result.append('\r')
            't' -> result.append('\t')
            'v' -> result.append('\u000b')
            'u' -> appendCodePoint(readHex(4))
            'U' -> appendCodePoint(readHex(8))
            'x', 'X' -> appendCodePoint(readHex(2))
            in '0'..'3' -> {
                if (index + 2 > end) throw ExpressionFailure("invalid octal escape")
                var value = escaped - '0'
                repeat(2) {
                    val digit = source[index++]
                    if (digit !in '0'..'7') throw ExpressionFailure("invalid octal escape")
                    value = value * 8 + (digit - '0')
                }
                appendCodePoint(value)
            }
            else -> throw ExpressionFailure("invalid string escape")
        }
    }
    return JsonPrimitive(result.toString())
}

/** Keeps only inert JSON values in the expression scope; host objects are never callable. */
internal fun jsonSafeConfig(config: Map<String, Any?>): JsonObject = JsonObject(config.mapNotNull { (key, value) ->
    jsonSafeValue(value)?.let { key to it }
}.toMap())

private fun jsonSafeValue(value: Any?): JsonElement? = when (value) {
    null -> JsonNull
    is JsonElement -> value
    is String -> JsonPrimitive(value)
    is Boolean -> JsonPrimitive(value)
    is Number -> JsonPrimitive(value)
    is Map<*, *> -> JsonObject(value.mapNotNull { (key, child) ->
        if (key !is String) null else jsonSafeValue(child)?.let { key to it }
    }.toMap())
    is List<*> -> JsonArray(value.map { jsonSafeValue(it) ?: JsonNull })
    else -> null
}

/**
 * A strict, side-effect-free partial expression evaluator for the portable Label profile.
 * Canonical CEL conformance remains open; unsupported expressions fail visibly.
 */
private class LabelExpressionEvaluator(
    private val scope: Map<String, JsonElement?>,
    private val translate: (String, String) -> String,
    private val locale: String,
    private val budget: ExpressionBudget = ExpressionBudget(),
) {
    fun evaluate(raw: String): JsonElement? {
        budget.enter()
        return try { evaluateBounded(raw) } finally { budget.leave() }
    }

    private fun evaluateBounded(raw: String): JsonElement? {
        val expression = raw.trim()
        if (expression.isEmpty()) throw ExpressionFailure("empty expression")
        if (wrappedByParentheses(expression)) return evaluate(expression.substring(1, expression.length - 1))
        val question = topLevel(expression, '?')
        if (question >= 0) {
            val colon = matchingConditionalColon(expression, question)
            if (colon < 0) throw ExpressionFailure("invalid conditional")
            val condition = boolean(evaluate(expression.substring(0, question)), "conditional")
            return evaluate(if (condition) expression.substring(question + 1, colon)
                else expression.substring(colon + 1))
        }
        val or = lastTopLevelOperator(expression, "||")
        if (or >= 0) return logical(expression, or, decisive = true)
        val and = lastTopLevelOperator(expression, "&&")
        if (and >= 0) return logical(expression, and, decisive = false)
        val equality = listOf("==", "!=").map { it to lastTopLevelOperator(expression, it) }
            .maxBy { it.second }
        if (equality.second >= 0) {
            val (operator, at) = equality
            val left = evaluate(expression.substring(0, at))
            val right = evaluate(expression.substring(at + operator.length))
            val equal = equalValues(left, right)
            return JsonPrimitive(if (operator == "==") equal else !equal)
        }
        val ordering = listOf("<=", ">=", "<", ">").map { it to lastTopLevelOperator(expression, it) }
            .plus("in" to lastTopLevelKeyword(expression, "in"))
            .maxBy { it.second }
        if (ordering.second >= 0) {
                val (operator, at) = ordering
                if (operator == "in") {
                    val left = evaluate(expression.substring(0, at))
                    val right = evaluate(expression.substring(at + 2))
                    return when (right) {
                        null -> null
                        is JsonArray -> JsonPrimitive(right.any { equalValues(left, it) })
                        is JsonObject -> {
                            val key = left as? JsonPrimitive
                            if (key == null || !key.isString)
                                throw ExpressionFailure("object membership requires a string key")
                            JsonPrimitive(key.content in right)
                        }
                        else -> throw ExpressionFailure("membership requires a list or object")
                    }
                }
                val left = evaluate(expression.substring(0, at)) as? JsonPrimitive
                val right = evaluate(expression.substring(at + operator.length)) as? JsonPrimitive
                if (left == null || right == null) return null
                val order = when {
                    left.isString && right.isString -> compareStringsByCodePoint(left.content, right.content)
                    !left.isString && !right.isString &&
                        left.content in listOf("false", "true") && right.content in listOf("false", "true") ->
                        (left.content == "true").compareTo(right.content == "true")
                    else -> compareNumbers(left, right)
                        ?: throw ExpressionFailure("ordering requires matching scalar types")
                }
                return JsonPrimitive(when (operator) {
                    "<" -> order < 0
                    "<=" -> order <= 0
                    ">" -> order > 0
                    else -> order >= 0
                })
        }
        val additive = lastTopLevelAdditive(expression)
        if (additive >= 0) return arithmetic(expression, additive)
        val multiplicative = lastTopLevelArithmetic(expression)
        if (multiplicative >= 0) return arithmetic(expression, multiplicative)
        if (expression.startsWith('!')) return JsonPrimitive(!boolean(evaluate(expression.drop(1)), "negation"))
        if (expression.startsWith('-')) {
            // The minimum signed integer has no positive Long counterpart to negate.
            expression.toLongOrNull()?.let { return JsonPrimitive(it) }
            val operand = evaluate(expression.drop(1)) as? JsonPrimitive
            if (operand == null || operand.isString) throw ExpressionFailure("negation requires a number")
            operand.content.toLongOrNull()?.let {
                if (it == Long.MIN_VALUE) throw ExpressionFailure("integer overflow")
                return JsonPrimitive(-it)
            }
            val number = operand.content.toDoubleOrNull()
                ?: throw ExpressionFailure("negation requires a number")
            return doublePrimitive(-number)
        }
        if ((expression.first() == '"' || expression.first() == '\'') &&
            expression.last() == expression.first()) return parseCelStringLiteral(expression)
        if (expression.first() in listOf('r', 'R') &&
            expression.getOrNull(1) in listOf('"', '\'') &&
            expression.last() == expression[1])
            return parseCelStringLiteral(expression.drop(1), raw = true)
        if (expression == "true" || expression == "false") return JsonPrimitive(expression == "true")
        if (expression == "null") return JsonNull
        expression.toLongOrNull()?.let { return JsonPrimitive(it) }
        expression.toDoubleOrNull()?.let {
            if (!it.isFinite()) throw ExpressionFailure("non-finite number")
            return runCatching { Json.parseToJsonElement(expression) as JsonPrimitive }.getOrNull()
                ?: throw ExpressionFailure("invalid number literal")
        }
        val finalIndex = finalIndexOpening(expression)
        if (finalIndex > 0) return selectIndex(
            evaluate(expression.substring(0, finalIndex)),
            evaluate(expression.substring(finalIndex + 1, expression.length - 1)),
        )
        if (finalIndex == 0 && expression.startsWith('[')) {
            val elements = splitArguments(expression.substring(1, expression.length - 1))
            if (elements.size > 10_000) throw ExpressionLimitFailure("expression collection limit exceeded")
            return JsonArray(elements.map { element ->
                evaluate(element) ?: throw ExpressionFailure("list element is missing")
            })
        }
        if (expression.startsWith('{') && expression.endsWith('}')) {
            val entries = splitArguments(expression.substring(1, expression.length - 1))
            if (entries.size > 10_000) throw ExpressionLimitFailure("expression collection limit exceeded")
            val values = linkedMapOf<String, JsonElement>()
            for (entry in entries) {
                val colon = topLevel(entry, ':')
                if (colon < 0) throw ExpressionFailure("map entry requires a key and value")
                val key = evaluate(entry.substring(0, colon)) as? JsonPrimitive
                if (key == null || !key.isString)
                    throw ExpressionFailure("JSON map key must be a string")
                if (key.content in values) throw ExpressionFailure("duplicate map key")
                values[key.content] = evaluate(entry.substring(colon + 1))
                    ?: throw ExpressionFailure("map value is missing")
            }
            return JsonObject(values)
        }
        val open = finalCallOpening(expression)
        if (open > 0 && expression.endsWith(')')) {
            val receiverAndName = expression.substring(0, open)
            val dot = lastTopLevelOperator(receiverAndName, ".")
            val name = if (dot >= 0) receiverAndName.substring(dot + 1) else ""
            if (dot >= 0) {
                val arguments = splitArguments(expression.substring(open + 1, expression.length - 1))
                if (name in setOf("all", "exists", "exists_one", "map", "filter"))
                    return collectionMacro(name, receiverAndName.substring(0, dot), arguments)
                if (name in setOf("size", "contains", "startsWith", "endsWith"))
                    return receiverCall(name, receiverAndName.substring(0, dot), arguments)
            }
        }
        if (open > 0 && expression.endsWith(')') && identifier.matches(expression.substring(0, open))) {
            val name = expression.substring(0, open)
            val rawArguments = splitArguments(expression.substring(open + 1, expression.length - 1))
            if (name == "has") {
                if (rawArguments.size != 1) throw ExpressionFailure("has expects one argument")
                return JsonPrimitive(hasSelectedField(rawArguments.single()))
            }
            val arguments = rawArguments.map(::evaluate)
            fun requireArgs(count: Int) {
                if (arguments.size != count) throw ExpressionFailure("$name expects $count arguments")
            }
            fun numeric(value: JsonElement?): Double? {
                if (value == null || value == JsonNull) return null
                val primitive = value as? JsonPrimitive
                if (primitive == null || primitive.isString) throw ExpressionFailure("$name requires a number")
                return primitive.content.toDoubleOrNull() ?: throw ExpressionFailure("$name requires a number")
            }
            fun string(value: JsonElement?): String? {
                if (value == null || value == JsonNull) return null
                val primitive = value as? JsonPrimitive
                if (primitive == null || !primitive.isString) throw ExpressionFailure("$name requires a string")
                return primitive.content
            }
            return when (name) {
                "size" -> {
                    requireArgs(1)
                    val value = arguments[0] ?: return null
                    sizeOf(value)
                }
                "bool", "int", "double" -> {
                    requireArgs(1)
                    val value = arguments[0] ?: return null
                    convertScalar(name, value)
                }
                "string" -> {
                    requireArgs(1)
                    val value = arguments[0] ?: return null
                    convertToString(value)
                }
                "dyn" -> {
                    requireArgs(1)
                    arguments[0]
                }
                "translate" -> {
                    if (arguments.size !in 1..2)
                        throw ExpressionFailure("translate expects one or two arguments")
                    val key = string(arguments[0]) ?: return null
                    val fallback = if (arguments.size == 2) string(arguments[1]) ?: return null else key
                    JsonPrimitive(translate(key, fallback))
                }
                "number" -> {
                    if (arguments.size !in 1..2) throw ExpressionFailure("number expects one or two arguments")
                    val value = numeric(arguments[0]) ?: return null
                    val digits = if (arguments.size == 2) {
                        val rawDigits = arguments[1] ?: return null
                        val parsed = (rawDigits as? JsonPrimitive)?.takeIf { !it.isString }?.content?.toIntOrNull()
                        if (parsed == null || parsed !in 0..20) throw ExpressionFailure("number digits must be 0 through 20")
                        parsed
                    } else null
                    JsonPrimitive(formatExpressionNumber(value, locale, digits))
                }
                "currency" -> {
                    requireArgs(2)
                    val value = numeric(arguments[0]) ?: return null
                    val code = string(arguments[1]) ?: return null
                    if (!Regex("[A-Z]{3}").matches(code)) throw ExpressionFailure("currency requires an ISO code")
                    JsonPrimitive(formatExpressionCurrency(value, code, locale))
                }
                "percent" -> {
                    requireArgs(1)
                    val value = numeric(arguments[0]) ?: return null
                    JsonPrimitive(formatExpressionPercent(value, locale))
                }
                "date", "time", "dateTime" -> {
                    requireArgs(1)
                    val value = string(arguments[0]) ?: return null
                    val formatted = try {
                        when (name) {
                            "date" -> formatExpressionDate(value, locale)
                            "time" -> formatExpressionTime(value, locale)
                            else -> formatExpressionDateTime(value, locale)
                        }
                    } catch (invalid: IllegalArgumentException) {
                        throw ExpressionFailure("$name requires a valid ISO date, time or offset date-time")
                    }
                    JsonPrimitive(formatted)
                }
                else -> throw ExpressionFailure("function is not registered: $name")
            }
        }
        if (expression.contains('(')) throw ExpressionFailure("function is not registered")
        return path(expression)
    }

    private fun booleanValue(expression: String): Boolean = boolean(evaluate(expression), "collection predicate")

    private fun sizeOf(value: JsonElement): JsonPrimitive = JsonPrimitive(when (value) {
        is JsonArray -> value.size
        is JsonObject -> value.size
        is JsonPrimitive -> if (value.isString) stringCodePointCount(value.content)
            else throw ExpressionFailure("size requires a string, list or object")
    })

    private fun convertScalar(name: String, value: JsonElement): JsonPrimitive {
        val primitive = value as? JsonPrimitive
            ?: throw ExpressionFailure("$name requires a scalar value")
        return when (name) {
            "bool" -> {
                val text = primitive.content.lowercase()
                if (text !in setOf("true", "false") || !primitive.isString &&
                    primitive.content !in setOf("true", "false"))
                    throw ExpressionFailure("bool requires a boolean or boolean string")
                JsonPrimitive(text == "true")
            }
            "int" -> {
                if (primitive.content in setOf("true", "false"))
                    throw ExpressionFailure("int requires a number or integer string")
                val integer = primitive.content.toLongOrNull()
                if (integer != null) JsonPrimitive(integer)
                else {
                    if (primitive.isString) throw ExpressionFailure("int requires an integer string")
                    val number = primitive.content.toDoubleOrNull()
                        ?: throw ExpressionFailure("int requires a number")
                    if (!number.isFinite() || number <= -9_223_372_036_854_775_808.0 ||
                        number >= 9_223_372_036_854_775_808.0)
                        throw ExpressionFailure("int conversion out of range")
                    JsonPrimitive(number.toLong())
                }
            }
            else -> {
                if (primitive.content in setOf("true", "false"))
                    throw ExpressionFailure("double requires a number or numeric string")
                val number = primitive.content.toDoubleOrNull()
                    ?: throw ExpressionFailure("double requires a number or numeric string")
                doublePrimitive(number)
            }
        }
    }

    private fun convertToString(value: JsonElement): JsonPrimitive {
        val primitive = value as? JsonPrimitive
            ?: throw ExpressionFailure("string conversion requires a scalar value")
        if (primitive.isString) return primitive
        if (primitive.content in setOf("true", "false") || primitive.content.toLongOrNull() != null)
            return JsonPrimitive(primitive.content)
        val number = primitive.content.toDoubleOrNull()
            ?: throw ExpressionFailure("string conversion requires a string, boolean or number")
        if (!number.isFinite()) throw ExpressionFailure("string conversion requires a finite number")
        return JsonPrimitive(celDoubleString(number))
    }

    private fun receiverCall(name: String, receiver: String, arguments: List<String>): JsonElement? {
        val expected = if (name == "size") 0 else 1
        if (arguments.size != expected) throw ExpressionFailure("$name expects $expected arguments")
        val value = evaluate(receiver) ?: return null
        if (name == "size") return sizeOf(value)
        val text = (value as? JsonPrimitive)?.takeIf { it.isString }?.content
            ?: throw ExpressionFailure("$name requires a string receiver")
        val argument = evaluate(arguments.single()) ?: return null
        val substring = (argument as? JsonPrimitive)?.takeIf { it.isString }?.content
            ?: throw ExpressionFailure("$name requires a string argument")
        return JsonPrimitive(when (name) {
            "contains" -> substring in text
            "startsWith" -> text.startsWith(substring)
            else -> text.endsWith(substring)
        })
    }

    private fun logical(expression: String, at: Int, decisive: Boolean): JsonPrimitive {
        val operation = if (decisive) "logical OR" else "logical AND"
        var firstError: ExpressionFailure? = null
        val left = try {
            boolean(evaluate(expression.substring(0, at)), operation)
        } catch (limit: ExpressionLimitFailure) {
            throw limit
        } catch (error: ExpressionFailure) {
            firstError = error
            null
        }
        if (left == decisive) return JsonPrimitive(decisive)
        var secondError: ExpressionFailure? = null
        val right = try {
            boolean(evaluate(expression.substring(at + 2)), operation)
        } catch (limit: ExpressionLimitFailure) {
            throw limit
        } catch (error: ExpressionFailure) {
            secondError = error
            null
        }
        if (right == decisive) return JsonPrimitive(decisive)
        if (firstError != null) throw firstError
        if (secondError != null) throw secondError
        return JsonPrimitive(!decisive)
    }

    private fun collectionMacro(macro: String, receiver: String, arguments: List<String>): JsonElement? {
        val expected = if (macro == "map") 2..3 else 2..2
        if (arguments.size !in expected || !identifier.matches(arguments[0].trim()))
            throw ExpressionFailure("$macro expects a variable and predicate or transform")
        val collection = evaluate(receiver) ?: return null
        val values = when (collection) {
            is JsonArray -> collection
            is JsonObject -> JsonArray(collection.keys.map { JsonPrimitive(it) })
            else -> throw ExpressionFailure("$macro requires a list or object")
        }
        if (values.size > 10_000) throw ExpressionLimitFailure("expression collection limit exceeded")
        val variable = arguments[0].trim()
        val predicateIndex = if (macro == "map" && arguments.size == 2) null else 1
        var matches = 0
        var deferredError: ExpressionFailure? = null
        val output = mutableListOf<JsonElement>()
        for (value in values) {
            val evaluator = LabelExpressionEvaluator(scope + (variable to value), translate, locale, budget)
            val predicate = if (predicateIndex == null) true else try {
                evaluator.booleanValue(arguments[predicateIndex])
            } catch (limit: ExpressionLimitFailure) {
                throw limit
            } catch (error: ExpressionFailure) {
                if (macro !in setOf("all", "exists")) throw error
                if (deferredError == null) deferredError = error
                continue
            }
            if (predicate) matches++
            when (macro) {
                "all" -> if (!predicate) return JsonPrimitive(false)
                "exists" -> if (predicate) return JsonPrimitive(true)
                "filter" -> if (predicate) output += value
                "map" -> if (predicate) output += evaluator.evaluate(arguments.last()) ?: JsonNull
            }
        }
        if (deferredError != null) throw deferredError
        return when (macro) {
            "all" -> JsonPrimitive(true)
            "exists" -> JsonPrimitive(false)
            "exists_one" -> JsonPrimitive(matches == 1)
            else -> JsonArray(output)
        }
    }

    private fun finalCallOpening(source: String): Int {
        if (!source.endsWith(')')) return -1
        val openings = mutableListOf<Int>()
        var finalOpening = -1
        var index = 0
        while (index < source.length) {
            val char = source[index]
            when (char) {
                '\'', '"' -> {
                    val closing = quotedLiteralEnd(source, index)
                    if (closing < 0) return -1
                    index = closing
                }
                '(' -> openings += index
                ')' -> {
                    if (openings.isEmpty()) return -1
                    val opening = openings.removeAt(openings.lastIndex)
                    if (index == source.lastIndex) finalOpening = opening
                }
            }
            index++
        }
        return if (openings.isEmpty()) finalOpening else -1
    }

    private fun finalIndexOpening(source: String): Int {
        if (!source.endsWith(']')) return -1
        val openings = mutableListOf<Int>()
        var finalOpening = -1
        var index = 0
        while (index < source.length) {
            val char = source[index]
            when (char) {
                '\'', '"' -> {
                    val closing = quotedLiteralEnd(source, index)
                    if (closing < 0) return -1
                    index = closing
                }
                '[' -> openings += index
                ']' -> {
                    if (openings.isEmpty()) return -1
                    val opening = openings.removeAt(openings.lastIndex)
                    if (index == source.lastIndex) finalOpening = opening
                }
            }
            index++
        }
        return if (openings.isEmpty()) finalOpening else -1
    }

    private fun boolean(value: JsonElement?, operation: String): Boolean {
        val primitive = value as? JsonPrimitive
        if (primitive == null || primitive.isString || primitive.content !in listOf("true", "false"))
            throw ExpressionFailure("$operation requires a boolean")
        return primitive.content == "true"
    }

    private fun hasSelectedField(source: String): Boolean {
        val dot = lastTopLevelOperator(source, ".")
        val field = if (dot >= 0) source.substring(dot + 1).trim() else ""
        if (!identifier.matches(field))
            throw ExpressionFailure("has requires field selection")
        val parent = evaluate(source.substring(0, dot))
        return when (parent) {
            null, JsonNull -> false
            is JsonObject -> field in parent
            else -> throw ExpressionFailure("has requires an object field")
        }
    }

    private fun equalValues(left: JsonElement?, right: JsonElement?): Boolean = when {
        left is JsonPrimitive && right is JsonPrimitive ->
            compareNumbers(left, right)?.let { it == 0 } ?: (left == right)
        left is JsonArray && right is JsonArray ->
            left.size == right.size && left.indices.all { equalValues(left[it], right[it]) }
        left is JsonObject && right is JsonObject ->
            left.keys == right.keys && left.keys.all { equalValues(left[it], right[it]) }
        else -> left == right
    }

    private fun compareNumbers(left: JsonPrimitive, right: JsonPrimitive): Int? {
        if (left.isString || right.isString) return null
        val leftInteger = left.content.toLongOrNull()
        val rightInteger = right.content.toLongOrNull()
        if (leftInteger != null && rightInteger != null) return leftInteger.compareTo(rightInteger)
        val leftDouble = left.content.toDoubleOrNull() ?: return null
        val rightDouble = right.content.toDoubleOrNull() ?: return null
        if (!leftDouble.isFinite() || !rightDouble.isFinite()) throw ExpressionFailure("non-finite comparison")
        return when {
            leftInteger != null -> compareIntegerToDouble(leftInteger, rightDouble)
            rightInteger != null -> -compareIntegerToDouble(rightInteger, leftDouble)
            leftDouble < rightDouble -> -1
            leftDouble > rightDouble -> 1
            else -> 0
        }
    }

    private fun compareIntegerToDouble(integer: Long, floating: Double): Int {
        // Long.MAX_VALUE rounds up to 2^63 as a Double, so check that boundary before conversion.
        if (floating >= 9_223_372_036_854_775_808.0) return -1
        if (floating < -9_223_372_036_854_775_808.0) return 1
        val whole = floating.toLong()
        return when {
            integer < whole -> -1
            integer > whole -> 1
            floating > whole.toDouble() -> -1
            floating < whole.toDouble() -> 1
            else -> 0
        }
    }

    private fun doublePrimitive(value: Double): JsonPrimitive {
        if (!value.isFinite()) throw ExpressionFailure("non-finite arithmetic result")
        val text = if (value == 0.0 && 1.0 / value < 0.0) "-0.0" else value.toString()
        val literal = if (text.any { it == '.' || it == 'e' || it == 'E' }) text else "$text.0"
        return Json.parseToJsonElement(literal) as JsonPrimitive
    }

    private fun arithmetic(expression: String, at: Int): JsonElement {
        val operator = expression[at]
        val leftValue = evaluate(expression.substring(0, at))
        val rightValue = evaluate(expression.substring(at + 1))
        if (operator == '+' && leftValue is JsonArray && rightValue is JsonArray) {
            if (leftValue.size > 10_000 || rightValue.size > 10_000 - leftValue.size)
                throw ExpressionLimitFailure("expression collection limit exceeded")
            return JsonArray(leftValue + rightValue)
        }
        val left = leftValue as? JsonPrimitive
        val right = rightValue as? JsonPrimitive
        if (left == null || right == null) throw ExpressionFailure("arithmetic requires matching scalar or list values")
        if (operator == '+' && left.isString && right.isString) return JsonPrimitive(left.content + right.content)
        if (left.isString || right.isString) throw ExpressionFailure("arithmetic requires numbers or matching strings")
        val integerLeft = left.content.toLongOrNull()
        val integerRight = right.content.toLongOrNull()
        if (operator == '%' && (integerLeft == null || integerRight == null))
            throw ExpressionFailure("remainder requires integers")
        if ((integerLeft == null) != (integerRight == null))
            throw ExpressionFailure("arithmetic requires matching numeric types")
        if (integerLeft != null && integerRight != null) {
            if ((operator == '/' || operator == '%') && integerRight == 0L)
                throw ExpressionFailure("division by zero")
            val result = when (operator) {
                '+' -> (integerLeft + integerRight).also {
                    if (((integerLeft xor it) and (integerRight xor it)) < 0) throw ExpressionFailure("integer overflow")
                }
                '-' -> (integerLeft - integerRight).also {
                    if (((integerLeft xor integerRight) and (integerLeft xor it)) < 0) throw ExpressionFailure("integer overflow")
                }
                '*' -> (integerLeft * integerRight).also {
                    if (integerLeft != 0L && (it / integerLeft != integerRight ||
                            integerLeft == -1L && integerRight == Long.MIN_VALUE))
                        throw ExpressionFailure("integer overflow")
                }
                '/' -> {
                    if (integerLeft == Long.MIN_VALUE && integerRight == -1L) throw ExpressionFailure("integer overflow")
                    integerLeft / integerRight
                }
                else -> integerLeft % integerRight
            }
            return JsonPrimitive(result)
        }
        val numberLeft = left.content.toDoubleOrNull()
        val numberRight = right.content.toDoubleOrNull()
        if (numberLeft == null || numberRight == null) throw ExpressionFailure("arithmetic requires numbers")
        if ((operator == '/' || operator == '%') && numberRight == 0.0) throw ExpressionFailure("division by zero")
        val result = when (operator) {
            '+' -> numberLeft + numberRight
            '-' -> numberLeft - numberRight
            '*' -> numberLeft * numberRight
            '/' -> numberLeft / numberRight
            else -> numberLeft % numberRight
        }
        if (!result.isFinite()) throw ExpressionFailure("non-finite arithmetic result")
        // Preserve the double type even if its value is whole; display handles its text form.
        return doublePrimitive(result)
    }

    private fun wrappedByParentheses(source: String): Boolean {
        if (!source.startsWith('(') || !source.endsWith(')')) return false
        var depth = 0
        var index = 0
        while (index < source.length) {
            val char = source[index]
            when (char) {
                '\'', '"' -> {
                    val closing = quotedLiteralEnd(source, index)
                    if (closing < 0) return false
                    index = closing
                }
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0 && index != source.lastIndex) return false
                }
            }
            index++
        }
        return depth == 0
    }

    private fun matchingConditionalColon(source: String, question: Int): Int {
        var cursor = question + 1
        var nested = 0
        while (cursor < source.length) {
            val nextQuestion = topLevel(source, '?', cursor)
            val nextColon = topLevel(source, ':', cursor)
            if (nextColon < 0) return -1
            if (nextQuestion >= 0 && nextQuestion < nextColon) {
                nested++
                cursor = nextQuestion + 1
            } else if (nested > 0) {
                nested--
                cursor = nextColon + 1
            } else return nextColon
        }
        return -1
    }

    private fun lastTopLevelOperator(source: String, operator: String): Int {
        var start = 0
        var last = -1
        while (true) {
            val index = topLevel(source, operator[0], start)
            if (index < 0) return last
            if (source.startsWith(operator, index) &&
                !(operator.length == 1 && operator[0] in "<>" && source.getOrNull(index + 1) == '='))
                last = index
            start = index + 1
        }
    }

    private fun lastTopLevelKeyword(source: String, keyword: String): Int {
        var start = 0
        var last = -1
        while (true) {
            val index = topLevel(source, keyword[0], start)
            if (index < 0) return last
            val before = source.getOrNull(index - 1)
            val after = source.getOrNull(index + keyword.length)
            if (source.startsWith(keyword, index) &&
                before != '.' &&
                before?.let { !it.isLetterOrDigit() && it != '_' } != false &&
                after?.let { !it.isLetterOrDigit() && it != '_' } != false) last = index
            start = index + 1
        }
    }

    private fun lastTopLevelAdditive(source: String): Int {
        var last = -1
        for (operator in "+-") {
            var start = 0
            while (true) {
                val index = topLevel(source, operator, start)
                if (index < 0) break
                val previous = source.substring(0, index).trimEnd().lastOrNull()
                val exponentSign = source.getOrNull(index - 1) in listOf('e', 'E') &&
                    source.getOrNull(index - 2)?.isDigit() == true &&
                    source.getOrNull(index + 1)?.isDigit() == true
                if (previous != null && previous !in "(+-*/%!<>=&|?:," &&
                    !exponentSign &&
                    !(operator == '-' && source.getOrNull(index + 1) == '>')) {
                    if (index > last) last = index
                }
                start = index + 1
            }
        }
        return last
    }

    private fun lastTopLevelArithmetic(source: String): Int =
        listOf('*', '/', '%').maxOf { lastTopLevelOperator(source, it.toString()) }

    private fun splitArguments(source: String): List<String> {
        if (source.isBlank()) return emptyList()
        val parts = mutableListOf<String>()
        var start = 0
        while (start <= source.length) {
            val comma = topLevel(source, ',', start)
            if (comma < 0) {
                parts += source.substring(start).trim()
                break
            }
            parts += source.substring(start, comma).trim()
            start = comma + 1
        }
        return parts
    }

    private fun path(expression: String): JsonElement? {
        var index = 0
        while (index < expression.length && (expression[index].isLetterOrDigit() || expression[index] == '_')) index++
        val root = expression.substring(0, index)
        if (!identifier.matches(root) || root !in scope) throw ExpressionFailure("unknown identifier: $root")
        var value = scope[root]
        while (index < expression.length) {
            var indexValue: JsonElement? = null
            when (expression[index]) {
                '.' -> {
                    index++
                    val start = index
                    while (index < expression.length && (expression[index].isLetterOrDigit() || expression[index] == '_')) index++
                    val member = expression.substring(start, index)
                    if (!identifier.matches(member)) throw ExpressionFailure("invalid member access")
                    indexValue = JsonPrimitive(member)
                }
                '[' -> {
                    val end = closingIndexBracket(expression, index)
                    if (end < 0) throw ExpressionFailure("invalid index")
                    val token = expression.substring(index + 1, end).trim()
                    indexValue = evaluate(token)
                    index = end + 1
                }
                else -> throw ExpressionFailure("unsupported expression")
            }
            value = selectIndex(value, indexValue)
        }
        return value
    }

    private fun selectIndex(value: JsonElement?, indexValue: JsonElement?): JsonElement? = when (value) {
        null, JsonNull -> null
        is JsonObject -> {
            if (indexValue == null || indexValue == JsonNull) null
            else {
                val member = indexValue as? JsonPrimitive
                if (member == null || !member.isString)
                    throw ExpressionFailure("object key must be a string")
                value[member.content]
            }
        }
        is JsonArray -> {
            if (indexValue == null || indexValue == JsonNull) null
            else {
                val position = (indexValue as? JsonPrimitive)
                    ?.takeIf { !it.isString }?.content?.toLongOrNull()
                    ?: throw ExpressionFailure("array index must be an integer")
                if (position < 0 || position >= value.size.toLong()) null
                else value[position.toInt()]
            }
        }
        else -> throw ExpressionFailure("member access requires an object or list")
    }

    private fun closingIndexBracket(source: String, opening: Int): Int {
        var depth = 1
        var index = opening + 1
        while (index < source.length) {
            val char = source[index]
            when (char) {
                '\'', '"' -> {
                    val closing = quotedLiteralEnd(source, index)
                    if (closing < 0) return -1
                    index = closing
                }
                '[' -> depth++
                ']' -> {
                    depth--
                    if (depth == 0) return index
                }
            }
            index++
        }
        return -1
    }

    private fun topLevel(source: String, sought: Char, start: Int = 0): Int {
        var depth = 0
        var index = start
        while (index < source.length) {
            val char = source[index]
            when (char) {
                '"', '\'' -> {
                    val closing = quotedLiteralEnd(source, index)
                    if (closing < 0) return -1
                    index = closing
                }
                '(', '[', '{' -> depth++
                ')', ']', '}' -> depth--
                sought -> if (depth == 0) return index
            }
            index++
        }
        return -1
    }

}

internal fun interpolateLabel(
    source: String,
    textParams: JsonObject?,
    data: JsonElement,
    locale: String,
    dynamicAllowed: Boolean,
    markdown: Boolean,
    translate: (String, String) -> String = { _, fallback -> fallback },
    config: JsonElement = JsonObject(emptyMap()),
    context: JsonElement = JsonObject(emptyMap()),
    item: JsonElement? = null,
): InterpolatedLabel {
    val failures = mutableListOf<String>()
    val warnings = mutableListOf<String>()
    if (looksLikeEscapedDynamicExpression(source)) warnings += "text contains a double-braced dynamic path"
    val namespace = mutableMapOf<String, JsonElement?>("locale" to JsonPrimitive(locale))
    if (dynamicAllowed) {
        namespace["data"] = data
        namespace["item"] = item
        namespace["config"] = config
        namespace["context"] = context
    }
    val parameterEvaluator = LabelExpressionEvaluator(namespace, translate, locale)
    val resolved = mutableMapOf<String, JsonElement?>()
    textParams.orEmpty().forEach { (name, raw) ->
        if (raw is JsonPrimitive && raw.isString && looksLikeEscapedDynamicExpression(raw.content))
            warnings += "textParams.$name contains a double-braced dynamic path"
        if (name == "locale") {
            failures += "textParams.locale is a reserved name"
            return@forEach
        }
        if (raw !is JsonPrimitive || !raw.isString) {
            resolved[name] = raw
            return@forEach
        }
        val parts = splitTextTemplate(raw.content)
        if (parts.size == 1 && parts.single() is TextSegment.Expression) {
            val expr = (parts.single() as TextSegment.Expression).source
            resolved[name] = runCatching { parameterEvaluator.evaluate(expr) }.getOrElse {
                failures += "$name: ${it.message}"
                null
            }
        } else {
            resolved[name] = JsonPrimitive(renderSegments(parts, parameterEvaluator, failures, markdown = false))
        }
    }
    val textScope = resolved + ("locale" to JsonPrimitive(locale))
    val text = renderSegments(splitTextTemplate(source), LabelExpressionEvaluator(textScope, translate, locale), failures, markdown)
    return InterpolatedLabel(text, failures, warnings)
}

private fun renderSegments(
    segments: List<TextSegment>,
    evaluator: LabelExpressionEvaluator,
    failures: MutableList<String>,
    markdown: Boolean,
): String = buildString {
    val linkTarget = if (markdown) MarkdownLinkTarget() else null
    for (segment in segments) when (segment) {
        is TextSegment.Literal -> {
            append(segment.text)
            linkTarget?.accept(segment.text)
        }
        is TextSegment.Expression -> {
            val rendered = runCatching { display(evaluator.evaluate(segment.source)) }.getOrElse {
                failures += "${segment.source.trim()}: ${it.message}"
                ""
            }
            append(when {
                linkTarget?.inUrl() == true -> encodeMarkdownUrlComponent(rendered)
                markdown -> escapeMarkdownValue(rendered)
                else -> rendered
            })
            linkTarget?.consumedExpression()
        }
    }
}
