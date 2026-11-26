package com.chobantonov.jsonfroms.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.chobantonov.jsonfroms.core.actions.Actions
import com.chobantonov.jsonfroms.core.actions.InitActionOptions
import com.chobantonov.jsonfroms.core.actions.JsonFormsAction
import com.chobantonov.jsonfroms.core.actions.MoveArrayContext
import com.chobantonov.jsonfroms.core.actions.UpdateAction
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticSeverity
import com.chobantonov.jsonfroms.core.generators.Generate
import com.chobantonov.jsonfroms.core.i18n.translateRendererText
import com.chobantonov.jsonfroms.core.mappers.ContainerErrorIndex
import com.chobantonov.jsonfroms.core.mappers.ContainerErrorIndexCache
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.reducers.Middleware
import com.chobantonov.jsonfroms.core.reducers.coreReducer
import com.chobantonov.jsonfroms.core.reducers.defaultMiddleware
import com.chobantonov.jsonfroms.core.store.CombinedValidity
import com.chobantonov.jsonfroms.core.store.OwnedAdditionalErrorStore
import com.chobantonov.jsonfroms.core.store.getCombinedValidity
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
import com.chobantonov.jsonfroms.core.util.Id
import com.chobantonov.jsonfroms.core.util.activeEditingSchemaForScopedProperty
import com.chobantonov.jsonfroms.core.util.scopedPropertyDataPointer
import com.chobantonov.jsonfroms.core.util.JsonPointer
import com.chobantonov.jsonfroms.core.util.JsonPath
import com.chobantonov.jsonfroms.core.util.Resolve
import com.chobantonov.jsonfroms.core.testers.isControl
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.jvm.JvmName

typealias ComposeJsonFormsState =
    JsonFormsState<ComposeJsonFormsRenderer, ComposeJsonFormsCellRenderer>

private val scopeFallbackLocaleBundles = mapOf(
    "bg" to JsonObject(mapOf("control.scopeUnresolved" to JsonPrimitive("Няма приложимо поле за този път в схемата."))),
)

private val unknownRendererLocaleBundles = mapOf(
    "bg" to JsonObject(mapOf(
        "renderer.unavailable" to JsonPrimitive("Няма приложим визуализатор."),
        "cell.unavailable" to JsonPrimitive("Няма приложим редактор за клетка."),
    )),
)

private fun unavailableRendererText(state: ComposeJsonFormsState, type: String): String =
    translateRendererText(
        state.jsonforms.i18n.translate,
        state.jsonforms.i18n.locale,
        "$type.unavailable",
        "No applicable $type found.",
        unknownRendererLocaleBundles,
    )

@Composable
private fun ReportUnavailableRenderer(
    config: Map<String, Any?>,
    type: String,
    path: String,
    elementType: String?,
) {
    val reporter = config[UiDiagnosticReporterConfigKey] as? UiDiagnosticReporter ?: return
    LaunchedEffect(reporter, type, path, elementType) {
        reporter.report(UiDiagnostic(
            code = "$type.unavailable",
            severity = UiDiagnosticSeverity.Warning,
            message = "No applicable $type found for ${elementType ?: "missing UI schema"}.",
            details = mapOf("path" to path, "elementType" to elementType),
        ))
    }
}

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
    val readonly: Boolean? = null,
    val occurrenceRevision: Long = 0L,
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

fun ComposeJsonFormsCellProps.asRendererProps(): ComposeJsonFormsRendererProps =
    ComposeJsonFormsRendererProps(
        state = state,
        dispatch = dispatch,
        uischema = uischema,
        schema = schema,
        rootSchema = rootSchema,
        path = path,
        enabled = enabled,
        renderers = renderers,
        cells = cells,
        id = id,
        config = config,
    )

data class JsonFormsChange(
    val data: JsonElement,
    val errors: List<ErrorObject>,
    val dataProvided: Boolean = true,
)

data class JsonFormsComposeContext(
    val state: ComposeJsonFormsState,
    val dispatch: (JsonFormsAction) -> Unit,
    val controller: JsonFormsComposeController,
    val containerErrorIndex: ContainerErrorIndex,
    val hostContext: JsonElement,
)

private data class RankedMatch<T>(
    val entry: T,
    val rank: Int,
)

private val LocalJsonFormsContext = compositionLocalOf<JsonFormsComposeContext>(referentialEqualityPolicy()) {
    error("JsonFormsComposeContext was not provided")
}
private val LocalHostContext = compositionLocalOf<JsonElement> { JsonNull }
private val LocalJsonFormsItem = compositionLocalOf<JsonElement?> { null }

@Composable
fun JsonFormsItemScope(item: JsonElement?, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalJsonFormsItem provides item, content = content)
}

@Composable
fun currentJsonFormsItem(): JsonElement? = LocalJsonFormsItem.current

@Stable
class JsonFormsComposeController(
    initialState: ComposeJsonFormsState = ComposeJsonFormsState(),
    private val middleware: Middleware = defaultMiddleware,
) {
    var state by mutableStateOf(initialState, referentialEqualityPolicy())
        private set

    private var ownedErrors = OwnedAdditionalErrorStore(hostErrors = initialState.jsonforms.core.additionalErrors)
    private var nextOwnedRequestVersion = 0L
    private data class OwnedRequest(val version: Long, val targetPath: String)
    private var activeOwnedRequests: Map<String, OwnedRequest> = emptyMap()

    var combinedValidity by mutableStateOf(getCombinedValidity(state, ownedErrors))
        private set

    var hasPendingOwnedWork by mutableStateOf(ownedErrors.pending)
        private set

    /** Changes only for an externally replaced data or schema subject, not a controlled edit echo. */
    var hostSubjectVersion by mutableStateOf(0L)
        private set

    /** Accepted row moves can keep JSON data structurally equal (for duplicate values). */
    var occurrenceRevision by mutableStateOf(0L)
        private set

    /** Start an async validation; only the returned version may publish its result. */
    fun beginAdditionalErrors(ownerId: String, targetPath: String = ""): Long {
        val version = ++nextOwnedRequestVersion
        activeOwnedRequests = activeOwnedRequests + (ownerId to OwnedRequest(version, targetPath))
        updateOwnedErrors(ownedErrors.clear(ownerId).setPending(ownerId, true))
        return version
    }

    /** Owner IDs identify stable editor instances; publication never replays an older data snapshot. */
    fun publishAdditionalErrors(
        ownerId: String,
        targetPath: String,
        errors: List<ErrorObject>,
        requestVersion: Long? = null,
    ) {
        if (requestVersion != null) {
            val request = activeOwnedRequests[ownerId]
            if (request?.version != requestVersion ||
                (request.targetPath.isNotEmpty() && request.targetPath != targetPath)) return
        }
        activeOwnedRequests = activeOwnedRequests - ownerId
        updateOwnedErrors(ownedErrors.publish(ownerId, targetPath, errors))
    }

    fun clearAdditionalErrors(ownerId: String) {
        activeOwnedRequests = activeOwnedRequests - ownerId
        updateOwnedErrors(ownedErrors.clear(ownerId))
    }

    fun setAdditionalErrorsPending(ownerId: String, pending: Boolean) {
        if (!pending) activeOwnedRequests = activeOwnedRequests - ownerId
        updateOwnedErrors(ownedErrors.setPending(ownerId, pending))
    }

    fun moveAdditionalErrors(ownerId: String, newTargetPath: String) {
        activeOwnedRequests[ownerId]?.let { request ->
            activeOwnedRequests = activeOwnedRequests + (ownerId to request.copy(targetPath = newTargetPath))
        }
        updateOwnedErrors(ownedErrors.move(ownerId, newTargetPath))
    }

    private fun updateOwnedErrors(next: OwnedAdditionalErrorStore) {
        if (next == ownedErrors) return
        ownedErrors = next
        hasPendingOwnedWork = next.pending
        val core = state.jsonforms.core as JsonFormsCoreState
        val merged = next.errors
        if (core.additionalErrors != merged) {
            state = state.copy(jsonforms = state.jsonforms.copy(core = core.copy(additionalErrors = merged)))
        }
        combinedValidity = getCombinedValidity(state, next)
    }

    fun dispatch(action: JsonFormsAction) {
        dispatchAndGetChange(action)
    }

    internal fun dispatchAndGetChange(action: JsonFormsAction): JsonFormsChange? {
        val currentCore = state.jsonforms.core as JsonFormsCoreState
        val nextCore = middleware(currentCore, action, coreReducer)
        val occurrenceMove = (action as? UpdateAction)?.let { update ->
            val moves = update.context as? MoveArrayContext ?: return@let false
            val array = JsonPath.get(currentCore.data, update.path) as? JsonArray ?: return@let false
            nextCore.data !== currentCore.data && moves.moves.any { move ->
                val from = move.from
                val to = move.to
                from != null && to != null && from != to && from in array.indices && to in array.indices
            }
        } == true
        if (nextCore == currentCore && !occurrenceMove) {
            return null
        }
        state = state.copy(
            jsonforms = state.jsonforms.copy(core = nextCore),
        )
        if (occurrenceMove) occurrenceRevision++
        if (nextCore.data != currentCore.data || occurrenceMove) {
            val editedPath = (action as? UpdateAction)?.path?.let { path ->
                if (path.isEmpty()) "" else path.split('.')
                    .filter(String::isNotEmpty)
                    .joinToString(separator = "/", prefix = "/") { JsonPointer.encode(it) }
            }
            val affected = if (editedPath == null) {
                ownedErrors.ownersAffectedBy("") + activeOwnedRequests.keys
            } else {
                ownedErrors.ownersAffectedBy(editedPath) + activeOwnedRequests.filterValues { request ->
                    pathsOverlap(request.targetPath, editedPath)
                }.keys
            }
            var nextOwnedErrors = ownedErrors
            affected.forEach { ownerId ->
                activeOwnedRequests = activeOwnedRequests - ownerId
                nextOwnedErrors = nextOwnedErrors.clear(ownerId)
            }
            updateOwnedErrors(nextOwnedErrors)
        }
        combinedValidity = getCombinedValidity(state, ownedErrors)
        return if (
            nextCore.data != currentCore.data || occurrenceMove || nextCore.errors != currentCore.errors ||
            nextCore.dataProvided != currentCore.dataProvided
        ) {
            JsonFormsChange(nextCore.data, nextCore.errors, nextCore.dataProvided)
        } else {
            null
        }
    }

    private fun pathsOverlap(first: String, second: String): Boolean =
        first == second || first.isEmpty() || second.isEmpty() ||
            first.startsWith("$second/") || second.startsWith("$first/")

    fun replaceState(newState: ComposeJsonFormsState) {
        // A controlled JsonForms instance feeds an internal edit back through its
        // data parameter. The freshly initialized state has a new validator and
        // derived errors, but otherwise describes the state we already hold. Do
        // not replace the whole tree for that echo: doing so causes a second
        // validation/recomposition pass and visible text flicker on JS.
        val currentCore = state.jsonforms.core
        val incomingCore = newState.jsonforms.core as JsonFormsCoreState
        val subjectChanged = currentCore.data != incomingCore.data ||
            currentCore.schema != incomingCore.schema
        if (subjectChanged) hostSubjectVersion++
        ownedErrors = if (subjectChanged) {
            activeOwnedRequests = emptyMap()
            OwnedAdditionalErrorStore(hostErrors = incomingCore.additionalErrors)
        } else {
            ownedErrors.replaceHost(incomingCore.additionalErrors)
        }
        hasPendingOwnedWork = ownedErrors.pending
        val mergedState = newState.copy(jsonforms = newState.jsonforms.copy(
            core = incomingCore.copy(additionalErrors = ownedErrors.errors),
        ))
        if (!state.hasSameExternalInputs(mergedState)) {
            state = mergedState
        }
        combinedValidity = getCombinedValidity(state, ownedErrors)
    }
}

private fun ComposeJsonFormsState.hasSameExternalInputs(other: ComposeJsonFormsState): Boolean {
    val current = jsonforms
    val next = other.jsonforms
    val currentCore = current.core
    val nextCore = next.core

    return currentCore.data == nextCore.data &&
        currentCore.dataProvided == nextCore.dataProvided &&
        currentCore.schema == nextCore.schema &&
        currentCore.uischema == nextCore.uischema &&
        currentCore.additionalErrors == nextCore.additionalErrors &&
        currentCore.validationMode == nextCore.validationMode &&
        current.config == next.config &&
        current.renderers == next.renderers &&
        current.cells == next.cells &&
        current.i18n == next.i18n &&
        current.uischemas == next.uischemas &&
        current.readonly == next.readonly &&
        current.additionalState == next.additionalState
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

/** Installs the form controller and owned-error publication channel required by registered renderers. */
@Composable
fun JsonFormsStateProvider(
    initState: ComposeJsonFormsState,
    middleware: Middleware = defaultMiddleware,
    onChange: ((JsonFormsChange) -> Unit)? = null,
    onValidityChange: ((CombinedValidity) -> Unit)? = null,
    context: JsonElement? = null,
    content: @Composable () -> Unit,
) {
    val hostContext = context ?: LocalHostContext.current
    val controller = rememberJsonFormsComposeController(
        initialState = initState,
        middleware = middleware,
    )
    val currentOnChange = rememberUpdatedState(onChange)
    val currentOnValidityChange = rememberUpdatedState(onValidityChange)
    val dispatch = remember(controller) {
        { action: JsonFormsAction ->
            controller.dispatchAndGetChange(action)?.let { change ->
                currentOnChange.value?.invoke(change)
            }
            Unit
        }
    }

    LaunchedEffect(initState) {
        controller.replaceState(initState)
    }

    val state = controller.state
    val core = state.jsonforms.core
    val errorIndexCache = remember { ContainerErrorIndexCache() }
    val containerErrorIndex = errorIndexCache.get(core.errors, core.additionalErrors, core.validationMode)
    val validity = controller.combinedValidity
    LaunchedEffect(validity, core.errors, core.additionalErrors) {
        currentOnValidityChange.value?.invoke(validity)
    }

    CompositionLocalProvider(
        LocalHostContext provides hostContext,
        LocalJsonFormsContext provides JsonFormsComposeContext(
            state = state,
            dispatch = dispatch,
            controller = controller,
            containerErrorIndex = containerErrorIndex,
            hostContext = hostContext,
        ),
        content = content,
    )
}

/** Requires [JsonFormsStateProvider]; renderer composition without it is unsupported. */
@Composable
fun useJsonForms(): JsonFormsComposeContext = LocalJsonFormsContext.current

/** A renderer-owned pending edit blocks a valid result until the edit is committed or discarded. */
@Composable
fun rememberPendingEditOwner(ownerId: String): (Boolean) -> Unit {
    val controller = useJsonForms().controller
    DisposableEffect(controller, ownerId) {
        onDispose { controller.clearAdditionalErrors(ownerId) }
    }
    return remember(controller, ownerId) {
        { pending: Boolean -> controller.setAdditionalErrorsPending(ownerId, pending) }
    }
}

/** Publishes one renderer-owned error for a scoped edit, clearing it on disposal. */
@Composable
fun rememberRendererErrorOwner(ownerId: String, path: String): (ErrorObject?) -> Unit {
    val controller = useJsonForms().controller
    val pointer = remember(path) {
        if (path.isBlank()) "" else path.split('.').filter(String::isNotEmpty)
            .joinToString(separator = "/", prefix = "/") { JsonPointer.encode(it) }
    }
    DisposableEffect(controller, ownerId) {
        onDispose { controller.clearAdditionalErrors(ownerId) }
    }
    return remember(controller, ownerId, pointer) {
        { error: ErrorObject? ->
            val hasSchemaError = error != null && controller.state.jsonforms.core.errors.any {
                it.instancePath == pointer && it.keyword == error.keyword
            }
            controller.publishAdditionalErrors(ownerId, pointer,
                if (error == null || hasSchemaError) emptyList() else listOf(error.copy(instancePath = pointer)))
        }
    }
}

@Composable
fun JsonForms(
    data: JsonElement,
    renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
    cells: List<JsonFormsCellRendererRegistryEntry<ComposeJsonFormsCellRenderer>> = emptyList(),
    schema: JsonSchema? = null,
    uischema: UiSchemaElement? = null,
    config: Map<String, Any?> = emptyMap(),
    context: JsonElement? = null,
    uischemas: List<JsonFormsUISchemaRegistryEntry> = emptyList(),
    readonly: Boolean = false,
    validationMode: ValidationMode? = null,
    i18n: JsonFormsI18nState = JsonFormsI18nState(),
    additionalErrors: List<ErrorObject> = emptyList(),
    middleware: Middleware = defaultMiddleware,
    dataProvided: Boolean = true,
    onChange: ((JsonFormsChange) -> Unit)? = null,
    onValidityChange: ((CombinedValidity) -> Unit)? = null,
) {
    val schemaToUse = remember(schema, data) {
        schema ?: Generate.jsonSchema(data)
    }
    val uiSchemaToUse = remember(uischema, schemaToUse) {
        uischema ?: Generate.uiSchema(schemaToUse, rootSchema = schemaToUse)
    }
    val initState = remember(
        data,
        dataProvided,
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
            dataProvided = dataProvided,
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

    val clearSelection = remember { ClearAffordanceSelection() }
    CompositionLocalProvider(LocalClearAffordanceSelection provides clearSelection) {
        JsonFormsStateProvider(
            initState = initState,
            middleware = middleware,
            onChange = onChange,
            onValidityChange = onValidityChange,
            context = context,
        ) {
            JsonFormsDispatchRenderer()
        }
    }
}

@Composable
fun JsonFormsDispatchRenderer() {
    val context = useJsonForms()
    val state = context.state
    val core = state.jsonforms.core
    val uischema = core.uischema ?: run {
        ReportUnavailableRenderer(getConfig(state), "renderer", "", null)
        UnknownRenderer("renderer", unavailableRendererText(state, "renderer"), state.jsonforms.renderers)
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
            occurrenceRevision = context.controller.occurrenceRevision,
        ),
    )
}

@Composable
fun JsonFormsDispatchRenderer(
    props: ComposeJsonFormsRendererProps,
) {
    val control = props.uischema as? ControlElement
    // Tuple detail controls use #/items/N and #/prefixItems/N; their renderer owns
    // invalid-position diagnostics and must see the authored scope unchanged.
    if (control != null && control.scope.startsWith("#/properties/") &&
        Resolve.schema(props.schema, control.scope, props.rootSchema) == null) {
        val reporter = props.config[UiDiagnosticReporterConfigKey] as? UiDiagnosticReporter
        LaunchedEffect(reporter, control.scope, props.path) {
            reporter?.report(UiDiagnostic(
                code = "control.scopeUnresolved",
                severity = UiDiagnosticSeverity.Warning,
                message = "Control scope ${control.scope} does not resolve against the schema; ignored.",
                details = mapOf("scope" to control.scope, "path" to props.path),
            ))
        }
        UnknownRenderer(
            "scope",
            translateRendererText(
                props.state.jsonforms.i18n.translate,
                props.state.jsonforms.i18n.locale,
                "control.scopeUnresolved",
                "No applicable scope found.",
                scopeFallbackLocaleBundles,
            ),
            props.renderers,
        )
        return
    }
    val effectiveProps = if (control != null) {
        val contextData = JsonPath.get(props.state.jsonforms.core.data, props.path)
        val value = scopedPropertyDataPointer(control.scope)?.let { pointer ->
            JsonPointer.resolve(contextData, pointer)
        }
        val activeSchema = activeEditingSchemaForScopedProperty(
            props.schema, control.scope, props.rootSchema, value, contextData)
        if (activeSchema != null) props.copy(schema = activeSchema) else props
    } else props
    if (control != null) ReportInvalidInputAdornments(effectiveProps)
    val testerContext = remember(props.rootSchema, props.config) {
        TesterContext(
            rootSchema = props.rootSchema,
            config = props.config,
        )
    }
    val rankedRenderer = remember(
        effectiveProps.renderers,
        effectiveProps.uischema,
        effectiveProps.schema,
        testerContext,
    ) {
        selectBestMatch(
            entries = effectiveProps.renderers,
            tester = { entry -> entry.tester },
            uischema = effectiveProps.uischema,
            schema = effectiveProps.schema,
            context = testerContext,
        )
    }
    if (rankedRenderer == null || rankedRenderer.rank == -1) {
        ReportUnavailableRenderer(props.config, "renderer", props.path, props.uischema.type)
        UnknownRenderer("renderer", unavailableRendererText(props.state, "renderer"), props.renderers)
        return
    }

    rankedRenderer.entry.renderer.Render(effectiveProps)
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
        ReportUnavailableRenderer(props.config, "cell", props.path, props.uischema.type)
        UnknownRenderer("cell", unavailableRendererText(props.state, "cell"), props.renderers)
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
        Id.createId(control.scope)
    }
    androidx.compose.runtime.DisposableEffect(id) {
        onDispose {
            Id.removeId(id)
        }
    }
    return id
}

private fun createComposeState(
    data: JsonElement,
    dataProvided: Boolean,
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
            dataProvided = dataProvided,
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
