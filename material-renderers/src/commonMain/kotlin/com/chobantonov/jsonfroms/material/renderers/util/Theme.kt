package com.chobantonov.jsonfroms.material.renderers.util

import com.chobantonov.jsonfroms.core.models.ControlElement

enum class MaterialInputVariant {
    Standard,
    Filled,
    Outlined,
}

val defaultInputVariant: MaterialInputVariant = MaterialInputVariant.Outlined

fun useInputVariant(
    config: Map<String, Any?>,
    uischema: ControlElement,
): MaterialInputVariant {
    val optionVariant = uischema.options["variant"] as? String
    val configVariant = config["variant"] as? String
    return parseInputVariant(optionVariant)
        ?: parseInputVariant(configVariant)
        ?: defaultInputVariant
}

private fun parseInputVariant(value: String?): MaterialInputVariant? =
    when (value?.lowercase()) {
        "standard" -> MaterialInputVariant.Standard
        "filled" -> MaterialInputVariant.Filled
        "outlined" -> MaterialInputVariant.Outlined
        else -> null
    }
