package com.chobantonov.jsonfroms.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.chobantonov.jsonfroms.core.actions.Actions
import com.chobantonov.jsonfroms.core.actions.InitActionOptions
import com.chobantonov.jsonfroms.core.actions.JsonFormsAction
import com.chobantonov.jsonfroms.core.generators.Generate
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.reducers.Middleware
import com.chobantonov.jsonfroms.core.reducers.coreReducer
import com.chobantonov.jsonfroms.core.reducers.defaultMiddleware
import com.chobantonov.jsonfroms.core.store.JsonFormsCellRendererRegistryEntry
import com.chobantonov.jsonfroms.core.store.JsonFormsCore
import com.chobantonov.jsonfroms.core.store.JsonFormsCoreState
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry
import com.chobantonov.jsonfroms.core.store.JsonFormsState
import com.chobantonov.jsonfroms.core.store.JsonFormsSubStates
import com.chobantonov.jsonfroms.core.store.JsonFormsUISchemaRegistryEntry
import com.chobantonov.jsonfroms.core.store.ValidationMode
import com.chobantonov.jsonfroms.core.store.getConfig
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.TesterContext
import com.chobantonov.jsonfroms.core.util.createId
import com.chobantonov.jsonfroms.core.util.removeId
import com.chobantonov.jsonfroms.core.testers.isControl
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import kotlinx.serialization.json.JsonElement
import kotlin.jvm.JvmName

typealias ComposeJsonFormsState =
    JsonFormsState<ComposeJsonFormsRenderer, ComposeJsonFormsCellRenderer>

fun interface ComposeJsonFormsRenderer {
    @Composable
    fun Render(props: ComposeJsonFormsRendererProps)
}

fun interface ComposeJsonFormsCellRenderer {
    @Composable
    fun Render(props: ComposeJsonFormsCellProps)
}

data class ComposeJsonFormsRendererProps(
    val state: ComposeJsonFormsState,
    val dispatch: (JsonFormsAction) -> Unit,
    val uischema: UiSchemaElement,
    val schema: JsonSchema,
    val rootSchema: JsonSchema,
    val path: String,
    val enabled: Boolean,
    val renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
    val cells: List<JsonFormsCellRendererRegistryEntry<ComposeJsonFormsCellRenderer>>,
    val id: String? = null,
    val config: Map<String, Any?> = emptyMap(),
)

data class ComposeJsonFormsCellProps(
    val state: ComposeJsonFormsState,
    val dispatch: (JsonFormsAction) -> Unit,
    val uischema: ControlElement,
    val schema: JsonSchema,
    val rootSchema: JsonSchema,
    val path: String,
    val enabled: Boolean,
    val renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
    val cells: List<JsonFormsCellRendererRegistryEntry<ComposeJsonFormsCellRenderer>>,
    val id: String? = null,
    val config: Map<String, Any?> = emptyMap(),
)

data class JsonFormsChange(
    val data: JsonElement,
    val errors: List<ErrorObject>,
)

data class JsonFormsComposeContext(
    val state: ComposeJsonFormsState,
    val dispatch: (JsonFormsAction) -> Unit,
)

private data class RankedMatch<T>(
    val entry: T,
    val rank: Int,
)

private val LocalJsonFormsContext = compositionLocalOf<JsonFormsComposeContext> {
    error("JsonFormsComposeContext was not provided")
}

@Stable
class JsonFormsComposeController(
    initialState: ComposeJsonFormsState = ComposeJsonFormsState(),
    private val middleware: Middleware = defaultMiddleware,
) {
    var state by mutableStateOf(initialState)
        private set

    fun dispatch(action: JsonFormsAction) {
        state = state.copy(
            jsonforms = state.jsonforms.copy(
                core = middleware(state.jsonforms.core as JsonFormsCoreState, action, coreReducer),
            ),
        )
    }

    fun replaceState(newState: ComposeJsonFormsState) {
        state = newState
    }
}

@Composable
fun rememberJsonFormsComposeController(
    initialState: ComposeJsonFormsState = ComposeJsonFormsState(),
    middleware: Middleware = defaultMiddleware,
): JsonFormsComposeController = remember(middleware) {
    JsonFormsComposeController(
        initialState = initialState,
        middleware = middleware,
    )
}

@Composable
fun JsonFormsStateProvider(
    initState: ComposeJsonFormsState,
    middleware: Middleware = defaultMiddleware,
    onChange: ((JsonFormsChange) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val controller = rememberJsonFormsComposeController(
        initialState = initState,
        middleware = middleware,
    )
    val currentOnChange by rememberUpdatedState(onChange)

    LaunchedEffect(initState) {
        controller.replaceState(initState)
    }

    val state = controller.state

    LaunchedEffect(state.jsonforms.core.data, state.jsonforms.core.errors) {
        currentOnChange?.invoke(
            JsonFormsChange(
                data = state.jsonforms.core.data,
                errors = state.jsonforms.core.errors,
            ),
        )
    }

    CompositionLocalProvider(
        LocalJsonFormsContext provides JsonFormsComposeContext(
            state = state,
            dispatch = controller::dispatch,
        ),
        content = content,
    )
}

@Composable
fun useJsonForms(): JsonFormsComposeContext = LocalJsonFormsContext.current

@Composable
fun JsonForms(
    data: JsonElement,
    renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
    cells: List<JsonFormsCellRendererRegistryEntry<ComposeJsonFormsCellRenderer>> = emptyList(),
    schema: JsonSchema? = null,
    uischema: UiSchemaElement? = null,
    config: Map<String, Any?> = emptyMap(),
    uischemas: List<JsonFormsUISchemaRegistryEntry> = emptyList(),
    readonly: Boolean = false,
    validationMode: ValidationMode? = null,
    i18n: JsonFormsI18nState = JsonFormsI18nState(),
    additionalErrors: List<ErrorObject> = emptyList(),
    middleware: Middleware = defaultMiddleware,
    onChange: ((JsonFormsChange) -> Unit)? = null,
) {
    val schemaToUse = remember(schema, data) {
        schema ?: Generate.jsonSchema(data)
    }
    val uiSchemaToUse = remember(uischema, schemaToUse) {
        uischema ?: Generate.uiSchema(schemaToUse, rootSchema = schemaToUse)
    }
    val initState = remember(
        data,
        schemaToUse,
        uiSchemaToUse,
        renderers,
        cells,
        config,
        uischemas,
        readonly,
        validationMode,
        i18n,
        additionalErrors,
        middleware,
    ) {
        createComposeState(
            data = data,
            schema = schemaToUse,
            uischema = uiSchemaToUse,
            renderers = renderers,
            cells = cells,
            config = config,
            uischemas = uischemas,
            readonly = readonly,
            validationMode = validationMode,
            i18n = i18n,
            additionalErrors = additionalErrors,
            middleware = middleware,
        )
    }

    JsonFormsStateProvider(
        initState = initState,
        middleware = middleware,
        onChange = onChange,
    ) {
        JsonFormsDispatchRenderer()
    }
}

@Composable
fun JsonFormsDispatchRenderer() {
    val context = useJsonForms()
    val state = context.state
    val core = state.jsonforms.core
    val uischema = core.uischema ?: run {
        UnknownRenderer("renderer")
        return
    }

    JsonFormsDispatchRenderer(
        props = ComposeJsonFormsRendererProps(
            state = state,
            dispatch = context.dispatch,
            uischema = uischema,
            schema = core.schema,
            rootSchema = core.schema,
            path = "",
            enabled = !state.jsonforms.readonly,
            renderers = state.jsonforms.renderers,
            cells = state.jsonforms.cells,
            id = rememberControlId(uischema, core.schema),
            config = getConfig(state),
        ),
    )
}

@Composable
fun JsonFormsDispatchRenderer(
    props: ComposeJsonFormsRendererProps,
) {
    val testerContext = remember(props.rootSchema, props.config) {
        TesterContext(
            rootSchema = props.rootSchema,
            config = props.config,
        )
    }
    val rankedRenderer = remember(
        props.renderers,
        props.uischema,
        props.schema,
        testerContext,
    ) {
        selectBestMatch(
            entries = props.renderers,
            tester = { entry -> entry.tester },
            uischema = props.uischema,
            schema = props.schema,
            context = testerContext,
        )
    }
    if (rankedRenderer == null || rankedRenderer.rank == -1) {
        UnknownRenderer("renderer")
        return
    }

    rankedRenderer.entry.renderer.Render(props)
}

@Composable
fun DispatchCell(
    props: ComposeJsonFormsCellProps,
) {
    val testerContext = remember(props.rootSchema, props.config) {
        TesterContext(
            rootSchema = props.rootSchema,
            config = props.config,
        )
    }
    val rankedCell = remember(
        props.cells,
        props.uischema,
        props.schema,
        testerContext,
    ) {
        selectBestMatch(
            entries = props.cells,
            tester = { entry -> entry.tester },
            uischema = props.uischema,
            schema = props.schema,
            context = testerContext,
        )
    }
    if (rankedCell == null || rankedCell.rank == -1) {
        UnknownRenderer("cell")
        return
    }

    rankedCell.entry.cell.Render(props)
}

@Composable
private fun rememberControlId(
    uischema: UiSchemaElement,
    schema: JsonSchema,
): String? {
    if (!isControl(uischema)) {
        return null
    }
    val control = uischema as ControlElement
    val id = remember(control.scope, schema) {
        createId(control.scope)
    }
    androidx.compose.runtime.DisposableEffect(id) {
        onDispose {
            removeId(id)
        }
    }
    return id
}

private fun createComposeState(
    data: JsonElement,
    schema: JsonSchema,
    uischema: UiSchemaElement,
    renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
    cells: List<JsonFormsCellRendererRegistryEntry<ComposeJsonFormsCellRenderer>>,
    config: Map<String, Any?>,
    uischemas: List<JsonFormsUISchemaRegistryEntry>,
    readonly: Boolean,
    validationMode: ValidationMode?,
    i18n: JsonFormsI18nState,
    additionalErrors: List<ErrorObject>,
    middleware: Middleware,
): ComposeJsonFormsState {
    val core = middleware(
        state = JsonFormsCoreState(),
        action = Actions.init(
            data = data,
            schema = schema,
            uischema = uischema,
            options = InitActionOptions(
                validationMode = validationMode,
                additionalErrors = additionalErrors,
            ),
        ),
        defaultReducer = coreReducer,
    )

    return ComposeJsonFormsState(
        jsonforms = JsonFormsSubStates(
            core = core,
            config = config,
            renderers = renderers,
            cells = cells,
            i18n = i18n,
            uischemas = uischemas,
            readonly = readonly,
        ),
    )
}

@JvmName("jsonFormsCore")
fun JsonFormsComposeContext.core(): JsonFormsCore = state.jsonforms.core

private fun <T> selectBestMatch(
    entries: List<T>,
    tester: (T) -> RankedTester,
    uischema: UiSchemaElement,
    schema: JsonSchema,
    context: TesterContext,
): RankedMatch<T>? {
    var bestMatch: RankedMatch<T>? = null
    for (entry in entries) {
        val rank = tester(entry)(uischema, schema, context)
        if (bestMatch == null || rank > bestMatch.rank) {
            bestMatch = RankedMatch(
                entry = entry,
                rank = rank,
            )
        }
    }
    return bestMatch
}
