package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasContentDescription
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
import com.chobantonov.jsonfroms.core.store.ValidationMode
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class ArrayCompositeCellValidationUiTest {
    @Test fun materialCellMarkerUsesBuiltInBulgarianLabel() = builtInCellLabel(false)
    @Test fun cupertinoCellMarkerUsesBuiltInBulgarianLabel() = builtInCellLabel(true)

    private fun builtInCellLabel(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"contact":{"type":"object","properties":{"city":{"type":"string","minLength":2}}}}}}}}"""
        ).jsonObject)
        var locale by mutableStateOf("en")
        var showCount by mutableStateOf(false)
        var writes = 0
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"rows":[{"contact":{"city":"X"}}]}"""),
                schema = schema,
                uischema = ControlElement("#/properties/rows", options = mapOf(
                    "table" to JsonPrimitive(true),
                    "cells" to Json.parseToJsonElement(
                        """{"contact":{"showValidationIndicatorCount":$showCount}}""").jsonObject,
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { writes++ })
        }
        onNodeWithContentDescription("Contact contains errors", substring = true).assertExists()
        runOnIdle { locale = "bg-BG" }
        onNodeWithContentDescription("Contact съдържа грешки", substring = true).assertExists()
        runOnIdle { showCount = true }
        onNodeWithContentDescription("Contact: 1 грешка", substring = true).assertExists()
        assertEquals(0, writes)
    }

    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"firstName":{"type":"string","minLength":2},"lastName":{"type":"string","minLength":2},"notes":{"type":"string","minLength":2},"contact":{"type":"object","properties":{"city":{"type":"string","minLength":2},"phone":{"type":"string","minLength":2}}}}}}}}"""
    ).jsonObject)
    private val initial = Json.parseToJsonElement(
        """{"rows":[{"firstName":"A","lastName":"B","notes":"N","contact":{"city":"X","phone":"Y"}}]}"""
    )
    private val ui = ControlElement("#/properties/rows", options = mapOf(
        "table" to JsonPrimitive(true),
        "columnDefs" to Json.parseToJsonElement("""[{"field":"contact","width":230},{"field":"fullName","scope":"#","headerName":"Full Name","width":240},{"field":"preview","scope":"#","headerName":"Preview","width":180}]"""),
        "cells" to Json.parseToJsonElement("""{
          "contact":{"summary":{"type":"Label","text":"Contact preview"},
            "detail":{"type":"Control","scope":"#/properties/city"},"showValidationIndicatorCount":true},
          "fullName":{"summary":{"type":"Label","text":"Name preview"},
            "detail":{"type":"VerticalLayout","elements":[{"type":"Control","scope":"#/properties/firstName"},{"type":"Control","scope":"#/properties/lastName"}]},
            "showValidationIndicatorCount":true},
          "preview":{"summary":{"type":"Label","text":"Read-only preview"},"showValidationIndicatorCount":true}
        }"""),
    ))

    @Test fun materialCellMarkersStayWithinDetailScopes() = verify(false)
    @Test fun cupertinoCellMarkersStayWithinDetailScopes() = verify(true)
    @Test fun materialHeaderSuppressionKeepsNestedCellErrors() = headerSuppression(false)
    @Test fun cupertinoHeaderSuppressionKeepsNestedCellErrors() = headerSuppression(true)
    @Test fun materialComposedObjectCellsExposeValidation() = composed(false)
    @Test fun cupertinoComposedObjectCellsExposeValidation() = composed(true)
    @Test fun materialCellIndicatorsFollowValidationMode() = validationMode(false)
    @Test fun cupertinoCellIndicatorsFollowValidationMode() = validationMode(true)
    @Test fun materialMixedCombinatorObjectValueUsesCompactDetail() = mixedObjectValue(false)
    @Test fun cupertinoMixedCombinatorObjectValueUsesCompactDetail() = mixedObjectValue(true)

    private fun mixedObjectValue(cupertino: Boolean) = runComposeUiTest {
        val mixedSchema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{
              "one":{"oneOf":[{"type":"object","properties":{"city":{"type":"string","minLength":2}},"required":["city"]},{"type":"string","minLength":2}]},
              "any":{"anyOf":[{"type":"object","properties":{"city":{"type":"string","minLength":2}},"required":["city"]},{"type":"string","minLength":2}]}
            }}}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement(
            """{"rows":[{"one":{"city":"X"},"any":{"city":"X"}},{"one":"ok","any":"ok"}]}"""
        ))
        setContent {
            JsonForms(data = data, schema = mixedSchema,
                uischema = ControlElement("#/properties/rows", options = mapOf(
                    "table" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        for (name in listOf("One", "Any")) {
            assertEquals(1, onAllNodes(hasContentDescription("Edit $name"))
                .fetchSemanticsNodes().size)
            assertEquals(1, onAllNodes(hasContentDescription("$name contains errors", substring = true))
                .fetchSemanticsNodes().size)
        }
        onNodeWithContentDescription("Edit One").performClick()
        assertTrue(onAllNodes(hasText("City", substring = true, ignoreCase = true))
            .fetchSemanticsNodes().isNotEmpty())
        onNodeWithText("Cancel").performClick()
        runOnIdle { data = Json.parseToJsonElement(
            """{"rows":[{"one":"ok","any":"ok"},{"one":"ok","any":"ok"}]}"""
        ) }
        for (name in listOf("One", "Any")) {
            onNodeWithContentDescription("Edit $name").assertDoesNotExist()
            onNodeWithContentDescription("$name contains errors", substring = true).assertDoesNotExist()
        }
    }

    private fun validationMode(cupertino: Boolean) = runComposeUiTest {
        val nestedSchema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{
              "contact":{"type":"object","properties":{"city":{"type":"string","minLength":2}}},
              "experience":{"type":"array","items":{"type":"object","properties":{"role":{"type":"string","minLength":2}}}}
            }}}}}"""
        ).jsonObject)
        var mode by mutableStateOf(ValidationMode.ValidateAndShow)
        var host by mutableStateOf(listOf(
            ErrorObject(instancePath = "/rows/0/contact/city", message = "server city")))
        setContent {
            JsonForms(
                data = Json.parseToJsonElement(
                    """{"rows":[{"contact":{"city":"X"},"experience":[{"role":"Y"}]}]}"""),
                schema = nestedSchema,
                uischema = ControlElement("#/properties/rows", options = mapOf(
                    "table" to JsonPrimitive(true),
                    "showValidationIndicator" to JsonPrimitive(false),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                validationMode = mode, additionalErrors = host,
            )
        }
        onNodeWithContentDescription("Contact contains errors", substring = true).assertExists()
        onNodeWithContentDescription("Experience contains errors", substring = true).assertExists()
        runOnIdle { mode = ValidationMode.ValidateAndHide }
        onNodeWithContentDescription("Contact contains errors", substring = true).assertExists()
        onNodeWithContentDescription("Experience contains errors", substring = true).assertDoesNotExist()
        runOnIdle { mode = ValidationMode.NoValidation }
        onNodeWithContentDescription("Contact contains errors", substring = true).assertExists()
        onNodeWithContentDescription("Experience contains errors", substring = true).assertDoesNotExist()
        runOnIdle { host = emptyList() }
        onNodeWithContentDescription("Contact contains errors", substring = true).assertDoesNotExist()
    }

    private fun composed(cupertino: Boolean) = runComposeUiTest {
        val composedSchema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{
              "one":{"oneOf":[{"type":"object","properties":{"kind":{"enum":["a","b"]}},"required":["kind"]},{"type":"object","properties":{"kind":{"enum":["a","c"]}},"required":["kind"]}]},
              "any":{"anyOf":[{"type":"object","properties":{"kind":{"const":"b"}},"required":["kind"]},{"type":"object","properties":{"kind":{"const":"c"}},"required":["kind"]}]},
              "all":{"allOf":[{"type":"object","properties":{"name":{"type":"string","minLength":2}},"required":["name"]},{"type":"object"}]}
            }}}}}"""
        ).jsonObject)
        val composedData = Json.parseToJsonElement(
            """{"rows":[{"one":{"kind":"a"},"any":{"kind":"a"},"all":{"name":"X"}},
              {"one":{"kind":"b"},"any":{"kind":"b"},"all":{"name":"XX"}}]}"""
        )
        val composedUi = ControlElement("#/properties/rows", options = mapOf(
            "table" to JsonPrimitive(true),
            "showValidationIndicator" to JsonPrimitive(false),
        ))
        setContent {
            JsonForms(data = composedData, schema = composedSchema, uischema = composedUi,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        for (name in listOf("One", "Any", "All")) {
            assertEquals(1, onAllNodes(hasContentDescription("$name contains errors", substring = true))
                .fetchSemanticsNodes().size)
            assertEquals(2, onAllNodes(hasContentDescription("Edit $name"))
                .fetchSemanticsNodes().size)
        }
        for ((name, field) in listOf("One" to "Kind", "Any" to "Kind", "All" to "Name")) {
            onAllNodes(hasContentDescription("Edit $name")).onFirst().performClick()
            onNodeWithText("$name details").assertExists()
            assertTrue(onAllNodes(hasText(field, substring = true, ignoreCase = true))
                .fetchSemanticsNodes().isNotEmpty(), "$name detail should render $field")
            onNodeWithText("Cancel").performClick()
        }
    }

    private fun headerSuppression(cupertino: Boolean) = runComposeUiTest {
        val nestedSchema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"contact":{"type":"object","properties":{"city":{"type":"string"}}},"experience":{"type":"array","items":{"type":"object","properties":{"role":{"type":"string"}}}}}}}}}"""
        ).jsonObject)
        val nestedData = Json.parseToJsonElement(
            """{"rows":[{"contact":{},"experience":[{}]},{"contact":{},"experience":[{}]}]}"""
        )
        val nestedUi = ControlElement("#/properties/rows", options = mapOf(
            "table" to JsonPrimitive(true),
            "showValidationIndicator" to JsonPrimitive(false),
            "cells" to Json.parseToJsonElement("""{
              "contact":{"showValidationIndicatorCount":true},
              "experience":{"showValidationIndicatorCount":true}
            }"""),
        ))
        setContent {
            JsonForms(data = nestedData, schema = nestedSchema, uischema = nestedUi,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                additionalErrors = listOf(
                    ErrorObject(instancePath = "/rows/0/contact/city", message = "city from server"),
                    ErrorObject(instancePath = "/rows/0/experience/0/role", message = "role from server"),
                ))
        }
        assertEquals(1, onAllNodes(androidx.compose.ui.test.hasContentDescription("Contact: 1 error", substring = true))
            .fetchSemanticsNodes().size)
        assertEquals(1, onAllNodes(androidx.compose.ui.test.hasContentDescription("Experience: 1 error", substring = true))
            .fetchSemanticsNodes().size)
        assertEquals(2, onAllNodes(androidx.compose.ui.test.hasContentDescription("Edit Contact"))
            .fetchSemanticsNodes().size)
        assertEquals(2, onAllNodes(androidx.compose.ui.test.hasContentDescription("Edit Experience"))
            .fetchSemanticsNodes().size)
    }

    private fun verify(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(initial)
        setContent {
            JsonForms(data = data, schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        onNodeWithContentDescription("Contact: 1 error", substring = true).assertExists()
        onNodeWithContentDescription("Full Name: 2 errors", substring = true).assertExists()
        onNodeWithContentDescription("Preview: 5 errors", substring = true).assertDoesNotExist()
        onNodeWithText("Read-only preview").assertExists()
        onNodeWithContentDescription("Edit Full Name").performClick()
        onAllNodes(hasSetTextAction() and hasText("A")).onFirst().performTextReplacement("Ada")
        onNodeWithText("Apply").performClick()
        waitForIdle()
        onNodeWithContentDescription("Full Name: 1 error", substring = true).assertExists()
        onNodeWithContentDescription("Contact: 1 error", substring = true).assertExists()
    }
}
