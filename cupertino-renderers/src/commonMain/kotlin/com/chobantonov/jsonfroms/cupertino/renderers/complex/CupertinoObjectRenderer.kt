package com.chobantonov.jsonfroms.cupertino.renderers.complex

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.JsonFormsDispatchRenderer
import com.chobantonov.jsonfroms.compose.LocalObjectDetailStack
import com.chobantonov.jsonfroms.compose.objectDetailSignature
import com.chobantonov.jsonfroms.compose.useJsonForms
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticSeverity
import com.chobantonov.jsonfroms.core.i18n.defaultErrorTranslator
import com.chobantonov.jsonfroms.core.i18n.defaultTranslator
import com.chobantonov.jsonfroms.core.i18n.getCombinedErrorMessage
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfControl
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.mapStateToControlWithDetailProps
import com.chobantonov.jsonfroms.core.mappers.detailValidationScopes
import com.chobantonov.jsonfroms.core.mappers.additionalPropertyValidationScopes
import com.chobantonov.jsonfroms.core.mappers.conditionalFieldsEnabled
import com.chobantonov.jsonfroms.core.mappers.omitInactiveObjectControls
import com.chobantonov.jsonfroms.core.mappers.projectConditionalObjectFields
import com.chobantonov.jsonfroms.core.i18n.translateConditionalFieldDiagnostic
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.store.getData
import com.chobantonov.jsonfroms.core.store.getValidator
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.isObjectControl
import com.chobantonov.jsonfroms.core.testers.isAllOfControl
import com.chobantonov.jsonfroms.core.testers.isAnyOfControl
import com.chobantonov.jsonfroms.core.testers.isOneOfControl
import com.chobantonov.jsonfroms.core.util.Runtime
import com.chobantonov.jsonfroms.cupertino.renderers.layouts.CupertinoGroupLayout
import com.chobantonov.jsonfroms.cupertino.renderers.util.LocalCupertinoColors
import com.chobantonov.jsonfroms.material.renderers.complex.resolveObjectDetailUiSchema
import com.chobantonov.jsonfroms.material.renderers.complex.generatedObjectDetailUiSchema
import com.slapps.cupertino.CupertinoText
import kotlinx.serialization.json.JsonObject

val CupertinoObjectRenderer = ComposeJsonFormsRenderer { CupertinoObjectRenderer(it) }

@Composable
fun CupertinoObjectRenderer(props: ComposeJsonFormsRendererProps) {
    val control = props.uischema as? ControlElement
        ?: error("CupertinoObjectRenderer can only render Control elements")
    val detailProps = mapStateToControlWithDetailProps(
        state = props.state,
        ownProps = OwnPropsOfControl(
            controlUiSchema = control,
            own = OwnPropsOfRenderer(
                uischema = props.uischema,
                schema = props.schema,
                enabled = props.enabled,
                path = props.path,
                renderers = props.renderers,
                cells = props.cells,
                uischemas = props.state.jsonforms.uischemas,
            ),
        ),
    )
    if (!detailProps.visible) return
    com.chobantonov.jsonfroms.compose.ReportUnsupportedInputAdornments(props, "object")

    val conditional = if (conditionalFieldsEnabled(control, detailProps.config))
        projectConditionalObjectFields(detailProps.schema, detailProps.rootSchema,
            detailProps.data as? JsonObject ?: JsonObject(emptyMap()), getValidator(props.state))
    else null
    val presentationProps = conditional?.let { detailProps.copy(schema = it.activeSchema) } ?: detailProps
    val selectedUiSchema = resolveObjectDetailUiSchema(presentationProps, control)
    val stack = LocalObjectDetailStack.current
    val signature = objectDetailSignature(selectedUiSchema, detailProps.path)
    val cycle = signature in stack
    val detailUiSchema = omitInactiveObjectControls(
        if (cycle) generatedObjectDetailUiSchema(presentationProps) else selectedUiSchema,
        conditional?.inactiveProperties.orEmpty(),
    )
    val additionalProps = conditional?.let { detailProps.copy(schema = it.reservedSchema) } ?: detailProps
    val unrepresented = useJsonForms().containerErrorIndex.unrepresentedDetailErrors(
        detailValidationScopes(detailUiSchema) { element ->
            Runtime.isVisible(element, getData(props.state), getValidator(props.state), detailProps.path,
                detailProps.config)
        } + additionalPropertyValidationScopes(additionalProps.schema,
            additionalProps.data as? JsonObject ?: JsonObject(emptyMap()),
            additionalProps.config, additionalProps.rootSchema), detailProps.path)
    val i18n = props.state.jsonforms.i18n
    val unrepresentedMessage = getCombinedErrorMessage(unrepresented,
        i18n.translateError ?: defaultErrorTranslator, i18n.translate ?: defaultTranslator,
        detailProps.schema, detailUiSchema, detailProps.path)
    val reporter = detailProps.config[UiDiagnosticReporterConfigKey] as? UiDiagnosticReporter
    var reportedCycle by remember(signature) { mutableStateOf(false) }
    LaunchedEffect(reporter, cycle, signature) {
        if (cycle && !reportedCycle && reporter != null) {
            reporter.report(UiDiagnostic(
                code = "uischema.registryCycle", severity = UiDiagnosticSeverity.Warning,
                message = "Object detail re-entered the same UI schema at this data path; a generated form was used.",
                details = mapOf("path" to detailProps.path, "type" to signature.type,
                    "scope" to signature.scope),
            ))
            reportedCycle = true
        }
    }
    val detailRendererProps = props.copy(
        schema = presentationProps.schema,
        uischema = detailUiSchema,
        path = detailProps.path,
        enabled = detailProps.enabled,
        renderers = props.renderers,
        cells = props.cells,
    )
    CompositionLocalProvider(LocalObjectDetailStack provides (stack + signature)) {
        Column(Modifier.fillMaxWidth()) {
            conditional?.diagnostic?.let { diagnostic ->
                val message = translateConditionalFieldDiagnostic(i18n, diagnostic)
                CupertinoText(
                message,
                modifier = Modifier.padding(bottom = 8.dp).semantics { error(message) },
                color = LocalCupertinoColors.current.destructive,
                )
            }
            if (detailProps.errors.isNotBlank()) CupertinoText(
                detailProps.errors,
                modifier = Modifier.padding(bottom = 8.dp).semantics { error(detailProps.errors) },
                color = LocalCupertinoColors.current.destructive,
            )
            if (unrepresentedMessage.isNotBlank()) CupertinoText(
                unrepresentedMessage,
                modifier = Modifier.padding(bottom = 8.dp).semantics { error(unrepresentedMessage) },
                color = LocalCupertinoColors.current.destructive,
            )
            if (detailUiSchema is GroupElement) {
                CupertinoGroupLayout(detailRendererProps) {
                    CupertinoAdditionalProperties(props, additionalProps)
                }
            } else {
                JsonFormsDispatchRenderer(detailRendererProps)
                CupertinoAdditionalProperties(props, additionalProps)
            }
        }
    }
}

val cupertinoObjectControlTester: RankedTester = { uiSchema, schema, context ->
    if (!isObjectControl(uiSchema, schema, context)) -1
    else if (uiSchema is ControlElement && conditionalFieldsEnabled(uiSchema, context.config) &&
        isAllOfControl(uiSchema, schema, context) &&
        !isOneOfControl(uiSchema, schema, context) && !isAnyOfControl(uiSchema, schema, context)) 104
    else 102
}
