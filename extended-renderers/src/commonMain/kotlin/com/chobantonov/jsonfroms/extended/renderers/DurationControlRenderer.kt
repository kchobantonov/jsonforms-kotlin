package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

val durationControlTester: RankedTester = rankWith(
    3, and(isStringControl, or(formatIs("duration"), optionIs("format", "duration"))),
)

@Composable
internal fun DurationControl(props: ComposeJsonFormsRendererProps, textRenderer: ComposeJsonFormsRenderer) {
    val control = props.uischema as? ControlElement ?: return
    val path = composeWithUi(control, props.path)
    val current = (com.chobantonov.jsonfroms.core.util.JsonPath.get(
        props.state.jsonforms.core.data, path,
    ) as? JsonPrimitive)?.contentOrNull.orEmpty()
    val parts = parseSimpleDuration(current)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        textRenderer.Render(props)
        if (props.enabled && (control.options["readonly"] as? JsonPrimitive)?.booleanOrNull != true) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Duration")
                listOf("PT15M", "PT1H", "P1D", "P1W").forEach { duration ->
                    OutlinedButton(onClick = {
                        props.dispatch(Actions.update(path = path, updater = { JsonPrimitive(duration) }))
                    }) { Text(duration) }
                }
            }
            if (parts != null) {
                listOf("Days", "Hours", "Minutes").forEachIndexed { index, label ->
                    val maximum = listOf(31, 23, 59)[index]
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("$label: ${parts[index]}", Modifier.width(96.dp))
                        Slider(
                            value = parts[index].toFloat().coerceIn(0f, maximum.toFloat()),
                            valueRange = 0f..maximum.toFloat(),
                            onValueChange = { next ->
                                val updated = parts.toMutableList()
                                updated[index] = next.roundToInt()
                                props.dispatch(Actions.update(path = path, updater = {
                                    JsonPrimitive(formatSimpleDuration(updated))
                                }))
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

private val simpleDuration = Regex("^P(?:(\\d+)D)?(?:T(?:(\\d+)H)?(?:(\\d+)M)?)?$")

internal fun parseSimpleDuration(value: String): List<Int>? {
    if (value.isEmpty()) return listOf(0, 0, 0)
    val groups = simpleDuration.matchEntire(value)?.groupValues ?: return null
    return groups.drop(1).map { it.toIntOrNull() ?: 0 }
}

internal fun formatSimpleDuration(parts: List<Int>): String {
    val days = if (parts[0] > 0) "${parts[0]}D" else ""
    val time = buildString {
        if (parts[1] > 0) append("${parts[1]}H")
        if (parts[2] > 0) append("${parts[2]}M")
    }
    return if (days.isEmpty() && time.isEmpty()) "P0D" else "P$days${if (time.isNotEmpty()) "T$time" else ""}"
}
