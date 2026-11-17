package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.compose.JsonFormsChange
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.UnknownUiSchemaElement
import com.chobantonov.jsonfroms.core.models.toUiSchemaElement
import com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class UnsupportedTemplateLayoutTest {
    private val authored = Json.parseToJsonElement("""{
      "type":"TemplateLayout","lang":"jsx","template":"<div>{elements}</div>",
      "elements":[{"type":"Control","scope":"#/properties/name","name":"editor"}]
    }""") as JsonObject

    @Test fun parserRetainsTemplateAndResolverDistinguishesUnavailableReasons() {
        val element = assertIs<UnknownUiSchemaElement>(authored.toUiSchemaElement())
        assertEquals(JsonPrimitive("<div>{elements}</div>"), element.properties["template"])
        assertEquals("editor", element.elements.single().name)
        assertEquals("template.evaluationDisabled", templateLayoutUnavailable(element, emptyMap()).code)
        assertEquals("template.evaluationDisabled", templateLayoutUnavailable(element,
            mapOf("jsonformsExtended" to mapOf("dynamic" to true))).code)
        assertEquals("template.evaluationDisabled", templateLayoutUnavailable(element,
            mapOf("jsonformsExtended" to mapOf("security" to mapOf("allowScriptEvaluation" to "true")))).code)
        assertEquals("template.engineUnavailable", templateLayoutUnavailable(element,
            mapOf("jsonformsExtended" to mapOf("security" to mapOf("allowScriptEvaluation" to true)))).code)
        assertEquals("template.unsupportedLanguage", templateLayoutUnavailable(
            element.copy(properties = Json.parseToJsonElement("""{"lang":"mustache"}""") as JsonObject),
            emptyMap(),
        ).code)
        val configured = element.copy(properties = JsonObject(emptyMap()))
        assertEquals("mustache", templateLayoutUnavailable(configured,
            mapOf("jsonformsExtended" to mapOf("defaultTemplateLang" to "mustache"))).language)
        assertEquals("ractive", templateLayoutUnavailable(configured, emptyMap()).language)
    }

    @Test fun materialShowsDiagnosticWithoutDispatchingTemplateChildren() = mounted(materialExtendedRenderers)

    @Test fun cupertinoShowsDiagnosticWithoutDispatchingTemplateChildren() = mounted(cupertinoExtendedRenderers)

    @Test fun materialShowsUnsupportedLanguage() = mounted(
        materialExtendedRenderers,
        element = (authored.toUiSchemaElement() as UnknownUiSchemaElement).copy(
            properties = Json.parseToJsonElement("""{"lang":"mustache","template":"<p>unused</p>"}""") as JsonObject,
        ),
        expectedCode = "template.unsupportedLanguage",
        expectedText = "Template language is unsupported: mustache",
    )

    @Test fun cupertinoShowsDisabledEvaluation() = mounted(
        cupertinoExtendedRenderers,
        securityEnabled = false,
        expectedCode = "template.evaluationDisabled",
        expectedText = "Template script evaluation is disabled",
    )

    private fun mounted(
        renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
        element: UnknownUiSchemaElement = authored.toUiSchemaElement() as UnknownUiSchemaElement,
        securityEnabled: Boolean = true,
        expectedCode: String = "template.engineUnavailable",
        expectedText: String = "Template engine is unavailable on this host: jsx",
    ) = runComposeUiTest {
        val diagnostics = mutableListOf<UiDiagnostic>()
        val changes = mutableListOf<JsonFormsChange>()
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"name":"Ada"}"""),
                schema = JsonSchema(),
                uischema = element,
                renderers = renderers,
                config = mapOf(
                    UiDiagnosticReporterConfigKey to UiDiagnosticReporter(diagnostics::add),
                    "jsonformsExtended" to mapOf("security" to mapOf("allowScriptEvaluation" to securityEnabled)),
                ),
                onChange = changes::add,
            )
        }
        onNodeWithText(expectedText).assertExists()
        waitForIdle()
        assertEquals(listOf(expectedCode), diagnostics.map { it.code })
        assertTrue(changes.isEmpty())
    }

    @Test fun materialNoCatalogTemplateNoticeFollowsLocale() = noCatalogLocale(materialExtendedRenderers)
    @Test fun cupertinoNoCatalogTemplateNoticeFollowsLocale() = noCatalogLocale(cupertinoExtendedRenderers)

    private fun noCatalogLocale(renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>) = runComposeUiTest {
        val locale = mutableStateOf("en-US")
        val changes = mutableListOf<JsonFormsChange>()
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"name":"Ada"}"""),
                schema = JsonSchema(),
                uischema = authored.toUiSchemaElement(),
                renderers = renderers,
                config = mapOf("jsonformsExtended" to mapOf(
                    "security" to mapOf("allowScriptEvaluation" to true),
                )),
                i18n = JsonFormsI18nState(locale = locale.value),
                onChange = changes::add,
            )
        }
        onNodeWithText("Template engine is unavailable on this host: jsx").assertExists()
        runOnIdle { locale.value = "bg-BG" }
        onNodeWithText("Шаблонният двигател не е наличен на този хост: jsx").assertExists()
        waitForIdle()
        assertTrue(changes.isEmpty())
    }
}
