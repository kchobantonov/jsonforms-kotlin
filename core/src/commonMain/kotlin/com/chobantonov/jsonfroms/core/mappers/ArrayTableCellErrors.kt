package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.validation.ErrorObject

/** Keeps a cell marker inside the fields that its detail form can edit. */
fun tableCellErrors(
    rowErrors: List<ErrorObject>,
    boundPath: String,
    detail: UiSchemaElement?,
    labelSummary: Boolean,
): List<ErrorObject> {
    if (labelSummary && detail == null) return emptyList()
    val scopes = if (detail == null) listOf(emptyList()) else detailValidationScopes(detail)
    if (scopes.isEmpty()) return emptyList()
    val base = pathSegments(boundPath)
    val prefixes = scopes.map { base + it }
    return rowErrors.filter { error ->
        val path = errorPathSegments(error)
        prefixes.any { prefix -> path.size >= prefix.size && path.subList(0, prefix.size) == prefix }
    }
}
