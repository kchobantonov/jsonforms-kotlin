package com.chobantonov.jsonfroms.compose

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chobantonov.jsonfroms.core.i18n.translateControlRequired
import com.chobantonov.jsonfroms.core.mappers.ControlProps
import com.chobantonov.jsonfroms.core.mappers.computeLabel
import com.chobantonov.jsonfroms.core.util.jsonStringLength
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/** One editable password field with independently drawn character boxes. */
enum class OtpTextRole { Label, Value, Supporting }

/** Foundation-only fallback for hosts that call the shared editor directly. */
@Composable
private fun DefaultOtpText(text: String, color: Color, role: OtpTextRole) {
    val size = when (role) {
        OtpTextRole.Label -> 16.sp
        OtpTextRole.Value -> 20.sp
        OtpTextRole.Supporting -> 12.sp
    }
    BasicText(text, style = TextStyle(color = color, fontSize = size))
}

@Composable
fun OtpPasswordInput(
    props: ControlProps,
    value: String,
    boxCount: Int,
    revealed: Boolean,
    textColor: Color,
    mutedColor: Color,
    borderColor: Color,
    focusColor: Color,
    errorColor: Color,
    storedValueStatus: String? = null,
    renderText: @Composable (String, Color, OtpTextRole) -> Unit = { text, color, role ->
        DefaultOtpText(text, color, role)
    },
    trailingContent: @Composable (Boolean) -> Unit,
    onChange: (String) -> Boolean,
) {
    if (!props.visible) return
    var editor by remember(props.path) {
        mutableStateOf(TextFieldValue(value, TextRange(value.length)))
    }
    var focused by remember(props.path) { mutableStateOf(false) }
    var controlFocused by remember(props.path) { mutableStateOf(false) }
    var hovered by remember(props.path) { mutableStateOf(false) }
    var touched by remember(props.path) { mutableStateOf(false) }
    val requester = remember(props.path) { FocusRequester() }
    LaunchedEffect(value) {
        if (editor.text != value) editor = editor.copy(
            text = value, selection = TextRange(editor.selection.start.coerceAtMost(value.length)))
    }
    val options = props.config + props.uischema.options
    fun option(key: String): Boolean = when (val raw = options[key]) {
        is Boolean -> raw
        is JsonPrimitive -> !raw.isString && raw.booleanOrNull == true
        else -> false
    }
    LaunchedEffect(requester) {
        if (option("focus") && props.enabled) requester.requestFocus()
    }
    val label = computeLabel(props.label, props.required, option("hideRequiredAsterisk"))
    val visibleError = if (touched) props.errors else props.errorsBeforeTouch
    val requiredState = if (props.required)
        translateControlRequired(useJsonForms().state.jsonforms.i18n) else null
    val description = props.description?.takeIf { it.isNotBlank() &&
        (focused || option("showUnfocusedDescription")) }
    val state = listOfNotNull(requiredState, storedValueStatus, description).joinToString(". ")
    val characters = otpCodePoints(editor.text)
    val selectedBox = jsonStringLength(editor.text.take(editor.selection.start))
        .coerceIn(0, boxCount - 1)
    val boxShape = RoundedCornerShape(8.dp)
    Column(modifier = Modifier.onFocusChanged { controlFocused = it.hasFocus }
        .focusGroup().pointerInput(props.path) {
            awaitPointerEventScope {
                while (true) when (awaitPointerEvent().type) {
                    PointerEventType.Enter -> hovered = true
                    PointerEventType.Exit -> hovered = false
                }
            }
        }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label.isNotBlank()) renderText(label, mutedColor, OtpTextRole.Label)
        BasicTextField(
            value = editor,
            onValueChange = { next ->
                if (next.text == editor.text ||
                    (props.enabled && !props.readonly && onChange(next.text))) editor = next
            },
            modifier = Modifier.fillMaxWidth().focusRequester(requester)
                .testTag("otp-password-field").semantics {
                if (visibleError.isNotBlank()) error(visibleError)
                if (state.isNotBlank()) stateDescription = state
            }.onFocusChanged {
                if (!it.isFocused && focused) touched = true
                focused = it.isFocused
            },
            enabled = props.enabled,
            readOnly = props.readonly,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(),
            decorationBox = { inner ->
                Box {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LazyRow(modifier = Modifier.weight(1f).height(50.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            items(boxCount) { index ->
                                val character = characters.getOrNull(index)
                                Box(Modifier.size(42.dp).testTag("otp-password-box-$index").border(1.dp, when {
                                    visibleError.isNotBlank() -> errorColor
                                    focused && index == selectedBox -> focusColor
                                    else -> borderColor
                                }, boxShape).clickable(enabled = props.enabled) {
                                    editor = editor.copy(selection = TextRange(
                                        otpOffsetOf(editor.text, index)))
                                    requester.requestFocus()
                                }, contentAlignment = Alignment.Center) {
                                    renderText(if (character == null) "" else if (revealed) character else "•",
                                        textColor, OtpTextRole.Value)
                                }
                            }
                        }
                        if (characters.size > boxCount) renderText(
                            "+${characters.size - boxCount}", errorColor, OtpTextRole.Supporting)
                        Spacer(Modifier.size(4.dp))
                        trailingContent(focused || controlFocused || hovered)
                    }
                    Box(Modifier.size(1.dp).alpha(0f)) { inner() }
                }
            },
        )
        if (visibleError.isNotBlank()) renderText(visibleError, errorColor, OtpTextRole.Supporting)
        if (storedValueStatus != null) renderText(storedValueStatus, errorColor, OtpTextRole.Supporting)
        if (description != null) renderText(description, mutedColor, OtpTextRole.Supporting)
    }
}

private fun otpCodePoints(text: String): List<String> = buildList {
    var index = 0
    while (index < text.length) {
        val first = text[index].code
        val width = if (first in 0xD800..0xDBFF && index + 1 < text.length &&
            text[index + 1].code in 0xDC00..0xDFFF) 2 else 1
        add(text.substring(index, index + width))
        index += width
    }
}

private fun otpOffsetOf(text: String, characterIndex: Int): Int {
    var offset = 0
    repeat(characterIndex.coerceAtLeast(0)) {
        if (offset >= text.length) return offset
        val first = text[offset].code
        offset += if (first in 0xD800..0xDBFF && offset + 1 < text.length &&
            text[offset + 1].code in 0xDC00..0xDFFF) 2 else 1
    }
    return offset
}
