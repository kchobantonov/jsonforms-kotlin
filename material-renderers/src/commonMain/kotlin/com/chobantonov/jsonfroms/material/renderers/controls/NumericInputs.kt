package com.chobantonov.jsonfroms.material.renderers.controls

import com.chobantonov.jsonfroms.core.util.parseIntegerDraft
import com.chobantonov.jsonfroms.core.util.parseNumberDraft
import kotlinx.serialization.json.JsonElement

internal data class NumericInputResult(
    val data: JsonElement?,
    val error: String? = null,
)

internal fun parseIntegerInput(value: String, invalidMessage: String = "Invalid integer."): NumericInputResult {
    if (value.isEmpty()) {
        return NumericInputResult(data = null)
    }

    val integer = parseIntegerDraft(value)
    return if (integer != null) {
        NumericInputResult(data = integer)
    } else {
        NumericInputResult(
            data = null,
            error = invalidMessage,
        )
    }
}

internal fun parseNumberInput(value: String, invalidMessage: String = "Invalid number."): NumericInputResult {
    if (value.isEmpty()) {
        return NumericInputResult(data = null)
    }

    val number = parseNumberDraft(value)
    return if (number != null) {
        NumericInputResult(data = number)
    } else {
        NumericInputResult(
            data = null,
            error = invalidMessage,
        )
    }
}
