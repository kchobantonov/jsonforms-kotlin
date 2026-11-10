package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.runtime.Composable
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.core.models.UnknownUiSchemaElement
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.uiTypeIs
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

const val ExtendedSlotsConfigKey = "extendedSlots"
val slotTester: RankedTester = rankWith(2, uiTypeIs("Slot"))

fun interface ExtendedNamedRenderer {
    @Composable
    fun Render(props: ComposeJsonFormsRendererProps)
}

@Composable
internal fun NamedContent(props: ComposeJsonFormsRendererProps, configKey: String, fallback: String) {
    val element = props.uischema as? UnknownUiSchemaElement ?: return
    val name = (element.properties["name"] as? JsonPrimitive)?.contentOrNull
        ?: (element.properties["slot"] as? JsonPrimitive)?.contentOrNull
        ?: fallback
    @Suppress("UNCHECKED_CAST")
    val renderers = props.config[configKey] as? Map<String, ExtendedNamedRenderer>
    val renderer = renderers?.get(name)
    when {
        renderer != null -> renderer.Render(props)
        element.elements.isNotEmpty() -> DispatchChild(props, element.elements.first())
    }
}
