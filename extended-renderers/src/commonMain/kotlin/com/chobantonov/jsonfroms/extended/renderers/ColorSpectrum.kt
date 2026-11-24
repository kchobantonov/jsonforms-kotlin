package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.core.validation.RgbaColor
import com.chobantonov.jsonfroms.core.validation.parseColor
import com.chobantonov.jsonfroms.core.validation.rgbToHsb
import kotlin.math.roundToInt

private val spectrumDefault = RgbaColor(255, 0, 0)

internal fun colorAtSpectrumPosition(
    base: RgbaColor?, saturation: Float, verticalPosition: Float,
): RgbaColor {
    val hue = rgbToHsb(base ?: spectrumDefault).h
    val s = (saturation.coerceIn(0f, 1f) * 100).roundToInt()
    val b = ((1f - verticalPosition.coerceIn(0f, 1f)) * 100).roundToInt()
    return requireNotNull(parseColor("hsb($hue, $s%, $b%)"))
        .copy(a = base?.a ?: 1.0)
}

@Composable
internal fun ColorSpectrum(
    color: RgbaColor?, label: String, saturationLabel: String,
    brightnessLabel: String, onChange: (RgbaColor) -> Unit,
) {
    val currentColor by rememberUpdatedState(color)
    val currentOnChange by rememberUpdatedState(onChange)
    val hsb = rgbToHsb(color ?: spectrumDefault)
    val hue = requireNotNull(parseColor("hsb(${hsb.h}, 100%, 100%)"))
    val hueColor = Color(hue.r / 255f, hue.g / 255f, hue.b / 255f)

    fun pick(position: Offset, width: Float, height: Float) {
        if (width <= 0f || height <= 0f) return
        currentOnChange(colorAtSpectrumPosition(currentColor,
            position.x / width, position.y / height))
    }

    Canvas(Modifier.fillMaxWidth().height(160.dp)
        .clip(RoundedCornerShape(6.dp))
        .border(1.dp, Color.Gray, RoundedCornerShape(6.dp))
        .semantics {
            contentDescription = label
            stateDescription = "$saturationLabel ${hsb.s}%, $brightnessLabel ${hsb.b}%"
        }
        .onKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
            val step = if (event.isShiftPressed) 10f else 1f
            val nextS = when (event.key) {
                Key.DirectionLeft -> hsb.s - step
                Key.DirectionRight -> hsb.s + step
                else -> hsb.s.toFloat()
            }
            val nextB = when (event.key) {
                Key.DirectionDown -> hsb.b - step
                Key.DirectionUp -> hsb.b + step
                else -> hsb.b.toFloat()
            }
            if (nextS == hsb.s.toFloat() && nextB == hsb.b.toFloat()) false
            else {
                currentOnChange(colorAtSpectrumPosition(currentColor,
                    nextS.coerceIn(0f, 100f) / 100f,
                    1f - nextB.coerceIn(0f, 100f) / 100f))
                true
            }
        }
        .focusable()
        .pointerInput(Unit) {
            detectTapGestures { pick(it, size.width.toFloat(), size.height.toFloat()) }
        }
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { pick(it, size.width.toFloat(), size.height.toFloat()) },
                onDrag = { change, _ ->
                    pick(change.position, size.width.toFloat(), size.height.toFloat())
                    change.consume()
                },
            )
        }) {
        drawRect(Brush.horizontalGradient(listOf(Color.White, hueColor)))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        val marker = Offset(size.width * hsb.s / 100f,
            size.height * (1f - hsb.b / 100f))
        drawCircle(Color.Black, radius = 7.dp.toPx(), center = marker)
        drawCircle(Color.White, radius = 5.dp.toPx(), center = marker,
            style = Stroke(width = 2.dp.toPx()))
    }
}
