package com.chobantonov.jsonfroms.cupertino.renderers.controls

import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.JsonFormsActionIcons
import com.chobantonov.jsonfroms.cupertino.renderers.ActionTooltip
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfControl
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfEnum
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.DispatchPropsOfControl
import com.chobantonov.jsonfroms.core.mappers.StatePropsOfControlWithOptions
import com.chobantonov.jsonfroms.core.mappers.computeLabel
import com.chobantonov.jsonfroms.core.mappers.mapDispatchToControlProps
import com.chobantonov.jsonfroms.core.mappers.mapStateToEnumControlProps
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.store.Dispatch
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.isEnumControl
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.withIncreasedRank
import com.chobantonov.jsonfroms.cupertino.renderers.util.LocalCupertinoColors
import com.slapps.cupertino.CupertinoDropdownMenu
import com.slapps.cupertino.ExperimentalCupertinoApi
import com.slapps.cupertino.MenuPickerAction

val CupertinoEnumControl = ComposeJsonFormsRenderer { CupertinoEnumControl(it) }

@OptIn(ExperimentalCupertinoApi::class)
@Composable
fun CupertinoEnumControl(props: ComposeJsonFormsRendererProps) {
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
                path = props.path,
                renderers = props.renderers,
                cells = props.cells,
            ),
        ),
        enumProps = OwnPropsOfEnum(),
    )
    if (!stateProps.visible) return
    val dispatch = mapDispatchToControlProps(Dispatch { action -> props.dispatch(action); action })
    val clearLabel = props.state.jsonforms.i18n.translate?.translate("clear", "Clear value", emptyMap()) ?: "Clear value"
    CupertinoSelectControl(stateProps, dispatch, clearLabel)
}

@OptIn(ExperimentalCupertinoApi::class)
@Composable
internal fun CupertinoSelectControl(
    stateProps: StatePropsOfControlWithOptions,
    dispatch: DispatchPropsOfControl,
    clearLabel: String,
) {
    val selected = stateProps.options.firstOrNull { it.value == stateProps.data }
    val enabled = stateProps.enabled && !stateProps.readonly && stateProps.options.isNotEmpty()
    val colors = LocalCupertinoColors.current
    val hideAsterisk = (stateProps.config + stateProps.uischema.options)["hideRequiredAsterisk"] == true
    var expanded by remember(stateProps.path) { mutableStateOf(false) }
    val displayedValue = selected?.label ?: stateProps.data.toCupertinoInputString()
    val borderColor = if (stateProps.errors.isNotBlank()) colors.destructive else colors.controlBorder

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (stateProps.label.isNotBlank()) {
            Text(
                computeLabel(stateProps.label, stateProps.required, hideAsterisk),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.secondaryLabel,
            )
        }
        Box {
            Row(
                Modifier.fillMaxWidth().border(1.dp, borderColor, RoundedCornerShape(10.dp))
                    .clickable(enabled) { expanded = !expanded }.padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    displayedValue,
                    modifier = Modifier.weight(1f),
                    color = if (enabled) colors.label else colors.secondaryLabel,
                )
                if (selected != null) {
                    ActionTooltip(clearLabel) {
                        IconButton(
                            onClick = { dispatch.handleChange(stateProps.path, null); expanded = false },
                            enabled = enabled,
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(JsonFormsActionIcons.Clear, clearLabel, tint = colors.secondaryLabel, modifier = Modifier.size(15.dp))
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                CupertinoDropdownChevron(
                    expanded = expanded,
                    color = colors.secondaryLabel,
                )
            }
            CupertinoDropdownMenu(
                expanded = expanded && enabled,
                onDismissRequest = { expanded = false },
            ) {
                stateProps.options.forEach { option ->
                    MenuPickerAction(
                        isSelected = option == selected,
                        onClick = {
                            dispatch.handleChange(stateProps.path, option.value)
                            expanded = false
                        },
                        title = { Text(option.label) },
                    )
                }
            }
        }
        when {
            stateProps.errors.isNotBlank() -> Text(stateProps.errors, style = MaterialTheme.typography.bodySmall, color = colors.destructive)
            !stateProps.description.isNullOrBlank() -> Text(stateProps.description!!, style = MaterialTheme.typography.bodySmall, color = colors.secondaryLabel)
        }
    }
}

@Composable
internal fun CupertinoDropdownChevron(expanded: Boolean, color: Color) {
    Canvas(Modifier.size(14.dp).rotate(if (expanded) 180f else 0f)) {
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

val cupertinoEnumControlTester: RankedTester = withIncreasedRank(100, rankWith(2, isEnumControl))
