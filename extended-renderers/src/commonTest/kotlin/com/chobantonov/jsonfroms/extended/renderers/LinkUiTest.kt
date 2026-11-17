package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.models.LinkElement
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class LinkUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement("""{"type":"object"}""").jsonObject)
    private val data = Json.parseToJsonElement("{}")

    @Test fun materialImportedLinksKeepNavigationDistinctFromInertText() = importedLinks(false)
    @Test fun cupertinoImportedLinksKeepNavigationDistinctFromInertText() = importedLinks(true)

    private fun importedLinks(cupertino: Boolean) = runComposeUiTest {
        val fixture = getBuiltInExamples().first { it.name == "spec-presentation" }
        val sections = assertIs<VerticalLayout>(fixture.uischema).elements
        val linkCategory = assertIs<CategorizationElement>(sections[1]).elements
            .filterIsInstance<CategoryElement>().first { it.name == "link" }
        val links = linkCategory.elements.filterIsInstance<LinkElement>()
        assertEquals(8, links.size)
        assertEquals("https://example.com/courses", links[0].href)
        assertEquals("", links[2].href)
        assertEquals("javascript:alert(1)", links[3].href)
        val opened = mutableListOf<String>()
        val diagnostics = mutableListOf<UiDiagnostic>()
        var changes = 0
        val handler = object : UriHandler {
            override fun openUri(uri: String) { opened += uri }
        }
        setContent {
            CompositionLocalProvider(LocalUriHandler provides handler) {
                JsonForms(data = fixture.data, schema = fixture.schema!!,
                    uischema = VerticalLayout(elements = listOf(links[0], links[2], links[3])),
                    renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                    config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter(diagnostics::add)),
                    onChange = { changes++ })
            }
        }
        onNodeWithText("Course catalogue").performClick()
        onNodeWithText("Not yet published").assertExists()
        onNodeWithText("Refused by policy").assertExists()
        waitForIdle()
        assertEquals(listOf("https://example.com/courses"), opened)
        assertEquals(1, onAllNodes(hasClickAction()).fetchSemanticsNodes().size)
        assertEquals(listOf("link.urlRefused"), diagnostics.map { it.code })
        assertEquals(0, changes)
    }

    @Test fun blankTargetRelAlwaysAddsSecurityTokensWithoutDuplicates() {
        assertEquals("noopener noreferrer", linkRel("_blank", null))
        assertEquals("nofollow noopener noreferrer", linkRel("_blank", "nofollow"))
        assertEquals("noopener noreferrer", linkRel("_blank", "noopener noreferrer"))
        assertEquals(null, linkRel("_self", null))
    }

    @Test fun materialLinkUsesPolicyAndRetranslates() = link(false)
    @Test fun cupertinoLinkUsesPolicyAndRetranslates() = link(true)

    @Test fun materialPolicyDiagnosticUsesLocaleDefault() = localizedDiagnostic(false)
    @Test fun cupertinoPolicyDiagnosticUsesLocaleDefault() = localizedDiagnostic(true)

    private fun localizedDiagnostic(cupertino: Boolean) = runComposeUiTest {
        val diagnostics = mutableListOf<UiDiagnostic>()
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = LinkElement(href = "javascript:alert(1)", label = "Help"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter(diagnostics::add)),
                i18n = JsonFormsI18nState(locale = "en-US"),
            )
        }
        waitForIdle()
        onNodeWithText("Help").assertExists()
        assertEquals("Link destination refused by URL policy", diagnostics.single().message)
    }

    private fun link(cupertino: Boolean) = runComposeUiTest {
        var ui by mutableStateOf(LinkElement(href = "https://example.com/help", label = "Help", i18n = "help"))
        var locale by mutableStateOf("en")
        var policyConfig by mutableStateOf<Map<String, Any?>>(emptyMap())
        val opened = mutableListOf<String>()
        val diagnostics = mutableListOf<UiDiagnostic>()
        var changes = 0
        val handler = object : UriHandler {
            override fun openUri(uri: String) { opened += uri }
        }
        setContent {
            CompositionLocalProvider(LocalUriHandler provides handler) {
                JsonForms(
                    data = data,
                    schema = schema,
                    uischema = ui,
                    renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                    config = policyConfig + mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter { diagnostics += it }),
                    i18n = JsonFormsI18nState(locale = locale, translate = { key, fallback, _ ->
                        if (locale == "bg" && key == "help.label") "Помощ" else fallback
                    }),
                    onChange = { changes++ },
                )
            }
        }
        onNodeWithText("Help").performClick()
        assertEquals(listOf("https://example.com/help"), opened)
        runOnIdle { locale = "bg" }
        onNodeWithText("Помощ").performClick()
        assertEquals(2, opened.size)

        runOnIdle { ui = ui.copy(href = "") }
        onNodeWithText("Помощ").assertExists()
        assertEquals(0, onAllNodes(hasClickAction()).fetchSemanticsNodes().size)

        runOnIdle { ui = ui.copy(href = "javascript:alert(1)") }
        waitForIdle()
        onNodeWithText("Помощ").assertExists()
        assertEquals(0, onAllNodes(hasClickAction()).fetchSemanticsNodes().size)
        assertEquals("link.urlRefused", diagnostics.last().code)
        runOnIdle {
            policyConfig = mapOf("jsonformsExtended" to mapOf("security" to mapOf(
                "urlPolicy" to mapOf("allowedSchemes" to listOf("https"), "allowRelative" to false),
            )))
            ui = ui.copy(href = "/help")
        }
        assertEquals(0, onAllNodes(hasClickAction()).fetchSemanticsNodes().size)
        runOnIdle { ui = ui.copy(href = "https://example.com/allowed") }
        onNodeWithText("Помощ").performClick()
        assertEquals("https://example.com/allowed", opened.last())
        runOnIdle { ui = ui.copy(target = "_popup") }
        waitForIdle()
        assertEquals(0, onAllNodes(hasClickAction()).fetchSemanticsNodes().size)
        assertEquals("link.invalidTarget", diagnostics.last().code)
        assertEquals(3, opened.size)
        assertEquals(0, changes)
    }
}
