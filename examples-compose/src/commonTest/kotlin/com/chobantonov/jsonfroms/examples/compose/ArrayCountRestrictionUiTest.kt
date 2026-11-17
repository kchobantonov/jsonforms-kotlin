package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ArrayCountRestrictionUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"rows":{"type":"array","minItems":1,"maxItems":2,
          "items":{"type":"object","properties":{"name":{"type":"string"}}}}}}"""
    ).jsonObject)

    @Test fun materialTableAddRechecksMaximum() = additions(false, true)
    @Test fun cupertinoTableAddRechecksMaximum() = additions(true, true)
    @Test fun materialExpandableAddRechecksMaximum() = additions(false, false)
    @Test fun cupertinoExpandableAddRechecksMaximum() = additions(true, false)
    @Test fun materialTableShowsAndRepairsImportedWrongShape() = importedShape(false, true)
    @Test fun cupertinoTableShowsAndRepairsImportedWrongShape() = importedShape(true, true)
    @Test fun materialExpandableShowsAndRepairsImportedWrongShape() = importedShape(false, false)
    @Test fun cupertinoExpandableShowsAndRepairsImportedWrongShape() = importedShape(true, false)
    @Test fun materialTableDistinguishesMissingAndNullArray() = missingAndNull(false, true)
    @Test fun cupertinoTableDistinguishesMissingAndNullArray() = missingAndNull(true, true)
    @Test fun materialExpandableDistinguishesMissingAndNullArray() = missingAndNull(false, false)
    @Test fun cupertinoExpandableDistinguishesMissingAndNullArray() = missingAndNull(true, false)

    private fun missingAndNull(cupertino: Boolean, table: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{}"""))
        var changes = 0
        setContent {
            val presentation = if (table) mapOf("table" to JsonPrimitive(true)) else mapOf(
                "detail" to Json.parseToJsonElement(
                    """{"type":"Control","scope":"#/properties/name"}"""),
            )
            JsonForms(
                data = data, schema = schema,
                uischema = ControlElement("#/properties/rows", options = presentation),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ },
            )
        }
        onNodeWithText("Invalid current value: null").assertDoesNotExist()
        onNodeWithContentDescription("Add item").performClick()
        waitForIdle()
        assertEquals(1, data.jsonObject.getValue("rows").jsonArray.size)
        assertEquals(1, changes)
        runOnIdle { data = Json.parseToJsonElement("""{"rows":null}""") }
        waitForIdle()
        onNodeWithText("Invalid current value: null").assertExists()
        onNodeWithContentDescription("Add item").performClick()
        waitForIdle()
        assertEquals(1, data.jsonObject.getValue("rows").jsonArray.size)
        assertEquals(2, changes)
        onNodeWithText("Invalid current value: null").assertDoesNotExist()
    }

    private fun importedShape(cupertino: Boolean, table: Boolean) = runComposeUiTest {
        val initial = Json.parseToJsonElement("""{"rows":7}""")
        var data by mutableStateOf(initial)
        var locale by mutableStateOf("en")
        var changes = 0
        var validity: CombinedValidityStatus? = null
        setContent {
            val presentation = if (table) mapOf("table" to JsonPrimitive(true)) else mapOf(
                "detail" to Json.parseToJsonElement(
                    """{"type":"Control","scope":"#/properties/name"}"""),
            )
            JsonForms(
                data = data, schema = schema,
                uischema = ControlElement("#/properties/rows", options = presentation),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { data = it.data; changes++ },
                onValidityChange = { validity = it.status },
            )
        }
        onNodeWithText("Invalid current value: 7").assertExists()
        assertEquals(initial, data)
        assertEquals(CombinedValidityStatus.Invalid, validity)
        assertEquals(0, changes)
        runOnIdle { locale = "bg-BG" }
        waitForIdle()
        onNodeWithText("Невалидна текуща стойност: 7").assertExists()
        runOnIdle { locale = "de-DE" }
        waitForIdle()
        onNodeWithText("Invalid current value: 7").assertExists()
        onNodeWithContentDescription("Add item").performClick()
        waitForIdle()
        assertEquals(1, data.jsonObject.getValue("rows").jsonArray.size)
        assertEquals(CombinedValidityStatus.Valid, validity)
        assertEquals(1, changes)
        onNodeWithText("Invalid current value: 7").assertDoesNotExist()
    }

    private fun additions(cupertino: Boolean, table: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"rows":[]}"""))
        var restrict by mutableStateOf(true)
        var validity: CombinedValidityStatus? = null
        setContent {
            val presentation = if (table) mapOf("table" to JsonPrimitive(true)) else mapOf(
                "detail" to Json.parseToJsonElement(
                    """{"type":"Control","scope":"#/properties/name"}"""),
            )
            JsonForms(
                data = data, schema = schema,
                uischema = ControlElement("#/properties/rows",
                    options = presentation + ("restrict" to JsonPrimitive(restrict))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data },
                onValidityChange = { validity = it.status },
            )
        }
        assertEquals(CombinedValidityStatus.Invalid, validity)
        onNodeWithContentDescription("Add item").performClick()
        waitForIdle()
        assertEquals(1, data.jsonObject.getValue("rows").jsonArray.size)
        assertEquals(CombinedValidityStatus.Valid, validity)
        onNodeWithContentDescription("Add item").performClick()
        waitForIdle()
        assertEquals(2, data.jsonObject.getValue("rows").jsonArray.size)
        onNodeWithContentDescription("Add item").assertIsNotEnabled()

        runOnIdle { restrict = false }
        waitForIdle()
        onNodeWithContentDescription("Add item").performClick()
        waitForIdle()
        assertEquals(3, data.jsonObject.getValue("rows").jsonArray.size)
        assertEquals(CombinedValidityStatus.Invalid, validity)
    }
}
