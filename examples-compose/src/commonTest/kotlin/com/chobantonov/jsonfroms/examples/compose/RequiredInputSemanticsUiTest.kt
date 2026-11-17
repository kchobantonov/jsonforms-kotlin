package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class RequiredInputSemanticsUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"name":{"type":"string","title":"Name"}},"required":["name"]}"""
    ).jsonObject)

    @Test fun materialRequiredStateSurvivesHiddenAsterisk() = requiredState(false)
    @Test fun cupertinoRequiredStateSurvivesHiddenAsterisk() = requiredState(true)

    private fun requiredState(cupertino: Boolean) = runComposeUiTest {
        val data = Json.parseToJsonElement("{}")
        var hideMarker by mutableStateOf<JsonElement>(JsonPrimitive(true))
        var locale by mutableStateOf("en")
        var changes = 0
        var validity: CombinedValidityStatus? = null
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ControlElement("#/properties/name",
                    options = mapOf("hideRequiredAsterisk" to hideMarker)),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { changes++ },
                onValidityChange = { validity = it.status },
            )
        }
        val input = onAllNodes(hasSetTextAction()).onFirst()
        assertEquals("Required field", input.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        onNodeWithText("Name*").assertDoesNotExist()
        assertEquals(CombinedValidityStatus.Invalid, validity)
        runOnIdle { locale = "bg-BG" }
        waitForIdle()
        assertEquals("Задължително поле", input.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        runOnIdle { hideMarker = JsonPrimitive(false) }
        waitForIdle()
        onNodeWithText("Name*").assertExists()
        assertEquals("Задължително поле", input.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        runOnIdle { hideMarker = JsonPrimitive("true") }
        waitForIdle()
        onNodeWithText("Name*").assertExists()
        assertEquals(0, changes)
    }
}
