package com.chobantonov.jsonfroms.material.renderers.controls

import androidx.compose.runtime.Composable
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsCellProps
import kotlinx.serialization.json.JsonPrimitive

@Composable
internal fun MaterialTemporalCell(props: ComposeJsonFormsCellProps) {
    when (props.schema.format ?: (props.uischema.options["format"] as? JsonPrimitive)?.content) {
        "date" -> MaterialDateCell(props)
        "time" -> MaterialTimeCell(props)
        "date-time" -> MaterialDateTimeCell(props)
    }
}
