package com.chobantonov.jsonfroms.core.diagnostics

const val UiDiagnosticReporterConfigKey = "uiDiagnosticReporter"

enum class UiDiagnosticSeverity { Info, Warning, Error }

/** Author-facing runtime feedback; it does not change form data or validation status. */
data class UiDiagnostic(
    val code: String,
    val severity: UiDiagnosticSeverity,
    val message: String,
    val details: Map<String, Any?> = emptyMap(),
)

fun interface UiDiagnosticReporter {
    fun report(diagnostic: UiDiagnostic)
}
