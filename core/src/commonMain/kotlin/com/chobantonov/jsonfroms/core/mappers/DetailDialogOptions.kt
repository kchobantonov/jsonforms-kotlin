package com.chobantonov.jsonfroms.core.mappers

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

enum class DialogDimensionUnit { Px, Dp, Vw, Vh, Percent, Rem }
enum class DialogDimensionAxis { Width, Height }

data class DialogDimension(val value: Float, val unit: DialogDimensionUnit) {
    fun pixels(viewportWidth: Float, viewportHeight: Float, density: Float, axis: DialogDimensionAxis): Float =
        when (unit) {
            DialogDimensionUnit.Px -> value
            DialogDimensionUnit.Dp -> value * density
            DialogDimensionUnit.Vw -> viewportWidth * value / 100f
            DialogDimensionUnit.Vh -> viewportHeight * value / 100f
            DialogDimensionUnit.Percent ->
                (if (axis == DialogDimensionAxis.Width) viewportWidth else viewportHeight) * value / 100f
            DialogDimensionUnit.Rem -> value * 16f
        }
}

data class DetailDialogOptions(
    val width: DialogDimension? = null,
    val height: DialogDimension? = null,
    val maximizable: Boolean = true,
    val draggable: Boolean = false,
    val resizable: Boolean = false,
)

/** Local properties override form-wide defaults individually, including explicit false. */
fun resolveDetailDialogOptions(local: JsonObject?, config: Map<String, Any?>): DetailDialogOptions {
    val global = config["jsonformsExtended"].field("dialog")
    fun value(name: String): Any? = local?.get(name) ?: global.field(name)
    return DetailDialogOptions(
        width = value("width").dialogDimension(),
        height = value("height").dialogDimension(),
        maximizable = value("maximizable").dialogBoolean() ?: true,
        draggable = value("draggable").dialogBoolean() ?: false,
        resizable = value("resizable").dialogBoolean() ?: false,
    )
}

private fun Any?.field(name: String): Any? = when (this) {
    is JsonObject -> this[name]
    is Map<*, *> -> this[name]
    else -> null
}

private fun Any?.dialogBoolean(): Boolean? = when (this) {
    is Boolean -> this
    is JsonPrimitive -> takeUnless { isString }?.booleanOrNull
    else -> null
}

private val dimensionPattern = Regex("^([0-9]+(?:\\.[0-9]+)?)\\s*(px|dp|vw|vh|%|rem)$", RegexOption.IGNORE_CASE)

private fun Any?.dialogDimension(): DialogDimension? {
    val primitive = this as? JsonPrimitive
    val number = when (this) {
        is Number -> toFloat()
        is JsonPrimitive -> if (!isString) content.toFloatOrNull() else null
        else -> null
    }
    if (number != null) return number.takeIf { it.isFinite() && it > 0f }
        ?.let { DialogDimension(it, DialogDimensionUnit.Px) }
    val raw = primitive?.contentOrNull ?: this as? String ?: return null
    val match = dimensionPattern.matchEntire(raw.trim()) ?: return null
    val value = match.groupValues[1].toFloatOrNull()?.takeIf { it.isFinite() && it > 0f } ?: return null
    val unit = when (match.groupValues[2].lowercase()) {
        "px" -> DialogDimensionUnit.Px
        "dp" -> DialogDimensionUnit.Dp
        "vw" -> DialogDimensionUnit.Vw
        "vh" -> DialogDimensionUnit.Vh
        "%" -> DialogDimensionUnit.Percent
        else -> DialogDimensionUnit.Rem
    }
    return DialogDimension(value, unit)
}
