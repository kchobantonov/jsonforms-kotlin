package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.core.actions.Actions
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.JsonSchemaType
import com.chobantonov.jsonfroms.core.models.JsonSchemaTypeValue
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.and
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.schemaMatches
import com.chobantonov.jsonfroms.core.testers.uiTypeIs
import com.chobantonov.jsonfroms.core.util.composeWithUi
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

private val isNullSchema: (JsonSchema) -> Boolean = { schema ->
    schema.typeValue == JsonSchemaTypeValue.Single(JsonSchemaType.Null)
}

val nullControlTester: RankedTester = rankWith(
    2, and(uiTypeIs("Control"), schemaMatches(isNullSchema)),
)

@Composable
internal fun NullControl(props: ComposeJsonFormsRendererProps) {
    val control = props.uischema as? ControlElement ?: return
    val current = com.chobantonov.jsonfroms.core.util.JsonPath.get(
        props.state.jsonforms.core.data,
        composeWithUi(control, props.path),
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = current == JsonNull,
            enabled = props.enabled && (control.options["readonly"] as? JsonPrimitive)?.booleanOrNull != true,
            onCheckedChange = { checked ->
                props.dispatch(
                    Actions.update(
                        path = composeWithUi(control, props.path),
                        updater = { if (checked) JsonNull else null },
                    ),
                )
            },
        )
        Text((props.schema["title"] as? JsonPrimitive)?.contentOrNull ?: control.scope.substringAfterLast('/'))
    }
}
