package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import com.chobantonov.jsonfroms.core.models.toUiSchemaElement
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.testers.NOT_APPLICABLE
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.TesterContext
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.cupertino.renderers.controls.cupertinoBooleanToggleControlTester
import com.chobantonov.jsonfroms.cupertino.renderers.controls.cupertinoDateControlTester
import com.chobantonov.jsonfroms.cupertino.renderers.controls.cupertinoDateTimeControlTester
import com.chobantonov.jsonfroms.cupertino.renderers.controls.cupertinoPasswordControlTester
import com.chobantonov.jsonfroms.cupertino.renderers.controls.cupertinoTimeControlTester
import com.chobantonov.jsonfroms.cupertino.renderers.controls.cupertinoTextControlTester
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import com.chobantonov.jsonfroms.material.renderers.controls.materialBooleanToggleControlTester
import com.chobantonov.jsonfroms.material.renderers.controls.materialDateControlTester
import com.chobantonov.jsonfroms.material.renderers.controls.materialDateTimeControlTester
import com.chobantonov.jsonfroms.material.renderers.controls.materialPasswordControlTester
import com.chobantonov.jsonfroms.material.renderers.controls.materialTimeControlTester
import com.chobantonov.jsonfroms.material.renderers.controls.materialTextControlTester
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

@OptIn(ExperimentalTestApi::class)
class PresentationSelectionTest {
    @Test fun materialFlatToggleSelectsMountedSwitch() = mountedFlatToggle(false)
    @Test fun cupertinoFlatToggleSelectsMountedSwitch() = mountedFlatToggle(true)

    private fun mountedFlatToggle(cupertino: Boolean) = runComposeUiTest {
        val schema = buildJsonSchema { put("type", "boolean") }
        val flatControl = Json.parseToJsonElement(
            """{"type":"Control","scope":"#","options":{"toggle":true}}"""
        ).jsonObject.toUiSchemaElement()
        val nestedControl = Json.parseToJsonElement(
            """{"type":"Control","scope":"#","options":{"jsonformsExtended":{"toggle":true}}}"""
        ).jsonObject.toUiSchemaElement()
        var flat by mutableStateOf(false)
        var changes = 0
        setContent {
            JsonForms(
                data = JsonPrimitive(false), schema = schema,
                uischema = if (flat) flatControl else nestedControl,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++ },
            )
        }
        val checkbox = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)
        val switch = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)
        onNode(checkbox).assertExists()
        onNode(switch).assertDoesNotExist()
        runOnIdle { flat = true }
        waitForIdle()
        onNode(switch).assertExists()
        onNode(checkbox).assertDoesNotExist()
        assertEquals(0, changes)
    }

    @Test fun serializedSelectionOptionStaysFlatAndNestedBagIsPreserved() {
        val schema = buildJsonSchema { put("type", "boolean") }
        val context = TesterContext(rootSchema = schema)
        val flat = Json.parseToJsonElement("""{"type":"Control","scope":"#","options":{"toggle":true}}""")
            .jsonObject.toUiSchemaElement() as ControlElement
        val nested = Json.parseToJsonElement("""{"type":"Control","scope":"#","options":{"jsonformsExtended":{"toggle":true}}}""")
            .jsonObject.toUiSchemaElement() as ControlElement

        assertEquals(JsonPrimitive(true), flat.options["toggle"])
        assertEquals(null, nested.options["toggle"])
        assertEquals(Json.parseToJsonElement("""{"toggle":true}"""), nested.options["jsonformsExtended"])
        assertTrue(materialBooleanToggleControlTester(flat, schema, context) > NOT_APPLICABLE)
        assertTrue(cupertinoBooleanToggleControlTester(flat, schema, context) > NOT_APPLICABLE)
        assertEquals(NOT_APPLICABLE, materialBooleanToggleControlTester(nested, schema, context))
        assertEquals(NOT_APPLICABLE, cupertinoBooleanToggleControlTester(nested, schema, context))
    }

    @Test fun materialUiPasswordEditsStoredString() = livePassword(cupertino = false, generatedUi = false)
    @Test fun cupertinoUiPasswordEditsStoredString() = livePassword(cupertino = true, generatedUi = false)
    @Test fun materialGeneratedUiSchemaPasswordEditsStoredString() = livePassword(cupertino = false, generatedUi = true)
    @Test fun cupertinoGeneratedUiSchemaPasswordEditsStoredString() = livePassword(cupertino = true, generatedUi = true)

    private fun livePassword(cupertino: Boolean, generatedUi: Boolean) = runComposeUiTest {
        val schema = buildJsonSchema {
            put("type", "string")
            if (generatedUi) put("format", "password")
        }
        var data: JsonElement by mutableStateOf(JsonPrimitive("old"))
        setContent {
            JsonForms(
                data = data,
                schema = schema,
                uischema = if (generatedUi) null else ControlElement(
                    scope = "#",
                    options = mapOf("format" to JsonPrimitive("password")),
                ),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data },
            )
        }
        if (!cupertino) onNodeWithContentDescription("Show password").assertExists()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("hunter2")
        waitForIdle()
        assertEquals(JsonPrimitive("hunter2"), data)
    }

    @Test fun materialUiChangeDiscardsDateDraft() = changedContractDiscardsDateDraft(false, false)
    @Test fun cupertinoUiChangeDiscardsDateDraft() = changedContractDiscardsDateDraft(true, false)
    @Test fun materialSchemaChangeDiscardsDateDraft() = changedContractDiscardsDateDraft(false, true)
    @Test fun cupertinoSchemaChangeDiscardsDateDraft() = changedContractDiscardsDateDraft(true, true)

    private fun changedContractDiscardsDateDraft(cupertino: Boolean, changeSchema: Boolean) = runComposeUiTest {
        var schema by mutableStateOf(buildJsonSchema { put("type", "string") })
        var control by mutableStateOf(ControlElement(
            scope = "#",
            options = mapOf("format" to JsonPrimitive("date"), "mask" to JsonPrimitive(false)),
        ))
        var validity: CombinedValidityStatus? by mutableStateOf(null)
        setContent {
            JsonForms(
                data = JsonPrimitive("2024-03-15"),
                schema = schema,
                uischema = control,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onValidityChange = { validity = it.status },
            )
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("bad")
        waitForIdle()
        assertEquals(CombinedValidityStatus.Pending, validity)
        runOnIdle {
            if (changeSchema) {
                schema = buildJsonSchema {
                    put("type", "string")
                    put("description", "Updated")
                }
            } else {
                control = control.copy(options = control.options + ("placeholder" to JsonPrimitive("Updated")))
            }
        }
        waitForIdle()
        assertEquals(CombinedValidityStatus.Valid, validity)
        onNodeWithText("03/15/2024").assertExists()
    }

    @Test fun materialHostReplacementDiscardsDateDraft() = hostReplacesDateDraft(false)
    @Test fun cupertinoHostReplacementDiscardsDateDraft() = hostReplacesDateDraft(true)

    private fun hostReplacesDateDraft(cupertino: Boolean) = runComposeUiTest {
        val schema = buildJsonSchema { put("type", "string") }
        val control = ControlElement(
            scope = "#",
            options = mapOf("format" to JsonPrimitive("date"), "mask" to JsonPrimitive(false)),
        )
        var data: JsonElement by mutableStateOf(JsonPrimitive("2024-03-15"))
        var validity: CombinedValidityStatus? by mutableStateOf(null)
        setContent {
            JsonForms(
                data = data,
                schema = schema,
                uischema = control,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data },
                onValidityChange = { validity = it.status },
            )
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("bad")
        waitForIdle()
        assertEquals(CombinedValidityStatus.Pending, validity)
        runOnIdle { data = JsonPrimitive("2024-04-10") }
        waitForIdle()
        assertEquals(JsonPrimitive("2024-04-10"), data)
        assertEquals(CombinedValidityStatus.Valid, validity)
        onNodeWithText("04/10/2024").assertExists()
    }

    @Test fun materialDateKeepsIncompleteDraft() = incompleteTemporalDraft("date", false)
    @Test fun cupertinoDateKeepsIncompleteDraft() = incompleteTemporalDraft("date", true)
    @Test fun materialTimeKeepsIncompleteDraft() = incompleteTemporalDraft("time", false)
    @Test fun cupertinoTimeKeepsIncompleteDraft() = incompleteTemporalDraft("time", true)
    @Test fun materialDateTimeKeepsIncompleteDraft() = incompleteTemporalDraft("date-time", false)
    @Test fun cupertinoDateTimeKeepsIncompleteDraft() = incompleteTemporalDraft("date-time", true)

    private fun incompleteTemporalDraft(format: String, cupertino: Boolean) = runComposeUiTest {
        val (initial, correctedInput, correctedStored) = when (format) {
            "date" -> Triple("2024-03-15", "03/16/2024", "2024-03-16")
            "time" -> Triple("14:05:00Z", "3:06 PM", "15:06:00Z")
            else -> Triple("2024-03-15 14:05", "03/16/2024 3:06 PM", "2024-03-16 15:06")
        }
        val schema = buildJsonSchema { put("type", "string") }
        val control = ControlElement(
            scope = "#",
            options = mapOf(
                "format" to JsonPrimitive(format),
                "mask" to JsonPrimitive(false),
                "dateTimeSaveFormat" to JsonPrimitive("YYYY-MM-DD HH:mm"),
            ),
        )
        var data: JsonElement by mutableStateOf(JsonPrimitive(initial))
        var validity: CombinedValidityStatus? by mutableStateOf(null)
        var changes = 0
        setContent {
            JsonForms(
                data = data,
                schema = schema,
                uischema = control,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++; data = it.data },
                onValidityChange = { validity = it.status },
            )
        }
        waitForIdle()
        assertEquals(CombinedValidityStatus.Valid, validity)
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("bad")
        waitForIdle()
        assertEquals(JsonPrimitive(initial), data)
        assertEquals(0, changes)
        assertEquals(CombinedValidityStatus.Pending, validity)
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement(correctedInput)
        waitForIdle()
        assertEquals(JsonPrimitive(correctedStored), data)
        assertEquals(1, changes)
        assertEquals(CombinedValidityStatus.Valid, validity)
    }

    @Test fun materialDateTypedSaveFormat() = typedTemporalSave("date", false)
    @Test fun cupertinoDateTypedSaveFormat() = typedTemporalSave("date", true)
    @Test fun materialTimeTypedSaveFormat() = typedTemporalSave("time", false)
    @Test fun cupertinoTimeTypedSaveFormat() = typedTemporalSave("time", true)
    @Test fun materialDateTimeTypedSaveFormat() = typedTemporalSave("date-time", false)
    @Test fun cupertinoDateTimeTypedSaveFormat() = typedTemporalSave("date-time", true)

    @Test fun materialDateViewsDoNotChangeSaveFormat() = typedTemporalSave("date", false, views = true)
    @Test fun cupertinoDateViewsDoNotChangeSaveFormat() = typedTemporalSave("date", true, views = true)
    @Test fun materialTimeViewsDoNotChangeSaveFormat() = typedTemporalSave("time", false, views = true)
    @Test fun cupertinoTimeViewsDoNotChangeSaveFormat() = typedTemporalSave("time", true, views = true)
    @Test fun materialDateTimeViewsDoNotChangeSaveFormat() = typedTemporalSave("date-time", false, views = true)
    @Test fun cupertinoDateTimeViewsDoNotChangeSaveFormat() = typedTemporalSave("date-time", true, views = true)

    private fun typedTemporalSave(format: String, cupertino: Boolean, views: Boolean = false) = runComposeUiTest {
        val (saveKey, input, stored) = when (format) {
            "date" -> Triple("dateSaveFormat", "03/15/2024", "2024-03")
            "time" -> Triple("timeSaveFormat", "2:05 PM", "14:05:00")
            else -> Triple("dateTimeSaveFormat", "03/15/2024 2:05 PM", "2024-03-15 14:05")
        }
        val saveFormat = when (format) {
            "date" -> "YYYY-MM"
            "time" -> "HH:mm:ss"
            else -> "YYYY-MM-DD HH:mm"
        }
        val schema = buildJsonSchema { put("type", "string") }
        val control = ControlElement(
            scope = "#",
            options = mapOf(
                "format" to JsonPrimitive(format),
                saveKey to JsonPrimitive(saveFormat),
                "mask" to JsonPrimitive(false),
            ) + if (views) mapOf("views" to JsonArray((when (format) {
                "date" -> listOf("year")
                "time" -> listOf("hours")
                else -> listOf("year", "month", "day")
            }).map(::JsonPrimitive))) else emptyMap(),
        )
        var data: JsonElement by mutableStateOf(JsonPrimitive(""))
        setContent {
            JsonForms(
                data = data,
                schema = schema,
                uischema = control,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data },
            )
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement(input)
        waitForIdle()
        assertEquals(JsonPrimitive(stored), data)
    }

    @Test fun cupertinoDatePickerTranslates() = cupertinoTemporalTranslations("date")
    @Test fun cupertinoTimePickerTranslates() = cupertinoTemporalTranslations("time")
    @Test fun cupertinoDateTimePickerTranslates() = cupertinoTemporalTranslations("date-time")

    private fun cupertinoTemporalTranslations(format: String) = runComposeUiTest {
        val schema = buildJsonSchema { put("type", "string") }
        val control = ControlElement(
            scope = "#",
            options = mapOf("format" to JsonPrimitive(format), "mask" to JsonPrimitive(false),
                "showActions" to JsonPrimitive(true)),
        )
        setContent {
            JsonForms(
                data = JsonPrimitive(""),
                schema = schema,
                uischema = control,
                renderers = cupertinoRenderers,
                i18n = JsonFormsI18nState(locale = "bg", translate = { key, fallback, _ ->
                    when (key.substringAfterLast('.')) {
                        "chooseDateTooltip", "chooseTimeTooltip", "chooseDateTimeTooltip" -> "Избери"
                        "chooseDateAriaLabel", "chooseTimeAriaLabel", "chooseDateTimeAriaLabel" -> "Отвори избор"
                        "selectDate", "selectTime", "selectDateTime" -> "Избери стойност"
                        "cancel" -> "Отказ"
                        "ok" -> "Готово"
                        "invalidDate", "invalidTime", "invalidDateTime" -> "Невалидно"
                        else -> fallback
                    }
                }),
            )
        }
        onNodeWithContentDescription("Отвори избор").assertExists()
        onNodeWithText("Избери").performClick()
        onNodeWithText("Избери стойност").assertExists()
        onNodeWithText("Отказ").performClick()
        onNodeWithText("Избери стойност").assertDoesNotExist()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("bad")
        onNodeWithText("Невалидно").assertExists()
    }

    @Test fun materialDateSelection() = temporalSelection("date", cupertino = false)
    @Test fun cupertinoDateSelection() = temporalSelection("date", cupertino = true)
    @Test fun materialTimeSelection() = temporalSelection("time", cupertino = false)
    @Test fun cupertinoTimeSelection() = temporalSelection("time", cupertino = true)
    @Test fun materialDateTimeSelection() = temporalSelection("date-time", cupertino = false)
    @Test fun cupertinoDateTimeSelection() = temporalSelection("date-time", cupertino = true)

    private fun temporalSelection(format: String, cupertino: Boolean) {
        liveTemporal(format, cupertino, generatedUi = false)
        liveTemporal(format, cupertino, generatedUi = true)
    }

    private fun liveTemporal(format: String, cupertino: Boolean, generatedUi: Boolean) = runComposeUiTest {
        val schema = buildJsonSchema {
            put("type", "string")
            if (generatedUi) put("format", format)
        }
        setContent {
            JsonForms(
                data = JsonPrimitive(""),
                schema = schema,
                uischema = if (generatedUi) null else ControlElement(
                    scope = "#",
                    options = mapOf("format" to JsonPrimitive(format)),
                ),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
            )
        }
        onAllNodes(hasSetTextAction()).onFirst().assertExists()
        if (cupertino) {
            onNodeWithText("Pick").assertExists()
        } else {
            val action = when (format) {
                "date" -> "Choose date"
                "time" -> "Choose time"
                else -> "Choose date and time"
            }
            onNodeWithContentDescription(action).assertExists()
        }
    }

    @Test
    fun temporalUiFormatsSelectOnlyStringControlsInBothFamilies() {
        val plainString = buildJsonSchema { put("type", "string") }
        val integer = buildJsonSchema { put("type", "integer") }
        val choices: List<Triple<String, RankedTester, RankedTester>> = listOf(
            Triple("date", materialDateControlTester, cupertinoDateControlTester),
            Triple("time", materialTimeControlTester, cupertinoTimeControlTester),
            Triple("date-time", materialDateTimeControlTester, cupertinoDateTimeControlTester),
        )

        choices.forEach { (format, material, cupertino) ->
            val control = ControlElement(
                scope = "#",
                options = mapOf("format" to JsonPrimitive(format)),
            )
            val stringContext = TesterContext(rootSchema = plainString)
            val integerContext = TesterContext(rootSchema = integer)
            assertTrue(material(control, plainString, stringContext) >
                materialTextControlTester(control, plainString, stringContext), format)
            assertTrue(cupertino(control, plainString, stringContext) >
                cupertinoTextControlTester(control, plainString, stringContext), format)
            assertEquals(NOT_APPLICABLE, material(control, integer, integerContext), format)
            assertEquals(NOT_APPLICABLE, cupertino(control, integer, integerContext), format)
        }
    }

    @Test
    fun uiPasswordFormatOutranksGenericStringInBothFamilies() {
        val schema = buildJsonSchema { put("type", "string") }
        val control = ControlElement(
            scope = "#",
            options = mapOf("format" to JsonPrimitive("password")),
        )
        val context = TesterContext(rootSchema = schema)

        assertEquals(2, materialPasswordControlTester(control, schema, context))
        assertEquals(102, cupertinoPasswordControlTester(control, schema, context))
        assertTrue(materialPasswordControlTester(control, schema, context) >
            materialTextControlTester(control, schema, context))
        assertTrue(cupertinoPasswordControlTester(control, schema, context) >
            cupertinoTextControlTester(control, schema, context))
    }

    @Test
    fun schemaPasswordFormatSelectsWithoutUiOption() {
        val schema = buildJsonSchema {
            put("type", "string")
            put("format", "password")
        }
        val control = ControlElement(scope = "#")
        val context = TesterContext(rootSchema = schema)

        assertEquals(2, materialPasswordControlTester(control, schema, context))
        assertEquals(102, cupertinoPasswordControlTester(control, schema, context))
    }

    @Test
    fun configFormatAloneDoesNotSelectPassword() {
        val schema = buildJsonSchema { put("type", "string") }
        val control = ControlElement(scope = "#")
        val context = TesterContext(rootSchema = schema, config = mapOf("format" to "password"))

        assertEquals(NOT_APPLICABLE, materialPasswordControlTester(control, schema, context))
        assertEquals(NOT_APPLICABLE, cupertinoPasswordControlTester(control, schema, context))
    }
}
