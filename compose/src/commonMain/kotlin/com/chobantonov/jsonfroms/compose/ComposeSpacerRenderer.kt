package com.chobantonov.jsonfroms.compose

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.mappers.resolveSpacerSize
import com.chobantonov.jsonfroms.core.mappers.spacerSizeDiagnostic
import com.chobantonov.jsonfroms.core.models.SpacerElement
import com.chobantonov.jsonfroms.core.store.getData
import com.chobantonov.jsonfroms.core.store.getValidator
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.uiTypeIs
import com.chobantonov.jsonfroms.core.util.Runtime

val composeSpacerTester: RankedTester = rankWith(1, uiTypeIs("Spacer"))

val ComposeSpacerRenderer: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    StandaloneSpacer(props)
}

/** Parent layouts resolve directional sizing; standalone Spacer uses height. */
@Composable
private fun StandaloneSpacer(props: ComposeJsonFormsRendererProps) {
    val spacer = props.uischema as? SpacerElement ?: return
    if (!Runtime.isVisible(spacer, getData(props.state), getValidator(props.state), props.path, props.config)) return
    val reporter = props.config[UiDiagnosticReporterConfigKey] as? UiDiagnosticReporter
    val diagnostic = spacerSizeDiagnostic(spacer)?.let {
        it.copy(details = it.details + ("path" to props.path))
    }
    LaunchedEffect(reporter, diagnostic) {
        if (reporter != null && diagnostic != null) reporter.report(diagnostic)
    }
    Spacer(Modifier.height(resolveSpacerSize(spacer).dp).clearAndSetSemantics { })
}
