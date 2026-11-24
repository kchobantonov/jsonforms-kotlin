package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry
import com.chobantonov.jsonfroms.cupertino.renderers.controls.CupertinoNativeControl
import com.chobantonov.jsonfroms.cupertino.renderers.controls.cupertinoNativeControlTester
import com.chobantonov.jsonfroms.material.renderers.controls.MaterialNativeControl
import com.chobantonov.jsonfroms.material.renderers.controls.materialNativeControlTester
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class NativePlaceholderUiTest {
    @Test fun materialFallbackHonorsAuthoredAndEmptyPlaceholder() = placeholder(false)
    @Test fun cupertinoFallbackHonorsAuthoredAndEmptyPlaceholder() = placeholder(true)

    private fun placeholder(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"value":{"type":"string","format":"date"}}}"""
        ).jsonObject)
        var control by mutableStateOf(ControlElement("#/properties/value", options = mapOf(
            "placeholder" to JsonPrimitive("Choose a day"),
        )))
        var changes = 0
        setContent {
            JsonForms(data = Json.parseToJsonElement("{}"), schema = schema, uischema = control,
                renderers = if (cupertino) listOf(JsonFormsRendererRegistryEntry(
                    cupertinoNativeControlTester, CupertinoNativeControl)) else listOf(
                    JsonFormsRendererRegistryEntry(materialNativeControlTester, MaterialNativeControl)),
                onChange = { changes++ })
        }
        fun hint(): String = onAllNodes(hasSetTextAction()).onFirst().fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.StateDescription).orEmpty()
        assertTrue(hint().contains("Choose a day"), hint())
        runOnIdle { control = control.copy(options = mapOf("placeholder" to JsonPrimitive(""))) }
        assertFalse(hint().contains("date"), hint())
        assertFalse(hint().contains("Choose a day"), hint())
        runOnIdle { control = control.copy(options = mapOf("placeholder" to JsonPrimitive(42))) }
        assertTrue(hint().contains("date"), hint())
        assertEquals(0, changes)
    }
}
