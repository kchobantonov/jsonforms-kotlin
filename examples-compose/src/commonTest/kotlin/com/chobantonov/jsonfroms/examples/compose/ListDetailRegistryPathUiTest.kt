package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.LabelElement
import com.chobantonov.jsonfroms.core.models.ListWithDetailElement
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.store.JsonFormsUISchemaRegistryEntry
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class ListDetailRegistryPathUiTest {
    @Test fun materialUsesSelectedItemPath() = scenario(false)
    @Test fun cupertinoUsesSelectedItemPath() = scenario(true)

    private fun scenario(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"}}}}}}"""
        ).jsonObject)
        val registry = listOf(
            JsonFormsUISchemaRegistryEntry(
                tester = { _, _, path -> if (path == "rows.0") 10 else -1 },
                uischema = LabelElement("First row detail"),
            ),
            JsonFormsUISchemaRegistryEntry(
                tester = { _, _, path -> if (path == "rows.1") 10 else -1 },
                uischema = VerticalLayout(elements = listOf(
                    LabelElement("Second row detail"), ControlElement("#/properties/name"),
                )),
            ),
        )
        var data by mutableStateOf(Json.parseToJsonElement("""{"rows":[{"name":"Ada"},{"name":"Bob"}]}"""))
        var changes = 0
        setContent {
            JsonForms(
                data = data,
                schema = schema,
                uischema = ListWithDetailElement("#/properties/rows", options = mapOf(
                    "elementLabelProp" to JsonPrimitive("name"))),
                uischemas = registry,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ },
            )
        }
        onNodeWithText("No selection").assertExists()
        onNodeWithText("Ada").performClick()
        onNodeWithText("First row detail").assertExists()
        onNodeWithText("Second row detail").assertDoesNotExist()
        onNodeWithText("Bob").performClick()
        onNodeWithText("First row detail").assertDoesNotExist()
        onNodeWithText("Second row detail").assertExists()
        assertEquals(0, changes)
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("Bea")
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"rows":[{"name":"Ada"},{"name":"Bea"}]}"""), data)
        onNodeWithText("Second row detail").assertExists()
        assertEquals(1, changes)
    }
}
