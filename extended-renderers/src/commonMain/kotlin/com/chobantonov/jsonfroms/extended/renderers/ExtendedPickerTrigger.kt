package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.cupertino.renderers.ActionTooltip as CupertinoActionTooltip
import com.chobantonov.jsonfroms.material.renderers.ActionTooltip as MaterialActionTooltip

internal enum class ExtendedPickerKind { Duration, Cron }

@Composable
internal fun ExtendedPickerTrigger(
    kind: ExtendedPickerKind,
    cupertino: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    @Composable fun button() {
        IconButton(onClick = onClick, modifier = Modifier.semantics {
            contentDescription = label
        }) {
            Icon(if (kind == ExtendedPickerKind.Duration) DurationPickerIcon else CronPickerIcon,
                contentDescription = null)
        }
    }
    if (cupertino) CupertinoActionTooltip(label) { button() }
    else MaterialActionTooltip(label) { button() }
}

private val DurationPickerIcon = outlineIcon("Duration picker") {
    moveTo(9f, 2f); horizontalLineTo(15f)
    moveTo(12f, 5f); verticalLineTo(3f)
    moveTo(19f, 6f); lineTo(21f, 4f)
    moveTo(12f, 22f); curveTo(7.03f, 22f, 3f, 17.97f, 3f, 13f)
    curveTo(3f, 8.03f, 7.03f, 4f, 12f, 4f)
    curveTo(16.97f, 4f, 21f, 8.03f, 21f, 13f)
    curveTo(21f, 17.97f, 16.97f, 22f, 12f, 22f)
    moveTo(12f, 8f); verticalLineTo(13f); lineTo(15f, 15f)
}

private val CronPickerIcon = outlineIcon("Schedule picker") {
    moveTo(7f, 2f); verticalLineTo(6f)
    moveTo(17f, 2f); verticalLineTo(6f)
    moveTo(3f, 9f); horizontalLineTo(21f)
    moveTo(5f, 4f); horizontalLineTo(19f)
    curveTo(20.1f, 4f, 21f, 4.9f, 21f, 6f)
    verticalLineTo(11f)
    moveTo(3f, 6f); verticalLineTo(19f)
    curveTo(3f, 20.1f, 3.9f, 21f, 5f, 21f)
    horizontalLineTo(11f)
    moveTo(17f, 22f); curveTo(14.24f, 22f, 12f, 19.76f, 12f, 17f)
    curveTo(12f, 14.24f, 14.24f, 12f, 17f, 12f)
    curveTo(19.76f, 12f, 22f, 14.24f, 22f, 17f)
    curveTo(22f, 19.76f, 19.76f, 22f, 17f, 22f)
    moveTo(17f, 14.5f); verticalLineTo(17f); lineTo(19f, 18f)
}

private fun outlineIcon(name: String, draw: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
            pathBuilder = draw)
    }.build()
