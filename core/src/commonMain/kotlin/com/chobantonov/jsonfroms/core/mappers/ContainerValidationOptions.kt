package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.JsonOptions
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

data class ContainerValidationOptions(
    val showIndicator: Boolean,
    val showCount: Boolean,
)

/** Local booleans win over namespaced global booleans, including explicit false. */
fun resolveContainerValidationOptions(
    options: JsonOptions,
    config: Map<String, Any?>,
    defaultIndicator: Boolean = false,
): ContainerValidationOptions {
    val global = config["jsonformsExtended"]
    fun value(key: String): Any? = if (options.containsKey(key)) options[key] else when (global) {
        is Map<*, *> -> global[key]
        is JsonObject -> global[key]
        else -> null
    }
    fun boolean(value: Any?): Boolean? = when (value) {
        is Boolean -> value
        is JsonPrimitive -> if (value.isString) null else value.content.toBooleanStrictOrNull()
        else -> null
    }
    return ContainerValidationOptions(
        showIndicator = boolean(value("showValidationIndicator")) ?: defaultIndicator,
        showCount = boolean(value("showValidationIndicatorCount")) ?: false,
    )
}
