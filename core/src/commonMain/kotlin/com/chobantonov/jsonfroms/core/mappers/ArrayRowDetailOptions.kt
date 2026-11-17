package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.JsonOptions
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.models.toUiSchemaElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

enum class ArrayRowDetailPresentation { Dialog, Panel }
enum class ArrayRowDetailPlacement { Right, Bottom }

data class ArrayRowDetailOptions(
    val presentation: ArrayRowDetailPresentation,
    val detail: UiSchemaElement?,
    val placement: ArrayRowDetailPlacement,
    val collapsed: Boolean,
    val resizable: Boolean,
    val dialog: JsonObject?,
)

fun resolveArrayRowDetailOptions(options: JsonOptions): ArrayRowDetailOptions? {
    val value = options["rowDetail"] as? JsonObject ?: return null
    val presentation = when ((value["presentation"] as? JsonPrimitive)?.contentOrNull) {
        "dialog" -> ArrayRowDetailPresentation.Dialog
        "panel" -> ArrayRowDetailPresentation.Panel
        else -> return null
    }
    val detail = (value["detail"] as? JsonObject)
        ?.let { runCatching { it.toUiSchemaElement() }.getOrNull() }
    val placement = if ((value["placement"] as? JsonPrimitive)?.contentOrNull == "bottom")
        ArrayRowDetailPlacement.Bottom else ArrayRowDetailPlacement.Right
    return ArrayRowDetailOptions(
        presentation = presentation,
        detail = detail,
        placement = placement,
        collapsed = (value["collapsed"] as? JsonPrimitive)?.takeUnless { it.isString }?.booleanOrNull ?: false,
        resizable = (value["resizable"] as? JsonPrimitive)?.takeUnless { it.isString }?.booleanOrNull ?: true,
        dialog = value["dialog"] as? JsonObject,
    )
}
