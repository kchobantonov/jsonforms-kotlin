package com.chobantonov.jsonfroms.cupertino.renderers.controls

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.Composable
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsCellProps
import com.chobantonov.jsonfroms.compose.asRendererProps
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.JsonFormsActionIcons
import com.chobantonov.jsonfroms.compose.LocalPreserveEmptyValue
import com.chobantonov.jsonfroms.compose.OtpPasswordInput
import com.chobantonov.jsonfroms.core.i18n.translatePasswordActionText
import com.chobantonov.jsonfroms.core.i18n.translateControlInvalidValue
import com.chobantonov.jsonfroms.core.mappers.invalidStringStoredValue
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.isPasswordControl
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.withIncreasedRank
import com.chobantonov.jsonfroms.core.util.acceptsRestrictedStringEdit
import com.chobantonov.jsonfroms.core.util.resolveRestrict
import com.chobantonov.jsonfroms.cupertino.renderers.ActionTooltip
import com.chobantonov.jsonfroms.cupertino.renderers.util.LocalCupertinoColors
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

val CupertinoPasswordControl = ComposeJsonFormsRenderer { props -> CupertinoPasswordControl(props) }

@Composable
internal fun CupertinoPasswordCell(props: ComposeJsonFormsCellProps) =
    CupertinoPasswordControl(props.asRendererProps(), compact = true)

@Composable
fun CupertinoPasswordControl(props: ComposeJsonFormsRendererProps, compact: Boolean = false) {
    val bindings = cupertinoControlBindings(props, "CupertinoPasswordControl")
    val preserveEmptyValue = LocalPreserveEmptyValue.current
    var passwordVisible by remember(bindings.stateProps.path) { mutableStateOf(false) }
    val visibilityText = translatePasswordActionText(props.state.jsonforms.i18n,
        bindings.stateProps.i18nKeyPrefix, passwordVisible,
        mapOf("label" to bindings.stateProps.label))
    val controlProps = bindings.controlProps
    val current = bindings.stateProps.data.toCupertinoInputString()
    val invalidStatus = invalidStringStoredValue(bindings.stateProps.data, bindings.stateProps.dataProvided)
        ?.let { "${translateControlInvalidValue(props.state.jsonforms.i18n)}: $it" }
    val otpCount = bindings.stateProps.schema.maxLength?.takeIf { count ->
        !compact &&
        count > 0 && bindings.stateProps.schema.minLength?.let { it in 0..count } == true &&
            (controlProps.uischema.options["variant"] as? JsonPrimitive)?.contentOrNull == "otp"
    }
    val clearable = (controlProps.uischema.options["clearable"] as? JsonPrimitive)?.booleanOrNull != false
    val onPasswordChange: (String) -> Boolean = { value ->
        if (!acceptsRestrictedStringEdit(current, value,
                bindings.stateProps.schema.maxLength,
                resolveRestrict(controlProps.uischema.options["restrict"], controlProps.config))) false
        else {
            bindings.dispatchProps.handleChange(bindings.stateProps.path,
                if (value.isNotEmpty() || preserveEmptyValue) JsonPrimitive(value) else null)
            true
        }
    }
    if (otpCount != null) {
        val colors = LocalCupertinoColors.current
        OtpPasswordInput(controlProps, current, otpCount, passwordVisible,
            textColor = colors.label, mutedColor = colors.secondaryLabel,
            borderColor = colors.controlBorder, focusColor = colors.accent,
            errorColor = colors.destructive,
            storedValueStatus = invalidStatus,
            trailingContent = {
                if (clearable && current.isNotEmpty() && controlProps.enabled && !controlProps.readonly)
                    ActionTooltip(bindings.clearLabel) {
                        IconButton(onClick = { onPasswordChange("") }) {
                            Icon(JsonFormsActionIcons.Clear, bindings.clearLabel)
                        }
                    }
                ActionTooltip(visibilityText.tooltip) {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(if (passwordVisible) JsonFormsActionIcons.VisibilityOff
                            else JsonFormsActionIcons.Visibility, visibilityText.ariaLabel)
                    }
                }
            }, onChange = onPasswordChange)
        return
    }
    CupertinoInputControl(
        props = controlProps,
        value = current,
        keyboardType = KeyboardType.Password,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        clearLabel = bindings.clearLabel,
        storedValueStatus = invalidStatus,
        compact = compact,
        inputModifier = if (compact) Modifier.testTag("cupertino-password-cell") else Modifier,
        trailingContent = {
            ActionTooltip(visibilityText.tooltip) {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(if (passwordVisible) JsonFormsActionIcons.VisibilityOff else JsonFormsActionIcons.Visibility,
                        contentDescription = visibilityText.ariaLabel)
                }
            }
        },
    ) { onPasswordChange(it) }
}

val cupertinoPasswordControlTester: RankedTester = withIncreasedRank(
    100,
    rankWith(2, isPasswordControl),
)
