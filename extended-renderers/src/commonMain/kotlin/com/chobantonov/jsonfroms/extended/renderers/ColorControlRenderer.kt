package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.core.actions.Actions
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.and
import com.chobantonov.jsonfroms.core.testers.formatIs
import com.chobantonov.jsonfroms.core.testers.isStringControl
import com.chobantonov.jsonfroms.core.testers.optionIs
import com.chobantonov.jsonfroms.core.testers.or
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.util.composeWithUi
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.booleanOrNull

val colorControlTester: RankedTester = rankWith(
    3, and(isStringControl, or(formatIs("color"), optionIs("format", "color"))),
)

@Composable
internal fun ColorControl(props: ComposeJsonFormsRendererProps, textRenderer: ComposeJsonFormsRenderer) {
    val control = props.uischema as? ControlElement ?: return
    val value = com.chobantonov.jsonfroms.core.util.JsonPath.get(
        props.state.jsonforms.core.data,
        composeWithUi(control, props.path),
    ) as? JsonPrimitive
    val color = value?.contentOrNull?.toComposeColor()
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(36.dp).background(color ?: Color.Transparent, RoundedCornerShape(6.dp)),
        )
        Box(Modifier.weight(1f)) { textRenderer.Render(props) }
    }
    if (props.enabled && (control.options["readonly"] as? JsonPrimitive)?.booleanOrNull != true) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("#ef4444", "#f59e0b", "#22c55e", "#3b82f6", "#8b5cf6", "#ffffff", "#000000").forEach { hex ->
                OutlinedButton(onClick = {
                    props.dispatch(Actions.update(path = composeWithUi(control, props.path), updater = { JsonPrimitive(hex) }))
                }) {
                    Box(Modifier.size(20.dp).background(hex.toComposeColor() ?: Color.Transparent))
                }
            }
        }
        val displayed = color ?: Color.White
        val channels = listOf(displayed.red, displayed.green, displayed.blue)
        listOf("Red", "Green", "Blue").forEachIndexed { index, label ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(label, Modifier.width(52.dp))
                Slider(
                    value = channels[index],
                    onValueChange = { next ->
                        val values = channels.mapIndexed { channel, value ->
                            ((if (channel == index) next else value) * 255).roundToInt().coerceIn(0, 255)
                        }
                        val hex = values.joinToString(separator = "") { it.toString(16).padStart(2, '0') }
                        props.dispatch(Actions.update(path = composeWithUi(control, props.path), updater = { JsonPrimitive("#$hex") }))
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private fun String.toComposeColor(): Color? {
    val hex = removePrefix("#")
    if (hex.length != 6 && hex.length != 8) return null
    val number = hex.toULongOrNull(16) ?: return null
    return if (hex.length == 6) Color((0xFF000000uL or number).toLong()) else Color(number.toLong())
}
