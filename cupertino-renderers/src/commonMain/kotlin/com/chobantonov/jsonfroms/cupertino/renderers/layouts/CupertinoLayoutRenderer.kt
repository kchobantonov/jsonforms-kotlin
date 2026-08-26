package com.chobantonov.jsonfroms.cupertino.renderers.layouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.JsonFormsDispatchRenderer
import com.chobantonov.jsonfroms.core.models.UiSchemaElement

internal enum class LayoutDirection { Row, Column }

@Composable
internal fun CupertinoLayoutRenderer(
    props: ComposeJsonFormsRendererProps,
    elements: List<UiSchemaElement>,
    direction: LayoutDirection,
) {
    when (direction) {
        LayoutDirection.Column -> Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            elements.forEach { child ->
                JsonFormsDispatchRenderer(props.copy(uischema = child, id = null))
            }
        }
        LayoutDirection.Row -> Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            elements.forEach { child ->
                Box(Modifier.weight(1f)) {
                    JsonFormsDispatchRenderer(props.copy(uischema = child, id = null))
                }
            }
        }
    }
}
