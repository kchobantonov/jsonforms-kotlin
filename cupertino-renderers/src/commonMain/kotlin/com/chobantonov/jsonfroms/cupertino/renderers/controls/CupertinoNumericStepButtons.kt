package com.chobantonov.jsonfroms.cupertino.renderers.controls

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.JsonFormsActionIcons
import com.chobantonov.jsonfroms.core.i18n.translateControlLocalError
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.ActionTooltip
import kotlinx.serialization.json.JsonPrimitive

@Composable
internal fun CupertinoNumericStepButtons(
    decrease: JsonPrimitive?,
    increase: JsonPrimitive?,
    enabled: Boolean,
    i18n: JsonFormsI18nState,
    onStep: (JsonPrimitive) -> Unit,
) {
    val decreaseLabel = translateControlLocalError(i18n, "input.decrement", "Decrease value")
    val increaseLabel = translateControlLocalError(i18n, "input.increment", "Increase value")
    Row {
        ActionTooltip(decreaseLabel) {
            IconButton(onClick = { decrease?.let(onStep) }, enabled = enabled && decrease != null,
                modifier = Modifier.size(30.dp)) {
                Icon(JsonFormsActionIcons.Remove, decreaseLabel, modifier = Modifier.size(17.dp))
            }
        }
        ActionTooltip(increaseLabel) {
            IconButton(onClick = { increase?.let(onStep) }, enabled = enabled && increase != null,
                modifier = Modifier.size(30.dp)) {
                Icon(JsonFormsActionIcons.Add, increaseLabel, modifier = Modifier.size(17.dp))
            }
        }
    }
}
