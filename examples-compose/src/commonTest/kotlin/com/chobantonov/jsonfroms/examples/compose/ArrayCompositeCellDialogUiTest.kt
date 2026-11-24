package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.store.Translator
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class ArrayCompositeCellDialogUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"firstName":{"type":"string"},"lastName":{"type":"string"},"contact":{"type":"object","properties":{"city":{"type":"string"},"phone":{"type":"string"}}},"experience":{"type":"array","items":{"type":"object","properties":{"company":{"type":"string"}}}}}}}}}"""
    ).jsonObject)
    private val initial = Json.parseToJsonElement(
        """{"rows":[{"firstName":"Ada","lastName":"Lovelace","contact":{"city":"Boston","phone":"555"},"experience":[{"company":"Acme"},{"company":"Beta"},{"company":"Gamma"}]}]}"""
    )
    private val ui = ControlElement("#/properties/rows", options = mapOf(
        "table" to JsonPrimitive(true),
        "columnDefs" to Json.parseToJsonElement("""[{"field":"contact","width":220},{"field":"experience","width":240},{"field":"fullName","scope":"#","headerName":"Full Name","width":220}]"""),
        "cells" to Json.parseToJsonElement("""{
          "contact":{"summary":{"type":"Control","scope":"#/properties/city"},
            "detail":{"type":"HorizontalLayout","elements":[{"type":"Control","scope":"#/properties/city"},{"type":"Control","scope":"#/properties/phone"}]},
            "dialog":{"width":720,"height":480}},
          "experience":{"summary":{"type":"Control","scope":"#/properties/company"}},
          "fullName":{"summary":{"type":"Label","text":"Full name preview"},
            "detail":{"type":"VerticalLayout","elements":[{"type":"Control","scope":"#/properties/firstName"},{"type":"Control","scope":"#/properties/lastName"}]},
            "dialog":{"width":700}}
        }"""),
    ))

    @Test fun materialCellCancelAndApplyStayScoped() = cell(false)
    @Test fun cupertinoCellCancelAndApplyStayScoped() = cell(true)
    @Test fun materialDetailElementDialogSetsCellGeometry() = detailElementDialog(false)
    @Test fun cupertinoDetailElementDialogSetsCellGeometry() = detailElementDialog(true)
    @Test fun materialLabelSummariesUseDeclaredRowContext() = labels(false)
    @Test fun cupertinoLabelSummariesUseDeclaredRowContext() = labels(true)
    @Test fun materialGeneratedSummaryFollowsLocaleAndValue() = generatedSummary(false)
    @Test fun cupertinoGeneratedSummaryFollowsLocaleAndValue() = generatedSummary(true)
    @Test fun materialTypeIndicatorFollowsPrecedenceAndValue() = typeIndicator(false)
    @Test fun cupertinoTypeIndicatorFollowsPrecedenceAndValue() = typeIndicator(true)
    @Test fun materialCellDialogActionsStayInDraft() = cellDialogActions(false, DialogActionPhase.Cancel)
    @Test fun cupertinoCellDialogActionsStayInDraft() = cellDialogActions(true, DialogActionPhase.Cancel)
    @Test fun materialCellDialogEmptyApplies() = cellDialogActions(false, DialogActionPhase.Empty)
    @Test fun cupertinoCellDialogEmptyApplies() = cellDialogActions(true, DialogActionPhase.Empty)
    @Test fun materialCellDialogRemoveConfirms() = cellDialogActions(false, DialogActionPhase.Remove)
    @Test fun cupertinoCellDialogRemoveConfirms() = cellDialogActions(true, DialogActionPhase.Remove)
    @Test fun materialRequiredParentStillOffersDialogRemove() = requiredCell(false)
    @Test fun cupertinoRequiredParentStillOffersDialogRemove() = requiredCell(true)
    @Test fun materialRestrictedRequiredChildBlocksEmpty() = protectedChild(false)
    @Test fun cupertinoRestrictedRequiredChildBlocksEmpty() = protectedChild(true)
    @Test fun materialClearableFalseHidesCellRemovalActions() = clearableFalse(false)
    @Test fun cupertinoClearableFalseHidesCellRemovalActions() = clearableFalse(true)
    @Test fun materialOpenDialogRechecksClearableConfig() = liveClearable(false)
    @Test fun cupertinoOpenDialogRechecksClearableConfig() = liveClearable(true)
    @Test fun materialContainsMinimumBlocksEmptyArrayCell() = containsEmpty(false)
    @Test fun cupertinoContainsMinimumBlocksEmptyArrayCell() = containsEmpty(true)

    private fun containsEmpty(cupertino: Boolean) = runComposeUiTest {
        val containsSchema = JsonSchema(Json.parseToJsonElement("""{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"tags":{"type":"array","title":"Tags","contains":{"const":"keep"},"items":{"type":"string"}}}}}}}""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"rows":[{"tags":["keep","other"]}]}"""))
        var restrict by mutableStateOf(true)
        var changes = 0
        val control = ControlElement("#/properties/rows", options = mapOf(
            "table" to JsonPrimitive(true),
            "cells" to Json.parseToJsonElement("""{"tags":{"showEmptyButton":true}}"""),
        ))
        setContent {
            JsonForms(data = data, schema = containsSchema, uischema = control,
                config = mapOf("restrict" to restrict),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Edit Tags").performClick()
        onNodeWithText("Clear").assertDoesNotExist()
        assertEquals(0, changes)
        runOnIdle { restrict = false }
        onNodeWithText("Clear").assertExists().performClick()
        onNodeWithText("Apply").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"rows":[{"tags":[]}]}"""), data)
        assertEquals(1, changes)
    }

    private fun liveClearable(cupertino: Boolean) = runComposeUiTest {
        var clearable by mutableStateOf(true)
        val actionUi = ui.copy(options = ui.options + ("cells" to Json.parseToJsonElement("""{
          "contact":{"summary":{"type":"Control","scope":"#/properties/city"},
            "showEmptyButton":true,"showRemoveButton":true}
        }""")))
        setContent {
            JsonForms(data = initial, schema = schema, uischema = actionUi,
                config = mapOf("clearable" to clearable),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onNodeWithContentDescription("Edit Contact").performClick()
        onNodeWithText("Clear").assertExists()
        onNodeWithText("Remove").assertExists()
        runOnIdle { clearable = false }
        onNodeWithText("Clear").assertDoesNotExist()
        onNodeWithText("Remove").assertDoesNotExist()
        runOnIdle { clearable = true }
        onNodeWithText("Clear").assertExists()
        onNodeWithText("Remove").assertExists()
        onNodeWithText("Cancel").performClick()
    }

    private fun clearableFalse(cupertino: Boolean) = runComposeUiTest {
        val uiNoClear = ui.copy(options = ui.options + ("cells" to Json.parseToJsonElement("""{
          "contact":{"summary":{"type":"Control","scope":"#/properties/city"},
            "showEmptyButton":true,"showRemoveButton":true,"clearable":false}
        }""")))
        setContent {
            JsonForms(data = initial, schema = schema, uischema = uiNoClear,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onNodeWithContentDescription("Clear Contact").assertDoesNotExist()
        onNodeWithContentDescription("Edit Contact").performClick()
        onNodeWithText("Clear").assertDoesNotExist()
        onNodeWithText("Remove").assertDoesNotExist()
        onNodeWithText("Cancel").performClick()
    }

    private fun requiredCell(cupertino: Boolean) = runComposeUiTest {
        val requiredSchema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","required":["contact"],"properties":{"contact":{"type":"object","properties":{"city":{"type":"string"}}}}}}}}"""
        ).jsonObject)
        val requiredUi = ControlElement("#/properties/rows", options = mapOf(
            "table" to JsonPrimitive(true),
            "restrict" to JsonPrimitive(true),
            "cells" to Json.parseToJsonElement("""{"contact":{"showEmptyButton":true,"showRemoveButton":true}}"""),
        ))
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"rows":[{"contact":{"city":"Boston"}}]}"""),
                schema = requiredSchema, uischema = requiredUi,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onNodeWithContentDescription("Edit Contact").performClick()
        onNodeWithText("Clear").assertExists()
        onNodeWithText("Remove").assertExists()
        onNodeWithText("Cancel").performClick()
    }

    private fun protectedChild(cupertino: Boolean) = runComposeUiTest {
        val protectedSchema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"contact":{"type":"object","required":["city"],"minProperties":1,"properties":{"city":{"type":"string"}}}}}}}}"""
        ).jsonObject)
        val protectedUi = ControlElement("#/properties/rows", options = mapOf(
            "table" to JsonPrimitive(true), "restrict" to JsonPrimitive(true),
            "cells" to Json.parseToJsonElement("""{"contact":{"showEmptyButton":true,"showRemoveButton":true}}"""),
        ))
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"rows":[{"contact":{"city":"Boston"}}]}"""),
                schema = protectedSchema, uischema = protectedUi,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onNodeWithContentDescription("Edit Contact").performClick()
        onNodeWithText("Clear").assertDoesNotExist()
        onNodeWithText("Remove").assertExists()
        onNodeWithText("Cancel").performClick()
    }

    private enum class DialogActionPhase { Cancel, Empty, Remove }

    private fun cellDialogActions(cupertino: Boolean, phase: DialogActionPhase) = runComposeUiTest {
        var data by mutableStateOf(initial)
        var changes = 0
        val actionUi = ui.copy(options = ui.options + ("cells" to Json.parseToJsonElement("""{
          "contact":{"summary":{"type":"Control","scope":"#/properties/city"},
            "detail":{"type":"VerticalLayout","elements":[{"type":"Control","scope":"#/properties/city"}]},
            "showEmptyButton":true,"showRemoveButton":true,
            "okLabel":"contact.save","cancelLabel":"contact.discard",
            "emptyLabel":"contact.empty","removeLabel":"contact.remove"}
        }""")))
        val translator = Translator { key, fallback, _ -> when (key) {
            "contact.save" -> "Save contact"
            "contact.discard" -> "Discard contact"
            "contact.empty" -> "Empty contact"
            "contact.remove" -> "Remove contact"
            else -> fallback
        } }
        setContent {
            JsonForms(data = data, schema = schema, uischema = actionUi,
                i18n = JsonFormsI18nState(locale = "en", translate = translator),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Edit Contact").performClick()
        when (phase) {
            DialogActionPhase.Cancel -> {
                onNodeWithText("Save contact").assertExists()
                onNodeWithText("Discard contact").assertExists()
                onNodeWithText("Empty contact").assertExists()
                onNodeWithText("Remove contact").assertExists()
                onAllNodes(hasContentDescription("Add item")).onFirst().assertIsNotEnabled()
                onAllNodes(hasContentDescription("Delete item")).onFirst().assertIsNotEnabled()
                onNodeWithContentDescription("Edit Contact").assertIsNotEnabled()
                assertEquals(1, onAllNodes(hasSetTextAction() and hasText("Boston")).fetchSemanticsNodes().size)
                onAllNodes(hasSetTextAction() and hasText("Boston")).onFirst().assertIsEnabled()
                onAllNodes(hasSetTextAction() and hasText("Boston")).onLast().performTextReplacement("Queued")
                onNodeWithText("Empty contact").performClick()
                onNodeWithText("Discard contact").performClick()
                assertEquals(initial, data)
                assertEquals(0, changes)
                onAllNodes(hasContentDescription("Add item")).onFirst().assertIsEnabled()
                onAllNodes(hasContentDescription("Delete item")).onFirst().assertIsEnabled()
                onNodeWithContentDescription("Edit Contact").assertIsEnabled()
                onAllNodes(hasSetTextAction() and hasText("Boston")).onFirst().assertIsEnabled()
            }
            DialogActionPhase.Empty -> {
                onAllNodes(hasSetTextAction() and hasText("Boston")).onLast().performTextReplacement("Queued")
                onNodeWithText("Empty contact").performClick()
                onNodeWithText("Save contact").performClick()
                waitForIdle()
                assertEquals(Json.parseToJsonElement(
                    """{"rows":[{"firstName":"Ada","lastName":"Lovelace","contact":{},"experience":[{"company":"Acme"},{"company":"Beta"},{"company":"Gamma"}]}]}"""), data)
                assertEquals(1, changes)
            }
            DialogActionPhase.Remove -> {
                onNodeWithText("Remove contact").performClick()
                onNodeWithText("Discard contact").performClick()
                assertEquals(initial, data)
                assertEquals(0, changes)
                onNodeWithContentDescription("Edit Contact").performClick()
                onNodeWithText("Remove contact").performClick()
                onNodeWithText("Save contact").performClick()
                onNodeWithText("Yes").performClick()
                waitForIdle()
                assertEquals(Json.parseToJsonElement(
                    """{"rows":[{"firstName":"Ada","lastName":"Lovelace","experience":[{"company":"Acme"},{"company":"Beta"},{"company":"Gamma"}]}]}"""), data)
                assertEquals(1, changes)
            }
        }
    }

    private fun typeIndicator(cupertino: Boolean) = runComposeUiTest {
        var global by mutableStateOf(false)
        var localOverride by mutableStateOf(false)
        var data by mutableStateOf(initial)
        val localUi = ui.copy(options = ui.options + ("cells" to Json.parseToJsonElement("""{
          "contact":{"summary":{"type":"Control","scope":"#/properties/city"},"showTypeIndicator":false},
          "experience":{"summary":{"type":"Control","scope":"#/properties/company"},"showTypeIndicator":true}
        }""")))
        setContent {
            JsonForms(data = data, schema = schema, uischema = if (localOverride) localUi else ui,
                config = mapOf("showTypeIndicator" to global),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onNodeWithTag("composite-type-0-contact").assertDoesNotExist()
        onNodeWithTag("composite-type-0-experience").assertDoesNotExist()
        runOnIdle { global = true }
        onNodeWithTag("composite-type-0-contact").assertExists()
        onNodeWithTag("composite-type-0-experience").assertExists()
        onNodeWithText("{}").assertDoesNotExist()
        onNodeWithText("[]").assertDoesNotExist()
        runOnIdle { localOverride = true }
        onNodeWithTag("composite-type-0-contact").assertDoesNotExist()
        onNodeWithTag("composite-type-0-experience").assertExists()
        runOnIdle { data = Json.parseToJsonElement(
            """{"rows":[{"firstName":"Ada","lastName":"Lovelace","contact":"plain","experience":null}]}"""
        ) }
        onNodeWithTag("composite-type-0-contact").assertDoesNotExist()
        onNodeWithTag("composite-type-0-experience").assertDoesNotExist()
    }

    private fun generatedSummary(cupertino: Boolean) = runComposeUiTest {
        var locale by mutableStateOf("en")
        var data by mutableStateOf(Json.parseToJsonElement(
            """{"rows":[{"firstName":"Ada","lastName":"Lovelace","contact":{"city":null,"phone":"555"},"experience":[{"company":"Acme"},{"company":"Beta"},{"company":"Gamma"}]}]}"""
        ))
        setContent {
            JsonForms(data = data, schema = schema, uischema = ui,
                i18n = JsonFormsI18nState(locale = locale, translate = Translator { key, fallback, _ ->
                    if (key == "City" && locale == "bg") "Град" else fallback
                }),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onNodeWithText("City not specified").assertExists()
        runOnIdle { locale = "bg" }
        onNodeWithText("Град не е посочено").assertExists()
        onNodeWithText("Acme, Beta (+1 още)").assertExists()
        runOnIdle { data = Json.parseToJsonElement(
            """{"rows":[{"firstName":"Ada","lastName":"Lovelace","contact":{},"experience":[]}]}"""
        ) }
        onNodeWithText("Празен обект").assertExists()
        onNodeWithText("0 елемента").assertExists()
        runOnIdle { data = initial }
        onNodeWithText("Boston").assertExists()
        onNodeWithText("Град не е посочено").assertDoesNotExist()
    }

    private fun labels(cupertino: Boolean) = runComposeUiTest {
        var enabled by mutableStateOf(false)
        var data by mutableStateOf(initial)
        val labelUi = ui.copy(options = ui.options + ("cells" to Json.parseToJsonElement("""{
          "contact":{"summary":{"type":"Label","text":"{city} · {phone}","options":{"interpolate":true,"textParams":{"city":"{item.contact.city}","phone":"{item.contact.phone}"}}}},
          "experience":{"summary":{"type":"Label","text":"Static {word}","options":{"interpolate":true,"textParams":{"word":"summary"}}}},
          "fullName":{"summary":{"type":"Label","text":"{first} {last}","options":{"interpolate":true,"textParams":{"first":"{item.firstName}","last":"{item.lastName}"}}}}
        }""")))
        setContent {
            JsonForms(data = data, schema = schema, uischema = labelUi,
                config = mapOf("jsonformsExtended" to mapOf("dynamicValues" to mapOf("enabled" to enabled))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onNodeWithText("{city} · {phone}").assertDoesNotExist()
        onNodeWithText("Boston · 555").assertDoesNotExist()
        onNodeWithText("Static summary").assertExists()
        runOnIdle { enabled = true }
        onNodeWithText("Boston · 555").assertExists()
        onNodeWithText("Ada Lovelace").assertExists()
        onNodeWithText("{city} · {phone}").assertDoesNotExist()
        onNodeWithContentDescription("Edit Contact").assertDoesNotExist()
        onNodeWithContentDescription("Clear Contact").assertDoesNotExist()
        onNodeWithContentDescription("Edit Full Name").assertDoesNotExist()
        onNodeWithContentDescription("Clear Full Name").assertDoesNotExist()
        runOnIdle { data = Json.parseToJsonElement(
            """{"rows":[{"firstName":"Grace","lastName":"Hopper","contact":{"city":"Arlington","phone":"777"},"experience":[]}]}"""
        ) }
        onNodeWithText("Arlington · 777").assertExists()
        onNodeWithText("Grace Hopper").assertExists()
        onNodeWithText("Boston · 555").assertDoesNotExist()
        onNodeWithContentDescription("Edit Contact").assertDoesNotExist()
        onNodeWithContentDescription("Clear Contact").assertDoesNotExist()
    }

    private fun detailElementDialog(cupertino: Boolean) = runComposeUiTest {
        val detailDialogUi = ui.copy(options = ui.options + ("cells" to Json.parseToJsonElement("""
          {"contact":{"summary":{"type":"Control","scope":"#/properties/city"},
            "detail":{"type":"Control","scope":"#","options":{"dialog":{"width":640}}}}}
        """)))
        setContent {
            JsonForms(data = initial, schema = schema, uischema = detailDialogUi,
                config = mapOf("jsonformsExtended" to mapOf("dialog" to mapOf("width" to 400))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onNodeWithContentDescription("Edit Contact").performClick()
        val width = onNodeWithTag("row-detail-dialog-frame").fetchSemanticsNode().boundsInRoot.width
        assertTrue(width > 620f && width < 660f, "detail dialog width=$width")
    }

    private fun cell(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(initial)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = ui,
                config = mapOf("jsonformsExtended" to mapOf("dialog" to mapOf("width" to 400))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Boston").assertExists()
        onNodeWithText("Acme, Beta (+1 more)").assertExists()
        onNodeWithText("Full name preview").assertExists()
        onNodeWithContentDescription("Edit Contact").performClick()
        val width = onNodeWithTag("row-detail-dialog-frame").fetchSemanticsNode().boundsInRoot.width
        assertTrue(width > 700f && width < 740f)
        onAllNodes(hasSetTextAction() and hasText("Boston")).onFirst().performTextReplacement("Discard")
        onNodeWithText("Cancel").performClick()
        assertEquals(initial, data)
        assertEquals(0, changes)
        onNodeWithContentDescription("Edit Contact").performClick()
        onAllNodes(hasSetTextAction() and hasText("Boston")).onFirst().performTextReplacement("Cambridge")
        onNodeWithText("Apply").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement(
            """{"rows":[{"firstName":"Ada","lastName":"Lovelace","contact":{"city":"Cambridge","phone":"555"},"experience":[{"company":"Acme"},{"company":"Beta"},{"company":"Gamma"}]}]}"""
        ), data)
        assertEquals(1, changes)
        onNodeWithContentDescription("Edit Experience").performClick()
        onNodeWithText("Experience details").assertExists()
        onNodeWithText("Cancel").performClick()
        assertEquals(1, changes)
    }

    @Test fun rowBoundDetailEditsOriginalRowWithoutCreatingVirtualProperty() = runComposeUiTest {
        var data by mutableStateOf(initial)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = ui,
                renderers = materialRenderers, onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Edit Full Name").performClick()
        onAllNodes(hasSetTextAction() and hasText("Ada")).onFirst().performTextReplacement("Grace")
        onNodeWithText("Apply").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement(
            """{"rows":[{"firstName":"Grace","lastName":"Lovelace","contact":{"city":"Boston","phone":"555"},"experience":[{"company":"Acme"},{"company":"Beta"},{"company":"Gamma"}]}]}"""
        ), data)
        assertEquals(1, changes)
    }

    @Test fun equalExternalArrayReplacementClosesCellDraft() = runComposeUiTest {
        var data by mutableStateOf(initial)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = ui,
                renderers = materialRenderers, onChange = { changes++ })
        }
        onNodeWithContentDescription("Edit Contact").performClick()
        onAllNodes(hasSetTextAction() and hasText("Boston")).onFirst().performTextReplacement("Draft")
        runOnIdle { data = Json.parseToJsonElement(
            """{"rows":[{"firstName":"Ada","lastName":"Lovelace","contact":{"city":"Boston","phone":"555"},"experience":[{"company":"Acme"},{"company":"Beta"},{"company":"Gamma"}]}],"other":"new"}"""
        ) }
        waitForIdle()
        onNodeWithText("Contact details").assertDoesNotExist()
        assertEquals(0, changes)
    }
}
