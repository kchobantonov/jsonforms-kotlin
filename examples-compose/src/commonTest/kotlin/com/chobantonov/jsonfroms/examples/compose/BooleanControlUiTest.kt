package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoCells
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import com.chobantonov.jsonfroms.material.renderers.materialCells
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalTestApi::class)
class BooleanControlUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement("""{
      "type":"object","properties":{"flag":{"type":"boolean"}}
    }""").jsonObject)

    @Test fun materialCheckboxFalseCanClear() = clearFalse(false, false)
    @Test fun materialSwitchFalseCanClear() = clearFalse(false, true)
    @Test fun cupertinoCheckboxFalseCanClear() = clearFalse(true, false)
    @Test fun cupertinoSwitchFalseCanClear() = clearFalse(true, true)
    @Test fun materialCheckboxMissingAndInvalidAreDistinct() = missingAndInvalid(false, false)
    @Test fun materialSwitchMissingAndInvalidAreDistinct() = missingAndInvalid(false, true)
    @Test fun cupertinoCheckboxMissingAndInvalidAreDistinct() = missingAndInvalid(true, false)
    @Test fun cupertinoSwitchMissingAndInvalidAreDistinct() = missingAndInvalid(true, true)
    @Test fun materialCheckboxShowsImportedInvalidValue() = invalidValueVisible(false, false)
    @Test fun materialSwitchShowsImportedInvalidValue() = invalidValueVisible(false, true)
    @Test fun cupertinoCheckboxShowsImportedInvalidValue() = invalidValueVisible(true, false)
    @Test fun cupertinoSwitchShowsImportedInvalidValue() = invalidValueVisible(true, true)
    @Test fun materialClearableAndReadOnlyGuards() = guards(false)
    @Test fun cupertinoClearableAndReadOnlyGuards() = guards(true)
    @Test fun materialCheckboxUsesGlobalClearable() = globalClearable(false, false)
    @Test fun materialSwitchUsesGlobalClearable() = globalClearable(false, true)
    @Test fun cupertinoCheckboxUsesGlobalClearable() = globalClearable(true, false)
    @Test fun cupertinoSwitchUsesGlobalClearable() = globalClearable(true, true)
    @Test fun materialCheckboxClearFollowsFocusAndHover() = clearVisibility(false, false)
    @Test fun materialSwitchClearFollowsFocusAndHover() = clearVisibility(false, true)
    @Test fun cupertinoCheckboxClearFollowsFocusAndHover() = clearVisibility(true, false)
    @Test fun cupertinoSwitchClearFollowsFocusAndHover() = clearVisibility(true, true)
    @Test fun materialCheckboxTouchSelectsClear() = touchSelection(false, false)
    @Test fun materialSwitchTouchSelectsClear() = touchSelection(false, true)
    @Test fun cupertinoCheckboxTouchSelectsClear() = touchSelection(true, false)
    @Test fun cupertinoSwitchTouchSelectsClear() = touchSelection(true, true)
    @Test fun materialCheckboxTouchSelectionTransfers() = touchSelectionTransfers(false, false)
    @Test fun materialSwitchTouchSelectionTransfers() = touchSelectionTransfers(false, true)
    @Test fun cupertinoCheckboxTouchSelectionTransfers() = touchSelectionTransfers(true, false)
    @Test fun cupertinoSwitchTouchSelectionTransfers() = touchSelectionTransfers(true, true)
    @Test fun materialCheckboxTouchSelectionMovesToText() = touchSelectionMovesToText(false, false)
    @Test fun materialSwitchTouchSelectionMovesToText() = touchSelectionMovesToText(false, true)
    @Test fun cupertinoCheckboxTouchSelectionMovesToText() = touchSelectionMovesToText(true, false)
    @Test fun cupertinoSwitchTouchSelectionMovesToText() = touchSelectionMovesToText(true, true)
    @Test fun materialCheckboxTouchSelectionMovesToEnum() = touchSelectionMovesToEnum(false, false)
    @Test fun materialSwitchTouchSelectionMovesToEnum() = touchSelectionMovesToEnum(false, true)
    @Test fun cupertinoCheckboxTouchSelectionMovesToEnum() = touchSelectionMovesToEnum(true, false)
    @Test fun cupertinoSwitchTouchSelectionMovesToEnum() = touchSelectionMovesToEnum(true, true)
    @Test fun materialCheckboxTouchSelectionMovesToRadio() = touchSelectionMovesToRadio(false, false)
    @Test fun materialSwitchTouchSelectionMovesToRadio() = touchSelectionMovesToRadio(false, true)
    @Test fun cupertinoCheckboxTouchSelectionMovesToRadio() = touchSelectionMovesToRadio(true, false)
    @Test fun cupertinoSwitchTouchSelectionMovesToRadio() = touchSelectionMovesToRadio(true, true)
    @Test fun materialCheckboxTouchSelectionMovesToOneOfBranch() = touchSelectionMovesToOneOf(false, false)
    @Test fun materialSwitchTouchSelectionMovesToOneOfBranch() = touchSelectionMovesToOneOf(false, true)
    @Test fun cupertinoCheckboxTouchSelectionMovesToOneOfBranch() = touchSelectionMovesToOneOf(true, false)
    @Test fun cupertinoSwitchTouchSelectionMovesToOneOfBranch() = touchSelectionMovesToOneOf(true, true)

    private fun touchSelectionMovesToRadio(cupertino: Boolean, toggle: Boolean) = runComposeUiTest {
        val mixedSchema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"flag":{"type":"boolean"},
            "choice":{"type":"string","enum":["Red","Blue"]}}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"flag":false}"""))
        var inputMode: InputModeManager? = null
        setContent {
            inputMode = LocalInputModeManager.current
            JsonForms(data = data, schema = mixedSchema,
                uischema = VerticalLayout(elements = listOf(
                    ControlElement("#/properties/flag", options = mapOf("toggle" to JsonPrimitive(toggle))),
                    ControlElement("#/properties/choice", options = mapOf("format" to JsonPrimitive("radio"))))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        runOnIdle { inputMode!!.requestInputMode(InputMode.Touch) }
        val role = if (toggle) Role.Switch else Role.Checkbox
        onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, role)).performTouchInput { click() }
        onAllNodes(hasContentDescription("Clear value")).assertCountEquals(1)
        onNodeWithContentDescription("Red").performTouchInput { click() }
        onAllNodes(hasContentDescription("Clear value")).assertCountEquals(1)
        onNodeWithContentDescription("Clear value")
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        runOnIdle {
            assertEquals(JsonPrimitive(true), data.jsonObject["flag"])
            assertFalse("choice" in data.jsonObject)
        }
        onAllNodes(hasContentDescription("Clear value")).assertCountEquals(0)
    }

    private fun touchSelectionMovesToOneOf(cupertino: Boolean, toggle: Boolean) = runComposeUiTest {
        val mixedSchema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"flag":{"type":"boolean"},
            "choice":{"type":"object","title":"Choice","oneOf":[
              {"title":"Email contact","type":"object","properties":{"kind":{"const":"email"}}},
              {"title":"Phone contact","type":"object","properties":{"kind":{"const":"phone"}}}
            ]}}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"flag":false}"""))
        var inputMode: InputModeManager? = null
        setContent {
            inputMode = LocalInputModeManager.current
            JsonForms(data = data, schema = mixedSchema,
                uischema = VerticalLayout(elements = listOf(
                    ControlElement("#/properties/flag", options = mapOf("toggle" to JsonPrimitive(toggle))),
                    ControlElement("#/properties/choice"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        runOnIdle { inputMode!!.requestInputMode(InputMode.Touch) }
        val role = if (toggle) Role.Switch else Role.Checkbox
        onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, role)).performTouchInput { click() }
        onAllNodes(hasContentDescription("Clear value")).assertCountEquals(1)
        onNodeWithText("Choice").performTouchInput { click() }
        onAllNodes(hasContentDescription("Clear value")).assertCountEquals(0)
        runOnIdle {
            assertEquals(JsonPrimitive(true), data.jsonObject["flag"])
            assertFalse("choice" in data.jsonObject)
        }
    }

    private fun touchSelectionMovesToEnum(cupertino: Boolean, toggle: Boolean) = runComposeUiTest {
        val mixedSchema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"flag":{"type":"boolean"},
            "choice":{"type":"string","enum":["A","B"]}}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"flag":false}"""))
        var inputMode: InputModeManager? = null
        setContent {
            inputMode = LocalInputModeManager.current
            JsonForms(data = data, schema = mixedSchema,
                uischema = VerticalLayout(elements = listOf(
                    ControlElement("#/properties/flag", options = mapOf("toggle" to JsonPrimitive(toggle))),
                    ControlElement("#/properties/choice"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        runOnIdle { inputMode!!.requestInputMode(InputMode.Touch) }
        onAllNodes(hasContentDescription("Clear value")).assertCountEquals(0)
        val role = if (toggle) Role.Switch else Role.Checkbox
        onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, role)).performTouchInput { click() }
        onAllNodes(hasContentDescription("Clear value")).assertCountEquals(1)
        onAllNodes(hasClickAction()).onLast().performTouchInput { click() }
        onAllNodes(hasContentDescription("Clear value")).assertCountEquals(0)
    }

    private fun touchSelectionMovesToText(cupertino: Boolean, toggle: Boolean) = runComposeUiTest {
        val mixedSchema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"flag":{"type":"boolean"},"note":{"type":"string"}}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"flag":false}"""))
        var inputMode: InputModeManager? = null
        setContent {
            inputMode = LocalInputModeManager.current
            JsonForms(data = data, schema = mixedSchema,
                uischema = VerticalLayout(elements = listOf(
                    ControlElement("#/properties/flag", options = mapOf("toggle" to JsonPrimitive(toggle))),
                    ControlElement("#/properties/note"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        runOnIdle { inputMode!!.requestInputMode(InputMode.Touch) }
        onAllNodes(hasContentDescription("Clear value")).assertCountEquals(0)
        val role = if (toggle) Role.Switch else Role.Checkbox
        onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, role)).performTouchInput { click() }
        onAllNodes(hasContentDescription("Clear value")).assertCountEquals(1)
        onAllNodes(hasSetTextAction()).onFirst().performTouchInput { click() }
        onAllNodes(hasContentDescription("Clear value")).assertCountEquals(0)
    }

    private fun touchSelectionTransfers(cupertino: Boolean, toggle: Boolean) = runComposeUiTest {
        val twoFlags = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"first":{"type":"boolean"},"second":{"type":"boolean"}}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"first":false,"second":false}"""))
        var inputMode: InputModeManager? = null
        setContent {
            inputMode = LocalInputModeManager.current
            JsonForms(data = data, schema = twoFlags,
                uischema = VerticalLayout(elements = listOf(
                    ControlElement("#/properties/first", options = mapOf("toggle" to JsonPrimitive(toggle))),
                    ControlElement("#/properties/second", options = mapOf("toggle" to JsonPrimitive(toggle))))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        runOnIdle { inputMode!!.requestInputMode(InputMode.Touch) }
        onAllNodes(hasContentDescription("Clear value")).assertCountEquals(0)
        val role = if (toggle) Role.Switch else Role.Checkbox
        val fields = onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, role))
        fields.onFirst().performTouchInput { click() }
        onAllNodes(hasContentDescription("Clear value")).assertCountEquals(1)
        fields.onLast().performTouchInput { click() }
        onAllNodes(hasContentDescription("Clear value")).assertCountEquals(1)
    }

    private fun touchSelection(cupertino: Boolean, toggle: Boolean) = runComposeUiTest {
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"flag":false}"""))
        var inputMode: InputModeManager? = null
        setContent {
            inputMode = LocalInputModeManager.current
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/flag", options = mapOf("toggle" to JsonPrimitive(toggle))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        runOnIdle { inputMode!!.requestInputMode(InputMode.Touch) }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        onAllNodes(hasClickAction()).onFirst().performTouchInput { click() }
        runOnIdle { assertEquals(Json.parseToJsonElement("""{"flag":true}"""), data) }
        onNodeWithContentDescription("Clear value").assertExists()
        onNodeWithContentDescription("Clear value").performTouchInput { click() }
        runOnIdle { assertFalse("flag" in data.jsonObject) }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
    }

    private fun clearVisibility(cupertino: Boolean, toggle: Boolean) = runComposeUiTest {
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"flag":false}"""))
        var writes = 0
        var inputMode: InputModeManager? = null
        setContent {
            inputMode = LocalInputModeManager.current
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/flag", options = mapOf(
                    "toggle" to JsonPrimitive(toggle))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; writes++ })
        }
        runOnIdle { inputMode!!.requestInputMode(InputMode.Touch) }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        runOnIdle { inputMode!!.requestInputMode(InputMode.Keyboard) }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        val field = onAllNodes(hasClickAction()).onFirst()
        field.performMouseInput { moveTo(center) }
        onNodeWithContentDescription("Clear value").assertExists()
        onNodeWithContentDescription("Clear value").performMouseInput { moveTo(center) }
        onNodeWithContentDescription("Clear value").assertExists()
        onRoot().performMouseInput { moveTo(Offset(-10f, -10f)) }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        field.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        val clear = onNodeWithContentDescription("Clear value")
        clear.assertExists()
        clear.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        clear.assertIsFocused()
        clear.performKeyInput { keyDown(Key.Enter); keyUp(Key.Enter) }
        runOnIdle {
            assertFalse("flag" in data.jsonObject)
            assertEquals(1, writes)
        }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
    }
    @Test fun materialNotSetTracksEnglishAndBulgarian() = localizedNotSet(false)
    @Test fun cupertinoNotSetTracksEnglishAndBulgarian() = localizedNotSet(true)
    @Test fun materialArrayBooleanClearKeepsPosition() = arrayPosition(false)
    @Test fun cupertinoArrayBooleanClearKeepsPosition() = arrayPosition(true)
    @Test fun materialInvalidBooleanTableCellCorrectsLocally() = tableCell(false)
    @Test fun cupertinoInvalidBooleanTableCellCorrectsLocally() = tableCell(true)
    @Test fun materialNativeToggleTableCellCorrectsLocally() = tableCell(false, toggle = true)
    @Test fun cupertinoNativeToggleTableCellCorrectsLocally() = tableCell(true, toggle = true)
    @Test fun materialNativeCheckboxCellTracksHostReplacement() = tableCellHostReplacement(false, false)
    @Test fun cupertinoNativeCheckboxCellTracksHostReplacement() = tableCellHostReplacement(true, false)
    @Test fun materialNativeToggleCellTracksHostReplacement() = tableCellHostReplacement(false, true)
    @Test fun cupertinoNativeToggleCellTracksHostReplacement() = tableCellHostReplacement(true, true)
    @Test fun materialDynamicBooleanClearRetainsKey() = dynamicProperty(false)
    @Test fun cupertinoDynamicBooleanClearRetainsKey() = dynamicProperty(true)
    @Test fun materialRequiredCheckboxAcceptsFalse() = requiredFalse(false, false)
    @Test fun materialRequiredSwitchAcceptsFalse() = requiredFalse(false, true)
    @Test fun cupertinoRequiredCheckboxAcceptsFalse() = requiredFalse(true, false)
    @Test fun cupertinoRequiredSwitchAcceptsFalse() = requiredFalse(true, true)
    @Test fun materialCheckboxConstTrueRequiresAgreement() = agreement(false, false)
    @Test fun materialSwitchConstTrueRequiresAgreement() = agreement(false, true)
    @Test fun cupertinoCheckboxConstTrueRequiresAgreement() = agreement(true, false)
    @Test fun cupertinoSwitchConstTrueRequiresAgreement() = agreement(true, true)
    @Test fun materialCheckboxQuotedClearableIsNotBoolean() = quotedClearable(false, false)
    @Test fun materialSwitchQuotedClearableIsNotBoolean() = quotedClearable(false, true)
    @Test fun cupertinoCheckboxQuotedClearableIsNotBoolean() = quotedClearable(true, false)
    @Test fun cupertinoSwitchQuotedClearableIsNotBoolean() = quotedClearable(true, true)

    private fun quotedClearable(cupertino: Boolean, toggle: Boolean) = runComposeUiTest {
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"flag":false}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/flag", options = mapOf(
                    "toggle" to JsonPrimitive(toggle), "clearable" to JsonPrimitive("false"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodes(hasClickAction()).onFirst().performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithContentDescription("Clear value").performClick()
        runOnIdle {
            assertFalse("flag" in data.jsonObject)
            assertEquals(1, changes)
        }
    }

    private fun agreement(cupertino: Boolean, toggle: Boolean) = runComposeUiTest {
        val agreementSchema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","required":["flag"],
          "properties":{"flag":{"type":"boolean","const":true}}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"flag":false}"""))
        var validity: CombinedValidityStatus? by mutableStateOf(null)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = agreementSchema,
                uischema = ControlElement("#/properties/flag",
                    options = mapOf("toggle" to JsonPrimitive(toggle))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ },
                onValidityChange = { validity = it.status })
        }
        runOnIdle {
            assertEquals(CombinedValidityStatus.Invalid, validity)
            assertEquals(0, changes)
        }
        onAllNodes(hasClickAction()).onFirst().performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithContentDescription("Clear value").assertExists()
        onAllNodes(hasClickAction()).onFirst().performClick()
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{"flag":true}"""), data)
            assertEquals(CombinedValidityStatus.Valid, validity)
            assertEquals(1, changes)
        }
        onAllNodes(hasClickAction()).onFirst().performClick()
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{"flag":false}"""), data)
            assertEquals(CombinedValidityStatus.Invalid, validity)
            assertEquals(2, changes)
        }
    }

    private fun requiredFalse(cupertino: Boolean, toggle: Boolean) = runComposeUiTest {
        val requiredSchema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","required":["flag"],
          "properties":{"flag":{"type":"boolean"}}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"flag":false}"""))
        var validity: CombinedValidityStatus? by mutableStateOf(null)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = requiredSchema,
                uischema = ControlElement("#/properties/flag",
                    options = mapOf("toggle" to JsonPrimitive(toggle))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ },
                onValidityChange = { validity = it.status })
        }
        runOnIdle {
            assertEquals(CombinedValidityStatus.Valid, validity)
            assertEquals(0, changes)
        }
        onAllNodes(hasClickAction()).onFirst().performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithContentDescription("Clear value").performClick()
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{}"""), data)
            assertEquals(CombinedValidityStatus.Invalid, validity)
            assertEquals(1, changes)
        }
        onNode(hasStateDescription("Required field. Not set")).performClick()
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{"flag":true}"""), data)
            assertEquals(CombinedValidityStatus.Valid, validity)
            assertEquals(2, changes)
        }
    }

    private fun dynamicProperty(cupertino: Boolean) = runComposeUiTest {
        val dynamicSchema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","additionalProperties":{"type":"boolean"}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"flag":true}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = dynamicSchema, uischema = ControlElement("#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithContentDescription("Clear value").performClick()
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{"flag":false}"""), data)
            assertEquals(1, changes)
        }
        onNodeWithContentDescription("Delete button").performClick()
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{}"""), data)
            assertEquals(2, changes)
        }
    }

    private fun tableCell(cupertino: Boolean, toggle: Boolean = false) = runComposeUiTest {
        val tableSchema = JsonSchema(Json.parseToJsonElement("""{
          "type":"array","items":{"type":"object","properties":{
            "active":{"type":"boolean","title":"Active"}
          }}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement(
            """[{"active":"false"},{"active":true}]"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = tableSchema,
                uischema = ControlElement("#", options = mapOf(
                    "table" to JsonPrimitive(true),
                    "columnDefs" to Json.parseToJsonElement("""[{"field":"active"}]"""),
                    "cells" to Json.parseToJsonElement("""{"active":{"toggle":$toggle}}"""),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                cells = if (cupertino) cupertinoCells else materialCells,
                i18n = JsonFormsI18nState(locale = "bg-BG", translateError = { _, _, _ ->
                    "Невалидна булева стойност"
                }),
                onChange = { data = it.data; changes++ })
        }
        onAllNodes(hasTestTag(if (cupertino) {
            if (toggle) "cupertino-boolean-toggle-cell" else "cupertino-boolean-cell"
        } else {
            if (toggle) "material-boolean-toggle-cell" else "material-boolean-cell"
        })).onFirst().assertExists()
        assertEquals("Невалидна булева стойност",
            onNodeWithContentDescription("Active съдържа грешки: Невалидна булева стойност").fetchSemanticsNode()
                .config[SemanticsProperties.StateDescription])
        onNode(hasStateDescription("Невалидна стойност: \"false\"")).performClick()
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""[{"active":true},{"active":true}]"""), data)
            assertEquals(1, changes)
        }
        onNodeWithContentDescription("Active съдържа грешки: Невалидна булева стойност").assertDoesNotExist()
        onNode(hasStateDescription("Невалидна стойност: \"false\"")).assertDoesNotExist()
    }

    private fun tableCellHostReplacement(cupertino: Boolean, toggle: Boolean) = runComposeUiTest {
        val tableSchema = JsonSchema(Json.parseToJsonElement("""{
          "type":"array","items":{"type":"object","properties":{"active":{"type":"boolean"}}}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement(
            """[{}, {"active":null}]"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = tableSchema,
                uischema = ControlElement("#", options = mapOf(
                    "table" to JsonPrimitive(true),
                    "cells" to Json.parseToJsonElement("""{"active":{"toggle":$toggle}}"""),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                cells = if (cupertino) cupertinoCells else materialCells,
                i18n = JsonFormsI18nState(locale = "bg-BG"),
                onChange = { changes++ })
        }
        onNode(hasStateDescription("Не е зададено")).assertExists()
        onNode(hasStateDescription("Невалидна стойност: null")).assertExists()
        runOnIdle {
            assertEquals(0, changes)
            data = Json.parseToJsonElement("""[{"active":true}, {"active":false}]""")
        }
        onNode(hasStateDescription("Не е зададено")).assertDoesNotExist()
        onNode(hasStateDescription("Невалидна стойност: null")).assertDoesNotExist()
        runOnIdle { assertEquals(0, changes) }
    }

    private fun arrayPosition(cupertino: Boolean) = runComposeUiTest {
        val arraySchema = JsonSchema(Json.parseToJsonElement("""{
          "type":"array","items":[
            {"type":"boolean","title":"First"},
            {"type":"boolean","title":"Second"}
          ],"additionalItems":false
        }""").jsonObject)
        val detail = Json.parseToJsonElement("""{
          "type":"VerticalLayout","elements":[
            {"type":"Control","scope":"#/items/0"},
            {"type":"Control","scope":"#/items/1"}
          ]
        }""")
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""[true,true]"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = arraySchema,
                uischema = ControlElement("#", options = mapOf("detail" to detail)),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodes(hasClickAction()).onFirst().performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onAllNodes(hasContentDescription("Clear value")).onFirst().performClick()
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""[false,true]"""), data)
            assertEquals(1, changes)
        }
    }

    private fun localizedNotSet(cupertino: Boolean) = runComposeUiTest {
        var locale by mutableStateOf("en")
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{}"""), schema = schema,
                uischema = ControlElement("#/properties/flag"),
                i18n = JsonFormsI18nState(locale = locale),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onNodeWithText("Not set").assertExists()
        runOnIdle { locale = "bg-BG" }
        onNodeWithText("Не е зададено").assertExists()
    }

    private fun invalidValueVisible(cupertino: Boolean, toggle: Boolean) = runComposeUiTest {
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"flag":"false"}"""))
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/flag", options = mapOf("toggle" to JsonPrimitive(toggle))),
                i18n = JsonFormsI18nState(locale = locale),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Invalid value: \"false\"").assertExists()
        onNode(hasStateDescription("Invalid value: \"false\"")).assertExists()
        runOnIdle {
            assertEquals(0, changes)
            locale = "bg-BG"
        }
        onNodeWithText("Невалидна стойност: \"false\"").assertExists()
        runOnIdle { data = Json.parseToJsonElement("""{"flag":null}""") }
        onNodeWithText("Невалидна стойност: null").assertExists()
        runOnIdle { data = Json.parseToJsonElement("""{"flag":7}""") }
        onNode(hasStateDescription("Невалидна стойност: 7")).performClick()
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{"flag":true}"""), data)
            assertEquals(1, changes)
        }
        onNodeWithText("Невалидна стойност: 7").assertDoesNotExist()
    }

    private fun clearFalse(cupertino: Boolean, toggle: Boolean) = runComposeUiTest {
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"flag":false}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/flag",
                    options = mapOf("toggle" to JsonPrimitive(toggle))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Not set").assertDoesNotExist()
        onAllNodes(hasClickAction()).onFirst().performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithContentDescription("Clear value").performClick()
        runOnIdle {
            assertFalse("flag" in data.jsonObject)
            assertEquals(1, changes)
        }
        onNodeWithText("Not set").assertExists()
    }

    private fun missingAndInvalid(cupertino: Boolean, toggle: Boolean) = runComposeUiTest {
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/flag",
                    options = mapOf("toggle" to JsonPrimitive(toggle))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Not set").assertExists()
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        runOnIdle {
            assertEquals(0, changes)
            data = Json.parseToJsonElement("""{"flag":"false"}""")
        }
        onNodeWithText("Invalid value: \"false\"").assertExists()
        onAllNodes(hasClickAction()).onFirst().performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithContentDescription("Clear value").performClick()
        runOnIdle {
            assertFalse("flag" in data.jsonObject)
            assertEquals(1, changes)
        }
    }

    private fun guards(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"flag":false}"""))
        var clearable by mutableStateOf(false)
        var readonly by mutableStateOf(false)
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/flag",
                    options = mapOf("clearable" to JsonPrimitive(clearable))),
                readonly = readonly,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        runOnIdle { clearable = true }
        onAllNodes(hasClickAction()).onFirst().performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithContentDescription("Clear value").assertExists()
        runOnIdle { readonly = true }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        runOnIdle { readonly = false }
        onAllNodes(hasClickAction()).onFirst().performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithContentDescription("Clear value").performClick()
        runOnIdle { assertFalse("flag" in data.jsonObject) }
    }

    private fun globalClearable(cupertino: Boolean, toggle: Boolean) = runComposeUiTest {
        val data = Json.parseToJsonElement("""{"flag":false}""")
        var local by mutableStateOf(JsonPrimitive("false"))
        var global by mutableStateOf(false)
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/flag", options = mapOf(
                    "toggle" to JsonPrimitive(toggle), "clearable" to local)),
                config = mapOf("clearable" to JsonPrimitive(global)),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        runOnIdle { global = true }
        onAllNodes(hasClickAction()).onFirst().performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithContentDescription("Clear value").assertExists()
        runOnIdle { local = JsonPrimitive(false) }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        runOnIdle { global = false; local = JsonPrimitive(true) }
        onAllNodes(hasClickAction()).onFirst().performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithContentDescription("Clear value").assertExists()
    }
}
