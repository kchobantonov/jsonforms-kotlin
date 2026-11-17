package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.VerticalLayout
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
class ArrayChoiceInitialFocusUiTest {
    @Test fun materialCheckboxInitialFocus() = focus(false, "checkbox")
    @Test fun cupertinoCheckboxInitialFocus() = focus(true, "checkbox")
    @Test fun materialMultiSelectInitialFocus() = focus(false, "multi-select")
    @Test fun cupertinoMultiSelectInitialFocus() = focus(true, "multi-select")
    @Test fun materialFiniteChipsInitialFocus() = focus(false, "chips")
    @Test fun cupertinoFiniteChipsInitialFocus() = focus(true, "chips")
    @Test fun materialFreeChipsInitialFocus() = focus(false, "free-chips")
    @Test fun cupertinoFreeChipsInitialFocus() = focus(true, "free-chips")
    @Test fun materialLocalFalseSuppressesMultiSelectFocus() =
        focus(false, "multi-select", JsonPrimitive(false), globalFocus = true, expectFocus = false)
    @Test fun cupertinoLocalFalseSuppressesMultiSelectFocus() =
        focus(true, "multi-select", JsonPrimitive(false), globalFocus = true, expectFocus = false)
    @Test fun materialQuotedTrueSuppressesFreeChipsFocus() =
        focus(false, "free-chips", JsonPrimitive("true"), globalFocus = true, expectFocus = false)
    @Test fun cupertinoQuotedTrueSuppressesFreeChipsFocus() =
        focus(true, "free-chips", JsonPrimitive("true"), globalFocus = true, expectFocus = false)
    @Test fun materialDisabledAddDoesNotFocusWhenLaterEnabled() = disabledAtMount(false)
    @Test fun cupertinoDisabledAddDoesNotFocusWhenLaterEnabled() = disabledAtMount(true)

    private fun disabledAtMount(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"teams":{"type":"array","title":"Teams",
                "uniqueItems":true,"items":{"type":"string","enum":["ops","fin"]}}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"teams":["ops","fin"]}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/teams", options = mapOf(
                    "variant" to JsonPrimitive("multi-select"), "focus" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++ })
        }
        val add = onNodeWithContentDescription("Add choice: Teams")
        add.assertIsNotEnabled()
        runOnIdle { data = Json.parseToJsonElement("""{"teams":["ops"]}""") }
        waitForIdle()
        add.assertIsEnabled()
        add.assertIsNotFocused()
        assertEquals(0, changes)
    }

    private fun focus(
        cupertino: Boolean,
        variant: String,
        initialOption: JsonElement = JsonPrimitive(true),
        globalFocus: Boolean = false,
        expectFocus: Boolean = true,
    ) = runComposeUiTest {
        val free = variant == "free-chips"
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"other":{"type":"string"},
                "teams":{"title":"Teams","type":"array","uniqueItems":true,"minItems":1,
                    "items":${if (free) """{"type":"string"}""" else """{"type":"string","enum":["ops","fin"]}"""}}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"teams":["ops"],"other":"x"}"""))
        var locale by mutableStateOf("en")
        var focusOption by mutableStateOf(initialOption)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = VerticalLayout(elements = listOf(
                    ControlElement("#/properties/teams", options = mapOf(
                        "variant" to JsonPrimitive(if (free) "chips" else variant),
                        "focus" to focusOption)),
                    ControlElement("#/properties/other"))),
                config = mapOf("focus" to globalFocus),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { changes++ })
        }
        val choice = when (variant) {
            "checkbox" -> onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)).onLast()
            "free-chips" -> onAllNodes(hasSetTextAction()).onFirst()
            else -> onNodeWithContentDescription("Add choice: Teams")
        }
        if (expectFocus) choice.assertIsFocused() else choice.assertIsNotFocused()
        if (!free && variant != "checkbox") onNodeWithContentDescription("fin").assertDoesNotExist()
        assertEquals(0, changes)
        val other = onAllNodes(hasSetTextAction()).onLast()
        other.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        other.assertIsFocused()
        choice.assertIsNotFocused()
        runOnIdle {
            data = Json.parseToJsonElement("""{"teams":["fin"],"other":"x"}""")
            locale = "bg-BG"
            focusOption = JsonPrimitive(false)
        }
        waitForIdle()
        other.assertIsFocused()
        assertEquals(0, changes)
    }
}
