package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
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
class AdditionalPatternPropertiesUiTest {
    @Test fun materialOverlappingBoundsHaveOneEditor() = overlap(false, false)
    @Test fun cupertinoOverlappingBoundsHaveOneEditor() = overlap(true, false)
    @Test fun materialReversedPatternsKeepBothBounds() = overlap(false, true)
    @Test fun cupertinoReversedPatternsKeepBothBounds() = overlap(true, true)
    @Test fun materialDeclaredPropertyShowsPatternErrors() = declared(false)
    @Test fun cupertinoDeclaredPropertyShowsPatternErrors() = declared(true)
    @Test fun materialRenameIntoOverlapPreservesInvalidValue() = rename(false, true)
    @Test fun cupertinoRenameIntoOverlapPreservesInvalidValue() = rename(true, true)
    @Test fun materialRenameOutOfOverlapClearsConstraint() = rename(false, false)
    @Test fun cupertinoRenameOutOfOverlapClearsConstraint() = rename(true, false)
    @Test fun materialStructuralOverlapEditsBothBranches() = structural(false)
    @Test fun cupertinoStructuralOverlapEditsBothBranches() = structural(true)
    @Test fun materialNestedDynamicValueKeepsExactPath() = nestedDynamic(false, "flag")
    @Test fun cupertinoNestedDynamicValueKeepsExactPath() = nestedDynamic(true, "flag")
    @Test fun materialNestedDottedDynamicValueKeepsExactPath() = nestedDynamic(false, "flag.dot")
    @Test fun cupertinoNestedDottedDynamicValueKeepsExactPath() = nestedDynamic(true, "flag.dot")

    private fun nestedDynamic(cupertino: Boolean, key: String) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"entries":{"type":"object","patternProperties":{"^item_":{"type":"object","properties":{"attributes":{"type":"object","additionalProperties":{"type":"string","minLength":2}}},"required":["attributes"]}},"additionalProperties":false}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement(
            """{"entries":{"item_1":{"attributes":{"$key":"x"}}}}"""
        ))
        var validity: CombinedValidityStatus? = null
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/entries"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(translateError = { error, _, _ ->
                    "ERR:${error.keyword}:${error.instancePath}"
                }),
                onValidityChange = { validity = it.status },
                onChange = { data = it.data; changes++ })
        }
        assertEquals(CombinedValidityStatus.Invalid, validity)
        val errorText = if ('.' in key) "ERR:minLength:" else
            "ERR:minLength:/entries/item_1/attributes/$key"
        onAllNodes(hasText(errorText, substring = true)).assertCountEquals(1)
        onAllNodes(hasSetTextAction() and hasText("x", substring = true)).onFirst()
            .performTextReplacement("ok")
        waitForIdle()
        assertEquals(JsonPrimitive("ok"), data.jsonObject["entries"]!!.jsonObject["item_1"]!!
            .jsonObject["attributes"]!!.jsonObject[key])
        assertEquals(CombinedValidityStatus.Valid, validity)
        assertEquals(1, changes)
        onAllNodes(hasText(errorText, substring = true)).assertCountEquals(0)
    }

    private fun structural(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"entries":{"type":"object","patternProperties":{"^sensor_":{"type":"object","properties":{"left":{"type":"string","title":"Left sensor","minLength":2}},"required":["left"]},"_pair$":{"type":"object","properties":{"right":{"type":"integer","title":"Right sensor","minimum":1}},"required":["right"]}},"additionalProperties":false}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"entries":{"sensor_pair":{"left":"x","right":0}}}"""))
        var validity: CombinedValidityStatus? = null
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/entries"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(translateError = { error, _, _ ->
                    "ERR:${error.keyword}:${error.instancePath}"
                }),
                onValidityChange = { validity = it.status },
                onChange = { data = it.data; changes++ })
        }
        assertEquals(CombinedValidityStatus.Invalid, validity)
        onAllNodes(hasText("Left sensor", substring = true)).onFirst().assertExists()
        onAllNodes(hasText("Right sensor", substring = true)).onFirst().assertExists()
        onAllNodes(hasText("ERR:minLength:/entries/sensor_pair/left", substring = true)).onFirst().assertExists()
        onAllNodes(hasText("ERR:minimum:/entries/sensor_pair/right", substring = true)).onFirst().assertExists()
        onAllNodes(hasSetTextAction() and hasText("x")).onFirst().performTextReplacement("ok")
        waitForIdle()
        assertEquals(CombinedValidityStatus.Invalid, validity)
        onAllNodes(hasSetTextAction() and hasText("0")).onFirst().performTextReplacement("2")
        waitForIdle()
        assertEquals(JsonPrimitive("ok"), data.jsonObject["entries"]!!.jsonObject["sensor_pair"]!!.jsonObject["left"])
        assertEquals(JsonPrimitive(2), data.jsonObject["entries"]!!.jsonObject["sensor_pair"]!!.jsonObject["right"])
        assertEquals(CombinedValidityStatus.Valid, validity)
        assertEquals(2, changes)
    }

    private fun rename(cupertino: Boolean, intoOverlap: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"entries":{"type":"object","patternProperties":{"^price_":{"type":"number","minimum":0},"_total$":{"maximum":1000}},"additionalProperties":true}}}"""
        ).jsonObject)
        val oldName = if (intoOverlap) "price_other" else "price_total"
        val newName = if (intoOverlap) "price_total" else "price_other"
        var data by mutableStateOf(Json.parseToJsonElement("""{"entries":{"$oldName":1500}}"""))
        var validity: CombinedValidityStatus? = null
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/entries"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(translateError = { error, _, _ ->
                    "ERR:${error.keyword}:${error.instancePath}"
                }),
                onValidityChange = { validity = it.status },
                onChange = { data = it.data; changes++ })
        }
        assertEquals(if (intoOverlap) CombinedValidityStatus.Valid else CombinedValidityStatus.Invalid, validity)
        onNodeWithContentDescription("Rename button").performClick()
        onAllNodes(hasSetTextAction() and hasText(oldName)).onFirst().performTextReplacement(newName)
        onNodeWithContentDescription("Save button").performClick()
        waitForIdle()
        val entries = data.jsonObject["entries"]!!.jsonObject
        assertEquals(null, entries[oldName])
        assertEquals(JsonPrimitive(1500), entries[newName])
        assertEquals(1, changes)
        assertEquals(if (intoOverlap) CombinedValidityStatus.Invalid else CombinedValidityStatus.Valid, validity)
        if (intoOverlap) onAllNodes(hasText("ERR:maximum:/entries/price_total")).assertCountEquals(1)
        else onAllNodes(hasText("ERR:maximum:/entries/price_total")).assertCountEquals(0)
    }

    private fun overlap(cupertino: Boolean, reverse: Boolean) = runComposeUiTest {
        val patterns = if (reverse)
            """"_total$":{"maximum":1000},"^price_":{"type":"number","minimum":0}"""
        else """"^price_":{"type":"number","minimum":0},"_total$":{"maximum":1000}"""
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"entries":{"type":"object","patternProperties":{$patterns},"additionalProperties":true}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"entries":{"price_total":500}}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/entries"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodes(hasSetTextAction() and hasText("500", substring = true)).assertCountEquals(1)
        onAllNodes(hasSetTextAction() and hasText("500", substring = true)).onFirst()
            .performTextReplacement("-1")
        assertEquals(JsonPrimitive(500), data.jsonObject["entries"]!!.jsonObject["price_total"])
        onAllNodes(hasSetTextAction() and hasText("-1", substring = true)).onFirst()
            .performTextReplacement("1001")
        assertEquals(JsonPrimitive(500), data.jsonObject["entries"]!!.jsonObject["price_total"])
        onAllNodes(hasSetTextAction() and hasText("1001", substring = true)).onFirst()
            .performTextReplacement("750")
        waitForIdle()
        assertEquals(JsonPrimitive(750.0), data.jsonObject["entries"]!!.jsonObject["price_total"])
        assertEquals(1, changes)
    }

    private fun declared(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"price_total":{"type":"number","title":"Total price"}},"patternProperties":{"^price_":{"minimum":0},"_total$":{"maximum":1000}},"additionalProperties":false}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"price_total":-1}"""))
        var validity: CombinedValidityStatus? = null
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/price_total"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(translateError = { error, _, _ ->
                    "ERR:${error.keyword}:${error.instancePath}"
                }),
                onValidityChange = { validity = it.status },
                onChange = { changes++ })
        }
        assertEquals(CombinedValidityStatus.Invalid, validity)
        onNodeWithText("ERR:minimum:/price_total").assertExists()
        runOnIdle { data = Json.parseToJsonElement("""{"price_total":1001}""") }
        onNodeWithText("ERR:maximum:/price_total").assertExists()
        runOnIdle { data = Json.parseToJsonElement("""{"price_total":500}""") }
        onAllNodes(hasText("ERR:minimum:/price_total")).assertCountEquals(0)
        onAllNodes(hasText("ERR:maximum:/price_total")).assertCountEquals(0)
        assertEquals(CombinedValidityStatus.Valid, validity)
        assertEquals(0, changes)
    }
}
