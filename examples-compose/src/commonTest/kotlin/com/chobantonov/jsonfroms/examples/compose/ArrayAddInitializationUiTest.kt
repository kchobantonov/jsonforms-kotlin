package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class ArrayAddInitializationUiTest {
    @Test fun materialExpandablePreservesExistingAndAddsDefaults() = scenario(false, false)
    @Test fun cupertinoExpandablePreservesExistingAndAddsDefaults() = scenario(true, false)
    @Test fun materialTablePreservesExistingAndAddsDefaults() = scenario(false, true)
    @Test fun cupertinoTablePreservesExistingAndAddsDefaults() = scenario(true, true)

    private fun scenario(cupertino: Boolean, table: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","default":{"nested":{"label":"new"}},"properties":{"nested":{"type":"object","properties":{"label":{"type":"string"}}}}}}}}"""
        ).jsonObject)
        val originalSchema = schema.value
        var data by mutableStateOf(Json.parseToJsonElement(
            """{"rows":[{"nested":{"label":"old"}}]}"""
        ))
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ControlElement("#/properties/rows", options =
                    if (table) mapOf("table" to JsonPrimitive(true))
                    else mapOf("detail" to JsonPrimitive("GENERATE"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ },
            )
        }
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"rows":[{"nested":{"label":"old"}}]}"""), data)
        assertEquals(0, changes)
        repeat(2) {
            onNodeWithContentDescription("Add item").performClick()
            waitForIdle()
        }
        assertEquals(Json.parseToJsonElement(
            """{"rows":[{"nested":{"label":"old"}},{"nested":{"label":"new"}},{"nested":{"label":"new"}}]}"""
        ), data)
        assertEquals(2, changes)
        assertEquals(originalSchema, schema.value)
    }
}
