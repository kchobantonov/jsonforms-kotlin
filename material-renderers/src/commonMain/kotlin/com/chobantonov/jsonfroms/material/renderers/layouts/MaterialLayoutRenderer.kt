package com.chobantonov.jsonfroms.material.renderers.layouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.JsonFormsDispatchRenderer
import com.chobantonov.jsonfroms.core.models.UiSchemaElement

enum class LayoutDirection {
    Row,
    Column,
}

@Composable
internal fun MaterialLayoutRenderer(
    props: ComposeJsonFormsRendererProps,
    elements: List<UiSchemaElement>,
    direction: LayoutDirection,
) {
    if (!props.enabled || elements.isEmpty()) {
        if (elements.isEmpty()) {
            return
        }
    }

    when (direction) {
        LayoutDirection.Row -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                elements.forEach { child ->
                    JsonFormsDispatchRenderer(
                        props = props.copy(
                            uischema = child,
                            id = null,
                        ),
                    )
                }
            }
        }

        LayoutDirection.Column -> {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                elements.forEach { child ->
                    JsonFormsDispatchRenderer(
                        props = props.copy(
                            uischema = child,
                            id = null,
                        ),
                    )
                }
            }
        }
    }
}
