package com.chobantonov.jsonfroms.material.renderers.controls

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.and
import com.chobantonov.jsonfroms.core.testers.formatIs
import com.chobantonov.jsonfroms.core.testers.isStringControl
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.material.renderers.ActionTooltip
import com.chobantonov.jsonfroms.material.renderers.translatedActionText
import kotlinx.serialization.json.JsonPrimitive

val MaterialPasswordControl: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialPasswordControl(props)
}

@Composable
fun MaterialPasswordControl(
    props: ComposeJsonFormsRendererProps,
) {
    val bindings = materialControlBindings(
        props = props,
        rendererName = "MaterialPasswordControl",
    )
    val controlProps = bindings.controlProps
    var passwordVisible by remember(bindings.stateProps.path) { mutableStateOf(false) }
    val translator = props.state.jsonforms.i18n.translate
    val translationValues = mapOf("label" to bindings.stateProps.label)
    val visibilityText = if (passwordVisible) {
        translatedActionText(
            translator = translator,
            i18nKeyPrefix = bindings.stateProps.i18nKeyPrefix,
            tooltipKey = "hidePasswordTooltip",
            ariaLabelKey = "hidePasswordAriaLabel",
            tooltipFallback = "Hide password",
            values = translationValues,
        )
    } else {
        translatedActionText(
            translator = translator,
            i18nKeyPrefix = bindings.stateProps.i18nKeyPrefix,
            tooltipKey = "showPasswordTooltip",
            ariaLabelKey = "showPasswordAriaLabel",
            tooltipFallback = "Show password",
            values = translationValues,
        )
    }

    MaterialInputControl(
        props = controlProps,
        value = bindings.stateProps.data.toMaterialInputString(),
        keyboardType = KeyboardType.Password,
        visualTransformation = if (passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            ActionTooltip(visibilityText.tooltip) {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) PasswordHiddenIcon else PasswordVisibleIcon,
                        contentDescription = visibilityText.ariaLabel,
                    )
                }
            }
        },
        onChange = { value ->
            bindings.dispatchProps.handleChange(
                bindings.stateProps.path,
                value.takeIf { it.isNotEmpty() }?.let(::JsonPrimitive),
            )
        },
    )
}

val materialPasswordControlTester: RankedTester = rankWith(
    2,
    and(isStringControl, formatIs("password")),
)

private val PasswordVisibleIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "PasswordVisible",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(2f, 12f)
            curveTo(4.5f, 7.5f, 7.8f, 5.5f, 12f, 5.5f)
            curveTo(16.2f, 5.5f, 19.5f, 7.5f, 22f, 12f)
            curveTo(19.5f, 16.5f, 16.2f, 18.5f, 12f, 18.5f)
            curveTo(7.8f, 18.5f, 4.5f, 16.5f, 2f, 12f)
            close()
            moveTo(15f, 12f)
            curveTo(15f, 13.66f, 13.66f, 15f, 12f, 15f)
            curveTo(10.34f, 15f, 9f, 13.66f, 9f, 12f)
            curveTo(9f, 10.34f, 10.34f, 9f, 12f, 9f)
            curveTo(13.66f, 9f, 15f, 10.34f, 15f, 12f)
            close()
        }
    }.build()
}

private val PasswordHiddenIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "PasswordHidden",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(3f, 3f)
            lineTo(21f, 21f)
            moveTo(10.6f, 5.7f)
            curveTo(11.05f, 5.57f, 11.52f, 5.5f, 12f, 5.5f)
            curveTo(16.2f, 5.5f, 19.5f, 7.5f, 22f, 12f)
            curveTo(21.25f, 13.35f, 20.4f, 14.47f, 19.45f, 15.37f)
            moveTo(14.12f, 14.12f)
            curveTo(13.58f, 14.66f, 12.83f, 15f, 12f, 15f)
            curveTo(10.34f, 15f, 9f, 13.66f, 9f, 12f)
            curveTo(9f, 11.17f, 9.34f, 10.42f, 9.88f, 9.88f)
            moveTo(6.61f, 6.61f)
            curveTo(4.83f, 7.72f, 3.31f, 9.52f, 2f, 12f)
            curveTo(4.5f, 16.5f, 7.8f, 18.5f, 12f, 18.5f)
            curveTo(13.19f, 18.5f, 14.31f, 18.34f, 15.36f, 18.02f)
        }
    }.build()
}
