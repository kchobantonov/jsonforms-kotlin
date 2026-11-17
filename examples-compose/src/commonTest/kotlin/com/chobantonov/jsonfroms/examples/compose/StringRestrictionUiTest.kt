package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import com.chobantonov.jsonfroms.core.store.ValidationMode
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class StringRestrictionUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"value":{"type":"string","maxLength":3}}}"""
    ).jsonObject)

    @Test fun materialDefaultPreventsExcessAndAllowsRepair() = defaultAndRepair(false)
    @Test fun cupertinoDefaultPreventsExcessAndAllowsRepair() = defaultAndRepair(true)
    @Test fun materialMultilinePreventsExcessAndAllowsRepair() = defaultAndRepair(false, multi = true)
    @Test fun cupertinoMultilinePreventsExcessAndAllowsRepair() = defaultAndRepair(true, multi = true)
    @Test fun materialLocalFalseAllowsInvalidCommit() = unrestricted(false)
    @Test fun cupertinoLocalFalseAllowsInvalidCommit() = unrestricted(true)
    @Test fun materialRestrictRemainsIndependentOfValidationMode() = modeIndependence(false)
    @Test fun cupertinoRestrictRemainsIndependentOfValidationMode() = modeIndependence(true)
    @Test fun materialAnyOfTextDoesNotTruncateOrSplitUnicode() = runComposeUiTest {
        val anyOfSchema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"value":{"anyOf":[{"type":"string","maxLength":2},{"type":"string","enum":["preset"]}]}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":""}"""))
        setContent {
            JsonForms(
                data = data, schema = anyOfSchema,
                uischema = ControlElement("#/properties/value"),
                renderers = materialRenderers,
                onChange = { data = it.data },
            )
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("😀😀")
        assertEquals("😀😀", (data.jsonObject["value"] as JsonPrimitive).content)
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("😀😀x")
        assertEquals("😀😀", (data.jsonObject["value"] as JsonPrimitive).content)
    }

    private fun defaultAndRepair(cupertino: Boolean, multi: Boolean = false) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":"abcde"}"""))
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ControlElement("#/properties/value", options = if (multi)
                    mapOf("multi" to JsonPrimitive(true)) else emptyMap()),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ },
            )
        }
        assertEquals("abcde", data.jsonObject["value"]?.let { (it as JsonPrimitive).content })
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("abcd")
        assertEquals("abcd", (data.jsonObject["value"] as JsonPrimitive).content)
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("abcdef")
        assertEquals("abcd", (data.jsonObject["value"] as JsonPrimitive).content)
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("abc")
        assertEquals("abc", (data.jsonObject["value"] as JsonPrimitive).content)
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("abcd")
        assertEquals("abc", (data.jsonObject["value"] as JsonPrimitive).content)
        assertEquals(2, changes)
    }

    private fun unrestricted(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":"ab"}"""))
        var validity by mutableStateOf<CombinedValidityStatus?>(null)
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ControlElement("#/properties/value", options = mapOf("restrict" to JsonPrimitive(false))),
                config = mapOf("restrict" to true),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data },
                onValidityChange = { validity = it.status },
            )
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("abcd")
        waitForIdle()
        assertEquals("abcd", (data.jsonObject["value"] as JsonPrimitive).content)
        assertEquals(CombinedValidityStatus.Invalid, validity)
    }

    private fun modeIndependence(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":"ab"}"""))
        var restrict by mutableStateOf(true)
        var mode by mutableStateOf(ValidationMode.NoValidation)
        var validity by mutableStateOf<CombinedValidityStatus?>(null)
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ControlElement("#/properties/value",
                    options = mapOf("restrict" to JsonPrimitive(restrict))),
                validationMode = mode,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ },
                onValidityChange = { validity = it.status },
            )
        }
        waitForIdle()
        assertEquals(CombinedValidityStatus.Unvalidated, validity)
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("abcd")
        assertEquals("ab", (data.jsonObject["value"] as JsonPrimitive).content)
        assertEquals(0, changes)

        runOnIdle { restrict = false }
        waitForIdle()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("abcde")
        assertEquals("abcde", (data.jsonObject["value"] as JsonPrimitive).content)
        assertEquals(1, changes)
        assertEquals(CombinedValidityStatus.Unvalidated, validity)

        runOnIdle { mode = ValidationMode.ValidateAndShow; restrict = true }
        waitForIdle()
        assertEquals(CombinedValidityStatus.Invalid, validity)
        assertEquals(1, changes)
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("abcdef")
        assertEquals("abcde", (data.jsonObject["value"] as JsonPrimitive).content)
        assertEquals(1, changes)
    }
}
