package com.chobantonov.jsonfroms.core.mappers

/** Distinguishes equal display labels without using those labels as value identity. */
fun accessibleChoiceLabel(option: EnumOption, options: List<EnumOption>): String =
    if (options.count { it.label == option.label } > 1) {
        "${option.label} (${option.value})"
    } else {
        option.label
    }
