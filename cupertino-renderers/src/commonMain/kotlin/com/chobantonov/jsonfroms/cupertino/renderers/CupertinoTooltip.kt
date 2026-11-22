package com.chobantonov.jsonfroms.cupertino.renderers

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipScope
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.cupertino.renderers.util.LocalCupertinoColors
import com.slapps.cupertino.CupertinoCheckBox as LibraryCupertinoCheckBox
import com.slapps.cupertino.CupertinoSwitch as LibraryCupertinoSwitch
import com.slapps.cupertino.CupertinoSwitchDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TooltipScope.CupertinoPlainTooltip(label: String) {
    val colors = LocalCupertinoColors.current
    PlainTooltip(
        modifier = Modifier.testTag("cupertino-tooltip"),
        shape = RoundedCornerShape(8.dp),
        containerColor = colors.secondaryBackground,
        contentColor = colors.label,
        shadowElevation = 4.dp,
    ) { Text(label, color = colors.label) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ActionTooltip(label: String, content: @Composable () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { CupertinoPlainTooltip(label) },
        state = rememberTooltipState(),
        content = content,
    )
}

@Composable
fun CupertinoSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val accent = LocalCupertinoColors.current.accent
    LibraryCupertinoSwitch(
        checked = checked,
        onCheckedChange = onCheckedChange ?: {},
        modifier = modifier,
        colors = CupertinoSwitchDefaults.colors(
            checkedTrackColor = accent,
            disabledCheckedTrackColor = accent.copy(alpha = 0.5f),
        ),
        enabled = enabled && onCheckedChange != null,
    )
}

@Composable
fun CupertinoCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    LibraryCupertinoCheckBox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
    )
}
