package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsCellProps
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.rememberInitialControlFocusRequester
import com.chobantonov.jsonfroms.compose.strictControlBooleanOption
import com.chobantonov.jsonfroms.core.actions.Actions
import com.chobantonov.jsonfroms.core.i18n.translateControlRequired
import com.chobantonov.jsonfroms.core.i18n.translateControlInvalidValue
import com.chobantonov.jsonfroms.core.i18n.translateRendererText
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfControl
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.computeLabel
import com.chobantonov.jsonfroms.core.mappers.mapStateToControlProps
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.JsonSchemaType
import com.chobantonov.jsonfroms.core.models.JsonSchemaTypeValue
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.and
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.schemaMatches
import com.chobantonov.jsonfroms.core.testers.uiTypeIs
import com.chobantonov.jsonfroms.core.util.JsonPath
import com.chobantonov.jsonfroms.cupertino.renderers.CupertinoCheckbox
import com.chobantonov.jsonfroms.cupertino.renderers.util.LocalCupertinoColors
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private val isNullSchema: (JsonSchema) -> Boolean = { schema ->
    schema.typeValue == JsonSchemaTypeValue.Single(JsonSchemaType.Null)
}

val nullControlTester: RankedTester = rankWith(
    2, and(uiTypeIs("Control"), schemaMatches(isNullSchema)),
)

private val nullControlLocaleBundles = mapOf(
    "bg" to JsonObject(mapOf(
        "null.set" to JsonPrimitive("Зададено на null"),
        "null.unset" to JsonPrimitive("Не е зададено"),
    )),
)

@Composable
internal fun NullControl(
    props: ComposeJsonFormsRendererProps,
    cupertino: Boolean = false,
    compact: Boolean = false,
) {
    val control = props.uischema as? ControlElement ?: return
    val stateProps = mapStateToControlProps(
        props.state,
        OwnPropsOfControl(
            id = props.id,
            controlUiSchema = control,
            own = OwnPropsOfRenderer(
                uischema = control,
                schema = props.schema,
                enabled = props.enabled,
                path = props.path,
                renderers = props.renderers,
                cells = props.cells,
            ),
        ),
    )
    if (!stateProps.visible) return

    val checked = JsonPath.get(props.state.jsonforms.core.data, stateProps.path) == JsonNull
    val enabled = stateProps.enabled && !stateProps.readonly
    val appliedOptions = stateProps.config + control.options
    var hasReceivedFocus by remember(stateProps.path) { mutableStateOf(false) }
    var touched by remember(stateProps.path) { mutableStateOf(false) }
    var focused by remember(stateProps.path) { mutableStateOf(false) }
    val focusModifier = Modifier.onFocusChanged { state ->
        focused = state.isFocused
        if (state.isFocused) hasReceivedFocus = true
        else if (hasReceivedFocus) touched = true
    }
    val error = if (touched) stateProps.errors else stateProps.errorsBeforeTouch
    val visibleDescription = stateProps.description?.takeIf {
        !compact && it.isNotBlank() && (focused ||
            strictControlBooleanOption(appliedOptions, "showUnfocusedDescription") == true)
    }
    val requiredState = translateControlRequired(props.state.jsonforms.i18n)
        .takeIf { stateProps.required }
    val primaryFocusRequester = rememberInitialControlFocusRequester(
        stateProps.path, appliedOptions, enabled)
    val translator = props.state.jsonforms.i18n.translate
    val invalidStatus = stateProps.data.takeIf { stateProps.dataProvided && it != JsonNull }
        ?.let { "${translateControlInvalidValue(props.state.jsonforms.i18n)}: $it" }
    val status = invalidStatus ?: translateRendererText(
        translator,
        props.state.jsonforms.i18n.locale,
        if (checked) "null.set" else "null.unset",
        if (checked) "Set to null" else "Not set",
        nullControlLocaleBundles,
        i18nKeyPrefix = stateProps.i18nKeyPrefix,
    )
    val hideAsterisk = strictControlBooleanOption(appliedOptions, "hideRequiredAsterisk") == true
    val label = computeLabel(stateProps.label, stateProps.required, hideAsterisk)
    val accessibleState = listOfNotNull(requiredState, invalidStatus, visibleDescription).joinToString(". ")
    val accessibleError = listOfNotNull(invalidStatus, error.takeIf { it.isNotBlank() })
        .distinct().joinToString(". ")
    val checkboxModifier = Modifier.focusRequester(primaryFocusRequester).then(focusModifier)
        .semantics {
            if (accessibleState.isNotBlank()) stateDescription = accessibleState
            if (accessibleError.isNotBlank()) error(accessibleError)
            if (compact && accessibleError.isNotBlank()) contentDescription =
                listOf(stateProps.label, status, error).filter(String::isNotBlank).joinToString(". ")
        }
    val errorColor = if (cupertino) LocalCupertinoColors.current.destructive else MaterialTheme.colorScheme.error
    val onCheckedChange: (Boolean) -> Unit = { next ->
        props.dispatch(Actions.update(stateProps.path, updater = { if (next) JsonNull else null }))
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!compact && label.isNotBlank()) Text(label)
            if (cupertino) {
                CupertinoCheckbox(checked, onCheckedChange, modifier = checkboxModifier, enabled = enabled)
            } else {
                Checkbox(checked = checked, onCheckedChange = onCheckedChange, modifier = checkboxModifier, enabled = enabled)
            }
            Text(status)
            if (compact && accessibleError.isNotBlank()) CompactFeedbackIcon(accessibleError, errorColor, cupertino)
        }
        if (visibleDescription != null) Text(visibleDescription)
        if (!compact && error.isNotBlank()) Text(error, color = errorColor, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun NullCell(props: ComposeJsonFormsCellProps, cupertino: Boolean) {
    NullControl(
        ComposeJsonFormsRendererProps(
            state = props.state,
            dispatch = props.dispatch,
            uischema = props.uischema,
            schema = props.schema,
            rootSchema = props.rootSchema,
            path = props.path,
            enabled = props.enabled,
            renderers = props.renderers,
            cells = props.cells,
            id = props.id,
            config = props.config,
        ),
        cupertino = cupertino,
        compact = true,
    )
}
