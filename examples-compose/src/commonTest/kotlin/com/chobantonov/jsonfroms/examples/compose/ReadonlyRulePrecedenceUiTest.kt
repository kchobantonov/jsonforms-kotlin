package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.LeafCondition
import com.chobantonov.jsonfroms.core.models.Rule
import com.chobantonov.jsonfroms.core.models.RuleEffect
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalTestApi::class)
class ReadonlyRulePrecedenceUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement("""{
      "type":"object","properties":{"name":{"type":"string"},"unlock":{"type":"boolean"}}
    }""").jsonObject)

    @Test fun materialWritableRuleOverridesReadonlyOption() = checkWritableRule(false)
    @Test fun cupertinoWritableRuleOverridesReadonlyOption() = checkWritableRule(true)
    @Test fun materialSeparatesReadonlyTextFromDisabled() = checkSeparateReadonly(false)
    @Test fun cupertinoSeparatesReadonlyTextFromDisabled() = checkSeparateReadonly(true)
    @Test fun materialReadonlyBooleanUsesDisabledWidget() = readonlyBoolean(false)
    @Test fun cupertinoReadonlyBooleanUsesDisabledWidget() = readonlyBoolean(true)

    private fun readonlyBoolean(cupertino: Boolean) = runComposeUiTest {
        var separate by mutableStateOf(false)
        var changes = 0
        setContent {
            JsonForms(
                data = buildJsonObject { put("name", "Ada"); put("unlock", false) },
                schema = schema,
                uischema = ControlElement("#/properties/unlock",
                    options = mapOf("readonly" to JsonPrimitive(true))),
                config = mapOf("separateReadonlyFromDisabled" to separate),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++ },
            )
        }
        val checkbox = onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ToggleableState))
            .onFirst()
        checkbox.assertIsNotEnabled()
        runOnIdle { separate = true }
        checkbox.assertIsNotEnabled()
        assertEquals(0, changes)
    }

    private fun checkSeparateReadonly(cupertino: Boolean) = runComposeUiTest {
        var separate by mutableStateOf(false)
        var changes = 0
        setContent {
            JsonForms(
                data = buildJsonObject { put("name", "Ada"); put("unlock", false) },
                schema = schema,
                uischema = ControlElement("#/properties/name",
                    options = mapOf("readonly" to JsonPrimitive(true), "clearable" to JsonPrimitive(true))),
                config = mapOf("separateReadonlyFromDisabled" to separate),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++ },
            )
        }
        onNodeWithText("Ada").assertIsNotEnabled()
        runOnIdle { separate = true }
        onNodeWithText("Ada").assertIsEnabled()
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        assertEquals(0, changes)
    }

    private fun checkWritableRule(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(buildJsonObject { put("name", "Ada"); put("unlock", false) })
        var changes = 0
        setContent {
            TestTouchInputMode()
            JsonForms(
                data = data,
                schema = schema,
                uischema = ControlElement(
                    scope = "#/properties/name",
                    options = mapOf("readonly" to JsonPrimitive(true), "clearable" to JsonPrimitive(true)),
                    rule = Rule(RuleEffect.WRITABLE,
                        LeafCondition("#/unlock", JsonPrimitive(true))),
                ),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data.jsonObject; changes++ },
            )
        }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        runOnIdle { data = buildJsonObject { put("name", "Ada"); put("unlock", true) } }
        onNodeWithContentDescription("Clear value").assertExists()
        onNodeWithContentDescription("Clear value").performClick()
        runOnIdle {
            assertFalse("name" in data)
            assertEquals(1, changes)
        }
    }
}
