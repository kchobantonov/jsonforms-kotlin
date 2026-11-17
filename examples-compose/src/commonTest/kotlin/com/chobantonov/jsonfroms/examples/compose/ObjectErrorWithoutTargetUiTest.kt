package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class ObjectErrorWithoutTargetUiTest {
    @Test fun materialObjectShowsUnrepresentedError() = scenario(false)
    @Test fun cupertinoObjectShowsUnrepresentedError() = scenario(true)
    @Test fun materialObjectKeepsBoundFieldErrorAtField() = boundField(false)
    @Test fun cupertinoObjectKeepsBoundFieldErrorAtField() = boundField(true)
    @Test fun materialObjectShowsHiddenFieldError() = hiddenField(false)
    @Test fun cupertinoObjectShowsHiddenFieldError() = hiddenField(true)

    private fun scenario(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"person":{"type":"object","properties":{"name":{"type":"string"}}}}}"""
        ).jsonObject)
        val original = Json.parseToJsonElement("""{"person":{"name":"Ada"}}""")
        var errors by mutableStateOf(listOf(
            ErrorObject(instancePath = "/person", message = "Object is rejected"),
            ErrorObject(instancePath = "/person/name", message = "Unrendered name is rejected"),
        ))
        var changes = 0
        setContent {
            JsonForms(
                data = original, schema = schema,
                uischema = ControlElement("#/properties/person", options = mapOf(
                    "detail" to Json.parseToJsonElement("""{"type":"Label","text":"Registered content"}"""))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                additionalErrors = errors,
                onChange = { changes++ },
            )
        }
        onNodeWithText("Registered content").assertExists()
        val feedback = onNodeWithText("Object is rejected")
        feedback.assertExists()
        assertTrue(feedback.fetchSemanticsNode().config[SemanticsProperties.Error].isNotBlank())
        val omitted = onNodeWithText("Unrendered name is rejected")
        omitted.assertExists()
        assertTrue(omitted.fetchSemanticsNode().config[SemanticsProperties.Error].isNotBlank())
        assertEquals(0, changes)
        runOnIdle { errors = emptyList() }
        onNodeWithText("Object is rejected").assertDoesNotExist()
        onNodeWithText("Unrendered name is rejected").assertDoesNotExist()
        assertEquals(0, changes)
    }

    private fun boundField(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"person":{"type":"object","properties":{"name":{"type":"string"}}}}}"""
        ).jsonObject)
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"person":{"name":"Ada"}}"""), schema = schema,
                uischema = ControlElement("#/properties/person", options = mapOf(
                    "detail" to Json.parseToJsonElement(
                        """{"type":"Control","scope":"#/properties/name"}"""))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                additionalErrors = listOf(ErrorObject(instancePath = "/person/name", message = "Name is rejected")),
            )
        }
        assertEquals(1, onAllNodes(hasText("Name is rejected")).fetchSemanticsNodes().size)
    }

    private fun hiddenField(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"person":{"type":"object","properties":{"name":{"type":"string"}}}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"person":{"name":"Ada"}}"""))
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ControlElement("#/properties/person", options = mapOf(
                    "detail" to Json.parseToJsonElement(
                        """{"type":"Control","scope":"#/properties/name","rule":{"effect":"HIDE","condition":{"scope":"#/properties/person/properties/name","expectedValue":"Ada"}}}"""))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                additionalErrors = listOf(ErrorObject(instancePath = "/person/name", message = "Hidden name is rejected")),
                onChange = { changes++ },
            )
        }
        val hiddenFeedback = onNodeWithText("Hidden name is rejected")
        hiddenFeedback.assertExists()
        assertTrue(hiddenFeedback.fetchSemanticsNode().config[SemanticsProperties.Error].isNotBlank())
        assertEquals(0, changes)
        runOnIdle { data = Json.parseToJsonElement("""{"person":{"name":"Bob"}}""") }
        assertEquals(1, onAllNodes(hasText("Hidden name is rejected")).fetchSemanticsNodes().size)
        assertEquals(0, changes)
    }
}
