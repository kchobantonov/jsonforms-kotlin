package com.chobantonov.jsonfroms.cupertino.renderers.controls

import com.chobantonov.jsonfroms.core.i18n.translateControlClear
import com.chobantonov.jsonfroms.core.i18n.translateControlRequired
import com.chobantonov.jsonfroms.core.i18n.translateRadioUnknownValue

import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.requestFocus
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsCellProps
import com.chobantonov.jsonfroms.compose.asRendererProps
import com.chobantonov.jsonfroms.compose.rememberInitialControlFocusRequester
import com.chobantonov.jsonfroms.compose.strictControlBooleanOption
import com.chobantonov.jsonfroms.compose.resolvedControlBooleanOption
import com.chobantonov.jsonfroms.compose.rememberClearAffordanceState
import com.chobantonov.jsonfroms.compose.JsonFormsActionIcons
import com.chobantonov.jsonfroms.compose.rememberPendingEditOwner
import com.chobantonov.jsonfroms.compose.useJsonForms
import com.chobantonov.jsonfroms.cupertino.renderers.ActionTooltip
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfControl
import com.chobantonov.jsonfroms.core.util.jsonSchemaChoiceEquals
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfEnum
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.DispatchPropsOfControl
import com.chobantonov.jsonfroms.core.mappers.StatePropsOfControlWithOptions
import com.chobantonov.jsonfroms.core.mappers.ControlProps
import com.chobantonov.jsonfroms.core.mappers.accessibleChoiceLabel
import com.chobantonov.jsonfroms.core.mappers.computeLabel
import com.chobantonov.jsonfroms.core.mappers.mapDispatchToControlProps
import com.chobantonov.jsonfroms.core.mappers.mapStateToEnumControlProps
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.store.Dispatch
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.withIncreasedRank
import com.chobantonov.jsonfroms.material.renderers.controls.materialEnumControlTester
import com.chobantonov.jsonfroms.core.util.JsonPath
import com.chobantonov.jsonfroms.core.util.resolveInputAdornments
import com.chobantonov.jsonfroms.cupertino.renderers.util.LocalCupertinoColors
import com.chobantonov.jsonfroms.core.i18n.translateControlLocalError
import com.chobantonov.jsonfroms.core.util.resolveAutocomplete
import com.slapps.cupertino.CupertinoDropdownMenu
import com.slapps.cupertino.ExperimentalCupertinoApi
import com.slapps.cupertino.MenuPickerAction
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.booleanOrNull

val CupertinoEnumControl = ComposeJsonFormsRenderer { CupertinoEnumControl(it) }

@Composable
internal fun CupertinoPlainEnumCell(props: ComposeJsonFormsCellProps) =
    CupertinoEnumControl(props.asRendererProps(), compact = true)

@OptIn(ExperimentalCupertinoApi::class)
@Composable
fun CupertinoEnumControl(props: ComposeJsonFormsRendererProps, compact: Boolean = false) {
    val control = props.uischema as? ControlElement ?: error("CupertinoEnumControl can only render Control elements")
    val stateProps = mapStateToEnumControlProps(
        state = props.state,
        ownProps = OwnPropsOfControl(
            id = props.id,
            controlUiSchema = control,
            own = OwnPropsOfRenderer(
                uischema = props.uischema,
                schema = props.schema,
                enabled = props.enabled,
                readonly = props.readonly,
                path = props.path,
                renderers = props.renderers,
                cells = props.cells,
            ),
        ),
        enumProps = OwnPropsOfEnum(),
    )
    if (!stateProps.visible) return
    val dispatch = mapDispatchToControlProps(Dispatch { action -> props.dispatch(action); action })
    val clearLabel = translateControlClear(props.state.jsonforms.i18n)
    CupertinoSelectControl(stateProps, dispatch, clearLabel,
        hasValue = JsonPath.get(props.state.jsonforms.core.data, stateProps.path) != null,
        compact = compact)
}

@OptIn(ExperimentalCupertinoApi::class)
@Composable
internal fun CupertinoSelectControl(
    stateProps: StatePropsOfControlWithOptions,
    dispatch: DispatchPropsOfControl,
    clearLabel: String,
    hasValue: Boolean,
    compact: Boolean = false,
) {
    if (resolveAutocomplete(stateProps.uischema.options["autocomplete"], stateProps.config)) {
        CupertinoSearchableSelectControl(stateProps, dispatch, clearLabel, compact)
        return
    }
    val selected = stateProps.options.firstOrNull { jsonSchemaChoiceEquals(it.value, stateProps.data) }
    val unknownStatus = stateProps.data.takeIf { stateProps.dataProvided && selected == null }
        ?.let { "${translateRadioUnknownValue(useJsonForms().state.jsonforms.i18n)}: $it" }
    val enabled = stateProps.enabled && !stateProps.readonly && stateProps.options.isNotEmpty()
    val focusRequester = rememberInitialControlFocusRequester(stateProps.path,
        stateProps.config + stateProps.uischema.options, enabled)
    val canClear = hasValue && enabled &&
        resolvedControlBooleanOption(stateProps.uischema.options, stateProps.config, "clearable") != false
    val translator = useJsonForms().state.jsonforms.i18n.translate
    val startAdornments = resolveInputAdornments(stateProps.uischema.options, "startAdornment", translator)
    val endAdornments = resolveInputAdornments(stateProps.uischema.options, "endAdornment", translator)
    val colors = LocalCupertinoColors.current
    val hideAsterisk = strictControlBooleanOption(stateProps.config + stateProps.uischema.options,
        "hideRequiredAsterisk") ?: false
    val requiredState = translateControlRequired(useJsonForms().state.jsonforms.i18n)
        .takeIf { stateProps.required }
    var expanded by remember(stateProps.path) { mutableStateOf(false) }
    var touched by remember(stateProps.path) { mutableStateOf(false) }
    var selectorFocused by remember(stateProps.path) { mutableStateOf(false) }
    var selectorHovered by remember(stateProps.path) { mutableStateOf(false) }
    var clearFocused by remember(stateProps.path) { mutableStateOf(false) }
    var clearHovered by remember(stateProps.path) { mutableStateOf(false) }
    val clearFocusRequester = remember(stateProps.path) { FocusRequester() }
    val clearAffordance = rememberClearAffordanceState(stateProps.path)
    val showClear = canClear && (selectorFocused || selectorHovered || clearFocused || clearHovered ||
        clearAffordance.isVisible())
    val clearModifier = Modifier.size(28.dp)
        .onFocusChanged { clearFocused = it.isFocused }
        .pointerInput(stateProps.path) {
            awaitPointerEventScope {
                while (true) {
                    when (awaitPointerEvent().type) {
                        PointerEventType.Enter -> clearHovered = true
                        PointerEventType.Exit -> clearHovered = false
                    }
                }
            }
        }
    val visibleError = if (touched) stateProps.errors else stateProps.errorsBeforeTouch
    val showUnfocusedDescription = strictControlBooleanOption(
        stateProps.config + stateProps.uischema.options, "showUnfocusedDescription") == true
    val visibleDescription = stateProps.description?.takeIf {
        !compact &&
        it.isNotBlank() && (selectorFocused || showUnfocusedDescription)
    }
    val accessibleState = listOfNotNull(requiredState, unknownStatus, visibleDescription).joinToString(". ")
    val accessibleError = listOfNotNull(unknownStatus, visibleError.takeIf(String::isNotBlank))
        .distinct().joinToString(". ")
    val displayedValue = selected?.label ?:
        if (unknownStatus != null && stateProps.data == JsonNull) "null"
        else stateProps.data.toCupertinoInputString()
    val borderColor = if (accessibleError.isNotBlank()) colors.destructive else colors.controlBorder

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (!compact && stateProps.label.isNotBlank()) {
            Text(
                computeLabel(stateProps.label, stateProps.required, hideAsterisk),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.secondaryLabel,
            )
        }
        Box {
            Row(
                Modifier.fillMaxWidth().then(if (compact) Modifier.testTag("cupertino-enum-cell") else Modifier)
                    .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                    .onFocusChanged {
                        if (it.isFocused) clearAffordance.selectField()
                        selectorFocused = it.hasFocus
                    }
                    .focusRequester(focusRequester)
                    .clickable(enabled) { clearAffordance.selectField(); expanded = !expanded }
                    .semantics {
                        if (accessibleState.isNotBlank()) stateDescription = accessibleState
                        if (accessibleError.isNotBlank()) error(accessibleError)
                        requestFocus { clearAffordance.selectField(); focusRequester.requestFocus(); true }
                    }
                    .pointerInput(stateProps.path) {
                        awaitPointerEventScope {
                            while (true) {
                                when (awaitPointerEvent().type) {
                                    PointerEventType.Press -> clearAffordance.selectField()
                                    PointerEventType.Enter -> selectorHovered = true
                                    PointerEventType.Exit -> selectorHovered = false
                                }
                            }
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CupertinoAdornments(startAdornments)
                Text(
                    displayedValue,
                    modifier = Modifier.weight(1f),
                    color = if (enabled) colors.label else colors.secondaryLabel,
                )
                if (showClear) {
                    ActionTooltip(clearLabel) {
                        IconButton(
                            onClick = { dispatch.handleChange(stateProps.path, null); expanded = false },
                            enabled = enabled,
                            modifier = clearModifier.focusRequester(clearFocusRequester).focusable()
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyUp &&
                                        (event.key == Key.Enter || event.key == Key.Spacebar)) {
                                        dispatch.handleChange(stateProps.path, null)
                                        expanded = false
                                        true
                                    } else false
                                }.semantics {
                                focused = clearFocused
                                requestFocus { clearFocusRequester.requestFocus(); true }
                            },
                        ) {
                            Icon(JsonFormsActionIcons.Clear, clearLabel, tint = colors.secondaryLabel, modifier = Modifier.size(15.dp))
                        }
                    }
                } else if (canClear) Spacer(Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                CupertinoDropdownChevron(
                    expanded = expanded,
                    color = colors.secondaryLabel,
                    modifier = Modifier.testTag("choice-dropdown-arrow"),
                )
                CupertinoAdornments(endAdornments)
            }
            CupertinoDropdownMenu(
                expanded = expanded && enabled,
                onDismissRequest = { expanded = false; touched = true },
            ) {
                stateProps.options.forEach { option ->
                    MenuPickerAction(
                        isSelected = option == selected,
                        modifier = Modifier.semantics {
                            contentDescription = accessibleChoiceLabel(option, stateProps.options)
                        },
                        onClick = {
                            dispatch.handleChange(stateProps.path, option.value)
                            expanded = false
                            touched = true
                        },
                        title = { Text(option.label) },
                    )
                }
            }
        }
        if (unknownStatus != null) Text(unknownStatus,
            style = MaterialTheme.typography.bodySmall, color = colors.destructive)
        if (!compact && visibleError.isNotBlank()) Text(visibleError,
            style = MaterialTheme.typography.bodySmall, color = colors.destructive)
        visibleDescription?.let { Text(it,
            style = MaterialTheme.typography.bodySmall, color = colors.secondaryLabel) }
    }
}

@OptIn(ExperimentalCupertinoApi::class)
@Composable
private fun CupertinoSearchableSelectControl(
    stateProps: StatePropsOfControlWithOptions,
    dispatch: DispatchPropsOfControl,
    clearLabel: String,
    compact: Boolean = false,
) {
    val selected = stateProps.options.firstOrNull { jsonSchemaChoiceEquals(it.value, stateProps.data) }
    val unknownStatus = stateProps.data.takeIf { stateProps.dataProvided && selected == null }
        ?.let { "${translateRadioUnknownValue(useJsonForms().state.jsonforms.i18n)}: $it" }
    val committedText = selected?.label ?:
        if (unknownStatus != null && stateProps.data == JsonNull) "null"
        else stateProps.data.toCupertinoInputString()
    var query by remember(stateProps.path) { mutableStateOf(committedText) }
    var observedData by remember(stateProps.path) { mutableStateOf(stateProps.data) }
    var observedText by remember(stateProps.path) { mutableStateOf(committedText) }
    var expanded by remember(stateProps.path) { mutableStateOf(false) }
    val setPending = rememberPendingEditOwner("cupertino-enum:${stateProps.path}:${stateProps.id}")
    LaunchedEffect(stateProps.data, committedText) {
        if (stateProps.data != observedData || committedText != observedText) {
            query = committedText
            observedData = stateProps.data
            observedText = committedText
            expanded = false
            setPending(false)
        }
    }
    LaunchedEffect(stateProps.enabled, stateProps.readonly) {
        if (!stateProps.enabled || stateProps.readonly) {
            query = committedText
            expanded = false
            setPending(false)
        }
    }
    val matches = stateProps.options.filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }
    val localError = translateControlLocalError(useJsonForms().state.jsonforms.i18n,
        "enum.invalidOption", "Select a valid option.")
        .takeIf { query.isNotEmpty() && query != selected?.label }
    val controlProps = ControlProps(
        config = stateProps.config,
        uischema = stateProps.uischema,
        schema = stateProps.schema,
        data = stateProps.data,
        enabled = stateProps.enabled,
        readonly = stateProps.readonly,
        visible = stateProps.visible,
        path = stateProps.path,
        renderers = stateProps.renderers,
        cells = stateProps.cells,
        errors = stateProps.errors,
        errorsBeforeTouch = stateProps.errorsBeforeTouch,
        rootSchema = stateProps.rootSchema,
        id = stateProps.id,
        label = stateProps.label,
        description = stateProps.description,
        required = stateProps.required,
        i18nKeyPrefix = stateProps.i18nKeyPrefix,
        handleChange = { path, value -> dispatch.handleChange(path, value) },
    )
    Box {
        CupertinoInputControl(
            props = controlProps,
            value = query,
            localError = localError,
            storedValueStatus = unknownStatus,
            compact = compact,
            inputModifier = if (compact) Modifier.testTag("cupertino-enum-cell") else Modifier,
            clearLabel = clearLabel,
        ) { value ->
            query = value
            expanded = value.isNotBlank() && stateProps.options.any {
                it.label.contains(value, ignoreCase = true)
            }
            if (value.isEmpty()) {
                dispatch.handleChange(stateProps.path, null)
                setPending(false)
            } else {
                setPending(value != committedText)
            }
        }
        CupertinoDropdownMenu(
            expanded = expanded && matches.isNotEmpty() && stateProps.enabled && !stateProps.readonly,
            onDismissRequest = { expanded = false },
        ) {
            matches.forEach { option ->
                MenuPickerAction(
                    isSelected = option == selected,
                    modifier = Modifier.semantics {
                        contentDescription = accessibleChoiceLabel(option, stateProps.options)
                    },
                    onClick = {
                        query = option.label
                        dispatch.handleChange(stateProps.path, option.value)
                        expanded = false
                        setPending(false)
                    },
                    title = { Text(option.label) },
                )
            }
        }
    }
}

@Composable
internal fun CupertinoDropdownChevron(expanded: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(14.dp).rotate(if (expanded) 180f else 0f)) {
        val strokeWidth = 1.5.dp.toPx()
        drawLine(
            color = color,
            start = Offset(size.width * .2f, size.height * .38f),
            end = Offset(size.width * .5f, size.height * .68f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * .5f, size.height * .68f),
            end = Offset(size.width * .8f, size.height * .38f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

val cupertinoEnumControlTester: RankedTester = withIncreasedRank(100, materialEnumControlTester)
