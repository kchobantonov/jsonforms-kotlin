@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.chobantonov.jsonfroms.material.renderers.controls

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticSeverity
import com.chobantonov.jsonfroms.core.i18n.translateTemporalControlText
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

data class TemporalSaveFormatResolution(val format: String, val rejected: Boolean)

/** A schema format constrains storage even when the UI offers a custom display/save pattern. */
fun resolveTemporalSaveFormat(
    schemaFormat: String?,
    requested: String?,
    default: String,
): TemporalSaveFormatResolution {
    if (requested == null) return TemporalSaveFormatResolution(default, false)
    val sample = formatTemporal(TemporalValue(2024, 2, 29, 13, 45, 30), requested)
    val compatible = when (schemaFormat) {
        "date" -> Regex("\\d{4}-\\d{2}-\\d{2}").matches(sample) &&
            runCatching { LocalDate.parse(sample) }.isSuccess
        "time" -> Regex("\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:\\d{2})").matches(sample) &&
            runCatching { Instant.parse("2024-02-29T$sample") }.isSuccess
        "date-time" -> Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:\\d{2})").matches(sample) &&
            runCatching { Instant.parse(sample) }.isSuccess
        else -> true
    }
    return if (compatible) TemporalSaveFormatResolution(requested, false)
        else TemporalSaveFormatResolution(default, true)
}

@Composable
fun temporalSaveFormatForControl(
    schemaFormat: String?,
    requested: String?,
    default: String,
    path: String,
    config: Map<String, Any?>,
    i18n: JsonFormsI18nState,
    i18nKeyPrefix: String?,
    displayFormat: String,
): String {
    val resolution = resolveTemporalSaveFormat(schemaFormat, requested, default)
    val reporter = config[UiDiagnosticReporterConfigKey] as? UiDiagnosticReporter
    val message = translateTemporalControlText(i18n, i18nKeyPrefix, "saveFormatIncompatible",
        "Custom save format is incompatible with the schema format; the default save format is used.",
        displayFormat, mapOf("schemaFormat" to schemaFormat.orEmpty(), "requestedFormat" to requested.orEmpty()))
    LaunchedEffect(reporter, schemaFormat, requested, path, resolution.rejected, message) {
        if (resolution.rejected) reporter?.report(UiDiagnostic(
            code = "temporal.saveFormatIncompatible",
            severity = UiDiagnosticSeverity.Warning,
            message = message,
            details = mapOf("path" to path, "schemaFormat" to schemaFormat.orEmpty()),
        ))
    }
    return resolution.format
}
