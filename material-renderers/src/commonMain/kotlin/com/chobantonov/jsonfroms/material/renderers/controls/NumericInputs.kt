package com.chobantonov.jsonfroms.material.renderers.controls

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

internal data class NumericInputResult(
    val data: JsonElement?,
    val error: String? = null,
)

internal fun parseIntegerInput(value: String): NumericInputResult {
    if (value.isEmpty()) {
        return NumericInputResult(data = null)
    }

    val integer = value.toLongOrNull()
    return if (integer != null) {
        NumericInputResult(data = JsonPrimitive(integer))
    } else {
        NumericInputResult(
            data = JsonPrimitive(value),
            error = "Invalid integer.",
        )
    }
}

internal fun parseNumberInput(value: String): NumericInputResult {
    if (value.isEmpty()) {
        return NumericInputResult(data = null)
    }

    val number = value.toDoubleOrNull()?.takeIf { it.isFinite() }
    return if (number != null) {
        NumericInputResult(data = JsonPrimitive(number))
    } else {
        NumericInputResult(
            data = JsonPrimitive(value),
            error = "Invalid number.",
        )
    }
}
