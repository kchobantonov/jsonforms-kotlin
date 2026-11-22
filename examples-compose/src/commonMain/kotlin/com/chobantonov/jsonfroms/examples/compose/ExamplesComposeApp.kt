package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.cupertino.renderers.CupertinoSwitch
import com.chobantonov.jsonfroms.extended.renderers.cupertinoExtendedCells
import com.chobantonov.jsonfroms.extended.renderers.cupertinoExtendedRenderers
import com.chobantonov.jsonfroms.extended.renderers.materialExtendedCells
import com.chobantonov.jsonfroms.extended.renderers.materialExtendedRenderers
import com.chobantonov.jsonfroms.extended.renderers.ExtendedActionHandler
import com.chobantonov.jsonfroms.extended.renderers.ExtendedActionHandlerConfigKey
import com.chobantonov.jsonfroms.extended.renderers.ExtendedFilePickerEvents
import com.chobantonov.jsonfroms.extended.renderers.ExtendedFilePickerEventsConfigKey
import com.chobantonov.jsonfroms.cupertino.renderers.util.CupertinoAppTheme
import com.chobantonov.jsonfroms.cupertino.renderers.util.CupertinoThemePreset
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.models.toUiSchemaElement
import com.chobantonov.jsonfroms.core.reducers.defaultMiddleware
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.store.Translator
import com.chobantonov.jsonfroms.core.store.ValidationMode
import com.chobantonov.jsonfroms.core.store.retainHostErrorsForUnchangedValues
import com.chobantonov.jsonfroms.examples.ExampleDescription
import com.chobantonov.jsonfroms.examples.ExampleFeatureGroup
import com.chobantonov.jsonfroms.examples.ExampleSource
import com.chobantonov.jsonfroms.examples.StateProps
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import com.chobantonov.jsonfroms.material.renderers.util.MaterialThemePreset
import com.chobantonov.jsonfroms.material.renderers.util.materialColorScheme
import com.russhwolf.settings.Settings
import com.slapps.cupertino.CupertinoButton
import com.slapps.cupertino.CupertinoText
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

@OptIn(ExperimentalSerializationApi::class)
private val prettyJson = Json { prettyPrint = true; prettyPrintIndent = "  " }
private val CompactWidth = 840.dp
private const val GitHubUrl = "https://github.com/kchobantonov/jsonforms-kotlin"
@Composable
internal expect fun createDemoFilePicker(): ExtendedFilePickerEvents?
private val LocalDemoUiStyle = compositionLocalOf { DemoUiStyle.Material }
internal val LocalDemoLocale = compositionLocalOf { DemoLocale.English }

@Composable
private fun demoText(english: String, bulgarian: String): String =
    if (LocalDemoLocale.current == DemoLocale.Bulgarian) bulgarian else english

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

internal enum class DemoLocale(val displayName: String, val languageTag: String) {
    English("English (en)", "en"),
    Bulgarian("Bulgarian (bg)", "bg"),
}

internal enum class DemoDirection(val displayName: String, val layoutDirection: LayoutDirection) {
    Ltr("LTR", LayoutDirection.Ltr),
    Rtl("RTL", LayoutDirection.Rtl),
}

internal enum class DemoLayout(val displayName: String) {
    Default("Default"),
    DemoAndData("Demo and Data"),
}

enum class DemoPane(val displayName: String) {
    Demo("Demo"),
    Data("Data"),
    Schema("Schema"),
    UiSchema("UI Schema"),
    UiSchemas("UI Schemas"),
    Internationalization("Internationalization"),
    Config("Config"),
}

private data class EditorDraft(
    val text: String,
    val error: String? = null,
    val dirty: Boolean = false,
)

private data class EditorDrafts(val values: Map<DemoPane, EditorDraft>) {
    operator fun get(pane: DemoPane) = values.getValue(pane)

    fun update(pane: DemoPane, transform: (EditorDraft) -> EditorDraft) =
        copy(values = values + (pane to transform(get(pane))))

    fun refreshDataAfterFormEdit(next: StateProps) =
        update(DemoPane.Data) { draft ->
            if (!draft.dirty)
                EditorDraft(editorText(DemoPane.Data, next))
            else draft
        }

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
    val formLocaleOverride: DemoLocale? = null,
    val formDarkThemeOverride: Boolean? = null,
    val direction: DemoDirection = DemoDirection.Ltr,
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
    val enableFilterErrorsBeforeTouch: Boolean? = null,
    val filterErrorKeywordsBeforeTouch: List<String>? = null,
    val allowAdditionalPropertiesByDefault: Boolean? = null,
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
    ) + buildMap {
        enableFilterErrorsBeforeTouch?.let { put("enableFilterErrorsBeforeTouch", it) }
        filterErrorKeywordsBeforeTouch?.let { put("filterErrorKeywordsBeforeTouch", it) }
        allowAdditionalPropertiesByDefault?.let { put("allowAdditionalPropertiesIfMissing", it) }
    }
}

@Composable
fun ExamplesComposeApp(
    examples: List<ExampleDescription> = getBuiltInExamples(),
    selectedExampleName: String? = null,
    onExampleSelected: (String) -> Unit = {},
    showLandingWhenUnselected: Boolean = false,
    onHomeSelected: (() -> Unit)? = null,
    settingsStore: Settings? = null,
    browserViewState: DemoUrlViewState? = null,
    onBrowserViewStateChange: (DemoUrlViewState) -> Unit = {},
    browserRuntime: DemoBrowserRuntime? = null,
    onBrowserRuntimeSwitch: ((DemoBrowserRuntime, DemoRuntimeHandoff?) -> Unit)? = null,
    initialRuntimeHandoff: DemoRuntimeHandoff? = null,
    onRuntimeHandoffConsumed: () -> Unit = {},
) {
    val preferences = remember(settingsStore) { DemoPreferences(settingsStore ?: Settings()) }
    var settings by remember(preferences) { mutableStateOf(preferences.load()) }
    var sessionView by remember { mutableStateOf(DemoUrlViewState()) }
    val view = browserViewState ?: sessionView
    val updateView: (DemoUrlViewState) -> Unit = { next ->
        sessionView = next
        if (browserViewState != null) onBrowserViewStateChange(next)
    }
    val effectiveSettings = if (browserViewState == null) settings else settings.copy(readonly = view.readonly)
    val updateSettings: (DemoSettings) -> Unit = { next ->
        settings = next
        preferences.save(next)
        if (browserViewState != null && next.readonly != view.readonly) {
            updateView(view.copy(readonly = next.readonly))
        }
    }
    val useDarkTheme = when (settings.themeMode) {
        DemoThemeMode.System -> isSystemInDarkTheme()
        DemoThemeMode.Light -> false
        DemoThemeMode.Dark -> true
    }

    val effectiveUiStyle = settings.effectiveUiStyle()
    DemoAppTheme(
        effectiveUiStyle,
        settings.locale,
        settings.direction,
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
        var state by remember(selected.name) {
            mutableStateOf(if (initialRuntimeHandoff?.exampleName == selected.name &&
                initialRuntimeHandoff.target == browserRuntime)
                originalState.copy(data = initialRuntimeHandoff.data,
                    dataProvided = initialRuntimeHandoff.dataProvided)
            else originalState)
        }
        LaunchedEffect(initialRuntimeHandoff) {
            if (initialRuntimeHandoff != null) onRuntimeHandoffConsumed()
        }
        var analysisPending by remember(selected.name) { mutableStateOf(false) }
        var editorDrafts by remember(selected.name) { mutableStateOf(EditorDrafts.from(state)) }
        var settingsOpen by remember { mutableStateOf(false) }
        var runtimeSwitchError by remember { mutableStateOf(false) }
        val settingsButtonFocus = remember { FocusRequester() }
        val closeSettings: () -> Unit = {
            settingsOpen = false
            settingsButtonFocus.requestFocus()
        }
        LaunchedEffect(view.formOnly) { if (view.formOnly) settingsOpen = false }

        BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()) {
            val compact = maxWidth < CompactWidth
            val showBrandLogo = maxWidth >= 280.dp
            val showBrandTitle = maxWidth >= 440.dp
            val navigationVisible = view.drawer ?: !compact
            val selectExample: (ExampleDescription) -> Unit = {
                localSelectedName = it.name
                onExampleSelected(it.name)
                if (compact) updateView(view.copy(drawer = null))
            }
            val updateState: (StateProps) -> Unit = { nextState ->
                if (nextState.data != state.data || nextState.dataProvided != state.dataProvided) {
                    editorDrafts = editorDrafts.refreshDataAfterFormEdit(nextState)
                }
                state = nextState
            }
            val switchRuntime: (DemoBrowserRuntime, Boolean) -> Unit = { target, openDemo ->
                if (target != browserRuntime && onBrowserRuntimeSwitch != null) {
                    val handoff = if (!showLanding || openDemo)
                        DemoRuntimeHandoff(target, selected.name, state.data, state.dataProvided)
                    else null
                    runCatching { onBrowserRuntimeSwitch(target, handoff) }
                        .onFailure { runtimeSwitchError = true }
                } else if (openDemo) {
                    updateView(view.copy(formOnly = false))
                    selectExample(examples.first())
                }
            }
            Column(Modifier.fillMaxSize()) {
                if (!view.formOnly) DemoTopBar(
                    navigationVisible, view.formOnly && !showLanding, settingsOpen,
                    { updateView(view.copy(drawer = (!navigationVisible).takeUnless { it == !compact })) },
                    {
                        if (showLanding) selectExample(examples.first())
                        updateView(view.copy(formOnly = !view.formOnly))
                        settingsOpen = false
                    },
                    { settingsOpen = !settingsOpen },
                    onHomeSelected?.let { home -> { updateView(view.copy(formOnly = false)); home() } },
                    view.webComponent,
                    { updateView(view.copy(webComponent = !view.webComponent)) },
                    showBrandLogo,
                    showBrandTitle,
                    settingsButtonFocus,
                )
                if (compact) {
                    CompactLayout(
                        examples, selected, originalState, state, navigationVisible && !view.formOnly,
                        view.formOnly, effectiveSettings, view.activePane, { updateView(view.copy(activePane = it)) },
                        editorDrafts, { editorDrafts = it }, selectExample, updateState, updateSettings,
                        showLanding, { updateView(view.copy(formOnly = false)); selectExample(examples.first()) },
                        onHomeSelected?.takeIf { !showBrandLogo },
                        analysisPending, { analysisPending = it },
                        browserRuntime, { target -> switchRuntime(target, true) },
                    )
                } else {
                    ExpandedLayout(
                        examples, selected, originalState, state, navigationVisible && !view.formOnly,
                        view.formOnly, effectiveSettings, view.activePane, { updateView(view.copy(activePane = it)) },
                        editorDrafts, { editorDrafts = it }, selectExample, updateState, updateSettings,
                        showLanding, { updateView(view.copy(formOnly = false)); selectExample(examples.first()) },
                        analysisPending, { analysisPending = it },
                        browserRuntime, { target -> switchRuntime(target, true) },
                    )
                }
            }
            if (view.formOnly && !showLanding) {
                Box(Modifier.zIndex(1f).align(Alignment.TopEnd).padding(8.dp)) {
                    ToolbarButton(PanelsIcon, demoText("Show full UI", "Покажи целия интерфейс")) {
                        updateView(view.copy(formOnly = false))
                    }
                }
            }
            if (settingsOpen && !view.formOnly) {
                Box(
                    Modifier.fillMaxSize().zIndex(2f).background(Color.Black.copy(alpha = .35f))
                        .testTag("settings-backdrop").clickable { closeSettings() },
                )
                SettingsPanel(
                    effectiveSettings, state.config, {
                        updateSettings(it)
                    }, closeSettings, browserRuntime,
                    { target -> switchRuntime(target, false) },
                    Modifier.zIndex(3f).align(Alignment.TopEnd)
                        .padding(top = 68.dp, end = 12.dp, bottom = 12.dp)
                        .width(if (compact) maxWidth - 24.dp else 390.dp).fillMaxHeight(),
                )
            }
            if (runtimeSwitchError) AlertDialog(
                onDismissRequest = { runtimeSwitchError = false },
                title = { Text(demoText("Unable to switch runtime", "Неуспешна смяна на средата")) },
                text = { Text(demoText(
                    "The browser could not preserve the current form data. Free browser storage and try again.",
                    "Браузърът не можа да запази данните на формуляра. Освободете място и опитайте отново.",
                )) },
                confirmButton = { TextButton(onClick = { runtimeSwitchError = false }) {
                    Text(demoText("OK", "Добре"))
                } },
            )
            if (view.webComponent && supportsWebComponentView) {
                WebComponentOverlay(
                    input = webComponentInput(state, settings,
                        readonly = state.readonly || effectiveSettings.readonly,
                        formOnly = view.formOnly, useDarkTheme = useDarkTheme),
                    onClose = { updateView(view.copy(webComponent = false)) },
                    onExitFormOnly = { updateView(view.copy(formOnly = false)) },
                    onDataChange = { value, provided ->
                        runCatching { Json.parseToJsonElement(value) }.getOrNull()?.let { data ->
                            updateState(state.copy(data = data, dataProvided = provided,
                                additionalErrors = retainHostErrorsForUnchangedValues(
                                    state.data, data, state.additionalErrors)))
                        }
                    },
                    onFormAction = { action, paramsText, currentFormDarkTheme ->
                        val params = runCatching { Json.parseToJsonElement(paramsText) as? JsonObject }
                            .getOrNull() ?: JsonObject(emptyMap())
                        val effect = demoFormActionEffect(state, settings, action, params, currentFormDarkTheme)
                        effect.settings?.let(updateSettings)
                        effect.state?.let(updateState)
                    },
                )
            }
        }
    }
}

@Composable
internal fun DemoAppTheme(
    style: DemoUiStyle,
    locale: DemoLocale,
    direction: DemoDirection,
    darkTheme: Boolean,
    materialTheme: MaterialThemePreset,
    cupertinoTheme: CupertinoThemePreset,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalDemoUiStyle provides style, LocalDemoLocale provides locale,
        LocalLayoutDirection provides direction.layoutDirection) {
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
    webComponentMode: Boolean,
    onWebComponentToggle: () -> Unit,
    showBrandLogo: Boolean,
    showBrandTitle: Boolean,
    settingsButtonFocus: FocusRequester,
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
            ToolbarButton(MenuIcon, if (navigationVisible)
                demoText("Hide example menu", "Скрий менюто с примери")
                else demoText("Show example menu", "Покажи менюто с примери"), navigationVisible,
                onClick = onNavigation)
            if (showBrandLogo) Icon(
                KotlinLogo,
                contentDescription = if (onHome != null)
                    demoText("Go to welcome page", "Към началната страница")
                    else demoText("Kotlin logo", "Лого на Kotlin"),
                tint = Color.Unspecified,
                modifier = Modifier.size(40.dp).clickable(enabled = onHome != null) { onHome?.invoke() }.padding(6.dp),
            )
            if (showBrandTitle) Text("JSON Forms Kotlin", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).clickable(enabled = onHome != null) { onHome?.invoke() })
            else Spacer(Modifier.weight(1f))
            ToolbarButton(PanelsIcon, if (formOnly)
                demoText("Show full UI", "Покажи целия интерфейс")
                else demoText("Show form only", "Покажи само формуляра"), formOnly,
                onClick = onFormOnly)
            if (supportsWebComponentView) {
                ToolbarButton(WebComponentIcon, if (webComponentMode)
                    demoText("Show Compose form", "Покажи Compose формуляр")
                    else demoText("Show web component", "Покажи уеб компонента"),
                    webComponentMode, onClick = onWebComponentToggle)
            }
            ToolbarButton(GitHubIcon, demoText("Open GitHub", "Отвори GitHub")) { uriHandler.openUri(GitHubUrl) }
            ToolbarButton(SettingsIcon, if (settingsOpen)
                demoText("Close settings", "Затвори настройките")
                else demoText("Open settings", "Отвори настройките"), settingsOpen,
                Modifier.focusRequester(settingsButtonFocus), onSettings)
        }
    }
}

@Composable
private fun ToolbarButton(icon: ImageVector, description: String, selected: Boolean = false,
    modifier: Modifier = Modifier, onClick: () -> Unit) {
    ActionTooltip(description) {
        Surface(
            color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
            shape = RoundedCornerShape(10.dp),
        ) {
            IconButton(onClick = onClick, modifier = modifier) { Icon(icon, contentDescription = description) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionTooltip(label: String, content: @Composable () -> Unit) {
    val tooltip = rememberTooltipState()
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(focused, tooltip) {
        if (focused) tooltip.show() else tooltip.dismiss()
    }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(label) } },
        state = tooltip,
        content = {
            Box(Modifier.onFocusChanged { focused = it.hasFocus }) { content() }
        },
    )
}

@Composable
private fun CompactLayout(
    examples: List<ExampleDescription>, selected: ExampleDescription, originalState: StateProps, state: StateProps,
    navigationVisible: Boolean, formOnly: Boolean, settings: DemoSettings,
    selectedPane: DemoPane, onPaneSelected: (DemoPane) -> Unit,
    editorDrafts: EditorDrafts, onEditorDraftsChange: (EditorDrafts) -> Unit,
    onSelected: (ExampleDescription) -> Unit, onStateChange: (StateProps) -> Unit,
    onSettingsChange: (DemoSettings) -> Unit,
    showLanding: Boolean, onOpenDemo: () -> Unit, onCompactHome: (() -> Unit)?,
    analysisPending: Boolean, onAnalysisPendingChange: (Boolean) -> Unit,
    browserRuntime: DemoBrowserRuntime?, onRuntimeSwitch: (DemoBrowserRuntime) -> Unit,
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
            onHome = onCompactHome,
        )
    } else if (showLanding) {
        DemoLandingPage(onOpenDemo, browserRuntime, onRuntimeSwitch, Modifier.fillMaxSize())
    } else {
        val validationIssues = remember(state.data, state.dataProvided, state.schema, state.uischema,
            state.additionalErrors, state.middleware, settings.validationMode) {
            demoValidationIssues(state, settings.validationMode)
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!formOnly) ExampleHeader(selected)
            if (!formOnly) {
                DemoPaneSelector(selectedPane, validationIssues.size,
                    settings.demoLayout == DemoLayout.DemoAndData, validationIssues,
                    analysisPending, onPaneSelected)
            }
            when {
                formOnly -> FormCard(state, true, settings, onStateChange, onSettingsChange,
                    onPendingChange = onAnalysisPendingChange)
                selectedPane.isEditor -> EditorCard(
                    selectedPane, originalState, state, editorDrafts, onEditorDraftsChange, onStateChange, settings.locale,
                    Modifier.fillMaxWidth().heightIn(min = 520.dp),
                )
                settings.demoLayout == DemoLayout.DemoAndData -> {
                    FormCard(state, true, settings, onStateChange, onSettingsChange,
                        onPendingChange = onAnalysisPendingChange)
                    EditorCard(
                        DemoPane.Data, originalState, state, editorDrafts,
                        onEditorDraftsChange, onStateChange, settings.locale,
                        Modifier.fillMaxWidth().heightIn(min = 520.dp),
                    )
                }
                selectedPane == DemoPane.Demo -> FormCard(state, true, settings, onStateChange,
                    onSettingsChange, onPendingChange = onAnalysisPendingChange)
            }
        }
    }
}

@Composable
private fun ExpandedLayout(
    examples: List<ExampleDescription>, selected: ExampleDescription, originalState: StateProps, state: StateProps,
    navigationVisible: Boolean, formOnly: Boolean, settings: DemoSettings,
    selectedPane: DemoPane, onPaneSelected: (DemoPane) -> Unit,
    editorDrafts: EditorDrafts, onEditorDraftsChange: (EditorDrafts) -> Unit,
    onSelected: (ExampleDescription) -> Unit, onStateChange: (StateProps) -> Unit,
    onSettingsChange: (DemoSettings) -> Unit,
    showLanding: Boolean, onOpenDemo: () -> Unit,
    analysisPending: Boolean, onAnalysisPendingChange: (Boolean) -> Unit,
    browserRuntime: DemoBrowserRuntime?, onRuntimeSwitch: (DemoBrowserRuntime) -> Unit,
) {
    var splitFraction by remember(selected.name) { mutableFloatStateOf(0.75f) }
    val validationIssues = remember(state.data, state.dataProvided, state.schema, state.uischema,
        state.additionalErrors, state.middleware, settings.validationMode) {
        demoValidationIssues(state, settings.validationMode)
    }
    Row(
        Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (navigationVisible) ExampleNavigation(
            examples, selected.takeUnless { showLanding }, onSelected, Modifier.width(260.dp).fillMaxHeight(),
        )
        if (showLanding) {
            DemoLandingPage(onOpenDemo, browserRuntime, onRuntimeSwitch, Modifier.fillMaxSize())
        } else Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (!formOnly) ExampleHeader(selected)
            if (!formOnly) {
                DemoPaneSelector(selectedPane, validationIssues.size,
                    settings.demoLayout == DemoLayout.DemoAndData, validationIssues,
                    analysisPending, onPaneSelected)
            }
            if (!formOnly && selectedPane.isEditor) {
                EditorCard(
                    selectedPane, originalState, state, editorDrafts, onEditorDraftsChange, onStateChange, settings.locale,
                    Modifier.weight(1f).fillMaxWidth(),
                )
            } else if (formOnly) {
                FormCard(
                    state, false, settings, onStateChange, onSettingsChange,
                    Modifier.weight(1f).fillMaxWidth(), true, onAnalysisPendingChange,
                )
            } else if (settings.demoLayout == DemoLayout.DemoAndData) {
                DemoSplitPane(
                    fraction = splitFraction,
                    onFractionChange = { splitFraction = it },
                    resizeLabel = if (settings.locale == DemoLocale.Bulgarian)
                        "Преоразмеряване на формуляра и данните" else "Resize form and data panes",
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    form = { pane -> FormCard(state, false, settings, onStateChange, onSettingsChange,
                        pane, true, onAnalysisPendingChange) },
                    data = { pane -> EditorCard(DemoPane.Data, originalState, state, editorDrafts,
                        onEditorDraftsChange, onStateChange, settings.locale, pane) },
                )
            } else if (selectedPane == DemoPane.Demo) {
                FormCard(
                    state, false, settings, onStateChange, onSettingsChange,
                    Modifier.weight(1f).fillMaxWidth(), true, onAnalysisPendingChange,
                )
            }
        }
    }
}

@Composable
internal fun DemoPaneSelector(
    selected: DemoPane,
    errorCount: Int,
    demoAndData: Boolean,
    validationIssues: List<DemoValidationIssue> = emptyList(),
    analysisPending: Boolean = false,
    onSelected: (DemoPane) -> Unit,
) {
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
                text = {
                    val label = demoPaneLabel(pane, demoAndData, LocalDemoLocale.current)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (pane == DemoPane.Demo && errorCount > 0) "$label ($errorCount)" else label)
                        if (pane == DemoPane.Demo && analysisPending) {
                            Text(demoText(" · Analysis pending", " · Анализът продължава"),
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (pane == DemoPane.Demo && validationIssues.isNotEmpty()) {
                            DemoValidationDetails(validationIssues)
                        }
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DemoValidationDetails(issues: List<DemoValidationIssue>) {
    val bulgarian = LocalDemoLocale.current == DemoLocale.Bulgarian
    val description = if (bulgarian) {
        "${issues.size} ${if (issues.size == 1) "грешка" else "грешки"}. Покажи подробности"
    } else {
        "${issues.size} validation ${if (issues.size == 1) "error" else "errors"}. Show details"
    }
    var expanded by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val tooltip = rememberTooltipState()
    LaunchedEffect(focused, tooltip) {
        if (focused) tooltip.show() else tooltip.dismiss()
    }
    Box {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = {
                PlainTooltip(Modifier.widthIn(max = 420.dp)) {
                    Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                        issues.forEach { Text(demoValidationIssueLabel(it, bulgarian)) }
                    }
                }
            },
            state = tooltip,
        ) {
            IconButton(onClick = { expanded = true },
                modifier = Modifier.size(32.dp).onFocusChanged { focused = it.isFocused }) {
                Icon(ValidationIssueIcon, contentDescription = description,
                    modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(max = 480.dp).heightIn(max = 400.dp)) {
            issues.forEach { issue ->
                Text(demoValidationIssueLabel(issue, bulgarian), Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        }
    }
}

internal fun demoValidationIssueLabel(issue: DemoValidationIssue, bulgarian: Boolean): String {
    val source = when (issue.source) {
        DemoErrorSource.Schema -> if (bulgarian) "Схема" else "Schema"
        DemoErrorSource.Host -> if (bulgarian) "Хост" else "Host"
    }
    val path = issue.path.ifEmpty { "/" }
    return "$source $path: ${issue.message}"
}

internal fun demoPaneLabel(pane: DemoPane, demoAndData: Boolean, locale: DemoLocale): String {
    if (locale == DemoLocale.English) {
        return if (pane == DemoPane.Demo && demoAndData) "Demo and Data" else pane.displayName
    }
    return when (pane) {
        DemoPane.Demo -> if (demoAndData) "Демо и данни" else "Демо"
        DemoPane.Data -> "Данни"
        DemoPane.Schema -> "Схема"
        DemoPane.UiSchema -> "UI схема"
        DemoPane.UiSchemas -> "UI схеми"
        DemoPane.Internationalization -> "Преводи"
        DemoPane.Config -> "Конфигурация"
    }
}

@Composable
private fun ExampleNavigation(
    examples: List<ExampleDescription>, selected: ExampleDescription?,
    onSelected: (ExampleDescription) -> Unit, modifier: Modifier,
    onHome: (() -> Unit)? = null,
) {
    var query by remember { mutableStateOf("") }
    var source by remember { mutableStateOf<ExampleSource?>(null) }
    var sourceMenuOpen by remember { mutableStateOf(false) }
    LaunchedEffect(selected?.source) { source = selected?.source }
    val filtered = remember(examples, query, source) { filterExamples(examples, query, source) }
    Surface(modifier, tonalElevation = 1.dp, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (onHome != null) OutlinedButton(onClick = onHome) {
                Text(demoText("Home", "Начало"))
            }
            Text(demoText("Example source", "Източник на пример"), style = MaterialTheme.typography.labelMedium)
            Box {
                OutlinedButton(onClick = { sourceMenuOpen = true }) {
                    Text(source?.let { demoExampleSourceLabel(it, LocalDemoLocale.current) }
                        ?: demoText("All examples", "Всички примери"))
                }
                DropdownMenu(expanded = sourceMenuOpen, onDismissRequest = { sourceMenuOpen = false }) {
                    listOf(ExampleSource.Spec, ExampleSource.JsonForms, null).forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option?.let { demoExampleSourceLabel(it, LocalDemoLocale.current) }
                                ?: demoText("All examples", "Всички примери")) },
                            onClick = { source = option; sourceMenuOpen = false },
                        )
                    }
                }
            }
            OutlinedTextField(
                query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text(demoText("Search examples...", "Търсене на примери...")) },
                leadingIcon = { Icon(SearchIcon, contentDescription = null) },
            )
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (filtered.isEmpty()) Text(demoText("No examples found.", "Няма намерени примери."), modifier = Modifier.padding(8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                val groups = filtered.groupBy(ExampleDescription::featureGroup)
                ExampleFeatureGroup.entries.forEach { group ->
                    val entries = groups[group].orEmpty()
                    if (entries.isNotEmpty()) {
                        Text(exampleFeatureGroupLabel(group, LocalDemoLocale.current),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp, start = 4.dp))
                        entries.forEach { example ->
                            val isSelected = example.name == selected?.name
                            var focused by remember(example.name) { mutableStateOf(false) }
                            Surface(
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                                border = if (focused) BorderStroke(3.dp, MaterialTheme.colorScheme.tertiary) else null,
                                modifier = Modifier.fillMaxWidth()
                                    .onFocusChanged { focused = it.isFocused }
                                    .semantics { this.selected = isSelected }
                                    .clickable(role = Role.Button) { onSelected(example) },
                            ) { Text(example.label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) }
                        }
                    }
                }
            }
        }
    }
}

internal fun demoExampleSourceLabel(source: ExampleSource, locale: DemoLocale): String =
    if (locale == DemoLocale.Bulgarian) when (source) {
        ExampleSource.Spec -> "Примери от спецификацията"
        ExampleSource.JsonForms -> "Примери от JSON Forms"
    } else source.displayName

internal fun exampleFeatureGroupLabel(group: ExampleFeatureGroup, locale: DemoLocale): String =
    if (locale == DemoLocale.Bulgarian) when (group) {
        ExampleFeatureGroup.Controls -> "Полета"
        ExampleFeatureGroup.Collections -> "Колекции и таблици"
        ExampleFeatureGroup.Layout -> "Оформление и навигация"
        ExampleFeatureGroup.Composition -> "Композиция и правила"
        ExampleFeatureGroup.Validation -> "Валидация"
        ExampleFeatureGroup.Internationalization -> "Преводи"
        ExampleFeatureGroup.Advanced -> "Разширени възможности"
        ExampleFeatureGroup.Other -> "Други примери"
    } else when (group) {
        ExampleFeatureGroup.Controls -> "Controls"
        ExampleFeatureGroup.Collections -> "Collections and tables"
        ExampleFeatureGroup.Layout -> "Layout and navigation"
        ExampleFeatureGroup.Composition -> "Composition and rules"
        ExampleFeatureGroup.Validation -> "Validation"
        ExampleFeatureGroup.Internationalization -> "Internationalization"
        ExampleFeatureGroup.Advanced -> "Advanced features"
        ExampleFeatureGroup.Other -> "Other examples"
    }

internal fun filterExamples(
    examples: List<ExampleDescription>, query: String, source: ExampleSource? = null,
): List<ExampleDescription> {
    val value = query.trim().lowercase()
    return examples.filter { example ->
        (source == null || example.source == source) &&
            (value.isEmpty() || example.name.lowercase().contains(value) || example.label.lowercase().contains(value))
    }
}

@Composable
internal fun ExampleHeader(example: ExampleDescription) {
    val uriHandler = LocalUriHandler.current
    val sourceLabel = when {
        example.sourceUrl == null -> demoText("Host example", "Пример от приложението")
        example.source == ExampleSource.Spec -> demoText("Specification fixture", "Пример от спецификацията")
        else -> demoText("JSON Forms original", "Оригинал от JSON Forms")
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp)) {
        Text(
            text = example.label,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(example.name, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(sourceLabel, color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall)
            example.sourceUrl?.let { url ->
                val linkLabel = demoText("View source", "Виж източника")
                if (LocalDemoUiStyle.current == DemoUiStyle.Cupertino) {
                    CupertinoButton(onClick = { uriHandler.openUri(url) },
                        modifier = Modifier.semantics { contentDescription = linkLabel }) {
                        CupertinoText(linkLabel)
                    }
                } else {
                    TextButton(onClick = { uriHandler.openUri(url) }) { Text(linkLabel) }
                }
            }
        }
        val locale = LocalDemoLocale.current
        example.capabilityNotice[locale.languageTag]?.let { notice ->
            Surface(color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text("${demoText("Capability notice", "Бележка за възможностите")}: $notice",
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(10.dp))
            }
        }
    }
}

@Composable
private fun DemoLandingPage(
    onOpenDemo: () -> Unit,
    browserRuntime: DemoBrowserRuntime?,
    onRuntimeSwitch: (DemoBrowserRuntime) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val compact = maxWidth < 600.dp
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 20.dp),
        ) {
            Icon(
                KotlinLogo,
                contentDescription = demoText("Kotlin logo", "Лого на Kotlin"),
                modifier = Modifier.size(if (compact) 88.dp else 120.dp),
                tint = Color.Unspecified,
            )
            Text(
                demoText("JSON FORMS RENDERER SET", "РЕНДЕРЕРИ ЗА JSON FORMS"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                demoText("Welcome to JSON Forms Kotlin", "Добре дошли в JSON Forms Kotlin"),
                style = if (compact) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 800.dp),
            )
            Text(
                demoText("Compose Multiplatform renderers for JSON Forms.",
                    "Compose Multiplatform рендерери за JSON Forms."),
                style = if (compact) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (browserRuntime == null) {
                Button(onClick = onOpenDemo) { Text(demoText("Open Demo", "Отвори демото")) }
            } else {
                Text(demoText("Choose a web runtime", "Изберете уеб среда"),
                    style = MaterialTheme.typography.titleMedium)
                val runtimeButtons: @Composable () -> Unit = {
                    DemoBrowserRuntime.entries.forEach { runtime ->
                        val action = { if (runtime == browserRuntime) onOpenDemo() else onRuntimeSwitch(runtime) }
                        if (runtime == browserRuntime) Button(onClick = action) {
                            Text(runtime.label)
                        } else OutlinedButton(onClick = action) {
                            Text(runtime.label)
                        }
                    }
                }
                if (compact) Column(verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) { runtimeButtons() }
                else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { runtimeButtons() }
            }
        }
    }
}

@Composable
internal fun FormCard(
    state: StateProps, compact: Boolean, settings: DemoSettings,
    onStateChange: (StateProps) -> Unit, onSettingsChange: (DemoSettings) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    scrollContent: Boolean = false,
    onPendingChange: (Boolean) -> Unit = {},
) {
    val currentOnPendingChange by rememberUpdatedState(onPendingChange)
    DisposableEffect(Unit) { onDispose { currentOnPendingChange(false) } }
    val cupertino = settings.effectiveUiStyle() == DemoUiStyle.Cupertino
    val formLocale = settings.formLocaleOverride ?: settings.locale
    val appDarkTheme = when (settings.themeMode) {
        DemoThemeMode.System -> isSystemInDarkTheme()
        DemoThemeMode.Light -> false
        DemoThemeMode.Dark -> true
    }
    val formDarkTheme = settings.formDarkThemeOverride ?: appDarkTheme
    val picker = createDemoFilePicker()
    val actionHandler = ExtendedActionHandler { action, params ->
        val effect = demoFormActionEffect(state, settings, action, params, formDarkTheme)
        effect.settings?.let(onSettingsChange)
        effect.state?.let(onStateChange)
    }
    val extendedConfig = settings.applyTo(state.config) + mapOf(
        ExtendedActionHandlerConfigKey to actionHandler,
        ExtendedFilePickerEventsConfigKey to picker,
    )
    val formContent: @Composable () -> Unit = { Card(modifier) {
        val configuredI18n = exampleI18nForLocale(
            state,
            formLocale.languageTag,
        )
        val contentModifier = if (scrollContent) Modifier.fillMaxSize().verticalScroll(rememberScrollState()) else Modifier.fillMaxWidth()
        Column(contentModifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (cupertino) CupertinoText("JSON Forms")
                else Text("JSON Forms", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold)
                if (state.actions.isNotEmpty()) Row(
                    Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    state.actions.forEach { action ->
                        if (cupertino) CupertinoButton(onClick = { onStateChange(action.apply(state)) }) {
                            CupertinoText(action.label)
                        } else FilledTonalButton(onClick = { onStateChange(action.apply(state)) }) {
                            Text(action.label)
                        }
                    }
                }
            }
            JsonForms(
                data = state.data, dataProvided = state.dataProvided, schema = state.schema,
                uischema = state.uischema, uischemas = state.uischemas,
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                cells = if (cupertino) cupertinoExtendedCells else materialExtendedCells,
                config = extendedConfig, readonly = state.readonly || settings.readonly,
                validationMode = settings.validationMode, i18n = configuredI18n,
                additionalErrors = state.additionalErrors, middleware = state.middleware ?: defaultMiddleware,
                onValidityChange = { currentOnPendingChange(it.pending) },
                onChange = { change -> onStateChange(state.copy(
                    data = change.data,
                    dataProvided = change.dataProvided,
                    additionalErrors = retainHostErrorsForUnchangedValues(state.data, change.data, state.additionalErrors),
                )) },
            )
        }
    } }
    if (settings.formLocaleOverride != null || settings.formDarkThemeOverride != null) {
        DemoAppTheme(settings.effectiveUiStyle(), formLocale, settings.direction, formDarkTheme,
            settings.materialTheme(formDarkTheme), settings.cupertinoTheme(formDarkTheme), formContent)
    } else formContent()
}

@Composable
private fun EditorCard(
    pane: DemoPane,
    originalState: StateProps,
    state: StateProps,
    drafts: EditorDrafts,
    onDraftsChange: (EditorDrafts) -> Unit,
    onStateChange: (StateProps) -> Unit,
    locale: DemoLocale,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    val draft = drafts[pane]
    val cupertino = LocalDemoUiStyle.current == DemoUiStyle.Cupertino
    Card(modifier) {
        Column(
            Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(demoPaneLabel(pane, false, locale), style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold)
                    Text(
                        demoText("Changes affect the active form only after Apply succeeds.",
                            "Промените засягат активния формуляр само след успешно прилагане."),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                EditorActionIconButton(ReloadIcon, editorActionLabel(locale, pane, reload = true), cupertino) {
                    val originalText = editorText(pane, originalState)
                    onDraftsChange(drafts.update(pane) {
                        EditorDraft(originalText, dirty = originalText != editorText(pane, state))
                    })
                }
                EditorActionIconButton(ApplyIcon, editorActionLabel(locale, pane, reload = false), cupertino) {
                    val result = applyEditorDraft(pane, draft.text, state)
                    result.fold(
                        onSuccess = { nextState ->
                            onStateChange(nextState)
                            onDraftsChange(drafts.update(pane) { it.copy(error = null, dirty = false) })
                        },
                        onFailure = { failure ->
                            val detail = failure.message?.takeIf(String::isNotBlank)
                            val message = if (locale == DemoLocale.Bulgarian) {
                                "JSON не може да се приложи." + (detail?.let { " $it" } ?: "")
                            } else {
                                detail ?: "The JSON could not be applied."
                            }
                            onDraftsChange(drafts.update(pane) { it.copy(error = message) })
                        },
                    )
                }
            }
            OutlinedTextField(
                value = draft.text,
                onValueChange = { value ->
                    onDraftsChange(drafts.update(pane) {
                        it.copy(text = value, error = null,
                            dirty = value != editorText(pane, state))
                    })
                },
                modifier = Modifier.fillMaxWidth().weight(1f).semantics {
                    draft.error?.let { error(it) }
                },
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                isError = draft.error != null,
                supportingText = draft.error?.let { error -> { Text(error) } },
                label = { Text("JSON") },
            )
            if (pane == DemoPane.UiSchemas) {
                Text(
                    demoText("Registry tester functions are retained by position; the number of entries cannot be changed here.",
                        "Функциите за избор в регистъра се запазват по позиция; броят на записите не може да се променя тук."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

internal fun editorActionLabel(locale: DemoLocale, pane: DemoPane, reload: Boolean): String {
    if (locale == DemoLocale.English) {
        return "${if (reload) "Reload" else "Apply"} ${pane.displayName}"
    }
    val paneName = when (pane) {
        DemoPane.Demo -> "Демо"
        DemoPane.Data -> "данни"
        DemoPane.Schema -> "схема"
        DemoPane.UiSchema -> "UI схема"
        DemoPane.UiSchemas -> "UI схеми"
        DemoPane.Internationalization -> "преводи"
        DemoPane.Config -> "конфигурация"
    }
    return "${if (reload) "Презареди" else "Приложи"} $paneName"
}

@Composable
private fun EditorActionIconButton(
    icon: ImageVector,
    label: String,
    cupertino: Boolean,
    onClick: () -> Unit,
) {
    ActionTooltip(label) {
        if (cupertino) CupertinoButton(onClick = onClick,
            modifier = Modifier.semantics { contentDescription = label }) {
            Icon(icon, contentDescription = null)
        } else IconButton(onClick = onClick,
            modifier = Modifier.semantics { contentDescription = label }) {
            Icon(icon, contentDescription = null)
        }
    }
}

internal fun webComponentInput(
    state: StateProps,
    settings: DemoSettings,
    readonly: Boolean,
    formOnly: Boolean,
    useDarkTheme: Boolean,
): WebComponentInput = WebComponentInput(
    data = state.data.toString(),
    dataProvided = state.dataProvided,
    schema = state.schema?.toJsonObject()?.toString(),
    uischema = state.uischema?.toJsonObject()?.toString(),
    uischemas = JsonArray(state.uischemas.filter { it.uischema.name != null }
        .map { entry -> buildJsonObject { put("uischema", entry.uischema.toJsonObject()) } }).toString(),
    config = settings.applyTo(state.config).toJsonObject().toString(),
    translations = JsonObject(state.localizedTranslations.ifEmpty {
        if (state.translations.isEmpty()) emptyMap() else mapOf("en" to state.translations)
    }.mapValues { (_, values) ->
        JsonObject(values.mapValues { (_, value) -> JsonPrimitive(value) })
    }).toString(),
    additionalErrors = JsonArray(state.additionalErrors.map { error -> buildJsonObject {
        put("instancePath", error.instancePath)
        error.dataPath?.let { put("dataPath", it) }
        error.schemaPath?.let { put("schemaPath", it) }
        error.keyword?.let { put("keyword", it) }
        put("message", error.message)
        put("params", JsonObject(error.params.mapValues { (_, value) -> JsonPrimitive(value) }))
        error.absoluteKeywordLocation?.let { put("absoluteKeywordLocation", it) }
    } }).toString(),
    readonly = readonly,
    validationMode = settings.validationMode,
    direction = settings.direction,
    formOnly = formOnly,
    locale = settings.locale,
    formLocale = settings.formLocaleOverride ?: settings.locale,
    darkTheme = settings.formDarkThemeOverride ?: useDarkTheme,
    unavailableRegistryTesters = state.uischemas.size,
)

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
    settings: DemoSettings, exampleConfig: Map<String, Any?>,
    onChange: (DemoSettings) -> Unit, onClose: () -> Unit,
    browserRuntime: DemoBrowserRuntime?, onRuntimeSwitch: (DemoBrowserRuntime) -> Unit,
    modifier: Modifier,
) {
    val closeButtonFocus = remember { FocusRequester() }
    val lastSwitchFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { closeButtonFocus.requestFocus() }
    var keywordDraft by remember(exampleConfig) {
        mutableStateOf((settings.filterErrorKeywordsBeforeTouch
            ?: configuredErrorKeywords(exampleConfig)).joinToString(", "))
    }
    Card(modifier.onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
            onClose()
            true
        } else false
    }.clickable { }) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(demoText("Settings", "Настройки"), style = MaterialTheme.typography.headlineSmall)
                    Text(demoText("Configure the renderer demo and JSON Forms behavior.",
                        "Настройте демото на рендерера и поведението на JSON Forms."),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                val closeLabel = demoText("Close settings", "Затвори настройките")
                ActionTooltip(closeLabel) {
                    IconButton(onClose, modifier = Modifier.focusRequester(closeButtonFocus)
                        .focusProperties { previous = lastSwitchFocus }) {
                        Icon(CloseIcon, contentDescription = closeLabel)
                    }
                }
            }
            if (browserRuntime != null) SettingsSelect(
                label = demoText("Web runtime", "Уеб среда"),
                selected = browserRuntime,
                values = DemoBrowserRuntime.entries,
                displayName = DemoBrowserRuntime::label,
                onSelected = { if (it != browserRuntime) onRuntimeSwitch(it) },
            )
            SettingsSelect(
                label = demoText("Locale", "Език"),
                selected = settings.locale,
                values = DemoLocale.entries,
                displayName = DemoLocale::displayName,
                onSelected = { onChange(settings.copy(locale = it)) },
            )
            SettingsSelect(
                label = demoText("Direction", "Посока"),
                selected = settings.direction,
                values = DemoDirection.entries,
                displayName = DemoDirection::displayName,
                onSelected = { onChange(settings.copy(direction = it)) },
            )
            SettingsSelect(
                label = demoText("Demo Layout", "Оформление на демото"),
                selected = settings.demoLayout,
                values = DemoLayout.entries,
                displayName = { layout -> if (settings.locale == DemoLocale.Bulgarian) when (layout) {
                    DemoLayout.Default -> "По подразбиране"
                    DemoLayout.DemoAndData -> "Демо и данни"
                } else layout.displayName },
                onSelected = { onChange(settings.copy(demoLayout = it)) },
            )
            HorizontalDivider()
            Text(demoText("Appearance", "Външен вид"), style = MaterialTheme.typography.titleMedium)
            SettingsSelect(
                label = demoText("Interface Style", "Стил на интерфейса"),
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
                    ) { Text(when (mode) {
                        DemoThemeMode.System -> demoText("System", "Системен")
                        DemoThemeMode.Light -> demoText("Light", "Светъл")
                        DemoThemeMode.Dark -> demoText("Dark", "Тъмен")
                    }) }
                }
            }
            if (settings.effectiveUiStyle() == DemoUiStyle.Material) {
                SettingsSelect(
                    label = demoText("Light Material Theme", "Светла Material тема"),
                    selected = settings.lightMaterialTheme,
                    values = MaterialThemePreset.entries,
                    displayName = MaterialThemePreset::displayName,
                    onSelected = { onChange(settings.copy(lightMaterialTheme = it)) },
                )
                SettingsSelect(
                    label = demoText("Dark Material Theme", "Тъмна Material тема"),
                    selected = settings.darkMaterialTheme,
                    values = MaterialThemePreset.entries,
                    displayName = MaterialThemePreset::displayName,
                    onSelected = { onChange(settings.copy(darkMaterialTheme = it)) },
                )
            } else {
                SettingsSelect(
                    label = demoText("Light iOS Theme", "Светла iOS тема"),
                    selected = settings.lightCupertinoTheme,
                    values = CupertinoThemePreset.entries,
                    displayName = CupertinoThemePreset::displayName,
                    onSelected = { onChange(settings.copy(lightCupertinoTheme = it)) },
                )
                SettingsSelect(
                    label = demoText("Dark iOS Theme", "Тъмна iOS тема"),
                    selected = settings.darkCupertinoTheme,
                    values = CupertinoThemePreset.entries,
                    displayName = CupertinoThemePreset::displayName,
                    onSelected = { onChange(settings.copy(darkCupertinoTheme = it)) },
                )
            }
            HorizontalDivider()
            Text(demoText("Validation", "Валидация"), style = MaterialTheme.typography.titleMedium)
            ValidationMode.entries.forEach { mode ->
                Surface(
                    color = if (settings.validationMode == mode) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onChange(settings.copy(validationMode = mode)) },
                ) { Text(if (settings.locale == DemoLocale.Bulgarian) when (mode) {
                    ValidationMode.ValidateAndShow -> "Проверявай и показвай"
                    ValidationMode.ValidateAndHide -> "Проверявай и скривай"
                    ValidationMode.NoValidation -> "Без валидация"
                } else mode.displayName(), modifier = Modifier.padding(12.dp)) }
            }
            HorizontalDivider()
            Text(demoText("Options", "Опции"), style = MaterialTheme.typography.titleMedium)
            SettingSwitch(demoText("Hide Required Asterisk", "Скрий звездичката за задължителни полета"),
                demoText("Hide asterisks in labels for required fields.", "Скрий звездичките в етикетите на задължителните полета."), settings.hideRequiredAsterisk) {
                onChange(settings.copy(hideRequiredAsterisk = it))
            }
            SettingSwitch(demoText("Show Unfocused Description", "Показвай описанието без фокус"),
                demoText("Keep input descriptions visible while controls are unfocused.", "Показвай описанията и когато полетата не са на фокус."), settings.showUnfocusedDescription) {
                onChange(settings.copy(showUnfocusedDescription = it))
            }
            SettingSwitch(demoText("Restrict", "Ограничения"),
                demoText("Enforce schema input and array size restrictions.", "Прилагай ограниченията за въвеждане и размер на масивите от схемата."), settings.restrict) { onChange(settings.copy(restrict = it)) }
            SettingSwitch(demoText("Read-Only", "Само за четене"),
                demoText("Set all controls to read-only.", "Направи всички полета само за четене."),
                settings.readonly, tag = "setting-readonly") { onChange(settings.copy(readonly = it)) }
            SettingSwitch(demoText("Collapse new array items", "Свивай новите елементи на масив"),
                demoText("Do not expand newly added array items.", "Не разгъвай новодобавените елементи на масив."), settings.collapseNewItems) {
                onChange(settings.copy(collapseNewItems = it))
            }
            SettingSwitch(demoText("Hide array summary validation", "Скрий обобщението на грешките в масив"),
                demoText("Hide validation summaries in array headers.", "Скрий обобщенията на грешките в заглавията на масивите."), settings.hideArraySummaryValidation) {
                onChange(settings.copy(hideArraySummaryValidation = it))
            }
            SettingSwitch(demoText("Collapse arrays initially", "Свий масивите първоначално"),
                demoText("Start array accordions collapsed.", "Първоначално показвай масивите свити."), settings.initCollapsed) {
                onChange(settings.copy(initCollapsed = it))
            }
            SettingSwitch(demoText("Hide Array Item Avatar", "Скрий номера на елемента"),
                demoText("Hide array index avatars.", "Скрий номерата на елементите в масивите."), settings.hideAvatar) { onChange(settings.copy(hideAvatar = it)) }
            SettingSwitch(demoText("Enable Filter Errors Before Touch", "Скрий грешките преди докосване"),
                demoText("Hide inline errors until a field loses focus.",
                    "Скрий грешките до излизане от полето."),
                settings.enableFilterErrorsBeforeTouch ?: (exampleConfig["enableFilterErrorsBeforeTouch"] == true),
                tag = "setting-filter-before-touch") {
                onChange(settings.copy(enableFilterErrorsBeforeTouch = it))
            }
            OutlinedTextField(
                value = keywordDraft,
                onValueChange = { draft ->
                    keywordDraft = draft
                    onChange(settings.copy(filterErrorKeywordsBeforeTouch = parseErrorKeywords(draft)))
                },
                modifier = Modifier.fillMaxWidth().testTag("setting-error-keywords"),
                label = { Text(demoText("Filter Error Keywords Before Touch", "Грешки за скриване преди докосване")) },
                supportingText = { Text(demoText("Comma-separated JSON Schema keywords; empty hides all inline errors when filtering is enabled.",
                    "Ключови думи от JSON Schema, разделени със запетая; празно поле скрива всички грешки при включено филтриране.")) },
                singleLine = true,
            )
            SettingSwitch(demoText("Allow Additional Properties By Default", "Позволявай допълнителни свойства по подразбиране"),
                demoText("Allow new object properties when the schema does not specify additionalProperties.",
                    "Позволявай нови свойства, когато схемата не задава additionalProperties."),
                settings.allowAdditionalPropertiesByDefault ?: (exampleConfig["allowAdditionalPropertiesIfMissing"] == true),
                tag = "setting-additional-properties-default",
                modifier = Modifier.focusRequester(lastSwitchFocus)
                    .focusProperties { next = closeButtonFocus }) {
                onChange(settings.copy(allowAdditionalPropertiesByDefault = it))
            }
        }
    }
}

internal fun parseErrorKeywords(draft: String): List<String> =
    draft.split(',').map(String::trim).filter(String::isNotEmpty).distinct()

internal fun configuredErrorKeywords(config: Map<String, Any?>): List<String> =
    when (val value = config["filterErrorKeywordsBeforeTouch"]) {
        is JsonArray -> value.mapNotNull { (it as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.contentOrNull }
        is List<*> -> value.filterIsInstance<String>()
        else -> emptyList()
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
private fun SettingSwitch(label: String, description: String, checked: Boolean, tag: String? = null,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit) {
    val switchModifier = Modifier.semantics { contentDescription = label }
        .then(if (tag == null) Modifier else Modifier.testTag(tag)).then(modifier)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        if (LocalDemoUiStyle.current == DemoUiStyle.Cupertino) {
            CupertinoSwitch(checked, onCheckedChange, modifier = switchModifier)
        } else {
            Switch(checked, onCheckedChange, modifier = switchModifier)
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
            modifier = modifier.semantics { this.selected = selected }
                .clickable(role = Role.Button, onClick = onClick),
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
        Translator { id, defaultMessage, parameters ->
            (values[id] ?: defaultMessage)?.let { message ->
                CatalogParameter.replace(message) { match ->
                    when (val value = parameters[match.groupValues[1]]) {
                        is String, is Number, is Boolean -> value.toString()
                        else -> match.value
                    }
                }
            }
        }
    } ?: state.i18n?.translate
    return (state.i18n ?: JsonFormsI18nState()).copy(locale = languageTag, translate = translator)
}

private val CatalogParameter = Regex("\\{([A-Za-z][A-Za-z0-9_]*)\\}")

internal fun requestedDemoLocale(action: String, params: JsonObject): DemoLocale? {
    val parameter = when (action) {
        "changeLang" -> "lang"
        "setLocale" -> "locale"
        else -> return null
    }
    val language = (params[parameter] as? JsonPrimitive)
        ?.takeIf { it.isString }
        ?.content
        ?.replace('_', '-')
        ?.substringBefore('-')
        ?.lowercase()
    return DemoLocale.entries.firstOrNull { it.languageTag == language }
}

internal fun demoActionStatus(params: JsonObject, locale: DemoLocale): String =
    ((params["statusByLocale"] as? JsonObject)?.get(locale.languageTag) as? JsonPrimitive)?.contentOrNull
        ?: "Action completed"

private fun ExampleDescription.toStateProps() = StateProps(
    data = data,
    dataProvided = dataProvided,
    schema = schema,
    uischema = uischema,
    renderers = materialExtendedRenderers,
    cells = materialExtendedCells,
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
        Text(demoText("No examples registered", "Няма регистрирани примери"),
            style = MaterialTheme.typography.titleLarge)
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
private val ReloadIcon = lineIcon("Reload") {
    moveTo(20f, 6f); verticalLineTo(11f); horizontalLineTo(15f)
    moveTo(20f, 11f); curveTo(19f, 6f, 15f, 4f, 12f, 4f)
    curveTo(7.5f, 4f, 4f, 7.5f, 4f, 12f)
    curveTo(4f, 16.5f, 7.5f, 20f, 12f, 20f)
    curveTo(15f, 20f, 17.5f, 18.5f, 19f, 16f)
}
private val ApplyIcon = lineIcon("Apply") {
    moveTo(4f, 12f); lineTo(10f, 18f); lineTo(20f, 6f)
}
private val ValidationIssueIcon = lineIcon("Validation errors") {
    moveTo(12f, 3f); lineTo(2f, 21f); horizontalLineTo(22f); close()
    moveTo(12f, 9f); verticalLineTo(14f)
    moveTo(12f, 17f); lineTo(12f, 17.1f)
}
private val PanelsIcon = lineIcon("Form only") { moveTo(3f, 4f); horizontalLineTo(21f); verticalLineTo(20f); horizontalLineTo(3f); close(); moveTo(8f, 4f); verticalLineTo(20f); moveTo(8f, 10f); horizontalLineTo(21f) }
private val WebComponentIcon = lineIcon("Web component") {
    moveTo(3f, 4f); horizontalLineTo(21f); verticalLineTo(20f); horizontalLineTo(3f); close()
    moveTo(9f, 9f); lineTo(6f, 12f); lineTo(9f, 15f)
    moveTo(15f, 9f); lineTo(18f, 12f); lineTo(15f, 15f)
}
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
