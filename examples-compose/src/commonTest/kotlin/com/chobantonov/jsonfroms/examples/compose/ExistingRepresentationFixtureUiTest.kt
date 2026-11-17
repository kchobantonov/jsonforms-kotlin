package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.models.toUiSchemaElement
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class ExistingRepresentationFixtureUiTest {
    @Test fun materialExistingRepresentations() = existingRepresentations(false)
    @Test fun cupertinoExistingRepresentations() = existingRepresentations(true)

    private fun existingRepresentations(cupertino: Boolean) = runComposeUiTest {
        val ui = Json.parseToJsonElement("""{
            "type":"VerticalLayout","elements":[
                {"type":"Group","label":"Details","elements":[
                    {"type":"HorizontalLayout","elements":[
                        {"type":"Control","scope":"#/properties/first"},
                        {"type":"Control","scope":"#/properties/second"}
                    ]}
                ]}
            ]
        }""").jsonObject.toUiSchemaElement()
        val vertical = assertIs<VerticalLayout>(ui)
        val group = assertIs<GroupElement>(vertical.elements.single())
        val horizontal = assertIs<HorizontalLayout>(group.elements.single())
        horizontal.elements.forEach { assertIs<ControlElement>(it) }

        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"first":{"type":"string"},"second":{"type":"string"}}
        }""").jsonObject)
        val data = Json.parseToJsonElement("""{"first":"Ada","second":"Lovelace"}""")
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { writes++ })
        }
        onNodeWithText("Details").assertExists()
        assertEquals(2, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onAllNodes(hasSetTextAction())[0].performTextReplacement("Grace")
        assertEquals(1, writes)
    }
}
