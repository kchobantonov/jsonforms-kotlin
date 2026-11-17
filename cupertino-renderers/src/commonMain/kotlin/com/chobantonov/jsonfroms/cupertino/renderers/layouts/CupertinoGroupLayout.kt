package com.chobantonov.jsonfroms.cupertino.renderers.layouts

import com.chobantonov.jsonfroms.core.i18n.translateGroupRendererText

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.useJsonForms
import com.chobantonov.jsonfroms.compose.LocalHandledDependencyErrors
import com.chobantonov.jsonfroms.compose.ReportLayoutDiagnostics
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.i18n.defaultErrorTranslator
import com.chobantonov.jsonfroms.core.i18n.defaultTranslator
import com.chobantonov.jsonfroms.core.i18n.getCombinedErrorMessage
import com.chobantonov.jsonfroms.core.i18n.translateGroupValidationIndicator
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfLayout
import com.chobantonov.jsonfroms.core.mappers.groupContainsData
import com.chobantonov.jsonfroms.core.mappers.groupBoundScopes
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.mapStateToLayoutProps
import com.chobantonov.jsonfroms.core.mappers.resolveGroupExpansionOptions
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.store.getData
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.uiTypeIs
import com.chobantonov.jsonfroms.core.testers.withIncreasedRank
import com.chobantonov.jsonfroms.cupertino.renderers.controls.CupertinoDropdownChevron
import com.chobantonov.jsonfroms.cupertino.renderers.util.LocalCupertinoColors
import com.slapps.cupertino.CupertinoButton
import com.slapps.cupertino.CupertinoButtonDefaults
import com.slapps.cupertino.CupertinoText

val CupertinoGroupLayout = ComposeJsonFormsRenderer { props ->
    CupertinoGroupLayout(props)
}

@Composable
internal fun CupertinoGroupLayout(
    props: com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps,
    footer: (@Composable () -> Unit)? = null,
) {
    val layout = props.uischema as? GroupElement
        ?: error("CupertinoGroupLayout can only render Group elements")
    val layoutProps = mapStateToLayoutProps(
        state = props.state,
        ownProps = OwnPropsOfLayout(
            own = OwnPropsOfRenderer(
                uischema = props.uischema,
                schema = props.schema,
                enabled = props.enabled,
                path = props.path,
                renderers = props.renderers,
                cells = props.cells,
            ),
        ),
    )
    if (!layoutProps.visible) return
    ReportLayoutDiagnostics(props, layout.elements, horizontal = false)
    val expansion = resolveGroupExpansionOptions(layout.options, props.config)
    val reporter = props.config[UiDiagnosticReporterConfigKey] as? UiDiagnosticReporter
    LaunchedEffect(reporter, expansion.diagnostic) {
        expansion.diagnostic?.let { reporter?.report(it.copy(details = it.details + ("path" to props.path))) }
    }
    var expanded by remember(props.path) { mutableStateOf(!expansion.collapsed) }
    LaunchedEffect(expansion.collapsed) { expanded = !expansion.collapsed }
    val i18n = props.state.jsonforms.i18n
    val title = layoutProps.label?.takeIf { it.isNotBlank() }
        ?: translateGroupRendererText(i18n, "group.label", "Group")
    val stateText = if (expanded) translateGroupRendererText(i18n, "group.expanded", "Expanded")
        else translateGroupRendererText(i18n, "group.collapsed", "Collapsed")
    val scopes = remember(layout) { groupBoundScopes(layout) }
    val containsData = expansion.showDataIndicator &&
        groupContainsData(scopes.controlScopes, getData(props.state), props.path)
    val dataLabel = translateGroupRendererText(i18n, "group.dataIndicator", "Contains data")
    val errorIndex = useJsonForms().containerErrorIndex
    val allMissingDependencies = errorIndex.unrepresentedDependencyErrors(
        scopes.controlScopes, scopes.validationScopes, props.path)
    val handledDependencies = LocalHandledDependencyErrors.current
    val missingDependencyErrors = allMissingDependencies.filterNot { it in handledDependencies }
    val unrepresentedDependencies = allMissingDependencies.size
    val missingDependencyMessage = getCombinedErrorMessage(missingDependencyErrors,
        i18n.translateError ?: defaultErrorTranslator, i18n.translate ?: defaultTranslator,
        props.schema, layout, props.path)
    val hasErrors = expansion.showValidationIndicator &&
        (errorIndex.containsAny(scopes.validationScopes, props.path) || unrepresentedDependencies > 0)
    val errorCount = if (hasErrors && expansion.showValidationIndicatorCount) {
        errorIndex.countAny(scopes.validationScopes, props.path) + unrepresentedDependencies
    } else null
    val errorLabel = translateGroupValidationIndicator(i18n, errorCount)
    val colors = LocalCupertinoColors.current
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        if (expansion.collapsible) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CupertinoButton(
                    onClick = { expanded = !expanded },
                    colors = CupertinoButtonDefaults.plainButtonColors(),
                    modifier = Modifier.weight(1f).semantics {
                        stateDescription = stateText
                        if (hasErrors) error(errorLabel)
                    },
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        CupertinoText(title, Modifier.weight(1f))
                        CupertinoDropdownChevron(expanded, colors.secondaryLabel)
                    }
                }
                if (hasErrors) CupertinoText(
                    errorCount?.toString() ?: "●",
                    modifier = Modifier.padding(end = 8.dp)
                        .clearAndSetSemantics { contentDescription = errorLabel },
                    color = colors.destructive,
                )
                if (containsData) DataIndicatorDot(
                    dataLabel, colors.accent, Modifier.padding(end = 16.dp))
            }
        } else if (!layoutProps.label.isNullOrBlank()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CupertinoText(
                    layoutProps.label.orEmpty(),
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    color = colors.secondaryLabel,
                )
                if (hasErrors) CupertinoText(
                    errorCount?.toString() ?: "●",
                    modifier = Modifier.padding(end = 8.dp)
                        .clearAndSetSemantics { contentDescription = errorLabel },
                    color = colors.destructive,
                )
                if (containsData) DataIndicatorDot(
                    dataLabel, colors.accent, Modifier.padding(end = 16.dp))
            }
        }
        if (expansion.collapsible && !expanded && missingDependencyMessage.isNotBlank()) CupertinoText(
            missingDependencyMessage,
            modifier = Modifier.padding(horizontal = 16.dp),
            color = colors.destructive,
        )
        if (!expansion.collapsible || expanded) {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(colors.tertiaryBackground).padding(16.dp),
            ) {
                Column {
                    if (missingDependencyMessage.isNotBlank()) CupertinoText(
                        missingDependencyMessage,
                        modifier = Modifier.padding(bottom = 8.dp),
                        color = colors.destructive,
                    )
                    CompositionLocalProvider(LocalHandledDependencyErrors provides
                        (handledDependencies + missingDependencyErrors)) {
                        CupertinoLayoutRenderer(
                            props.copy(enabled = layoutProps.enabled),
                            layout.elements,
                            LayoutDirection.Column,
                            reportDiagnostics = false,
                        )
                    }
                    footer?.invoke()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DataIndicatorDot(label: String, color: Color, modifier: Modifier = Modifier) {
    val tooltip = rememberTooltipState()
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(focused, tooltip) {
        if (focused) tooltip.show() else tooltip.dismiss()
    }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(label) } },
        state = tooltip,
        modifier = modifier,
    ) {
        CupertinoText("●", modifier = Modifier.onFocusChanged { focused = it.isFocused }
            .semantics { contentDescription = label }.focusable(), color = color)
    }
}

val cupertinoGroupTester: RankedTester = withIncreasedRank(100, rankWith(2, uiTypeIs("Group")))
