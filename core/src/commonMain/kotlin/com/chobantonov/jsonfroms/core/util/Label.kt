package com.chobantonov.jsonfroms.core.util

fun createCleanLabel(label: String): String =
    label
        .replace('_', ' ')
        .replace('-', ' ')
        .replace(Regex("([a-z0-9])([A-Z])"), "$1 $2")
        .replace('\n', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()
        .split(' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { token ->
            token.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
