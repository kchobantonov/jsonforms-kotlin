package com.chobantonov.jsonfroms.material.renderers.complex

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfControl
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.StatePropsOfControl
import com.chobantonov.jsonfroms.core.mappers.mapStateToControlProps
import com.chobantonov.jsonfroms.core.mappers.validationSchemaForCombinatorBranch
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsCore
import com.chobantonov.jsonfroms.core.store.ValidationMode
import com.chobantonov.jsonfroms.core.util.JsonPath
import com.chobantonov.jsonfroms.core.util.JsonPointer
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

data class ActiveBranchFeedback(
    val props: ComposeJsonFormsRendererProps,
    val errors: String,
    val errorsBeforeTouch: String,
)

/** Add active-branch errors only to the rendered subtree, never to document validity. */
@Composable
fun activeBranchFeedback(
    props: ComposeJsonFormsRendererProps,
    control: ControlElement,
    controlProps: StatePropsOfControl,
    branch: JsonSchema,
): ActiveBranchFeedback {
    val path = controlProps.path
    fun unchanged() = ActiveBranchFeedback(props, controlProps.errors, controlProps.errorsBeforeTouch)
    val enabled = (control.options["validateActiveBranch"].asBoolean()
        ?: props.config["validateActiveBranch"].asBoolean()) != false
    val core = props.state.jsonforms.core
    val data = JsonPath.get(core.data, path)
    if (!enabled || core.validationMode != ValidationMode.ValidateAndShow || data == null) return unchanged()

    val validationSchema = remember(branch, props.rootSchema) {
        validationSchemaForCombinatorBranch(branch, props.rootSchema)
    }
    val validator = remember(core.validatorEngine, validationSchema) {
        runCatching { core.validatorEngine.compile(validationSchema) }.getOrNull()
    } ?: return unchanged()
    val branchErrors = remember(validator, data, path) {
        val prefix = path.split('.').filter(String::isNotEmpty)
            .joinToString(separator = "/", prefix = if (path.isBlank()) "" else "/") {
                JsonPointer.encode(it)
            }
        val errors = runCatching { validator.validate(data) }.getOrDefault(emptyList())
        if (errors.any { it.keyword == "\$schema" &&
                it.message.startsWith("Unable to compile JSON Schema") }) emptyList()
        else errors.map { error ->
            error.copy(instancePath = prefix + error.instancePath, dataPath = null)
        }
    }
    if (branchErrors.isEmpty()) return unchanged()
    val localCore = object : JsonFormsCore by core {
        override val errors: List<ErrorObject> = core.errors + branchErrors.filterNot { local ->
            core.errors.any { document -> document.instancePath == local.instancePath &&
                document.keyword == local.keyword && document.message == local.message }
        }
    }
    val localProps = props.copy(state = props.state.copy(
        jsonforms = props.state.jsonforms.copy(core = localCore),
    ))
    val own = mapStateToControlProps(localProps.state, OwnPropsOfControl(
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
    ))
    return ActiveBranchFeedback(localProps, own.errors, own.errorsBeforeTouch)
}

private fun Any?.asBoolean(): Boolean? = when (this) {
    is Boolean -> this
    is JsonPrimitive -> booleanOrNull
    else -> null
}
