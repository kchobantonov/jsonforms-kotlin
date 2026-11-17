package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class AdditionalPropertySchemaUiTest {
    @Test fun materialComposedPropertyNamesUsesRootReference() = composedNames(false)
    @Test fun cupertinoComposedPropertyNamesUsesRootReference() = composedNames(true)
    @Test fun materialIsolatedNestedEditorKeepsRootReference() = isolatedRef(false)
    @Test fun cupertinoIsolatedNestedEditorKeepsRootReference() = isolatedRef(true)

    private fun composedNames(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","${'$'}defs":{"sensorPrefix":{"pattern":"^sensor-"}},"properties":{"telemetry":{"type":"object","propertyNames":{"allOf":[{"${'$'}ref":"#/${'$'}defs/sensorPrefix"},{"minLength":12}]},"additionalProperties":{"type":"number"}}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"telemetry":{}}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/telemetry"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodes(hasSetTextAction() and hasText("")).onFirst().performTextReplacement("sensor-x")
        onNodeWithContentDescription("Add button").performClick()
        assertEquals(null, data.jsonObject["telemetry"]!!.jsonObject["sensor-x"])
        onAllNodes(hasSetTextAction() and hasText("sensor-x")).onFirst()
            .performTextReplacement("sensor-deck-b")
        onNodeWithContentDescription("Add button").performClick()
        waitForIdle()
        assertEquals(JsonPrimitive(0), data.jsonObject["telemetry"]!!.jsonObject["sensor-deck-b"])
        assertEquals(1, changes)
    }

    private fun isolatedRef(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","${'$'}defs":{"code":{"type":"string","minLength":3}},"properties":{"labels":{"type":"object","additionalProperties":{"type":"object","properties":{"code":{"${'$'}ref":"#/${'$'}defs/code"}}}}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"labels":{"a.b":{"code":"x"}}}"""))
        var changes = 0
        var validity: CombinedValidityStatus? = null
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/labels"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(translateError = { error, _, _ ->
                    "ERR:${error.keyword}:${error.instancePath}"
                }),
                onChange = { data = it.data; changes++ },
                onValidityChange = { validity = it.status })
        }
        assertEquals(CombinedValidityStatus.Invalid, validity)
        onAllNodes(hasText("ERR:minLength", substring = true)).onFirst().assertExists()
        onAllNodes(hasSetTextAction() and hasText("x", substring = true)).onFirst()
            .performTextReplacement("valid")
        waitForIdle()
        assertEquals(JsonPrimitive("valid"), data.jsonObject["labels"]!!.jsonObject["a.b"]!!.jsonObject["code"])
        assertEquals(1, changes)
        assertEquals(CombinedValidityStatus.Valid, validity)
    }
}
