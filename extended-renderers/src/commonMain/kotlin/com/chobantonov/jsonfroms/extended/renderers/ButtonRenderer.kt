package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.useJsonForms
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticSeverity
import com.chobantonov.jsonfroms.core.i18n.defaultTranslator
import com.chobantonov.jsonfroms.core.i18n.translateRendererText
import com.chobantonov.jsonfroms.core.models.UnknownUiSchemaElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.uiTypeIs
import com.chobantonov.jsonfroms.cupertino.renderers.util.LocalCupertinoColors
import com.slapps.cupertino.CupertinoButton
import com.slapps.cupertino.CupertinoButtonDefaults
import com.slapps.cupertino.CupertinoActivityIndicator
import com.slapps.cupertino.CupertinoText
import com.slapps.cupertino.ExperimentalCupertinoApi
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonElement
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

const val ExtendedActionHandlerConfigKey = "extendedActionHandler"
const val ExtendedActionEventHandlerConfigKey = "extendedActionEventHandler"
const val ExtendedScriptHandlerConfigKey = "extendedScriptHandler"
val buttonTester: RankedTester = rankWith(1, uiTypeIs("Button"))

data class ExtendedActionContext(
    val data: JsonElement,
    val schema: JsonSchema,
    val uischema: UiSchemaElement?,
    val errors: List<ErrorObject>,
    val additionalErrors: List<ErrorObject>,
    val readonly: Boolean,
    val locale: String?,
    val config: Map<String, Any?>,
    val hostContext: JsonElement = JsonNull,
) {
    val external: JsonElement? get() = (hostContext as? JsonObject)?.get("external")
}

data class ExtendedActionEvent(
    val action: String,
    val label: String,
    val params: JsonObject,
    val element: UnknownUiSchemaElement,
    val path: String,
    val context: ExtendedActionContext,
)

fun interface ExtendedActionEventHandler {
    suspend fun invoke(event: ExtendedActionEvent)
}

fun interface ExtendedScriptHandler {
    suspend fun invoke(script: String, event: ExtendedActionEvent)
}

fun interface ExtendedActionHandler {
    suspend fun invoke(action: String, params: JsonObject)
}

private class ButtonDispatchGuard { var active = false }

internal enum class ButtonSemanticStyle { Primary, Secondary, Alternative, Success, Warning, Error }

internal fun buttonSemanticStyle(color: String?): ButtonSemanticStyle = when (color) {
    "secondary" -> ButtonSemanticStyle.Secondary
    "alternative" -> ButtonSemanticStyle.Alternative
    "success" -> ButtonSemanticStyle.Success
    "warning" -> ButtonSemanticStyle.Warning
    "error" -> ButtonSemanticStyle.Error
    else -> ButtonSemanticStyle.Primary
}

private data class ButtonContent(
    val label: String,
    val icon: String?,
    val enabled: Boolean,
    val style: ButtonSemanticStyle,
    val fullWidth: Boolean,
    val pending: Boolean,
    val pendingDescription: String,
    val pendingEditDescription: String?,
    val onClick: () -> Unit,
)

@Composable
private fun buttonContent(props: ComposeJsonFormsRendererProps): ButtonContent? {
    val element = props.uischema as? UnknownUiSchemaElement ?: return null
    val fallback = element.stringProperty("label") ?: element.stringProperty("text") ?: "Run"
    val key = element.stringProperty("i18n")?.let { "$it.label" } ?: fallback
    val translator = props.state.jsonforms.i18n.translate ?: defaultTranslator
    val label = translator.translate(key, fallback, mapOf("uischema" to element)) ?: fallback
    val action = element.stringProperty("action")
    val script = element.stringProperty("script")
    val command = action ?: if (script == null) element.name.orEmpty() else null
    val params = element.properties["params"] as? JsonObject ?: JsonObject(emptyMap())
    val handler = props.config[ExtendedActionHandlerConfigKey] as? ExtendedActionHandler
    val eventHandler = props.config[ExtendedActionEventHandlerConfigKey] as? ExtendedActionEventHandler
    val scriptHandler = props.config[ExtendedScriptHandlerConfigKey] as? ExtendedScriptHandler
    val scriptOnly = script != null && action == null
    val scriptAllowed = props.config.scriptEvaluationAllowed()
    val canDispatch = if (scriptOnly) scriptHandler != null && scriptAllowed else command != null
    val reporter = props.config[UiDiagnosticReporterConfigKey] as? UiDiagnosticReporter
    val reportedUnhandled = remember(props.path) { mutableSetOf<String>() }
    if (eventHandler != null || handler != null) reportedUnhandled.clear()
    val unhandledMessage = translateRendererText(
        props.state.jsonforms.i18n.translate, props.state.jsonforms.i18n.locale,
        "action.unhandled", "No host action handler is registered for this action.",
        presentationLocaleBundles,
    )
    val diagnosticCode = when {
        action != null && script != null -> "button.actionScriptConflict"
        scriptOnly && scriptHandler == null -> "button.scriptUnsupported"
        scriptOnly && !scriptAllowed -> "button.evaluationDisabled"
        else -> null
    }
    val diagnosticMessage = diagnosticCode?.let { code -> translateRendererText(
        props.state.jsonforms.i18n.translate, props.state.jsonforms.i18n.locale, code,
        when (code) {
            "button.actionScriptConflict" -> "Button action takes precedence over script"
            "button.evaluationDisabled" -> "Button script evaluation is disabled"
            else -> "Button script execution is unavailable on this host"
        },
        presentationLocaleBundles,
    ) }
    LaunchedEffect(reporter, diagnosticCode, diagnosticMessage, props.path) {
        if (reporter != null && diagnosticCode != null) reporter.report(UiDiagnostic(
            code = diagnosticCode,
            severity = if (action != null) UiDiagnosticSeverity.Warning else UiDiagnosticSeverity.Error,
            message = diagnosticMessage.orEmpty(),
            details = mapOf("path" to props.path),
        ))
    }
    val scope = rememberCoroutineScope()
    // A rejected host action must not cancel the scope used by later activations.
    val actionScope = remember(scope, props.path) {
        CoroutineScope(scope.coroutineContext + SupervisorJob(scope.coroutineContext[Job]))
    }
    DisposableEffect(actionScope) { onDispose { actionScope.cancel() } }
    val guard = remember(props.path) { ButtonDispatchGuard() }
    val controller = useJsonForms().controller
    val hostContext = useJsonForms().hostContext
    val pendingEdit = controller.hasPendingOwnedWork
    var pending by remember(props.path) { mutableStateOf(false) }
    val pendingDescription = translateRendererText(
        props.state.jsonforms.i18n.translate, props.state.jsonforms.i18n.locale,
        "button.running", "Running", presentationLocaleBundles,
    )
    val permitted = props.enabled && !element.booleanOption("readonly") && !element.booleanOption("disabled")
    val pendingEditDescription = if (pendingEdit) translateRendererText(
        props.state.jsonforms.i18n.translate, props.state.jsonforms.i18n.locale,
        "button.pendingEdit", "Finish the pending edit before running this action.",
        presentationLocaleBundles,
    ) else null
    return ButtonContent(
        label = label,
        icon = element.stringProperty("icon")?.takeIf(String::isNotEmpty),
        enabled = permitted && canDispatch && !pending && !pendingEdit,
        style = buttonSemanticStyle(element.stringProperty("color")),
        fullWidth = element.booleanOption("fullWidth"),
        pending = pending,
        pendingDescription = pendingDescription,
        pendingEditDescription = pendingEditDescription,
        onClick = {
            if (permitted && !guard.active && canDispatch && !controller.hasPendingOwnedWork) {
                guard.active = true
                pending = true
                actionScope.launch {
                    try {
                        if (controller.hasPendingOwnedWork) return@launch
                        val form = controller.state.jsonforms
                        val event = ExtendedActionEvent(
                                action = command.orEmpty(),
                                label = label,
                                params = params,
                                element = element,
                                path = props.path,
                                context = ExtendedActionContext(
                                    data = form.core.data,
                                    schema = form.core.schema,
                                    uischema = form.core.uischema,
                                    errors = form.core.errors,
                                    additionalErrors = form.core.additionalErrors,
                                    readonly = form.readonly,
                                    locale = form.i18n.locale,
                                    config = props.config,
                                    hostContext = hostContext,
                                ),
                            )
                        if (scriptOnly) scriptHandler?.invoke(script.orEmpty(), event)
                        else if (eventHandler != null) eventHandler.invoke(event)
                        else if (handler != null) handler.invoke(command.orEmpty(), params)
                        else if (reporter != null && reportedUnhandled.add(command.orEmpty())) reporter.report(UiDiagnostic(
                            code = "action.unhandled",
                            severity = UiDiagnosticSeverity.Warning,
                            message = unhandledMessage,
                            details = mapOf("path" to props.path, "action" to command.orEmpty(), "elementType" to "Button"),
                        ))
                    }
                    finally {
                        guard.active = false
                        pending = false
                    }
                }
            }
        },
    )
}

@Composable
internal fun ExtendedButton(props: ComposeJsonFormsRendererProps) {
    val content = buttonContent(props) ?: return
    val modifier = (if (content.fullWidth) Modifier.fillMaxWidth() else Modifier).semantics {
        if (content.pending) stateDescription = content.pendingDescription
        else content.pendingEditDescription?.let { stateDescription = it }
    }
    when (content.style) {
        ButtonSemanticStyle.Secondary -> OutlinedButton(
            enabled = content.enabled, onClick = content.onClick, modifier = modifier,
        ) { ButtonLabel(content, cupertino = false) }
        ButtonSemanticStyle.Alternative -> FilledTonalButton(
            enabled = content.enabled, onClick = content.onClick, modifier = modifier,
        ) { ButtonLabel(content, cupertino = false) }
        ButtonSemanticStyle.Warning -> OutlinedButton(
            enabled = content.enabled, onClick = content.onClick, modifier = modifier,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) { ButtonLabel(content, cupertino = false) }
        ButtonSemanticStyle.Error -> Button(
            enabled = content.enabled, onClick = content.onClick, modifier = modifier,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) { ButtonLabel(content, cupertino = false) }
        ButtonSemanticStyle.Primary, ButtonSemanticStyle.Success -> Button(
            enabled = content.enabled, onClick = content.onClick, modifier = modifier,
        ) { ButtonLabel(content, cupertino = false) }
    }
}

@OptIn(ExperimentalCupertinoApi::class)
@Composable
internal fun ExtendedCupertinoButton(props: ComposeJsonFormsRendererProps) {
    val content = buttonContent(props) ?: return
    val modifier = (if (content.fullWidth) Modifier.fillMaxWidth() else Modifier).semantics {
        if (content.pending) stateDescription = content.pendingDescription
        else content.pendingEditDescription?.let { stateDescription = it }
    }
    val palette = LocalCupertinoColors.current
    CupertinoButton(
        modifier = modifier,
        enabled = content.enabled,
        onClick = content.onClick,
        colors = when (content.style) {
            ButtonSemanticStyle.Secondary -> CupertinoButtonDefaults.tintedButtonColors()
            ButtonSemanticStyle.Alternative -> CupertinoButtonDefaults.grayButtonColors()
            ButtonSemanticStyle.Warning -> CupertinoButtonDefaults.tintedButtonColors(
                contentColor = palette.warning,
            )
            ButtonSemanticStyle.Error -> CupertinoButtonDefaults.filledButtonColors(
                contentColor = Color.White, containerColor = palette.destructive,
            )
            ButtonSemanticStyle.Primary, ButtonSemanticStyle.Success -> CupertinoButtonDefaults.filledButtonColors()
        },
    ) { ButtonLabel(content, cupertino = true) }
}

@OptIn(ExperimentalCupertinoApi::class)
@Composable
private fun ButtonLabel(content: ButtonContent, cupertino: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (content.pending) {
            if (cupertino) CupertinoActivityIndicator(
                size = 16.dp,
                color = when (content.style) {
                    ButtonSemanticStyle.Secondary, ButtonSemanticStyle.Alternative -> LocalCupertinoColors.current.accent
                    ButtonSemanticStyle.Warning -> LocalCupertinoColors.current.warning
                    else -> Color.White
                },
                modifier = Modifier.clearAndSetSemantics {},
            ) else CircularProgressIndicator(
                modifier = Modifier.size(16.dp).clearAndSetSemantics {},
                strokeWidth = 2.dp,
                color = LocalContentColor.current,
            )
            Spacer(Modifier.width(8.dp))
        }
        content.icon?.let { icon ->
            if (cupertino) CupertinoText(icon, modifier = Modifier.clearAndSetSemantics {})
            else Text(icon, modifier = Modifier.clearAndSetSemantics {})
            Spacer(Modifier.width(8.dp))
        }
        if (cupertino) CupertinoText(content.label) else Text(content.label)
    }
}

private fun UnknownUiSchemaElement.stringProperty(name: String): String? =
    (properties[name] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull

private fun UnknownUiSchemaElement.booleanOption(name: String): Boolean =
    (options[name] as? JsonPrimitive)?.takeUnless { it.isString }?.booleanOrNull == true

private fun Map<String, Any?>.scriptEvaluationAllowed(): Boolean {
    val extended = this["jsonformsExtended"].member("security")
    val enabled = extended.member("allowScriptEvaluation")
    return enabled == true || (enabled is JsonPrimitive && !enabled.isString && enabled.booleanOrNull == true)
}

private fun Any?.member(name: String): Any? = when (this) {
    is Map<*, *> -> this[name]
    is JsonObject -> this[name]
    else -> null
}
