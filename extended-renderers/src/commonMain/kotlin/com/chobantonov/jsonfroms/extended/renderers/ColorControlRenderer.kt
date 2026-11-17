package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import kotlin.math.roundToInt
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.rememberInitialControlFocusRequester
import com.chobantonov.jsonfroms.compose.rememberPendingEditOwner
import com.chobantonov.jsonfroms.compose.strictControlBooleanOption
import com.chobantonov.jsonfroms.core.actions.Actions
import com.chobantonov.jsonfroms.core.i18n.translateControlClear
import com.chobantonov.jsonfroms.core.i18n.translateControlInvalidValue
import com.chobantonov.jsonfroms.core.mappers.invalidStringStoredValue
import com.chobantonov.jsonfroms.core.i18n.translateControlRequired
import com.chobantonov.jsonfroms.core.i18n.translateRendererText
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.mappers.ControlProps
import com.chobantonov.jsonfroms.core.mappers.computeLabel
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfControl
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.mapStateToControlProps
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.and
import com.chobantonov.jsonfroms.core.testers.formatIs
import com.chobantonov.jsonfroms.core.testers.isStringControl
import com.chobantonov.jsonfroms.core.testers.optionIs
import com.chobantonov.jsonfroms.core.testers.or
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.util.composeWithUi
import com.chobantonov.jsonfroms.core.validation.ColorChannelView
import com.chobantonov.jsonfroms.core.validation.ColorSaveFormat
import com.chobantonov.jsonfroms.core.validation.RgbaColor
import com.chobantonov.jsonfroms.core.validation.colorSaveFormat
import com.chobantonov.jsonfroms.core.validation.editColorChannel
import com.chobantonov.jsonfroms.core.validation.initialColorView
import com.chobantonov.jsonfroms.core.validation.parseColor
import com.chobantonov.jsonfroms.core.validation.rgbToHsb
import com.chobantonov.jsonfroms.core.validation.serializeColor
import com.chobantonov.jsonfroms.cupertino.renderers.controls.CupertinoInputControl
import com.chobantonov.jsonfroms.material.renderers.controls.MaterialInputControl
import com.slapps.cupertino.CupertinoButton
import com.slapps.cupertino.CupertinoButtonDefaults
import com.slapps.cupertino.CupertinoSlider
import com.slapps.cupertino.CupertinoText
import com.slapps.cupertino.ExperimentalCupertinoApi
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.JsonObject

val colorControlTester: RankedTester = rankWith(
    3, and(isStringControl, or(formatIs("color"), optionIs("format", "color"))),
)

@OptIn(ExperimentalCupertinoApi::class)
@Composable
private fun ColorPickerAction(
    cupertino: Boolean, label: String, modifier: Modifier = Modifier,
    selected: Boolean = false, onClick: () -> Unit,
) {
    if (cupertino) CupertinoButton(modifier = modifier, onClick = onClick,
        colors = if (selected) CupertinoButtonDefaults.filledButtonColors()
            else CupertinoButtonDefaults.plainButtonColors()) { CupertinoText(label) }
    else if (selected) Button(modifier = modifier, onClick = onClick) { Text(label) }
    else OutlinedButton(modifier = modifier, onClick = onClick) { Text(label) }
}

@OptIn(ExperimentalCupertinoApi::class)
@Composable
private fun ColorPickerSlider(
    cupertino: Boolean, value: Float, modifier: Modifier, onValueChange: (Float) -> Unit,
) {
    if (cupertino) CupertinoSlider(value = value, onValueChange = onValueChange,
        modifier = modifier, valueRange = 0f..1f)
    else Slider(value = value, onValueChange = onValueChange, modifier = modifier)
}

@OptIn(ExperimentalCupertinoApi::class)
@Composable
internal fun ColorControl(props: ComposeJsonFormsRendererProps, cupertino: Boolean,
    compact: Boolean = false) {
    val control = props.uischema as? ControlElement ?: return
    val value = com.chobantonov.jsonfroms.core.util.JsonPath.get(
        props.state.jsonforms.core.data,
        composeWithUi(control, props.path),
    ) as? JsonPrimitive
    val parsed = parseColor(value?.contentOrNull)
    val color = parsed?.let { Color(it.r / 255f, it.g / 255f, it.b / 255f, it.a.toFloat()) }
    val stateProps = mapStateToControlProps(props.state, OwnPropsOfControl(
        id = props.id, controlUiSchema = control,
        own = OwnPropsOfRenderer(uischema = control, schema = props.schema,
            enabled = props.enabled, path = props.path, renderers = props.renderers,
            cells = props.cells),
    ))
    if (!stateProps.visible) return
    val inputProps = ControlProps(
        config = stateProps.config, uischema = stateProps.uischema,
        schema = stateProps.schema, data = stateProps.data,
        enabled = stateProps.enabled, readonly = stateProps.readonly,
        visible = stateProps.visible, path = stateProps.path,
        renderers = stateProps.renderers, cells = stateProps.cells,
        errors = stateProps.errors, errorsBeforeTouch = stateProps.errorsBeforeTouch,
        rootSchema = stateProps.rootSchema, id = stateProps.id,
        label = stateProps.label, description = stateProps.description,
        required = stateProps.required, i18nKeyPrefix = stateProps.i18nKeyPrefix,
        handleChange = { changedPath, changedValue ->
            props.dispatch(Actions.update(changedPath, updater = { changedValue }))
        },
    )
    val extended = props.config["jsonformsExtended"]
    fun globalOption(key: String): Any? = when (extended) {
        is Map<*, *> -> extended[key]
        is JsonObject -> extended[key]
        else -> null
    }
    fun option(key: String): Any? = control.options[key] ?: globalOption(key)
    fun stringOption(key: String): String? = when (val raw = option(key)) {
        is JsonPrimitive -> raw.contentOrNull
        is String -> raw
        else -> null
    }
    fun booleanOption(key: String): Boolean? = when (val raw = option(key)) {
        is JsonPrimitive -> raw.booleanOrNull
        is Boolean -> raw
        else -> null
    }
    val format = colorSaveFormat(stringOption("colorSaveFormat"))
    val textEntry = booleanOption("colorTextEntry") != false
    val editable = stateProps.enabled && !stateProps.readonly
    val appliedOptions = stateProps.config + control.options
    val showUnfocusedDescription = strictControlBooleanOption(
        appliedOptions, "showUnfocusedDescription") == true
    val hideRequiredAsterisk = strictControlBooleanOption(
        appliedOptions, "hideRequiredAsterisk") == true
    var pickerFocused by remember(stateProps.path) { mutableStateOf(false) }
    var pickerTouched by remember(stateProps.path) { mutableStateOf(false) }
    var pickerHasReceivedFocus by remember(stateProps.path) { mutableStateOf(false) }
    val visibleDescription = stateProps.description?.takeIf {
        it.isNotBlank() && (pickerFocused || showUnfocusedDescription)
    }
    val visibleError = if (pickerTouched) stateProps.errors else stateProps.errorsBeforeTouch
    val pickerFocusRequester = rememberInitialControlFocusRequester(
        stateProps.path, appliedOptions, !textEntry && editable)
    val i18n = props.state.jsonforms.i18n
    val invalidStatus = invalidStringStoredValue(stateProps.data, stateProps.dataProvided)
        ?.let { "${translateControlInvalidValue(i18n)}: $it" }
    val requiredState = translateControlRequired(i18n).takeIf { stateProps.required }
    val pickerAccessibleState = listOfNotNull(requiredState, invalidStatus, visibleDescription).joinToString(". ")
    fun colorText(key: String, fallback: String,
        bundles: Map<String, JsonObject> = colorControlLocaleBundles,
    ): String = translateRendererText(i18n.translate, i18n.locale, "color.$key", fallback,
        bundles, i18nKeyPrefix = stateProps.i18nKeyPrefix)
    val importedColorError = if (!textEntry && !value?.contentOrNull.isNullOrEmpty() && parsed == null)
        colorText("invalid", "Invalid color.") else null
    val pickerError = listOfNotNull(invalidStatus, importedColorError,
        visibleError.takeIf { it.isNotBlank() }).distinct().joinToString(". ")
    val clearable = (control.options["clearable"] as? JsonPrimitive)?.booleanOrNull
        ?: (stateProps.config["clearable"] as? Boolean)
        ?: (stateProps.config["clearable"] as? JsonPrimitive)?.booleanOrNull
        ?: true
    val textInputProps = inputProps.copy(uischema = control.copy(
        options = control.options + ("clearable" to JsonPrimitive(clearable))))
    val path = composeWithUi(control, props.path)
    var textDraft by remember(path) { mutableStateOf(value?.contentOrNull.orEmpty()) }
    var textDraftDirty by remember(path) { mutableStateOf(false) }
    val setPending = rememberPendingEditOwner("color:$path:${props.id}")
    var pickerOpen by remember(path) { mutableStateOf(false) }
    var view by remember(path) { mutableStateOf(initialColorView(format)) }
    var draft by remember(path) { mutableStateOf<Map<String, String>>(emptyMap()) }
    var problem by remember(path) { mutableStateOf<String?>(null) }
    var ownCommit by remember(path) { mutableStateOf<String?>(null) }
    var observedValue by remember(path) { mutableStateOf(value?.contentOrNull) }
    var observedFormat by remember(path) { mutableStateOf(format) }
    LaunchedEffect(value?.contentOrNull, format, editable) {
        if (!editable) pickerOpen = false
        if (format != observedFormat) {
            view = initialColorView(format)
            draft = emptyMap()
            problem = null
        }
        if (observedValue != value?.contentOrNull && ownCommit != value?.contentOrNull) {
            pickerOpen = false
            draft = emptyMap()
            problem = null
            textDraft = value?.contentOrNull.orEmpty()
            textDraftDirty = false
        }
        observedValue = value?.contentOrNull
        observedFormat = format
        ownCommit = null
    }
    fun save(next: RgbaColor) {
        val encoded = serializeColor(next, format)
        if (encoded == null) {
            problem = "hex3Transparency"
            return
        }
        problem = null
        ownCommit = encoded
        textDraft = encoded
        textDraftDirty = false
        props.dispatch(Actions.update(path = path, updater = { JsonPrimitive(encoded) }))
    }
    val textError = when {
        textDraft.isEmpty() -> null
        parseColor(textDraft) == null -> colorText("invalid", "Invalid color.")
        serializeColor(parseColor(textDraft)!!, format) == null ->
            colorText("hex3Transparency", "Transparency cannot be saved as #RGB.")
        else -> null
    }
    LaunchedEffect(textEntry, textError, textDraftDirty) {
        setPending(textEntry && textDraftDirty && textError != null)
    }
    val onTextChange: (String) -> Unit = { next ->
        textDraft = next
        if (next.isEmpty()) {
            textDraftDirty = false
            ownCommit = null
            inputProps.handleChange(path, null)
        } else {
            val nextColor = parseColor(next)
            if (nextColor == null || serializeColor(nextColor, format) == null) textDraftDirty = true
            else save(nextColor)
        }
    }
    @Composable fun pickerField(
        key: String, label: String, value: String, accessibleName: String,
        modifier: Modifier, numeric: Boolean = false, onChange: (String) -> Unit,
    ) {
        val fieldModifier = Modifier.fillMaxWidth().semantics { contentDescription = accessibleName }
        if (cupertino) Box(modifier) {
            CupertinoInputControl(
                props = textInputProps.copy(
                    config = textInputProps.config - "focus",
                    uischema = control.copy(options = control.options - "focus" +
                        ("clearable" to JsonPrimitive(false))),
                    path = "$path.__color_$key", label = label,
                    description = null, required = false, errors = "", errorsBeforeTouch = "",
                ),
                value = value,
                keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text,
                inputModifier = fieldModifier,
                onChange = onChange,
            )
        } else OutlinedTextField(value = value, onValueChange = onChange,
            label = { Text(label) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text),
            modifier = modifier.semantics { contentDescription = accessibleName })
    }
    val togglePicker = {
        if (!pickerOpen) {
            view = initialColorView(format)
            draft = emptyMap()
            problem = null
        }
        pickerOpen = !pickerOpen
    }
    @Composable fun compactPickerAction() {
        val accessibleName = if (pickerOpen) colorText("close", "Close")
            else colorText("choose", "Choose color")
        val actionModifier = Modifier.semantics { contentDescription = accessibleName }
        if (cupertino) CupertinoButton(onClick = togglePicker, modifier = actionModifier,
            colors = CupertinoButtonDefaults.plainButtonColors()) { CupertinoText("⋯") }
        else IconButton(onClick = togglePicker, modifier = actionModifier) { Text("⋯") }
    }
    Column(
        modifier = if (!textEntry) Modifier.onFocusChanged { focusState ->
            pickerFocused = focusState.hasFocus
            if (focusState.hasFocus) pickerHasReceivedFocus = true
            else if (pickerHasReceivedFocus) pickerTouched = true
        } else Modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!textEntry && stateProps.label.isNotBlank()) {
            Text(computeLabel(stateProps.label, stateProps.required, hideRequiredAsterisk))
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!compact || !textEntry) Box(Modifier.size(36.dp).background(
                color ?: Color.Transparent, RoundedCornerShape(6.dp)))
            Box(Modifier.weight(1f)) {
                if (textEntry) {
                    if (cupertino) CupertinoInputControl(textInputProps, textDraft,
                        localError = textError, storedValueStatus = invalidStatus,
                        compact = compact,
                        trailingContent = if (compact && editable) ({ compactPickerAction() }) else null,
                        clearLabel = translateControlClear(props.state.jsonforms.i18n),
                        onChange = onTextChange)
                    else MaterialInputControl(textInputProps, textDraft,
                        localError = textError, storedValueStatus = invalidStatus,
                        compact = compact,
                        trailingIcon = if (compact && editable) ({ compactPickerAction() }) else null,
                        clearLabel = translateControlClear(props.state.jsonforms.i18n),
                        onChange = onTextChange)
                }
                else if (cupertino) CupertinoText(value?.contentOrNull.orEmpty())
                else Text(value?.contentOrNull.orEmpty())
            }
            if (compact && !textEntry && editable) compactPickerAction()
        }
        if (importedColorError != null) {
            if (cupertino) CupertinoText(importedColorError) else Text(importedColorError)
        }
        if (!textEntry && invalidStatus != null) {
            if (cupertino) CupertinoText(invalidStatus) else Text(invalidStatus)
        }
        if (editable && !compact) {
            ColorPickerAction(cupertino = cupertino, label = if (pickerOpen) {
                colorText("close", "Close")
            } else colorText("choose", "Choose color"),
                modifier = if (!textEntry) Modifier.focusRequester(pickerFocusRequester).semantics {
                    if (pickerAccessibleState.isNotBlank()) stateDescription = pickerAccessibleState
                    if (pickerError.isNotBlank()) error(pickerError)
                } else Modifier,
                onClick = togglePicker)
        }
        if (editable && pickerOpen) {
            if (!textEntry && clearable && value != null) {
                ColorPickerAction(cupertino = cupertino,
                    label = colorText("clear", "Clear"),
                    onClick = { props.dispatch(Actions.update(path = path, updater = { null })) })
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("#ef4444", "#f59e0b", "#22c55e", "#3b82f6", "#8b5cf6", "#ffffff", "#000000")
                    .forEach { hex ->
                        val swatch = parseColor(hex)!!
                        val swatchModifier = Modifier.semantics {
                            contentDescription = colorText("swatch", "Color $hex",
                                colorSwatchLocaleBundles(hex))
                        }
                        @Composable fun swatchContent() {
                            Box(Modifier.size(20.dp).background(Color(swatch.r / 255f,
                                swatch.g / 255f, swatch.b / 255f)))
                        }
                        if (cupertino) CupertinoButton(modifier = swatchModifier,
                            onClick = { save(swatch) },
                            colors = CupertinoButtonDefaults.plainButtonColors()) { swatchContent() }
                        else OutlinedButton(modifier = swatchModifier,
                            onClick = { save(swatch) }) { swatchContent() }
                    }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ColorChannelView.entries.forEach { channelView ->
                    val label = when (channelView) {
                        ColorChannelView.Hex -> colorText("hex", "Hex")
                        ColorChannelView.Rgb -> colorText("rgb", "RGB")
                        ColorChannelView.Hsb -> colorText("hsb", "HSB")
                    }
                    ColorPickerAction(cupertino = cupertino, label = label,
                        selected = view == channelView,
                        modifier = Modifier.semantics {
                        selected = view == channelView
                        contentDescription = colorText(when (channelView) {
                            ColorChannelView.Hex -> "hexChannels"
                            ColorChannelView.Rgb -> "rgbChannels"
                            ColorChannelView.Hsb -> "hsbChannels"
                        }, "$label channels")
                    }, onClick = {
                        view = channelView
                        draft = emptyMap()
                        problem = null
                    })
                }
            }
            if (view == ColorChannelView.Hex) {
                pickerField(key = "hex", label = colorText("hex", "Hex"), value = draft["hex"] ?: parsed?.let {
                    serializeColor(it, ColorSaveFormat.Hex)
                }.orEmpty(), accessibleName = colorText("hexValue", "Hex channel value"),
                    modifier = Modifier.fillMaxWidth(), onChange = { next ->
                    draft = draft + ("hex" to next)
                    val edited = editColorChannel(parsed, view, "hex", next)
                    if (edited != null) save(edited)
                    else problem = "invalid"
                })
            }
            if (parsed != null) {
                if (view != ColorChannelView.Hex) {
                val hsb = rgbToHsb(parsed)
                val channels = if (view == ColorChannelView.Rgb)
                    listOf(parsed.r / 255f, parsed.g / 255f, parsed.b / 255f)
                    else listOf(hsb.h / 360f, hsb.s / 100f, hsb.b / 100f)
                val keys = if (view == ColorChannelView.Rgb) listOf("r", "g", "b") else listOf("h", "s", "b")
                val names = if (view == ColorChannelView.Rgb)
                    listOf("red" to "Red", "green" to "Green", "blue" to "Blue")
                else listOf("hue" to "Hue", "saturation" to "Saturation",
                    "brightness" to "Brightness")
                val labels = names.map { (key, english) -> colorText(key, english) }
                labels.forEachIndexed { index, label ->
                    val name = names[index].first
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        if (cupertino) Box(Modifier.width(100.dp)) { CupertinoText(label) }
                        else Text(label, Modifier.width(100.dp))
                        ColorPickerSlider(cupertino = cupertino,
                            value = channels[index],
                            onValueChange = { next ->
                                val maximum = if (view == ColorChannelView.Rgb) 255 else if (index == 0) 360 else 100
                                editColorChannel(parsed, view, keys[index],
                                    (next * maximum).roundToInt().toString())?.let(::save)
                            },
                            modifier = Modifier.weight(1f).semantics {
                                contentDescription = colorText("$name.channel", "${names[index].second} channel",
                                    colorChannelLocaleBundles(name))
                            },
                        )
                        pickerField(key = keys[index], label = label,
                            value = draft[keys[index]] ?: ((channels[index] *
                            (if (view == ColorChannelView.Rgb) 255 else if (index == 0) 360 else 100)).roundToInt()).toString(),
                            onChange = { next ->
                                draft = draft + (keys[index] to next)
                                val edited = editColorChannel(parsed, view, keys[index], next)
                                if (edited != null) save(edited)
                                else problem = "invalid"
                            }, accessibleName = colorText("$name.value", "${names[index].second} channel value",
                                colorChannelLocaleBundles(name)), numeric = true,
                            modifier = Modifier.width(100.dp))
                    }
                }
                }
                if (format != ColorSaveFormat.Hex3) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        val alphaLabel = colorText("alpha", "Alpha")
                        if (cupertino) Box(Modifier.width(100.dp)) { CupertinoText(alphaLabel) }
                        else Text(alphaLabel, Modifier.width(100.dp))
                        ColorPickerSlider(cupertino = cupertino, value = parsed.a.toFloat(),
                            onValueChange = { save(parsed.copy(a = it.toDouble())) },
                            modifier = Modifier.weight(1f).semantics {
                                contentDescription = colorText("alphaChannel", "Alpha channel")
                            })
                        pickerField(key = "alpha", label = "%",
                            value = draft["alpha"] ?: (parsed.a * 100).roundToInt().toString(),
                            onChange = { next ->
                                draft = draft + ("alpha" to next)
                                val edited = editColorChannel(parsed, view, "alpha", next)
                                if (edited != null) save(edited)
                                else problem = "invalid"
                            }, accessibleName = colorText("alphaValue", "Alpha channel value"), numeric = true,
                            modifier = Modifier.width(90.dp))
                    }
                }
            }
            if (problem != null) {
                val message = when (problem) {
                    "hex3Transparency" -> colorText("hex3Transparency", "Transparency cannot be saved as #RGB.")
                    else -> colorText("invalid", "Invalid color.")
                }
                if (cupertino) CupertinoText(message) else Text(message)
            }
        }
        if (!textEntry && visibleDescription != null) Text(visibleDescription)
        if (!textEntry && visibleError.isNotBlank()) Text(visibleError)
    }
}
