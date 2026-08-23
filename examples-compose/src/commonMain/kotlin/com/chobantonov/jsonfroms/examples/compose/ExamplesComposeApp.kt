package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.reducers.defaultMiddleware
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.store.ValidationMode
import com.chobantonov.jsonfroms.examples.ExampleDescription
import com.chobantonov.jsonfroms.examples.StateProps
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import com.chobantonov.jsonfroms.material.renderers.materialCells
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
private val prettyJson = Json { prettyPrint = true; prettyPrintIndent = "  " }
private val CompactWidth = 840.dp
private const val GitHubUrl = "https://github.com/kchobantonov/jsonforms-kotlin"

private enum class DemoThemeMode(val displayName: String) {
    System("System"),
    Light("Light"),
    Dark("Dark"),
}

private data class DemoSettings(
    val themeMode: DemoThemeMode = DemoThemeMode.System,
    val validationMode: ValidationMode = ValidationMode.ValidateAndShow,
    val hideRequiredAsterisk: Boolean = false,
    val showUnfocusedDescription: Boolean = false,
    val restrict: Boolean = true,
    val readonly: Boolean = false,
    val collapseNewItems: Boolean = false,
    val hideArraySummaryValidation: Boolean = false,
    val initCollapsed: Boolean = false,
    val hideAvatar: Boolean = false,
) {
    fun applyTo(config: Map<String, Any?>) = config + mapOf(
        "hideRequiredAsterisk" to hideRequiredAsterisk,
        "showUnfocusedDescription" to showUnfocusedDescription,
        "restrict" to restrict,
        "collapseNewItems" to collapseNewItems,
        "hideArraySummaryValidation" to hideArraySummaryValidation,
        "initCollapsed" to initCollapsed,
        "hideAvatar" to hideAvatar,
    )
}

@Composable
fun ExamplesComposeApp(examples: List<ExampleDescription> = getBuiltInExamples()) {
    var settings by remember { mutableStateOf(DemoSettings()) }
    val useDarkTheme = when (settings.themeMode) {
        DemoThemeMode.System -> isSystemInDarkTheme()
        DemoThemeMode.Light -> false
        DemoThemeMode.Dark -> true
    }

    MaterialTheme(colorScheme = if (useDarkTheme) darkColorScheme() else lightColorScheme()) {
        if (examples.isEmpty()) return@MaterialTheme EmptyExamplesState()
        var selectedName by remember(examples) { mutableStateOf(examples.first().name) }
        val selected = examples.firstOrNull { it.name == selectedName } ?: examples.first()
        var state by remember(selected.name) { mutableStateOf(selected.toStateProps()) }
        var formOnly by remember { mutableStateOf(false) }
        var settingsOpen by remember { mutableStateOf(false) }

        BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()) {
            val compact = maxWidth < CompactWidth
            var navigationVisible by remember(compact) { mutableStateOf(!compact) }
            val selectExample: (ExampleDescription) -> Unit = {
                selectedName = it.name
                if (compact) navigationVisible = false
            }
            Column(Modifier.fillMaxSize()) {
                DemoTopBar(
                    navigationVisible, formOnly, settingsOpen,
                    { navigationVisible = !navigationVisible },
                    { formOnly = !formOnly },
                    { settingsOpen = !settingsOpen },
                )
                if (compact) {
                    CompactLayout(
                        examples, selected, state, navigationVisible && !formOnly, formOnly, settings,
                        selectExample, { state = it },
                    )
                } else {
                    ExpandedLayout(
                        examples, selected, state, navigationVisible && !formOnly, formOnly, settings,
                        selectExample, { state = it },
                    )
                }
            }
            if (settingsOpen) {
                Box(
                    Modifier.fillMaxSize().zIndex(2f).background(Color.Black.copy(alpha = .35f))
                        .clickable { settingsOpen = false },
                )
                SettingsPanel(
                    settings, { settings = it }, { settingsOpen = false },
                    Modifier.zIndex(3f).align(Alignment.TopEnd)
                        .padding(top = 68.dp, end = 12.dp, bottom = 12.dp)
                        .width(if (compact) maxWidth - 24.dp else 390.dp).fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun DemoTopBar(
    navigationVisible: Boolean,
    formOnly: Boolean,
    settingsOpen: Boolean,
    onNavigation: () -> Unit,
    onFormOnly: () -> Unit,
    onSettings: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    Surface(tonalElevation = 3.dp, shadowElevation = 3.dp) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolbarButton(MenuIcon, if (navigationVisible) "Hide example menu" else "Show example menu", navigationVisible, onNavigation)
            Text("//", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp))
            Text("JSON Forms Kotlin", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            ToolbarButton(PanelsIcon, if (formOnly) "Show full UI" else "Show form only", formOnly, onFormOnly)
            ToolbarButton(GitHubIcon, "Open GitHub") { uriHandler.openUri(GitHubUrl) }
            ToolbarButton(SettingsIcon, if (settingsOpen) "Close settings" else "Open settings", settingsOpen, onSettings)
        }
    }
}

@Composable
private fun ToolbarButton(icon: ImageVector, description: String, selected: Boolean = false, onClick: () -> Unit) {
    ActionTooltip(description) {
        Surface(
            color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
            shape = RoundedCornerShape(10.dp),
        ) {
            IconButton(onClick = onClick) { Icon(icon, contentDescription = description) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionTooltip(label: String, content: @Composable () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
        content = content,
    )
}

@Composable
private fun CompactLayout(
    examples: List<ExampleDescription>, selected: ExampleDescription, state: StateProps,
    navigationVisible: Boolean, formOnly: Boolean, settings: DemoSettings,
    onSelected: (ExampleDescription) -> Unit, onStateChange: (StateProps) -> Unit,
) {
    if (navigationVisible) {
        // On a phone the picker is the current navigation surface. Keeping the
        // form composed below it both wastes space and lets content show through
        // when the list is short.
        ExampleNavigation(
            examples = examples,
            selected = selected,
            onSelected = onSelected,
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!formOnly) ExampleHeader(selected)
            FormCard(selected, state, true, settings, onStateChange)
            if (!formOnly) DataCard(state)
        }
    }
}

@Composable
private fun ExpandedLayout(
    examples: List<ExampleDescription>, selected: ExampleDescription, state: StateProps,
    navigationVisible: Boolean, formOnly: Boolean, settings: DemoSettings,
    onSelected: (ExampleDescription) -> Unit, onStateChange: (StateProps) -> Unit,
) {
    Row(
        Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (navigationVisible) ExampleNavigation(
            examples, selected, onSelected, Modifier.width(260.dp).fillMaxHeight(),
        )
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (!formOnly) ExampleHeader(selected)
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FormCard(
                    selected, state, false, settings, onStateChange,
                    Modifier.weight(if (formOnly) 1f else 3f).fillMaxHeight(), true,
                )
                if (!formOnly) DataCard(state, Modifier.weight(1f).fillMaxHeight(), true)
            }
        }
    }
}

@Composable
private fun ExampleNavigation(
    examples: List<ExampleDescription>, selected: ExampleDescription,
    onSelected: (ExampleDescription) -> Unit, modifier: Modifier,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(examples, query) { filterExamples(examples, query) }
    Surface(modifier, tonalElevation = 1.dp, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("Search examples...") },
                leadingIcon = { Icon(SearchIcon, contentDescription = null) },
            )
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (filtered.isEmpty()) Text("No examples found.", modifier = Modifier.padding(8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                filtered.forEach { example ->
                    val isSelected = example.name == selected.name
                    Surface(
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().clickable { onSelected(example) },
                    ) { Text(example.label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) }
                }
            }
        }
    }
}

internal fun filterExamples(examples: List<ExampleDescription>, query: String): List<ExampleDescription> {
    val value = query.trim().lowercase()
    if (value.isEmpty()) return examples
    return examples.filter { it.name.lowercase().contains(value) || it.label.lowercase().contains(value) }
}

@Composable
private fun ExampleHeader(example: ExampleDescription) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp)) {
        Text(
            text = example.label,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(example.name, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FormCard(
    example: ExampleDescription, state: StateProps, compact: Boolean, settings: DemoSettings,
    onStateChange: (StateProps) -> Unit, modifier: Modifier = Modifier.fillMaxWidth(),
    scrollContent: Boolean = false,
) {
    Card(modifier) {
        val contentModifier = if (scrollContent) Modifier.fillMaxSize().verticalScroll(rememberScrollState()) else Modifier.fillMaxWidth()
        Column(contentModifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Form", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (state.actions.isNotEmpty()) {
                if (compact) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.actions.forEach { action -> FilledTonalButton(
                        { onStateChange(action.apply(state)) }, Modifier.fillMaxWidth(),
                    ) { Text(action.label) } }
                } else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.actions.forEach { action -> FilledTonalButton(
                        { onStateChange(action.apply(state)) },
                    ) { Text(action.label) } }
                }
            }
            JsonForms(
                data = state.data, schema = state.schema ?: example.schema,
                uischema = state.uischema ?: example.uischema, uischemas = state.uischemas,
                renderers = materialRenderers, cells = materialCells,
                config = settings.applyTo(state.config), readonly = state.readonly || settings.readonly,
                validationMode = settings.validationMode, i18n = state.i18n ?: JsonFormsI18nState(),
                additionalErrors = state.additionalErrors, middleware = state.middleware ?: defaultMiddleware,
                onChange = { onStateChange(state.copy(data = it.data)) },
            )
        }
    }
}

@Composable
private fun DataCard(state: StateProps, modifier: Modifier = Modifier.fillMaxWidth(), scrollContent: Boolean = false) {
    Card(modifier) {
        val contentModifier = if (scrollContent) Modifier.fillMaxSize().verticalScroll(rememberScrollState()) else Modifier.fillMaxWidth()
        Column(contentModifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Data", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(prettyJson.encodeToString(state.data), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun SettingsPanel(
    settings: DemoSettings, onChange: (DemoSettings) -> Unit, onClose: () -> Unit, modifier: Modifier,
) {
    Card(modifier.clickable { }) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Settings", style = MaterialTheme.typography.headlineSmall)
                    Text("Configure the renderer demo and JSON Forms behavior.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                ActionTooltip("Close settings") {
                    IconButton(onClose) { Icon(CloseIcon, contentDescription = "Close settings") }
                }
            }
            HorizontalDivider()
            Text("Appearance", style = MaterialTheme.typography.titleMedium)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DemoThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.themeMode == mode,
                        onClick = { onChange(settings.copy(themeMode = mode)) },
                        label = { Text(mode.displayName) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            HorizontalDivider()
            Text("Validation", style = MaterialTheme.typography.titleMedium)
            ValidationMode.entries.forEach { mode ->
                Surface(
                    color = if (settings.validationMode == mode) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onChange(settings.copy(validationMode = mode)) },
                ) { Text(mode.displayName(), modifier = Modifier.padding(12.dp)) }
            }
            HorizontalDivider()
            Text("Options", style = MaterialTheme.typography.titleMedium)
            SettingSwitch("Hide Required Asterisk", "Hide asterisks in labels for required fields.", settings.hideRequiredAsterisk) {
                onChange(settings.copy(hideRequiredAsterisk = it))
            }
            SettingSwitch("Show Unfocused Description", "Keep input descriptions visible while controls are unfocused.", settings.showUnfocusedDescription) {
                onChange(settings.copy(showUnfocusedDescription = it))
            }
            SettingSwitch("Restrict", "Enforce schema input and array size restrictions.", settings.restrict) { onChange(settings.copy(restrict = it)) }
            SettingSwitch("Read-Only", "Set all controls to read-only.", settings.readonly) { onChange(settings.copy(readonly = it)) }
            SettingSwitch("Collapse new array items", "Do not expand newly added array items.", settings.collapseNewItems) {
                onChange(settings.copy(collapseNewItems = it))
            }
            SettingSwitch("Hide array summary validation", "Hide validation summaries in array headers.", settings.hideArraySummaryValidation) {
                onChange(settings.copy(hideArraySummaryValidation = it))
            }
            SettingSwitch("Collapse arrays initially", "Start array accordions collapsed.", settings.initCollapsed) {
                onChange(settings.copy(initCollapsed = it))
            }
            SettingSwitch("Hide Array Item Avatar", "Hide array index avatars.", settings.hideAvatar) { onChange(settings.copy(hideAvatar = it)) }
        }
    }
}

@Composable
private fun SettingSwitch(label: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Switch(checked, onCheckedChange)
        Column(Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Medium)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun ValidationMode.displayName() = when (this) {
    ValidationMode.ValidateAndShow -> "Validate And Show"
    ValidationMode.ValidateAndHide -> "Validate And Hide"
    ValidationMode.NoValidation -> "No Validation"
}

private fun ExampleDescription.toStateProps() = StateProps(
    data, schema, uischema, materialRenderers, materialCells, config, uischemas,
    additionalErrors, actions, middleware, i18n, readonly,
)

@Composable
private fun EmptyExamplesState() {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding(), contentAlignment = Alignment.Center) {
        Text("No examples registered", style = MaterialTheme.typography.titleLarge)
    }
}

private val MenuIcon = lineIcon("Menu") { moveTo(4f, 6f); horizontalLineTo(20f); moveTo(4f, 12f); horizontalLineTo(20f); moveTo(4f, 18f); horizontalLineTo(20f) }
private val SearchIcon = lineIcon("Search") {
    moveTo(10.5f, 4f); curveTo(6.91f, 4f, 4f, 6.91f, 4f, 10.5f); curveTo(4f, 14.09f, 6.91f, 17f, 10.5f, 17f)
    curveTo(14.09f, 17f, 17f, 14.09f, 17f, 10.5f); curveTo(17f, 6.91f, 14.09f, 4f, 10.5f, 4f); moveTo(15.5f, 15.5f); lineTo(21f, 21f)
}
private val PanelsIcon = lineIcon("Form only") { moveTo(3f, 4f); horizontalLineTo(21f); verticalLineTo(20f); horizontalLineTo(3f); close(); moveTo(8f, 4f); verticalLineTo(20f); moveTo(8f, 10f); horizontalLineTo(21f) }
private val GitHubIcon = lineIcon("GitHub") {
    moveTo(12f, 2.5f); curveTo(6.75f, 2.5f, 2.5f, 6.75f, 2.5f, 12f); curveTo(2.5f, 16.2f, 5.22f, 19.76f, 9f, 21f)
    moveTo(15f, 21f); curveTo(18.78f, 19.76f, 21.5f, 16.2f, 21.5f, 12f); curveTo(21.5f, 6.75f, 17.25f, 2.5f, 12f, 2.5f)
    moveTo(9f, 21f); verticalLineTo(17.5f); curveTo(7f, 16.8f, 6.5f, 15f, 6.5f, 13.5f); moveTo(15f, 21f); verticalLineTo(17.5f)
}
private val SettingsIcon = lineIcon("Settings") { moveTo(4f, 6f); horizontalLineTo(20f); moveTo(4f, 12f); horizontalLineTo(20f); moveTo(4f, 18f); horizontalLineTo(20f); moveTo(8f, 3f); verticalLineTo(9f); moveTo(16f, 9f); verticalLineTo(15f); moveTo(10f, 15f); verticalLineTo(21f) }
private val CloseIcon = lineIcon("Close") { moveTo(5f, 5f); lineTo(19f, 19f); moveTo(19f, 5f); lineTo(5f, 19f) }

private fun lineIcon(name: String, draw: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) = ImageVector.Builder(
    name, 24.dp, 24.dp, 24f, 24f,
).apply {
    path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathBuilder = draw)
}.build()
