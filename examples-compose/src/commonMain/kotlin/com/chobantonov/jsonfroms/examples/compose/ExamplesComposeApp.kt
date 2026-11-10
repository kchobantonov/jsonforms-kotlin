package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.cupertino.renderers.CupertinoSwitch
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoCells
import com.chobantonov.jsonfroms.extended.renderers.cupertinoExtendedRenderers
import com.chobantonov.jsonfroms.extended.renderers.materialExtendedRenderers
import com.chobantonov.jsonfroms.extended.renderers.ExtendedActionHandler
import com.chobantonov.jsonfroms.extended.renderers.ExtendedActionHandlerConfigKey
import com.chobantonov.jsonfroms.extended.renderers.ExtendedFilePicker
import com.chobantonov.jsonfroms.extended.renderers.ExtendedFilePickerConfigKey
import com.chobantonov.jsonfroms.cupertino.renderers.util.CupertinoAppTheme
import com.chobantonov.jsonfroms.cupertino.renderers.util.CupertinoThemePreset
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.models.toUiSchemaElement
import com.chobantonov.jsonfroms.core.reducers.defaultMiddleware
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.store.Translator
import com.chobantonov.jsonfroms.core.store.ValidationMode
import com.chobantonov.jsonfroms.examples.ExampleDescription
import com.chobantonov.jsonfroms.examples.StateProps
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import com.chobantonov.jsonfroms.material.renderers.materialCells
import com.chobantonov.jsonfroms.material.renderers.util.MaterialThemePreset
import com.chobantonov.jsonfroms.material.renderers.util.materialColorScheme
import com.russhwolf.settings.Settings
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

@OptIn(ExperimentalSerializationApi::class)
private val prettyJson = Json { prettyPrint = true; prettyPrintIndent = "  " }
private val CompactWidth = 840.dp
private const val GitHubUrl = "https://github.com/kchobantonov/jsonforms-kotlin"
internal expect fun createDemoFilePicker(): ExtendedFilePicker?
private val LocalDemoUiStyle = compositionLocalOf { DemoUiStyle.Material }

internal enum class DemoThemeMode(val displayName: String) {
    System("System"),
    Light("Light"),
    Dark("Dark"),
}

internal enum class DemoUiStyle(val displayName: String) {
    System("System"),
    Material("Material"),
    Cupertino("iOS"),
}

internal enum class DemoLocale(val displayName: String, val languageTag: String?) {
    English("English (en)", "en"),
    German("German (de)", "de"),
    Bulgarian("Bulgarian (bg)", "bg"),
    BrowserLanguage("Browser Language", null),
}

internal enum class DemoLayout(val displayName: String) {
    Default("Default"),
    DemoAndData("Demo and Data"),
}

internal enum class DemoPane(val displayName: String) {
    Demo("Demo"),
    Data("Data"),
    Schema("Schema"),
    UiSchema("UI Schema"),
    UiSchemas("UI Schemas"),
    Internationalization("Internationalization"),
    Config("Config"),
}

private data class EditorDraft(val text: String, val error: String? = null)

private data class EditorDrafts(val values: Map<DemoPane, EditorDraft>) {
    operator fun get(pane: DemoPane) = values.getValue(pane)

    fun update(pane: DemoPane, transform: (EditorDraft) -> EditorDraft) =
        copy(values = values + (pane to transform(get(pane))))

    companion object {
        fun from(state: StateProps) = EditorDrafts(
            DemoPane.entries.filter { it.isEditor }.associateWith { pane ->
                EditorDraft(editorText(pane, state))
            },
        )
    }
}

private val DemoPane.isEditor: Boolean
    get() = this != DemoPane.Demo

internal data class DemoSettings(
    val themeMode: DemoThemeMode = DemoThemeMode.System,
    val uiStyle: DemoUiStyle = DemoUiStyle.System,
    val lightMaterialTheme: MaterialThemePreset = MaterialThemePreset.Purple,
    val darkMaterialTheme: MaterialThemePreset = MaterialThemePreset.Purple,
    val lightCupertinoTheme: CupertinoThemePreset = CupertinoThemePreset.Blue,
    val darkCupertinoTheme: CupertinoThemePreset = CupertinoThemePreset.Blue,
    val locale: DemoLocale = DemoLocale.English,
    val demoLayout: DemoLayout = DemoLayout.Default,
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
    fun effectiveUiStyle(applePlatform: Boolean = isApplePlatform): DemoUiStyle =
        resolveUiStyle(uiStyle, applePlatform)

    fun cupertinoTheme(darkTheme: Boolean): CupertinoThemePreset =
        if (darkTheme) darkCupertinoTheme else lightCupertinoTheme

    fun materialTheme(darkTheme: Boolean): MaterialThemePreset =
        if (darkTheme) darkMaterialTheme else lightMaterialTheme

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
fun ExamplesComposeApp(
    examples: List<ExampleDescription> = getBuiltInExamples(),
    selectedExampleName: String? = null,
    onExampleSelected: (String) -> Unit = {},
    showLandingWhenUnselected: Boolean = false,
    onHomeSelected: (() -> Unit)? = null,
) {
    val preferences = remember { DemoPreferences(Settings()) }
    var settings by remember(preferences) { mutableStateOf(preferences.load()) }
    val useDarkTheme = when (settings.themeMode) {
        DemoThemeMode.System -> isSystemInDarkTheme()
        DemoThemeMode.Light -> false
        DemoThemeMode.Dark -> true
    }

    val effectiveUiStyle = settings.effectiveUiStyle()
    DemoAppTheme(
        effectiveUiStyle,
        useDarkTheme,
        settings.materialTheme(useDarkTheme),
        settings.cupertinoTheme(useDarkTheme),
    ) {
        if (examples.isEmpty()) return@DemoAppTheme EmptyExamplesState()
        var localSelectedName by remember(examples) { mutableStateOf(examples.first().name) }
        val effectiveSelectedName = selectedExampleName ?: localSelectedName
        val selected = examples.firstOrNull { it.name == effectiveSelectedName } ?: examples.first()
        val showLanding = showLandingWhenUnselected && selectedExampleName == null
        val originalState = remember(selected.name) { selected.toStateProps() }
        var state by remember(selected.name) { mutableStateOf(originalState) }
        var editorDrafts by remember(selected.name) { mutableStateOf(EditorDrafts.from(state)) }
        var formOnly by remember { mutableStateOf(false) }
        var settingsOpen by remember { mutableStateOf(false) }

        BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()) {
            val compact = maxWidth < CompactWidth
            var navigationVisible by remember(compact) { mutableStateOf(!compact) }
            val selectExample: (ExampleDescription) -> Unit = {
                localSelectedName = it.name
                onExampleSelected(it.name)
                if (compact) navigationVisible = false
            }
            val updateState: (StateProps) -> Unit = { nextState ->
                if (nextState.data != state.data || nextState.dataProvided != state.dataProvided) {
                    editorDrafts = editorDrafts.update(DemoPane.Data) {
                        EditorDraft(editorText(DemoPane.Data, nextState))
                    }
                }
                state = nextState
            }
            Column(Modifier.fillMaxSize()) {
                DemoTopBar(
                    navigationVisible, formOnly && !showLanding, settingsOpen,
                    { navigationVisible = !navigationVisible },
                    {
                        if (showLanding) selectExample(examples.first())
                        formOnly = !formOnly
                    },
                    { settingsOpen = !settingsOpen },
                    onHomeSelected?.let { home -> { formOnly = false; home() } },
                )
                if (compact) {
                    CompactLayout(
                        examples, selected, originalState, state, navigationVisible && !formOnly, formOnly, settings,
                        editorDrafts, { editorDrafts = it }, selectExample, updateState, { settings = it },
                        showLanding, { formOnly = false; selectExample(examples.first()) },
                    )
                } else {
                    ExpandedLayout(
                        examples, selected, originalState, state, navigationVisible && !formOnly, formOnly, settings,
                        editorDrafts, { editorDrafts = it }, selectExample, updateState, { settings = it },
                        showLanding, { formOnly = false; selectExample(examples.first()) },
                    )
                }
            }
            if (settingsOpen) {
                Box(
                    Modifier.fillMaxSize().zIndex(2f).background(Color.Black.copy(alpha = .35f))
                        .clickable { settingsOpen = false },
                )
                SettingsPanel(
                    settings, {
                        settings = it
                        preferences.save(it)
                    }, { settingsOpen = false },
                    Modifier.zIndex(3f).align(Alignment.TopEnd)
                        .padding(top = 68.dp, end = 12.dp, bottom = 12.dp)
                        .width(if (compact) maxWidth - 24.dp else 390.dp).fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun DemoAppTheme(
    style: DemoUiStyle,
    darkTheme: Boolean,
    materialTheme: MaterialThemePreset,
    cupertinoTheme: CupertinoThemePreset,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalDemoUiStyle provides style) {
        if (style == DemoUiStyle.Cupertino) {
            CupertinoAppTheme(darkTheme, cupertinoTheme, content)
        } else {
            MaterialTheme(
                colorScheme = materialColorScheme(darkTheme, materialTheme),
                content = content,
            )
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
    onHome: (() -> Unit)?,
) {
    val uriHandler = LocalUriHandler.current
    val cupertino = LocalDemoUiStyle.current == DemoUiStyle.Cupertino
    Surface(
        tonalElevation = if (cupertino) 0.dp else 3.dp,
        shadowElevation = if (cupertino) 0.dp else 3.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolbarButton(MenuIcon, if (navigationVisible) "Hide example menu" else "Show example menu", navigationVisible, onNavigation)
            Icon(
                KotlinLogo,
                contentDescription = if (onHome != null) "Go to welcome page" else "Kotlin logo",
                tint = Color.Unspecified,
                modifier = Modifier.size(40.dp).clickable(enabled = onHome != null) { onHome?.invoke() }.padding(6.dp),
            )
            Text("JSON Forms Kotlin", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f).clickable(enabled = onHome != null) { onHome?.invoke() })
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
    examples: List<ExampleDescription>, selected: ExampleDescription, originalState: StateProps, state: StateProps,
    navigationVisible: Boolean, formOnly: Boolean, settings: DemoSettings,
    editorDrafts: EditorDrafts, onEditorDraftsChange: (EditorDrafts) -> Unit,
    onSelected: (ExampleDescription) -> Unit, onStateChange: (StateProps) -> Unit,
    onSettingsChange: (DemoSettings) -> Unit,
    showLanding: Boolean, onOpenDemo: () -> Unit,
) {
    if (navigationVisible) {
        // On a phone the picker is the current navigation surface. Keeping the
        // form composed below it both wastes space and lets content show through
        // when the list is short.
        ExampleNavigation(
            examples = examples,
            selected = selected.takeUnless { showLanding },
            onSelected = onSelected,
            modifier = Modifier.fillMaxSize(),
        )
    } else if (showLanding) {
        DemoLandingPage(onOpenDemo, Modifier.fillMaxSize())
    } else {
        var selectedPane by remember(selected.name) { mutableStateOf(DemoPane.Demo) }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!formOnly) ExampleHeader(selected)
            if (!formOnly) {
                DemoPaneSelector(selectedPane) { selectedPane = it }
            }
            when {
                formOnly -> FormCard(state, true, settings, onStateChange, onSettingsChange)
                selectedPane.isEditor -> EditorCard(
                    selectedPane, originalState, state, editorDrafts, onEditorDraftsChange, onStateChange,
                    Modifier.fillMaxWidth().heightIn(min = 520.dp),
                )
                settings.demoLayout == DemoLayout.DemoAndData -> {
                    FormCard(state, true, settings, onStateChange, onSettingsChange)
                    EditorCard(
                        DemoPane.Data, originalState, state, editorDrafts,
                        onEditorDraftsChange, onStateChange,
                        Modifier.fillMaxWidth().heightIn(min = 520.dp),
                    )
                }
                selectedPane == DemoPane.Demo -> FormCard(state, true, settings, onStateChange, onSettingsChange)
            }
        }
    }
}

@Composable
private fun ExpandedLayout(
    examples: List<ExampleDescription>, selected: ExampleDescription, originalState: StateProps, state: StateProps,
    navigationVisible: Boolean, formOnly: Boolean, settings: DemoSettings,
    editorDrafts: EditorDrafts, onEditorDraftsChange: (EditorDrafts) -> Unit,
    onSelected: (ExampleDescription) -> Unit, onStateChange: (StateProps) -> Unit,
    onSettingsChange: (DemoSettings) -> Unit,
    showLanding: Boolean, onOpenDemo: () -> Unit,
) {
    var selectedPane by remember(selected.name) { mutableStateOf(DemoPane.Demo) }
    Row(
        Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (navigationVisible) ExampleNavigation(
            examples, selected.takeUnless { showLanding }, onSelected, Modifier.width(260.dp).fillMaxHeight(),
        )
        if (showLanding) {
            DemoLandingPage(onOpenDemo, Modifier.fillMaxSize())
        } else Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (!formOnly) ExampleHeader(selected)
            if (!formOnly) {
                DemoPaneSelector(selectedPane) { selectedPane = it }
            }
            if (!formOnly && selectedPane.isEditor) {
                EditorCard(
                    selectedPane, originalState, state, editorDrafts, onEditorDraftsChange, onStateChange,
                    Modifier.weight(1f).fillMaxWidth(),
                )
            } else if (formOnly || settings.demoLayout == DemoLayout.DemoAndData) Row(
                Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                FormCard(
                    state, false, settings, onStateChange, onSettingsChange,
                    Modifier.weight(if (formOnly) 1f else 3f).fillMaxHeight(), true,
                )
                if (!formOnly) EditorCard(
                    DemoPane.Data, originalState, state, editorDrafts,
                    onEditorDraftsChange, onStateChange,
                    Modifier.weight(1f).fillMaxHeight(),
                )
            } else if (selectedPane == DemoPane.Demo) {
                FormCard(
                    state, false, settings, onStateChange, onSettingsChange,
                    Modifier.weight(1f).fillMaxWidth(), true,
                )
            }
        }
    }
}

@Composable
private fun DemoPaneSelector(selected: DemoPane, onSelected: (DemoPane) -> Unit) {
    SecondaryScrollableTabRow(
        selectedTabIndex = DemoPane.entries.indexOf(selected),
        modifier = Modifier.fillMaxWidth(),
        edgePadding = 0.dp,
        minTabWidth = 80.dp,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        DemoPane.entries.forEach { pane ->
            Tab(
                selected = selected == pane,
                onClick = { onSelected(pane) },
                text = { Text(pane.displayName) },
            )
        }
    }
}

@Composable
private fun ExampleNavigation(
    examples: List<ExampleDescription>, selected: ExampleDescription?,
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
                    val isSelected = example.name == selected?.name
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
private fun DemoLandingPage(onOpenDemo: () -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val compact = maxWidth < 600.dp
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 20.dp),
        ) {
            Icon(
                KotlinLogo,
                contentDescription = "Kotlin logo",
                modifier = Modifier.size(if (compact) 88.dp else 120.dp),
                tint = Color.Unspecified,
            )
            Text(
                "JSON FORMS RENDERER SET",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                "Welcome to JSON Forms Kotlin",
                style = if (compact) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 800.dp),
            )
            Text(
                "Compose Multiplatform renderers for JSON Forms.",
                style = if (compact) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onOpenDemo) { Text("Open Demo") }
        }
    }
}

@Composable
private fun FormCard(
    state: StateProps, compact: Boolean, settings: DemoSettings,
    onStateChange: (StateProps) -> Unit, onSettingsChange: (DemoSettings) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    scrollContent: Boolean = false,
) {
    val cupertino = settings.effectiveUiStyle() == DemoUiStyle.Cupertino
    val picker = remember { createDemoFilePicker() }
    val actionHandler = ExtendedActionHandler { action, params ->
        if (action == "changeLang") {
            val language = (params["lang"] as? JsonPrimitive)?.contentOrNull
            DemoLocale.entries.firstOrNull { it.languageTag == language }?.let { locale ->
                onSettingsChange(settings.copy(locale = locale))
            }
        }
        val data = (state.data as? JsonObject).orEmpty()
        val status = when (action) {
            "applyStatus", "simulateHttpSubmit", "changeLang" -> {
                val locale = settings.locale.languageTag ?: "en"
                ((params["statusByLocale"] as? JsonObject)?.get(locale) as? JsonPrimitive)?.contentOrNull
                    ?: "Action completed"
            }
            "runScript" -> "Updated from host action"
            else -> "Action: $action"
        }
        onStateChange(state.copy(data = JsonObject(data + mapOf(
            "status" to JsonPrimitive(status),
            "lastTriggeredBy" to JsonPrimitive(action),
        ))))
    }
    val extendedConfig = settings.applyTo(state.config) + mapOf(
        ExtendedActionHandlerConfigKey to actionHandler,
        ExtendedFilePickerConfigKey to picker,
    )
    Card(modifier) {
        val configuredI18n = exampleI18nForLocale(
            state,
            settings.locale.languageTag ?: Locale.current.toLanguageTag(),
        )
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
                data = state.data, dataProvided = state.dataProvided, schema = state.schema,
                uischema = state.uischema, uischemas = state.uischemas,
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                cells = if (cupertino) cupertinoCells else materialCells,
                config = extendedConfig, readonly = state.readonly || settings.readonly,
                validationMode = settings.validationMode, i18n = configuredI18n,
                additionalErrors = state.additionalErrors, middleware = state.middleware ?: defaultMiddleware,
                onChange = { onStateChange(state.copy(data = it.data, dataProvided = it.dataProvided)) },
            )
        }
    }
}

@Composable
private fun EditorCard(
    pane: DemoPane,
    originalState: StateProps,
    state: StateProps,
    drafts: EditorDrafts,
    onDraftsChange: (EditorDrafts) -> Unit,
    onStateChange: (StateProps) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    val draft = drafts[pane]
    Card(modifier) {
        Column(
            Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(pane.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Changes affect the active form only after Apply succeeds.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                TextButton(onClick = {
                    onDraftsChange(drafts.update(pane) { EditorDraft(editorText(pane, originalState)) })
                }) { Text("Reload") }
                Button(onClick = {
                    val result = applyEditorDraft(pane, draft.text, state)
                    result.fold(
                        onSuccess = { nextState ->
                            onStateChange(nextState)
                            onDraftsChange(drafts.update(pane) { it.copy(error = null) })
                        },
                        onFailure = { failure ->
                            val message = failure.message ?: "The JSON could not be applied."
                            onDraftsChange(drafts.update(pane) { it.copy(error = message) })
                        },
                    )
                }) { Text("Apply") }
            }
            OutlinedTextField(
                value = draft.text,
                onValueChange = { value ->
                    onDraftsChange(drafts.update(pane) { it.copy(text = value, error = null) })
                },
                modifier = Modifier.fillMaxWidth().weight(1f),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                isError = draft.error != null,
                supportingText = draft.error?.let { error -> { Text(error) } },
                label = { Text("JSON") },
            )
            if (pane == DemoPane.UiSchemas) {
                Text(
                    "Registry tester functions are retained by position; the number of entries cannot be changed here.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

internal fun editorText(pane: DemoPane, state: StateProps): String {
    val json = when (pane) {
        DemoPane.Data -> state.data.takeIf { state.dataProvided }
        DemoPane.Schema -> state.schema?.toJsonObject()
        DemoPane.UiSchema -> state.uischema?.toJsonObject()
        DemoPane.UiSchemas -> JsonArray(state.uischemas.map { it.uischema.toJsonObject() })
        DemoPane.Internationalization -> {
            val byLocale = state.localizedTranslations.ifEmpty {
                if (state.translations.isEmpty()) emptyMap() else mapOf("en" to state.translations)
            }
            JsonObject(byLocale.mapValues { (_, translations) ->
                JsonObject(translations.mapValues { (_, value) -> JsonPrimitive(value) })
            })
        }
        DemoPane.Config -> state.config.toJsonObject()
        else -> error("$pane is not a JSON editor")
    }
    return json?.let { prettyJson.encodeToString(it) }.orEmpty()
}

internal fun applyEditorDraft(pane: DemoPane, text: String, state: StateProps): Result<StateProps> = runCatching {
    when (pane) {
        DemoPane.Data -> if (text.isBlank()) {
            state.copy(data = JsonNull, dataProvided = false)
        } else {
            state.copy(data = prettyJson.parseToJsonElement(text), dataProvided = true)
        }
        DemoPane.Schema -> state.copy(
            schema = if (text.isBlank()) null else JsonSchema(parseJsonObject(text, "Schema")),
        )
        DemoPane.UiSchema -> state.copy(
            uischema = if (text.isBlank()) null else parseJsonObject(text, "UI Schema").toUiSchemaElement(),
        )
        DemoPane.UiSchemas -> {
            val values = parseJsonArray(text, "UI Schemas")
            require(values.size == state.uischemas.size) {
                "UI Schemas must contain ${state.uischemas.size} entries so their tester functions can be retained."
            }
            state.copy(uischemas = state.uischemas.zip(values).mapIndexed { index, (entry, value) ->
                entry.copy(uischema = (value as? JsonObject
                    ?: throw IllegalArgumentException("UI Schemas[$index] must be an object")).toUiSchemaElement())
            })
        }
        DemoPane.Internationalization -> {
            val values = parseJsonObject(text, "Internationalization").mapValues { (locale, entries) ->
                (entries as? JsonObject)?.mapValues { (key, value) ->
                    (value as? JsonPrimitive)?.takeIf { it.isString }?.content
                        ?: throw IllegalArgumentException("Internationalization.$locale.$key must be a string")
                } ?: throw IllegalArgumentException("Internationalization.$locale must be an object")
            }
            val english = values["en"].orEmpty()
            state.copy(
                translations = english,
                localizedTranslations = values,
                i18n = (state.i18n ?: JsonFormsI18nState()).copy(
                    translate = Translator { id, defaultMessage, _ -> english[id] ?: defaultMessage },
                ),
            )
        }
        DemoPane.Config -> state.copy(config = parseJsonObject(text, "Config").toConfigMap(state.config))
        else -> error("$pane is not a JSON editor")
    }
}

private fun parseJsonObject(text: String, label: String): JsonObject =
    prettyJson.parseToJsonElement(text.ifBlank { "{}" }) as? JsonObject
        ?: throw IllegalArgumentException("$label must be a JSON object.")

private fun parseJsonArray(text: String, label: String): JsonArray =
    prettyJson.parseToJsonElement(text.ifBlank { "[]" }) as? JsonArray
        ?: throw IllegalArgumentException("$label must be a JSON array.")

private fun Map<String, Any?>.toJsonObject() = JsonObject(mapValues { (_, value) -> value.toJsonElement() })

private fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is JsonElement -> this
    is UiSchemaElement -> toJsonObject()
    is Boolean -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is String -> JsonPrimitive(this)
    is Map<*, *> -> JsonObject(entries.associate { (key, value) ->
        (key as? String ?: throw IllegalArgumentException("Config object keys must be strings")) to value.toJsonElement()
    })
    is List<*> -> JsonArray(map { it.toJsonElement() })
    else -> throw IllegalArgumentException("Config value of type ${this::class.simpleName} cannot be edited as JSON")
}

private fun JsonObject.toConfigMap(original: Map<String, Any?>): Map<String, Any?> =
    mapValues { (key, value) ->
        if (original[key] is UiSchemaElement) {
            (value as? JsonObject)?.toUiSchemaElement()
                ?: throw IllegalArgumentException("Config.$key must be a UI schema object")
        } else {
            value.toKotlinValue()
        }
    }

private fun JsonObject.toKotlinMap(): Map<String, Any?> = mapValues { (_, value) -> value.toKotlinValue() }

private fun JsonElement.toKotlinValue(): Any? = when (this) {
    JsonNull -> null
    is JsonObject -> toKotlinMap()
    is JsonArray -> map { it.toKotlinValue() }
    is JsonPrimitive -> when {
        isString -> content
        content == "true" -> true
        content == "false" -> false
        else -> content.toLongOrNull() ?: content.toDoubleOrNull() ?: content
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
            SettingsSelect(
                label = "Locale",
                selected = settings.locale,
                values = DemoLocale.entries,
                displayName = DemoLocale::displayName,
                onSelected = { onChange(settings.copy(locale = it)) },
            )
            SettingsSelect(
                label = "Demo Layout",
                selected = settings.demoLayout,
                values = DemoLayout.entries,
                displayName = DemoLayout::displayName,
                onSelected = { onChange(settings.copy(demoLayout = it)) },
            )
            HorizontalDivider()
            Text("Appearance", style = MaterialTheme.typography.titleMedium)
            SettingsSelect(
                label = "Interface Style",
                selected = settings.uiStyle,
                values = DemoUiStyle.entries,
                displayName = DemoUiStyle::displayName,
                onSelected = { onChange(settings.copy(uiStyle = it)) },
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DemoThemeMode.entries.forEach { mode ->
                    DemoChoiceChip(
                        selected = settings.themeMode == mode,
                        onClick = { onChange(settings.copy(themeMode = mode)) },
                        modifier = Modifier.weight(1f),
                    ) { Text(mode.displayName) }
                }
            }
            if (settings.effectiveUiStyle() == DemoUiStyle.Material) {
                SettingsSelect(
                    label = "Light Material Theme",
                    selected = settings.lightMaterialTheme,
                    values = MaterialThemePreset.entries,
                    displayName = MaterialThemePreset::displayName,
                    onSelected = { onChange(settings.copy(lightMaterialTheme = it)) },
                )
                SettingsSelect(
                    label = "Dark Material Theme",
                    selected = settings.darkMaterialTheme,
                    values = MaterialThemePreset.entries,
                    displayName = MaterialThemePreset::displayName,
                    onSelected = { onChange(settings.copy(darkMaterialTheme = it)) },
                )
            } else {
                SettingsSelect(
                    label = "Light iOS Theme",
                    selected = settings.lightCupertinoTheme,
                    values = CupertinoThemePreset.entries,
                    displayName = CupertinoThemePreset::displayName,
                    onSelected = { onChange(settings.copy(lightCupertinoTheme = it)) },
                )
                SettingsSelect(
                    label = "Dark iOS Theme",
                    selected = settings.darkCupertinoTheme,
                    values = CupertinoThemePreset.entries,
                    displayName = CupertinoThemePreset::displayName,
                    onSelected = { onChange(settings.copy(darkCupertinoTheme = it)) },
                )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SettingsSelect(
    label: String,
    selected: T,
    values: List<T>,
    displayName: (T) -> String,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = displayName(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(
                type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                enabled = true,
            ).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            values.forEach { value ->
                DropdownMenuItem(
                    text = { Text(displayName(value)) },
                    onClick = {
                        onSelected(value)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun SettingSwitch(label: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        if (LocalDemoUiStyle.current == DemoUiStyle.Cupertino) {
            CupertinoSwitch(checked, onCheckedChange)
        } else {
            Switch(checked, onCheckedChange)
        }
        Column(Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Medium)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DemoChoiceChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (LocalDemoUiStyle.current == DemoUiStyle.Cupertino) {
        Surface(
            modifier = modifier.clickable(onClick = onClick),
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            shape = RoundedCornerShape(8.dp),
        ) {
            Box(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), contentAlignment = Alignment.Center) {
                content()
            }
        }
    } else {
        FilterChip(
            selected = selected,
            onClick = onClick,
            label = content,
            modifier = modifier,
        )
    }
}

private fun ValidationMode.displayName() = when (this) {
    ValidationMode.ValidateAndShow -> "Validate And Show"
    ValidationMode.ValidateAndHide -> "Validate And Hide"
    ValidationMode.NoValidation -> "No Validation"
}

internal fun exampleI18nForLocale(state: StateProps, languageTag: String): JsonFormsI18nState {
    val language = languageTag.substringBefore('-').lowercase()
    val localized = state.localizedTranslations[languageTag]
        ?: state.localizedTranslations[language]
    val translator = localized?.let { values ->
        Translator { id, defaultMessage, _ -> values[id] ?: defaultMessage }
    } ?: state.i18n?.translate
    return (state.i18n ?: JsonFormsI18nState()).copy(locale = languageTag, translate = translator)
}

private fun ExampleDescription.toStateProps() = StateProps(
    data = data,
    dataProvided = dataProvided,
    schema = schema,
    uischema = uischema,
    renderers = materialExtendedRenderers,
    cells = materialCells,
    config = config,
    uischemas = uischemas,
    additionalErrors = additionalErrors,
    actions = actions,
    middleware = middleware,
    i18n = i18n,
    readonly = readonly,
    translations = translations,
    localizedTranslations = localizedTranslations,
)

@Composable
private fun EmptyExamplesState() {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding(), contentAlignment = Alignment.Center) {
        Text("No examples registered", style = MaterialTheme.typography.titleLarge)
    }
}

private val MenuIcon = lineIcon("Menu") { moveTo(4f, 6f); horizontalLineTo(20f); moveTo(4f, 12f); horizontalLineTo(20f); moveTo(4f, 18f); horizontalLineTo(20f) }
private val KotlinLogo = ImageVector.Builder("Kotlin logo", 48.dp, 48.dp, 48f, 48f).apply {
    addPath(
        pathData = PathParser().parsePathString("M40 40H8V8H40L23.67 23.765L40 40Z").toNodes(),
        fill = Brush.radialGradient(
            0f to Color(0xFFE44857),
            0.504494f to Color(0xFFC711E1),
            1f to Color(0xFF7F52FF),
            center = Offset(40f, 8f),
            radius = 32f,
        ),
    )
}.build()
private val SearchIcon = lineIcon("Search") {
    moveTo(10.5f, 4f); curveTo(6.91f, 4f, 4f, 6.91f, 4f, 10.5f); curveTo(4f, 14.09f, 6.91f, 17f, 10.5f, 17f)
    curveTo(14.09f, 17f, 17f, 14.09f, 17f, 10.5f); curveTo(17f, 6.91f, 14.09f, 4f, 10.5f, 4f); moveTo(15.5f, 15.5f); lineTo(21f, 21f)
}
private val PanelsIcon = lineIcon("Form only") { moveTo(3f, 4f); horizontalLineTo(21f); verticalLineTo(20f); horizontalLineTo(3f); close(); moveTo(8f, 4f); verticalLineTo(20f); moveTo(8f, 10f); horizontalLineTo(21f) }
private val GitHubIcon = ImageVector.Builder("GitHub", 24.dp, 24.dp, 16f, 16f).apply {
    addPath(
        pathData = PathParser().parsePathString(
            "M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 " +
                "0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13" +
                "-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66" +
                ".07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15" +
                "-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82a7.65 7.65 0 0 1 2-.27" +
                "c.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12" +
                ".51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48" +
                " 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0 0 16 8" +
                "c0-4.42-3.58-8-8-8z",
        ).toNodes(),
        fill = SolidColor(Color.Black),
    )
}.build()
private val SettingsIcon = lineIcon("Settings") { moveTo(4f, 6f); horizontalLineTo(20f); moveTo(4f, 12f); horizontalLineTo(20f); moveTo(4f, 18f); horizontalLineTo(20f); moveTo(8f, 3f); verticalLineTo(9f); moveTo(16f, 9f); verticalLineTo(15f); moveTo(10f, 15f); verticalLineTo(21f) }
private val CloseIcon = lineIcon("Close") { moveTo(5f, 5f); lineTo(19f, 19f); moveTo(19f, 5f); lineTo(5f, 19f) }

private fun lineIcon(name: String, draw: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) = ImageVector.Builder(
    name, 24.dp, 24.dp, 24f, 24f,
).apply {
    path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathBuilder = draw)
}.build()
