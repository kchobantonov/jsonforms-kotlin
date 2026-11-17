package com.chobantonov.jsonfroms.material.renderers.layouts

import com.chobantonov.jsonfroms.core.i18n.translateGroupRendererText

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.focusable
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.JsonFormsActionIcons
import com.chobantonov.jsonfroms.compose.LocalHandledDependencyErrors
import com.chobantonov.jsonfroms.compose.ReportLayoutDiagnostics
import com.chobantonov.jsonfroms.compose.useJsonForms
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.i18n.defaultErrorTranslator
import com.chobantonov.jsonfroms.core.i18n.defaultTranslator
import com.chobantonov.jsonfroms.core.i18n.getCombinedErrorMessage
import com.chobantonov.jsonfroms.core.i18n.translateGroupValidationIndicator
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfLayout
import com.chobantonov.jsonfroms.core.mappers.groupContainsData
import com.chobantonov.jsonfroms.core.mappers.groupBoundScopes
import com.chobantonov.jsonfroms.core.mappers.resolveGroupExpansionOptions
import com.chobantonov.jsonfroms.core.mappers.mapStateToLayoutProps
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.store.getData
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.uiTypeIs
import com.chobantonov.jsonfroms.core.testers.withIncreasedRank

private val groupTester: RankedTester = rankWith(
    1,
    uiTypeIs("Group"),
)

val MaterialGroupLayout: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialGroupLayout(props)
}

@Composable
fun MaterialGroupLayout(
    props: ComposeJsonFormsRendererProps,
    elevation: Dp = 2.dp,
    footer: (@Composable () -> Unit)? = null,
) {
    val layout = props.uischema as? GroupElement
        ?: error("MaterialGroupLayout can only render Group elements")
    val layoutProps = mapStateToLayoutProps(
        state = props.state,
        ownProps = OwnPropsOfLayout(
            own = com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer(
                uischema = props.uischema,
                schema = props.schema,
                enabled = props.enabled,
                path = props.path,
                renderers = props.renderers,
                cells = props.cells,
            ),
        ),
    )

    if (!layoutProps.visible) {
        return
    }
    ReportLayoutDiagnostics(props, layout.elements, horizontal = false)
    val label = layoutProps.label
    val expansion = resolveGroupExpansionOptions(layout.options, props.config)
    val reporter = props.config[UiDiagnosticReporterConfigKey] as? UiDiagnosticReporter
    LaunchedEffect(reporter, expansion.diagnostic) {
        expansion.diagnostic?.let { reporter?.report(it.copy(details = it.details + ("path" to props.path))) }
    }
    var expanded by remember(props.path) { mutableStateOf(!expansion.collapsed) }
    LaunchedEffect(expansion.collapsed) { expanded = !expansion.collapsed }
    val i18n = props.state.jsonforms.i18n
    val title = label?.takeIf { it.isNotBlank() }
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
    ) {
        if (expansion.collapsible) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.weight(1f).semantics {
                        stateDescription = stateText
                        if (hasErrors) error(errorLabel)
                    },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(title, Modifier.weight(1f))
                        Icon(
                            JsonFormsActionIcons.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.rotate(if (expanded) 90f else 0f),
                        )
                    }
                }
                if (hasErrors) Badge(
                    modifier = Modifier.padding(end = 8.dp)
                        .clearAndSetSemantics { contentDescription = errorLabel },
                ) { if (errorCount != null) Text(errorCount.toString()) }
                if (containsData) DataIndicatorBadge(dataLabel, Modifier.padding(end = 16.dp))
            }
        } else if (!label.isNullOrBlank()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label.orEmpty(),
                    modifier = Modifier.weight(1f).padding(start = 16.dp, top = 16.dp),
                )
                if (hasErrors) Badge(
                    modifier = Modifier.padding(top = 16.dp, end = 8.dp)
                        .clearAndSetSemantics { contentDescription = errorLabel },
                ) { if (errorCount != null) Text(errorCount.toString()) }
                if (containsData) DataIndicatorBadge(
                    dataLabel, Modifier.padding(top = 16.dp, end = 16.dp))
            }
        }
        if (expansion.collapsible && !expanded && missingDependencyMessage.isNotBlank()) Text(
            missingDependencyMessage,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.error,
        )
        if (!expansion.collapsible || expanded) {
            Box(Modifier.padding(16.dp)) {
                Column {
                    if (missingDependencyMessage.isNotBlank()) Text(
                        missingDependencyMessage,
                        modifier = Modifier.padding(bottom = 8.dp),
                        color = MaterialTheme.colorScheme.error,
                    )
                    CompositionLocalProvider(LocalHandledDependencyErrors provides
                        (handledDependencies + missingDependencyErrors)) {
                        MaterialLayoutRenderer(
                            props = props.copy(enabled = layoutProps.enabled),
                            elements = layout.elements,
                            direction = LayoutDirection.Column,
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
private fun DataIndicatorBadge(label: String, modifier: Modifier = Modifier) {
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
        Badge(Modifier.onFocusChanged { focused = it.isFocused }
            .semantics { contentDescription = label }.focusable())
    }
}

val materialGroupTester: RankedTester = withIncreasedRank(
    1,
    groupTester,
)
