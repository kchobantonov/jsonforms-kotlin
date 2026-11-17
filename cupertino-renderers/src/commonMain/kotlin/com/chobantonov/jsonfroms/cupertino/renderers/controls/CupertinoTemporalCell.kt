package com.chobantonov.jsonfroms.cupertino.renderers.controls

import androidx.compose.runtime.Composable
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsCellProps
import kotlinx.serialization.json.JsonPrimitive

@Composable
internal fun CupertinoTemporalCell(props: ComposeJsonFormsCellProps) {
    when (props.schema.format ?: (props.uischema.options["format"] as? JsonPrimitive)?.content) {
        "date" -> CupertinoDateCell(props)
        "time" -> CupertinoTimeCell(props)
        "date-time" -> CupertinoDateTimeCell(props)
    }
}
