package com.chobantonov.jsonfroms.core.confirmation

import com.chobantonov.jsonfroms.core.models.JsonOptions
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

enum class ConfirmationOperation(val key: String) {
    Delete("delete"),
    TypeChange("typeChange"),
    BranchChange("branchChange"),
}

enum class ConfirmationMode { Always, Never, Complex }

data class ConfirmationDecision(
    val mode: ConfirmationMode,
    val required: Boolean,
)

/** The caller supplies only values actually discarded; null means absent, JsonNull is present. */
fun resolveConfirmationDecision(
    operation: ConfirmationOperation,
    catalogId: String,
    discardedValues: List<JsonElement?>,
    elementOptions: JsonOptions = emptyMap(),
    config: Map<String, Any?> = emptyMap(),
): ConfirmationDecision {
    val elementMode = confirmationMode((elementOptions["confirmation"] as? JsonObject)?.get(operation.key))
    val namespaced = config["jsonformsExtended"].key("confirmation")
    val rendererMode = confirmationMode(namespaced.key("renderers").key(catalogId).key(operation.key))
    val defaultMode = confirmationMode(namespaced.key("default"))
    val fallback = if (operation == ConfirmationOperation.Delete ||
        (operation == ConfirmationOperation.TypeChange && catalogId == "mixed")) {
        ConfirmationMode.Complex
    } else {
        ConfirmationMode.Always
    }
    val mode = elementMode ?: rendererMode ?: defaultMode ?: fallback
    val present = discardedValues.filterNotNull()
    val required = when (mode) {
        ConfirmationMode.Never -> false
        ConfirmationMode.Always -> present.isNotEmpty()
        ConfirmationMode.Complex -> present.any { value ->
            when (value) {
                is JsonObject -> value.isNotEmpty()
                is JsonArray -> value.isNotEmpty()
                else -> false
            }
        }
    }
    return ConfirmationDecision(mode, required)
}

private fun Any?.key(name: String): Any? = (this as? Map<*, *>)?.get(name)

private fun confirmationMode(value: Any?): ConfirmationMode? = when (value) {
    is String -> value.toConfirmationMode()
    is JsonPrimitive -> value.contentOrNull?.toConfirmationMode()
    else -> null
}

private fun String.toConfirmationMode(): ConfirmationMode? = when (this) {
    "always" -> ConfirmationMode.Always
    "never" -> ConfirmationMode.Never
    "complex" -> ConfirmationMode.Complex
    else -> null
}
