package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.ListWithDetailElement
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoCells
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialCells
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ComposedScopeSelectionUiTest {
    @Test fun materialUntypedAllOfBranchEditsTypedProperties() = composedObject(false)
    @Test fun cupertinoUntypedAllOfBranchEditsTypedProperties() = composedObject(true)
    @Test fun materialScalarAllOfEditsString() = scalar(false)
    @Test fun cupertinoScalarAllOfEditsString() = scalar(true)
    @Test fun materialAnyOfBranchFieldsKeepTheirTypes() = alternativeBranches(false)
    @Test fun cupertinoAnyOfBranchFieldsKeepTheirTypes() = alternativeBranches(true)
    @Test fun materialOneOfStringBranchEditsTypedValue() = oneOfBranch(false, false)
    @Test fun cupertinoOneOfStringBranchEditsTypedValue() = oneOfBranch(true, false)
    @Test fun materialOneOfIntegerBranchEditsTypedValue() = oneOfBranch(false, true)
    @Test fun cupertinoOneOfIntegerBranchEditsTypedValue() = oneOfBranch(true, true)
    @Test fun materialThenBranchEditsString() = conditionalBranch(false, false)
    @Test fun cupertinoThenBranchEditsString() = conditionalBranch(true, false)
    @Test fun materialElseBranchEditsInteger() = conditionalBranch(false, true)
    @Test fun cupertinoElseBranchEditsInteger() = conditionalBranch(true, true)
    @Test fun materialOverlappingOneOfSwitchesEditorWithHostValue() = overlappingValue(false, "oneOf")
    @Test fun cupertinoOverlappingOneOfSwitchesEditorWithHostValue() = overlappingValue(true, "oneOf")
    @Test fun materialOverlappingAnyOfSwitchesEditorWithHostValue() = overlappingValue(false, "anyOf")
    @Test fun cupertinoOverlappingAnyOfSwitchesEditorWithHostValue() = overlappingValue(true, "anyOf")
    @Test fun materialNestedOneOfSwitchesEditorWithHostValue() = overlappingValue(false, "oneOf", true)
    @Test fun cupertinoNestedOneOfSwitchesEditorWithHostValue() = overlappingValue(true, "oneOf", true)
    @Test fun materialNestedAnyOfSwitchesEditorWithHostValue() = overlappingValue(false, "anyOf", true)
    @Test fun cupertinoNestedAnyOfSwitchesEditorWithHostValue() = overlappingValue(true, "anyOf", true)
    @Test fun materialArrayItemOneOfSwitchesEditorWithHostValue() = arrayItemValue(false, "oneOf")
    @Test fun cupertinoArrayItemOneOfSwitchesEditorWithHostValue() = arrayItemValue(true, "oneOf")
    @Test fun materialArrayItemAnyOfSwitchesEditorWithHostValue() = arrayItemValue(false, "anyOf")
    @Test fun cupertinoArrayItemAnyOfSwitchesEditorWithHostValue() = arrayItemValue(true, "anyOf")
    @Test fun materialListDetailOneOfSwitchesEditorWithHostValue() = listDetailItemValue(false, "oneOf")
    @Test fun cupertinoListDetailOneOfSwitchesEditorWithHostValue() = listDetailItemValue(true, "oneOf")
    @Test fun materialListDetailAnyOfSwitchesEditorWithHostValue() = listDetailItemValue(false, "anyOf")
    @Test fun cupertinoListDetailAnyOfSwitchesEditorWithHostValue() = listDetailItemValue(true, "anyOf")
    @Test fun materialTableOneOfUsesEachRowsStoredType() = tableItemValue(false, "oneOf")
    @Test fun cupertinoTableOneOfUsesEachRowsStoredType() = tableItemValue(true, "oneOf")
    @Test fun materialTableAnyOfUsesEachRowsStoredType() = tableItemValue(false, "anyOf")
    @Test fun cupertinoTableAnyOfUsesEachRowsStoredType() = tableItemValue(true, "anyOf")
    @Test fun materialOneOfRequiredConstSwitchesPasswordEditor() = constDiscriminator(false, "oneOf")
    @Test fun cupertinoOneOfRequiredConstSwitchesPasswordEditor() = constDiscriminator(true, "oneOf")
    @Test fun materialAnyOfRequiredConstSwitchesPasswordEditor() = constDiscriminator(false, "anyOf")
    @Test fun cupertinoAnyOfRequiredConstSwitchesPasswordEditor() = constDiscriminator(true, "anyOf")
    @Test fun materialOneOfNumericConstSwitchesPasswordEditor() = numericConstDiscriminator(false, "oneOf")
    @Test fun cupertinoOneOfNumericConstSwitchesPasswordEditor() = numericConstDiscriminator(true, "oneOf")
    @Test fun materialAnyOfNumericConstSwitchesPasswordEditor() = numericConstDiscriminator(false, "anyOf")
    @Test fun cupertinoAnyOfNumericConstSwitchesPasswordEditor() = numericConstDiscriminator(true, "anyOf")
    @Test fun materialNestedOneOfRequiredConstSwitchesPasswordEditor() = constDiscriminator(false, "oneOf", true)
    @Test fun cupertinoNestedOneOfRequiredConstSwitchesPasswordEditor() = constDiscriminator(true, "oneOf", true)
    @Test fun materialNestedAnyOfRequiredConstSwitchesPasswordEditor() = constDiscriminator(false, "anyOf", true)
    @Test fun cupertinoNestedAnyOfRequiredConstSwitchesPasswordEditor() = constDiscriminator(true, "anyOf", true)
    @Test fun materialTableOneOfUsesEachRowsRequiredConst() = tableConstDiscriminator(false, "oneOf")
    @Test fun cupertinoTableOneOfUsesEachRowsRequiredConst() = tableConstDiscriminator(true, "oneOf")
    @Test fun materialTableAnyOfUsesEachRowsRequiredConst() = tableConstDiscriminator(false, "anyOf")
    @Test fun cupertinoTableAnyOfUsesEachRowsRequiredConst() = tableConstDiscriminator(true, "anyOf")
    @Test fun materialMissingOneOfValueUsesRequiredConst() = missingConstDiscriminator(false, "oneOf")
    @Test fun cupertinoMissingOneOfValueUsesRequiredConst() = missingConstDiscriminator(true, "oneOf")
    @Test fun materialMissingAnyOfValueUsesRequiredConst() = missingConstDiscriminator(false, "anyOf")
    @Test fun cupertinoMissingAnyOfValueUsesRequiredConst() = missingConstDiscriminator(true, "anyOf")
    @Test fun materialTableMissingOneOfValueUsesRequiredConst() = tableMissingConstDiscriminator(false, "oneOf")
    @Test fun cupertinoTableMissingOneOfValueUsesRequiredConst() = tableMissingConstDiscriminator(true, "oneOf")
    @Test fun materialTableMissingAnyOfValueUsesRequiredConst() = tableMissingConstDiscriminator(false, "anyOf")
    @Test fun cupertinoTableMissingAnyOfValueUsesRequiredConst() = tableMissingConstDiscriminator(true, "anyOf")
    @Test fun materialOneOfBranchOnlyFieldUsesRequiredConst() = branchOnlyField(false, "oneOf")
    @Test fun cupertinoOneOfBranchOnlyFieldUsesRequiredConst() = branchOnlyField(true, "oneOf")
    @Test fun materialAnyOfBranchOnlyFieldUsesRequiredConst() = branchOnlyField(false, "anyOf")
    @Test fun cupertinoAnyOfBranchOnlyFieldUsesRequiredConst() = branchOnlyField(true, "anyOf")
    @Test fun materialOneOfAuthoredColumnsUseBranchOnlyFields() = branchOnlyTableColumns(false, "oneOf")
    @Test fun cupertinoOneOfAuthoredColumnsUseBranchOnlyFields() = branchOnlyTableColumns(true, "oneOf")
    @Test fun materialAnyOfAuthoredColumnsUseBranchOnlyFields() = branchOnlyTableColumns(false, "anyOf")
    @Test fun cupertinoAnyOfAuthoredColumnsUseBranchOnlyFields() = branchOnlyTableColumns(true, "anyOf")
    @Test fun materialOneOfAutomaticTableAddsChosenBranch() = automaticBranchTable(false, "oneOf")
    @Test fun cupertinoOneOfAutomaticTableAddsChosenBranch() = automaticBranchTable(true, "oneOf")
    @Test fun materialAnyOfAutomaticTableAddsChosenBranch() = automaticBranchTable(false, "anyOf")
    @Test fun cupertinoAnyOfAutomaticTableAddsChosenBranch() = automaticBranchTable(true, "anyOf")
    @Test fun materialAutomaticBranchMenuUsesBulgarianFallback() = automaticBranchTable(false, "oneOf", true)
    @Test fun cupertinoAutomaticBranchMenuUsesBulgarianFallback() = automaticBranchTable(true, "oneOf", true)
    @Test fun materialOneOfSharedColumnsAddChosenBranch() = sharedBranchTable(false, "oneOf")
    @Test fun cupertinoOneOfSharedColumnsAddChosenBranch() = sharedBranchTable(true, "oneOf")
    @Test fun materialAnyOfSharedColumnsAddChosenBranch() = sharedBranchTable(false, "anyOf")
    @Test fun cupertinoAnyOfSharedColumnsAddChosenBranch() = sharedBranchTable(true, "anyOf")
    @Test fun materialOneOfAutomaticTableRepairsUnknownBranch() = repairAutomaticBranchTable(false, "oneOf", false)
    @Test fun cupertinoOneOfAutomaticTableRepairsUnknownBranch() = repairAutomaticBranchTable(true, "oneOf", false)
    @Test fun materialAnyOfAutomaticTableRepairsMissingBranch() = repairAutomaticBranchTable(false, "anyOf", true)
    @Test fun cupertinoAnyOfAutomaticTableRepairsMissingBranch() = repairAutomaticBranchTable(true, "anyOf", true)

    private fun sharedBranchTable(cupertino: Boolean, keyword: String) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"array","items":{"type":"object","$keyword":[
                {"title":"Plain","properties":{"mode":{"const":"plain"},
                    "note":{"type":"string"}},"required":["mode"]},
                {"title":"Secret","properties":{"mode":{"const":"secret"},
                    "note":{"type":"string"}},"required":["mode"]}
            ]}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement(
            """[{"mode":"plain","note":"Ada"}]"""))
        var writes = 0
        var documentErrors = -1
        setContent {
            JsonForms(data = data, schema = schema, uischema = ControlElement("#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                cells = if (cupertino) cupertinoCells else materialCells,
                onChange = { data = it.data; documentErrors = it.errors.size; writes++ })
        }
        onNodeWithTag("table-add-item").performClick()
        onNodeWithTag("add-branch-1").assertExists()
        runOnIdle { assertEquals(0, writes) }
        onNodeWithTag("add-branch-1").performClick()
        runOnIdle {
            assertEquals(Json.parseToJsonElement(
                """[{"mode":"plain","note":"Ada"},{"mode":"secret"}]"""), data)
            assertEquals(1, writes)
            assertEquals(0, documentErrors)
        }
    }

    private fun repairAutomaticBranchTable(cupertino: Boolean, keyword: String, missing: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"array","items":{"type":"object","$keyword":[
                {"properties":{"mode":{"const":"plain"},"note":{"type":"string"}},
                    "required":["mode","note"]},
                {"properties":{"mode":{"const":"secret"},
                    "secret":{"type":"string","format":"password"}},
                    "required":["mode","secret"]}
            ]}
        }""").jsonObject)
        val imported = if (missing) """[{"secret":"legacy"}]"""
            else """[{"mode":"other","secret":"legacy"}]"""
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement(imported))
        var writes = 0
        var documentErrors = -1
        setContent {
            JsonForms(data = data, schema = schema, uischema = ControlElement("#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                cells = if (cupertino) cupertinoCells else materialCells,
                onChange = { data = it.data; documentErrors = it.errors.size; writes++ })
        }
        val tag = if (cupertino) "cupertino-enum-cell" else "material-enum-cell"
        onNodeWithTag(tag, useUnmergedTree = true).assertExists()
        if (!missing) onNodeWithText("Current value outside choices: \"other\"").assertExists()
        runOnIdle {
            assertEquals(Json.parseToJsonElement(imported), data)
            assertEquals(0, writes)
        }
        onNodeWithTag(tag, useUnmergedTree = true).performClick()
        onNodeWithContentDescription("secret").performClick()
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""[{"mode":"secret","secret":"legacy"}]"""), data)
            assertEquals(1, writes)
            assertEquals(0, documentErrors)
        }
        onNodeWithContentDescription("Show password").assertExists()
    }

    private fun automaticBranchTable(cupertino: Boolean, keyword: String, bulgarian: Boolean = false) = runComposeUiTest {
        val plainTitle = if (bulgarian) "" else "\"title\":\"Plain\","
        val secretTitle = if (bulgarian) "" else "\"title\":\"Secret\","
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"array","items":{"type":"object","$keyword":[
                {$plainTitle"properties":{"mode":{"const":"plain"},
                    "note":{"type":"string"}},"required":["mode","note"]},
                {$secretTitle"properties":{"mode":{"const":"secret"},
                    "secret":{"type":"string","format":"password","default":"Draft"}},
                    "required":["mode","secret"]}
            ]}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement(
            """[{"mode":"plain","note":"Ada"}]"""))
        var writes = 0
        var documentErrors = -1
        setContent {
            JsonForms(data = data, schema = schema, uischema = ControlElement("#"),
                i18n = JsonFormsI18nState(locale = if (bulgarian) "bg" else "en"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                cells = if (cupertino) cupertinoCells else materialCells,
                onChange = { data = it.data; documentErrors = it.errors.size; writes++ })
        }
        onNodeWithTag("table-add-item").performClick()
        onNodeWithTag("add-branch-1").assertExists()
        if (bulgarian) onNodeWithText("Вариант 2").assertExists()
        assertEquals(0, writes)
        onNodeWithTag("add-branch-1").performClick()
        runOnIdle {
            assertEquals(Json.parseToJsonElement(
                """[{"mode":"plain","note":"Ada"},{"mode":"secret","secret":"Draft"}]"""), data)
            assertEquals(1, writes)
            assertEquals(0, documentErrors)
        }
        if (!bulgarian) onNodeWithContentDescription("Show password").assertExists()
        onAllNodes(hasSetTextAction())[1].performTextReplacement("New secret")
        runOnIdle {
            assertEquals(Json.parseToJsonElement(
                """[{"mode":"plain","note":"Ada"},{"mode":"secret","secret":"New secret"}]"""), data)
            assertEquals(2, writes)
            assertEquals(0, documentErrors)
        }
        onNodeWithTag("table-add-item").performClick()
        onNodeWithTag("add-branch-1").assertExists()
        runOnIdle { data = Json.parseToJsonElement(
            """[{"mode":"plain","note":"Eve"},{"mode":"secret","secret":"New secret"}]""") }
        waitForIdle()
        onNodeWithTag("add-branch-1").assertDoesNotExist()
        assertEquals(2, writes)
    }

    private fun branchOnlyField(cupertino: Boolean, keyword: String) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "$keyword":[
                {"properties":{"mode":{"const":"plain"},"note":{"type":"string"}},
                    "required":["mode","note"]},
                {"properties":{"mode":{"const":"secret"},
                    "secret":{"type":"string","format":"password"}},
                    "required":["mode","secret"]}
            ]
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"mode":"secret"}"""))
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = ControlElement("#/properties/secret"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; writes++ })
        }
        onNodeWithContentDescription("Show password").assertExists()
        assertEquals(0, writes)
        onAllNodes(hasSetTextAction())[0].performTextReplacement("New secret")
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{"mode":"secret","secret":"New secret"}"""), data)
            assertEquals(1, writes)
        }
    }

    private fun branchOnlyTableColumns(cupertino: Boolean, keyword: String) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"array","items":{"type":"object","$keyword":[
                {"properties":{"mode":{"const":"plain"},"note":{"type":"string"}},
                    "required":["mode","note"]},
                {"properties":{"mode":{"const":"secret"},
                    "secret":{"type":"string","format":"password"}},
                    "required":["mode","secret"]}
            ]}
        }""").jsonObject)
        val options = mapOf("columnDefs" to Json.parseToJsonElement("""[
            {"field":"mode"},{"field":"note"},{"field":"secret"}
        ]"""))
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement(
            """[{"mode":"plain","note":"Ada","secret":"legacy"},{"mode":"secret"}]"""))
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#", options = options),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                cells = if (cupertino) cupertinoCells else materialCells,
                onChange = { data = it.data; writes++ })
        }
        fun revealCount() = onAllNodes(hasContentDescription("Show password")).fetchSemanticsNodes().size
        assertEquals(1, revealCount())
        onNodeWithText("\"legacy\"").assertExists()
        assertEquals(0, writes)
        onAllNodes(hasSetTextAction())[1].performTextReplacement("New secret")
        runOnIdle {
            assertEquals(Json.parseToJsonElement(
                """[{"mode":"plain","note":"Ada","secret":"legacy"},{"mode":"secret","secret":"New secret"}]"""), data)
            assertEquals(1, writes)
        }
        runOnIdle { data = Json.parseToJsonElement(
            """[{"mode":"plain","note":"Ada"},{"mode":"plain","note":"Bee"}]""") }
        waitForIdle()
        assertEquals(0, revealCount())
        assertEquals(1, writes)
    }

    private fun missingConstDiscriminator(cupertino: Boolean, keyword: String) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","$keyword":[
                {"properties":{"mode":{"const":"plain"},"value":{"type":"string"}},
                    "required":["mode","value"]},
                {"properties":{"mode":{"const":"secret"},
                    "value":{"type":"string","format":"password"}},
                    "required":["mode","value"]}
            ]
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"mode":"secret"}"""))
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = ControlElement("#/properties/value"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; writes++ })
        }
        onNodeWithContentDescription("Show password").assertExists()
        assertEquals(0, writes)
        onAllNodes(hasSetTextAction())[0].performTextReplacement("New secret")
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{"mode":"secret","value":"New secret"}"""), data)
            assertEquals(1, writes)
        }
    }

    private fun tableMissingConstDiscriminator(cupertino: Boolean, keyword: String) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"array","items":{"type":"object","$keyword":[
                {"properties":{"mode":{"type":"string","const":"plain"},
                    "value":{"type":"string"}},"required":["mode","value"]},
                {"properties":{"mode":{"type":"string","const":"secret"},
                    "value":{"type":"string","format":"password"}},
                    "required":["mode","value"]}
            ]}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement(
            """[{"mode":"secret"},{"mode":"plain","value":"Ada"}]"""))
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#", options = mapOf(
                    "table" to kotlinx.serialization.json.JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                cells = if (cupertino) cupertinoCells else materialCells,
                onChange = { data = it.data; writes++ })
        }
        fun revealCount() = onAllNodes(hasContentDescription("Show password")).fetchSemanticsNodes().size
        assertEquals(1, revealCount())
        assertEquals(0, writes)
        runOnIdle { data = Json.parseToJsonElement(
            """[{"mode":"plain"},{"mode":"plain","value":"Ada"}]""") }
        waitForIdle()
        assertEquals(0, revealCount())
        assertEquals(0, writes)
    }

    private fun tableConstDiscriminator(cupertino: Boolean, keyword: String) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"array","items":{"type":"object","$keyword":[
                {"properties":{"mode":{"type":"string","const":"plain"},
                    "value":{"type":"string"}},"required":["mode","value"]},
                {"properties":{"mode":{"type":"string","const":"secret"},
                    "value":{"type":"string","format":"password"}},
                    "required":["mode","value"]}
            ]}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement(
            """[{"mode":"plain","value":"Ada"},{"mode":"secret","value":"Bee"}]"""))
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#", options = mapOf(
                    "table" to kotlinx.serialization.json.JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                cells = if (cupertino) cupertinoCells else materialCells,
                onChange = { data = it.data; writes++ })
        }
        fun revealCount() = onAllNodes(hasContentDescription("Show password")).fetchSemanticsNodes().size
        assertEquals(1, revealCount())
        runOnIdle { data = Json.parseToJsonElement(
            """[{"mode":"secret","value":"Ada"},{"mode":"secret","value":"Bee"}]""") }
        waitForIdle()
        assertEquals(0, writes)
        assertEquals(2, revealCount())
        runOnIdle { data = Json.parseToJsonElement(
            """[{"mode":"plain","value":"Ada"},{"mode":"plain","value":"Bee"}]""") }
        waitForIdle()
        assertEquals(0, writes)
        assertEquals(0, revealCount())
        onAllNodes(hasSetTextAction())[0].performTextReplacement("Eve")
        runOnIdle {
            assertEquals(Json.parseToJsonElement(
                """[{"mode":"plain","value":"Eve"},{"mode":"plain","value":"Bee"}]"""), data)
            assertEquals(1, writes)
        }
    }

    private fun numericConstDiscriminator(cupertino: Boolean, keyword: String) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","$keyword":[
                {"properties":{"mode":{"const":1},"value":{"type":"string"}},
                    "required":["mode","value"]},
                {"properties":{"mode":{"const":2},
                    "value":{"type":"string","format":"password"}},
                    "required":["mode","value"]}
            ]
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement(
            """{"mode":1.0,"value":"Ada"}"""))
        var writes = 0
        var documentErrors = -1
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; documentErrors = it.errors.size; writes++ })
        }
        onNodeWithContentDescription("Show password").assertDoesNotExist()
        runOnIdle { assertEquals(0, writes); data = Json.parseToJsonElement(
            """{"mode":2e0,"value":"Ada"}""") }
        onNodeWithContentDescription("Show password").assertExists()
        runOnIdle { assertEquals(0, writes) }
        onAllNodes(hasSetTextAction())[0].performTextReplacement("New secret")
        runOnIdle {
            assertEquals(Json.parseToJsonElement(
                """{"mode":2e0,"value":"New secret"}"""), data)
            assertEquals(1, writes)
            assertEquals(0, documentErrors)
        }
    }

    private fun constDiscriminator(cupertino: Boolean, keyword: String, nested: Boolean = false) = runComposeUiTest {
        fun branch(mode: String, password: Boolean): String {
            val value = if (password) """{"type":"string","format":"password"}"""
                else """{"type":"string"}"""
            val fields = """"mode":{"const":"$mode"},"value":$value"""
            return if (nested) """{"properties":{"record":{"type":"object",
                "properties":{$fields},"required":["mode","value"]}}}"""
            else """{"properties":{$fields},"required":["mode","value"]}"""
        }
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","$keyword":[
                ${branch("plain", false)},${branch("secret", true)}
            ]
        }""").jsonObject)
        fun stored(mode: String, value: String) = Json.parseToJsonElement(
            if (nested) """{"record":{"mode":"$mode","value":"$value"}}"""
            else """{"mode":"$mode","value":"$value"}""")
        var data by mutableStateOf(stored("plain", "Ada"))
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement(if (nested) "#/properties/record/properties/value"
                    else "#/properties/value"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; writes++ })
        }
        onNodeWithContentDescription("Show password").assertDoesNotExist()
        onAllNodes(hasSetTextAction())[0].performTextReplacement("Eve")
        runOnIdle {
            assertEquals(stored("plain", "Eve"), data)
            assertEquals(1, writes)
            data = stored("secret", "Eve")
        }
        waitForIdle()
        assertEquals(1, writes)
        onNodeWithContentDescription("Show password").assertExists()
        onAllNodes(hasSetTextAction())[0].performTextReplacement("New secret")
        runOnIdle {
            assertEquals(stored("secret", "New secret"), data)
            assertEquals(2, writes)
        }
    }

    private fun tableItemValue(cupertino: Boolean, keyword: String) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"array","items":{"type":"object","$keyword":[
                {"properties":{"value":{"type":"string"}},"required":["value"]},
                {"properties":{"value":{"type":"integer"}},"required":["value"]}
            ]}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement(
            """[{"value":"Ada"},{"value":7}]"""))
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#", options = if (keyword == "oneOf") mapOf(
                    "table" to kotlinx.serialization.json.JsonPrimitive(true)) else emptyMap()),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                cells = if (cupertino) cupertinoCells else materialCells,
                onChange = { data = it.data; writes++ })
        }
        assertEquals(2, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onAllNodes(hasSetTextAction())[0].performTextReplacement("Eve")
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""[{"value":"Eve"},{"value":7}]"""), data)
            assertEquals(1, writes)
        }
        onAllNodes(hasSetTextAction())[1].performTextReplacement("8")
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""[{"value":"Eve"},{"value":8}]"""), data)
            assertEquals(2, writes)
            data = Json.parseToJsonElement("""[{"value":9},{"value":8}]""")
        }
        waitForIdle()
        assertEquals(2, writes)
        onAllNodes(hasSetTextAction())[0].performTextReplacement("10")
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""[{"value":10},{"value":8}]"""), data)
            assertEquals(3, writes)
        }
    }

    private fun listDetailItemValue(cupertino: Boolean, keyword: String) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"rows":{"type":"array","items":{
                "type":"object","properties":{"label":{"type":"string"}},
                "$keyword":[
                    {"properties":{"value":{"type":"string"}},"required":["value"]},
                    {"properties":{"value":{"type":"integer"}},"required":["value"]}
                ]
            }}}
        }""").jsonObject)
        fun stored(value: JsonElement) = Json.parseToJsonElement("""{"rows":[{"label":"First","value":$value}]}""")
        var data by mutableStateOf(stored(kotlinx.serialization.json.JsonPrimitive("Ada")))
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ListWithDetailElement("#/properties/rows", options = mapOf(
                    "elementLabelProp" to kotlinx.serialization.json.JsonPrimitive("label"),
                    "detail" to Json.parseToJsonElement(
                        """{"type":"Control","scope":"#/properties/value"}"""))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; writes++ })
        }
        onNodeWithText("First").performClick()
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        runOnIdle { data = stored(kotlinx.serialization.json.JsonPrimitive(7)) }
        waitForIdle()
        assertEquals(0, writes)
        onNodeWithText("First").performClick()
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onAllNodes(hasSetTextAction())[0].performTextReplacement("8")
        runOnIdle {
            assertEquals(stored(kotlinx.serialization.json.JsonPrimitive(8)), data)
            assertEquals(1, writes)
        }
    }

    private fun arrayItemValue(cupertino: Boolean, keyword: String) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"rows":{"type":"array","items":{
                "type":"object","$keyword":[
                    {"properties":{"value":{"type":"string"}},"required":["value"]},
                    {"properties":{"value":{"type":"integer"}},"required":["value"]}
                ]
            }}}
        }""").jsonObject)
        fun stored(value: JsonElement) = Json.parseToJsonElement("""{"rows":[{"value":$value}]}""")
        var data by mutableStateOf(stored(kotlinx.serialization.json.JsonPrimitive("Ada")))
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/rows", options = mapOf(
                    "detail" to Json.parseToJsonElement(
                        """{"type":"Control","scope":"#/properties/value"}"""))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; writes++ })
        }
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onAllNodes(hasSetTextAction())[0].performTextReplacement("Eve")
        runOnIdle {
            assertEquals(stored(kotlinx.serialization.json.JsonPrimitive("Eve")), data)
            assertEquals(1, writes)
            data = stored(kotlinx.serialization.json.JsonPrimitive(7))
        }
        waitForIdle()
        assertEquals(1, writes)
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onAllNodes(hasSetTextAction())[0].performTextReplacement("8")
        runOnIdle {
            assertEquals(stored(kotlinx.serialization.json.JsonPrimitive(8)), data)
            assertEquals(2, writes)
        }
    }

    private fun overlappingValue(cupertino: Boolean, keyword: String, nested: Boolean = false) = runComposeUiTest {
        fun branch(type: String) = if (nested) """{
            "properties":{"record":{"type":"object","properties":{"value":{"type":"$type"}},
                "required":["value"]}},"required":["record"]
        }""" else """{"properties":{"value":{"type":"$type"}},"required":["value"]}"""
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","$keyword":[${branch("string")},${branch("integer")}]
        }""").jsonObject)
        fun stored(value: JsonElement) = Json.parseToJsonElement(
            if (nested) """{"record":{"value":$value}}""" else """{"value":$value}""")
        var data by mutableStateOf(stored(kotlinx.serialization.json.JsonPrimitive("Ada")))
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement(
                    if (nested) "#/properties/record/properties/value" else "#/properties/value"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; writes++ })
        }
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onAllNodes(hasSetTextAction())[0].performTextReplacement("Eve")
        runOnIdle {
            assertEquals(stored(kotlinx.serialization.json.JsonPrimitive("Eve")), data)
            assertEquals(1, writes)
            data = stored(kotlinx.serialization.json.JsonPrimitive(7))
        }
        waitForIdle()
        assertEquals(1, writes)
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onAllNodes(hasSetTextAction())[0].performTextReplacement("8")
        runOnIdle {
            assertEquals(stored(kotlinx.serialization.json.JsonPrimitive(8)), data)
            assertEquals(2, writes)
        }
    }

    private fun composedObject(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","allOf":[{"${'$'}ref":"#/${'$'}defs/fields"}],
            "${'$'}defs":{"fields":{"properties":{
                "name":{"type":"string"},
                "count":{"${'$'}ref":"#/${'$'}defs/count"}
            }},"count":{"type":"integer"}}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"name":"Ada","count":2}"""))
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = VerticalLayout(elements = listOf(
                    ControlElement("#/properties/name"), ControlElement("#/properties/count"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        assertEquals(2, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onAllNodes(hasSetTextAction())[0].performTextReplacement("Eve")
        onAllNodes(hasSetTextAction())[1].performTextReplacement("7")
        runOnIdle { assertEquals(Json.parseToJsonElement("""{"name":"Eve","count":7}"""), data) }
    }

    private fun scalar(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "allOf":[{"type":"string"}]
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement(""""old""""))
        setContent {
            JsonForms(data = data, schema = schema, uischema = ControlElement("#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onAllNodes(hasSetTextAction())[0].performTextReplacement("new")
        runOnIdle { assertEquals(Json.parseToJsonElement(""""new""""), data) }
    }

    private fun alternativeBranches(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","anyOf":[
                {"properties":{"name":{"type":"string"}},"required":["name"]},
                {"properties":{"count":{"type":"integer"}},"required":["count"]}
            ]
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"name":"Ada","count":2}"""))
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = VerticalLayout(elements = listOf(
                    ControlElement("#/properties/name"), ControlElement("#/properties/count"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        assertEquals(2, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onAllNodes(hasSetTextAction())[0].performTextReplacement("Eve")
        onAllNodes(hasSetTextAction())[1].performTextReplacement("7")
        runOnIdle { assertEquals(Json.parseToJsonElement("""{"name":"Eve","count":7}"""), data) }
    }

    private fun oneOfBranch(cupertino: Boolean, integer: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","oneOf":[
                {"properties":{"name":{"type":"string"}},"required":["name"]},
                {"properties":{"count":{"type":"integer"}},"required":["count"]}
            ]
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement(
            if (integer) """{"count":2}""" else """{"name":"Ada"}"""))
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement(if (integer) "#/properties/count" else "#/properties/name"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onAllNodes(hasSetTextAction())[0].performTextReplacement(if (integer) "7" else "Eve")
        runOnIdle { assertEquals(Json.parseToJsonElement(
            if (integer) """{"count":7}""" else """{"name":"Eve"}"""), data) }
    }

    private fun conditionalBranch(cupertino: Boolean, useElse: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object",
            "if":{"properties":{"mode":{"const":"email"}},"required":["mode"]},
            "then":{"properties":{"email":{"type":"string"}},"required":["email"]},
            "else":{"properties":{"count":{"type":"integer"}},"required":["count"]}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement(
            if (useElse) """{"mode":"other","count":2}"""
            else """{"mode":"email","email":"Ada"}"""))
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement(if (useElse) "#/properties/count" else "#/properties/email"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onAllNodes(hasSetTextAction())[0].performTextReplacement(if (useElse) "7" else "Eve")
        runOnIdle { assertEquals(Json.parseToJsonElement(
            if (useElse) """{"mode":"other","count":7}"""
            else """{"mode":"email","email":"Eve"}"""), data) }
    }
}
