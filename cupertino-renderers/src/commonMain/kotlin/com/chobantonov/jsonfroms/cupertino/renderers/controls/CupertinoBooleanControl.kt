package com.chobantonov.jsonfroms.cupertino.renderers.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.core.mappers.computeLabel
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.isBooleanControl
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.withIncreasedRank
import com.chobantonov.jsonfroms.cupertino.renderers.CupertinoCheckbox
import com.chobantonov.jsonfroms.cupertino.renderers.CupertinoSwitch
import com.chobantonov.jsonfroms.cupertino.renderers.util.LocalCupertinoColors
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

val CupertinoBooleanControl = ComposeJsonFormsRenderer { CupertinoBooleanControl(it, toggle = false) }

@Composable
internal fun CupertinoBooleanControl(props: ComposeJsonFormsRendererProps, toggle: Boolean) {
    val bindings = cupertinoControlBindings(props, "CupertinoBooleanControl")
    val control = bindings.controlProps
    if (!control.visible) return
    val colors = LocalCupertinoColors.current
    val checked = (control.data as? JsonPrimitive)?.booleanOrNull ?: false
    val hideAsterisk = (control.config + control.uischema.options)["hideRequiredAsterisk"] == true
    val enabled = control.enabled && !control.readonly
    val change: (Boolean) -> Unit = { value ->
        bindings.dispatchProps.handleChange(bindings.stateProps.path, JsonPrimitive(value))
    }
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                computeLabel(control.label, control.required, hideAsterisk),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) colors.label else colors.secondaryLabel,
            )
            if (toggle) CupertinoSwitch(checked, change, enabled = enabled)
            else CupertinoCheckbox(checked, change, enabled = enabled)
        }
        if (control.errors.isNotBlank()) {
            Text(control.errors, style = MaterialTheme.typography.bodySmall, color = colors.destructive)
        } else if (!control.description.isNullOrBlank()) {
            Text(control.description!!, style = MaterialTheme.typography.bodySmall, color = colors.secondaryLabel)
        }
    }
}

val cupertinoBooleanControlTester: RankedTester = withIncreasedRank(100, rankWith(2, isBooleanControl))
