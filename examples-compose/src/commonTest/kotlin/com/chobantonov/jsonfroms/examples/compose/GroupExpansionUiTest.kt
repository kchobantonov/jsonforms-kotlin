package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.toUiSchemaElement
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalTestApi::class)
class GroupExpansionUiTest {
    @Test fun materialSerializedCollapsibleGroup() = serializedCollapsibleGroup(false)

    @Test fun cupertinoSerializedCollapsibleGroup() = serializedCollapsibleGroup(true)

    private fun serializedCollapsibleGroup(cupertino: Boolean) = runComposeUiTest {
        val ui = Json.parseToJsonElement("""{
            "type":"Group","label":"Details","options":{"collapsible":true},
            "elements":[{"type":"Control","scope":"#/properties/note"}]
        }""").jsonObject.toUiSchemaElement()
        assertTrue(ui is GroupElement)
        assertEquals(JsonPrimitive(true), ui.options["collapsible"])
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { writes++ })
        }
        fun fields() = onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size
        assertEquals(1, fields())
        onNodeWithText("Details").performClick()
        assertEquals(0, fields())
        onNodeWithText("Details").performClick()
        assertEquals(1, fields())
        assertEquals(0, writes)
    }

    @Test fun materialUnrelatedHostUpdatesPreserveLocalExpansion() = unrelatedHostUpdates(false)

    @Test fun cupertinoUnrelatedHostUpdatesPreserveLocalExpansion() = unrelatedHostUpdates(true)

    private fun unrelatedHostUpdates(cupertino: Boolean) = runComposeUiTest {
        var current by mutableStateOf(data)
        var globalCollapsed by mutableStateOf(false)
        var showDataIndicator by mutableStateOf(false)
        var writes = 0
        val ui = GroupElement(
            label = "Details",
            options = mapOf("collapsible" to JsonPrimitive(true)),
            elements = listOf(ControlElement(scope = "#/properties/note")),
        )
        setContent {
            JsonForms(data = current, schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf("jsonformsExtended" to mapOf(
                    "collapsed" to globalCollapsed,
                    "showDataIndicator" to showDataIndicator,
                )),
                onChange = { writes++ })
        }
        fun fields() = onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size
        assertEquals(1, fields())
        onNodeWithText("Details").performClick()
        assertEquals(0, fields())
        runOnIdle { current = Json.parseToJsonElement("""{"note":"Replaced"}""") }
        assertEquals(0, fields())
        runOnIdle { showDataIndicator = true }
        assertEquals(0, fields())
        runOnIdle { globalCollapsed = true }
        assertEquals(0, fields())
        runOnIdle { globalCollapsed = false }
        assertEquals(1, fields())
        assertEquals("Replaced", onAllNodes(hasSetTextAction())[0].fetchSemanticsNode()
            .config[SemanticsProperties.EditableText].text)
        assertEquals(0, writes)
    }

    @Test fun materialCollapsePreservesEditedInvalidField() = collapsePreservesEditedInvalidField(false)

    @Test fun cupertinoCollapsePreservesEditedInvalidField() = collapsePreservesEditedInvalidField(true)

    private fun collapsePreservesEditedInvalidField(cupertino: Boolean) = runComposeUiTest {
        val constrainedSchema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"note":{"type":"string","minLength":5}}}"""
        ).jsonObject)
        var current by mutableStateOf(Json.parseToJsonElement("""{"note":"Stored"}"""))
        var writes = 0
        var status = CombinedValidityStatus.Valid
        var errorCount = 0
        setContent {
            JsonForms(data = current, schema = constrainedSchema,
                uischema = group(true, JsonPrimitive(false)),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { change ->
                    current = change.data
                    errorCount = change.errors.size
                    writes++
                },
                onValidityChange = { status = it.status })
        }
        onAllNodes(hasSetTextAction())[0].performTextReplacement("A")
        waitForIdle()
        assertEquals("A", current.jsonObject["note"]?.jsonPrimitive?.content)
        assertEquals(1, errorCount)
        assertEquals(CombinedValidityStatus.Invalid, status)
        assertEquals(1, writes)
        onNodeWithText("Details").performClick()
        assertEquals(0, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        assertEquals("A", current.jsonObject["note"]?.jsonPrimitive?.content)
        assertEquals(CombinedValidityStatus.Invalid, status)
        onNodeWithText("Details").performClick()
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        assertEquals("A", onAllNodes(hasSetTextAction())[0].fetchSemanticsNode()
            .config[SemanticsProperties.EditableText].text)
        assertEquals(1, writes)
        assertEquals(1, errorCount)
        assertEquals(CombinedValidityStatus.Invalid, status)
    }

    @Test fun materialGroupExpansionSynchronizesOnlyOnOptionChanges() = expansion(false)

    @Test fun cupertinoGroupExpansionSynchronizesOnlyOnOptionChanges() = expansion(true)

    @Test fun materialNoncollapsibleGroupStaysOpenAndReportsInvalidCollapsed() = noncollapsible(false)

    @Test fun cupertinoNoncollapsibleGroupStaysOpenAndReportsInvalidCollapsed() = noncollapsible(true)

    @Test fun materialDisclosureStateDescriptionFollowsLocale() = localizedState(false)

    @Test fun cupertinoDisclosureStateDescriptionFollowsLocale() = localizedState(true)

    @Test fun materialDisclosureUsesBundleWithoutFormCatalog() = noCatalogState(false)
    @Test fun cupertinoDisclosureUsesBundleWithoutFormCatalog() = noCatalogState(true)

    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"note":{"type":"string"}}}"""
    ).jsonObject)
    private val data = Json.parseToJsonElement("""{"note":"Stored"}""")

    private fun group(collapsible: Boolean, collapsed: kotlinx.serialization.json.JsonElement) = GroupElement(
        label = "Details",
        options = mapOf("collapsible" to JsonPrimitive(collapsible), "collapsed" to collapsed),
        elements = listOf(ControlElement(scope = "#/properties/note")),
    )

    private fun expansion(cupertino: Boolean) = runComposeUiTest {
        var ui by mutableStateOf(group(true, JsonPrimitive(true)))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++ })
        }
        fun fields() = onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size
        assertEquals(0, fields())
        onNodeWithText("Details").performClick()
        assertEquals(1, fields())
        runOnIdle { ui = group(true, JsonPrimitive(true)) }
        assertEquals(1, fields())
        onNodeWithText("Details").performClick()
        assertEquals(0, fields())
        runOnIdle { ui = group(true, JsonPrimitive(false)) }
        assertEquals(1, fields())
        runOnIdle { ui = group(true, JsonPrimitive(true)) }
        assertEquals(0, fields())
        assertEquals(0, changes)
    }

    private fun localizedState(cupertino: Boolean) = runComposeUiTest {
        var locale by mutableStateOf("en")
        setContent {
            JsonForms(data = data, schema = schema, uischema = group(true, JsonPrimitive(true)),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale, translate = { key, fallback, _ ->
                    if (locale == "bg") when (key) {
                        "group.collapsed" -> "Свито"
                        "group.expanded" -> "Разгънато"
                        else -> fallback
                    } else fallback
                }))
        }
        fun description() = onNodeWithText("Details").fetchSemanticsNode()
            .config[SemanticsProperties.StateDescription]
        assertEquals("Collapsed", description())
        runOnIdle { locale = "bg" }
        assertEquals("Свито", description())
        onNodeWithText("Details").performClick()
        assertEquals("Разгънато", description())
    }

    private fun noCatalogState(cupertino: Boolean) = runComposeUiTest {
        var locale by mutableStateOf("en-US")
        setContent {
            JsonForms(data = data, schema = schema, uischema = group(true, JsonPrimitive(true)),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale))
        }
        fun description() = onNodeWithText("Details").fetchSemanticsNode()
            .config[SemanticsProperties.StateDescription]
        assertEquals("Collapsed", description())
        runOnIdle { locale = "bg-BG" }
        assertEquals("Свита", description())
        onNodeWithText("Details").performClick()
        assertEquals("Разгъната", description())
    }

    private fun noncollapsible(cupertino: Boolean) = runComposeUiTest {
        val received = mutableListOf<UiDiagnostic>()
        val reporter = UiDiagnosticReporter { received += it }
        val ui = group(false, JsonNull)
        setContent {
            JsonForms(data = data, schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf(UiDiagnosticReporterConfigKey to reporter))
        }
        waitForIdle()
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        assertEquals(listOf("section.invalidCollapsed"), received.map { it.code })
    }
}
