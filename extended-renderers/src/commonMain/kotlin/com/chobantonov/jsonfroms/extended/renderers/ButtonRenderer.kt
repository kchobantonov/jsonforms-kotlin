package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.core.models.UnknownUiSchemaElement
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.uiTypeIs
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

const val ExtendedActionHandlerConfigKey = "extendedActionHandler"
val buttonTester: RankedTester = rankWith(1, uiTypeIs("Button"))

fun interface ExtendedActionHandler {
    fun invoke(action: String, params: JsonObject)
}

@Composable
internal fun ExtendedButton(props: ComposeJsonFormsRendererProps) {
    val element = props.uischema as? UnknownUiSchemaElement ?: return
    val label = element.stringProperty("label") ?: element.stringProperty("text") ?: "Run"
    val action = element.stringProperty("action").orEmpty()
    val params = element.properties["params"] as? JsonObject ?: JsonObject(emptyMap())
    val handler = props.config[ExtendedActionHandlerConfigKey] as? ExtendedActionHandler
    Button(
        enabled = props.enabled && (element.options["readonly"] as? JsonPrimitive)?.booleanOrNull != true &&
            action.isNotEmpty() && handler != null,
        onClick = { handler?.invoke(action, params) },
    ) { Text(label) }
}

private fun UnknownUiSchemaElement.stringProperty(name: String): String? =
    (properties[name] as? JsonPrimitive)?.contentOrNull
