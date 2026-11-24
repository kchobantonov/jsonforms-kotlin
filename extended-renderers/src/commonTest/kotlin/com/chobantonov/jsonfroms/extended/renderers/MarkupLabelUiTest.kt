package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.LinkAnnotation
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.LabelElement
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class MarkupLabelUiTest {
    @Test fun materialQuotedInterpolateDoesNotEnableTemplates() = quotedInterpolate(materialExtendedRenderers)
    @Test fun cupertinoQuotedInterpolateDoesNotEnableTemplates() = quotedInterpolate(cupertinoExtendedRenderers)

    private fun quotedInterpolate(renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>) = runComposeUiTest {
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"name":"Ada"}"""),
                schema = com.chobantonov.jsonfroms.core.models.JsonSchema(),
                uischema = LabelElement("Hello {name}", options = mapOf(
                    "markup" to JsonPrimitive("markdown"),
                    "interpolate" to JsonPrimitive("true"),
                    "textParams" to JsonObject(mapOf("name" to JsonPrimitive("{data.name}"))),
                )),
                renderers = renderers,
                config = mapOf("jsonformsExtended" to mapOf("dynamicValues" to mapOf("enabled" to true))),
            )
        }
        onNodeWithText("Hello {name}").assertExists()
        onAllNodesWithText("Hello Ada").assertCountEquals(0)
    }

    @Test fun quotedBooleanConfigurationKeepsDocumentedDefaults() {
        val label = LabelElement("Text", options = mapOf("typography" to JsonPrimitive("false")))
        val markdownConfig = mapOf("jsonformsExtended" to mapOf(
            "markup" to mapOf("markdown" to mapOf("enabled" to JsonPrimitive("false"))),
        ))
        val interpolationConfig = mapOf("jsonformsExtended" to mapOf(
            "dynamicValues" to mapOf("enabled" to JsonPrimitive("true")),
        ))
        assertTrue(markdownEnabled(markdownConfig))
        assertEquals(false, interpolationDataEnabled(interpolationConfig))
        assertTrue(markupTypographyEnabled(label, emptyMap()))
    }

    @Test fun materialUnknownProfileFallsBackToBasicGrammar() = unknownProfile(materialExtendedRenderers)
    @Test fun cupertinoUnknownProfileFallsBackToBasicGrammar() = unknownProfile(cupertinoExtendedRenderers)

    private fun unknownProfile(renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>) = runComposeUiTest {
        val diagnostics = mutableListOf<UiDiagnostic>()
        setContent {
            JsonForms(
                data = kotlinx.serialization.json.JsonNull,
                schema = com.chobantonov.jsonfroms.core.models.JsonSchema(),
                uischema = LabelElement("# Literal heading\n\n| A | B |\n| --- | --- |\n| one | two |",
                    options = mapOf("markup" to JsonPrimitive("markdown"))),
                renderers = renderers,
                config = mapOf(
                    "jsonformsExtended" to mapOf("markup" to mapOf("markdown" to mapOf("profile" to "future"))),
                    UiDiagnosticReporterConfigKey to UiDiagnosticReporter(diagnostics::add),
                ),
            )
        }
        onNodeWithText("# Literal heading", substring = true).assertExists()
        onNodeWithText("| A | B |", substring = true).assertExists()
        assertEquals(0, onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading),
            useUnmergedTree = true).fetchSemanticsNodes().size)
        waitForIdle()
        assertEquals(emptyList(), diagnostics)
    }

    @Test fun materialExtendedMarkdownIsStructuralAndSafe() = extendedMarkdown(materialExtendedRenderers)
    @Test fun cupertinoExtendedMarkdownIsStructuralAndSafe() = extendedMarkdown(cupertinoExtendedRenderers)

    private fun extendedMarkdown(renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>) = runComposeUiTest {
        val diagnostics = mutableListOf<UiDiagnostic>()
        setContent {
            JsonForms(
                data = kotlinx.serialization.json.JsonNull,
                schema = com.chobantonov.jsonfroms.core.models.JsonSchema(),
                uischema = LabelElement(
                    "# Guide **bold**\n\n> quoted text\n\n```html\n<script>safe text</script>\n```\n\n| Name | Link |\n| --- | --- |\n| one | [guide](/help) |\n| two | [blocked](javascript:alert(1)) |",
                    options = mapOf("markup" to JsonPrimitive("markdown")),
                ),
                renderers = renderers,
                config = mapOf(
                    "jsonformsExtended" to mapOf("markup" to mapOf("markdown" to mapOf("profile" to "extended"))),
                    UiDiagnosticReporterConfigKey to UiDiagnosticReporter(diagnostics::add),
                ),
            )
        }
        onNodeWithText("Guide bold").assertExists()
        onNodeWithText("quoted text").assertExists()
        onNodeWithText("<script>safe text</script>", substring = true).assertExists()
        val tables = onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.CollectionInfo),
            useUnmergedTree = true).fetchSemanticsNodes()
        assertEquals(1, tables.size)
        assertEquals(3, tables.single().config[SemanticsProperties.CollectionInfo].rowCount)
        assertEquals(6, onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.CollectionItemInfo),
            useUnmergedTree = true).fetchSemanticsNodes().size)
        assertEquals(3, onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading),
            useUnmergedTree = true).fetchSemanticsNodes().size)
        val link = onNodeWithText("guide").fetchSemanticsNode().config[SemanticsProperties.Text].single()
        assertEquals("/help", assertIs<LinkAnnotation.Url>(link.getLinkAnnotations(0, link.length).single().item).url)
        val blocked = onNodeWithText("blocked").fetchSemanticsNode().config[SemanticsProperties.Text].single()
        assertEquals(emptyList(), blocked.getLinkAnnotations(0, blocked.length))
        waitForIdle()
        assertEquals(listOf("markup.urlRefused"), diagnostics.map { it.code })
    }

    @Test fun materialMarkdownListsHaveItemSemantics() = structuredLists(materialExtendedRenderers)
    @Test fun cupertinoMarkdownListsHaveItemSemantics() = structuredLists(cupertinoExtendedRenderers)

    private fun structuredLists(renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>) = runComposeUiTest {
        val diagnostics = mutableListOf<UiDiagnostic>()
        setContent {
            JsonForms(
                data = kotlinx.serialization.json.JsonNull,
                schema = com.chobantonov.jsonfroms.core.models.JsonSchema(),
                uischema = LabelElement(
                    "Before\n\n- first [guide][ref]\n- second\n  - nested\n- [blocked](javascript:alert(1))\n\n3. one\n1. two\n\n[ref]: /help",
                    options = mapOf("markup" to JsonPrimitive("markdown")),
                ),
                renderers = renderers,
                config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter(diagnostics::add)),
            )
        }
        val lists = onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.CollectionInfo),
            useUnmergedTree = true).fetchSemanticsNodes()
        assertEquals(3, lists.size)
        assertEquals(listOf(3, 1, 2), lists.map {
            it.config[SemanticsProperties.CollectionInfo].rowCount
        })
        assertEquals(6, onAllNodes(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.CollectionItemInfo),
            useUnmergedTree = true).fetchSemanticsNodes().size)
        onAllNodesWithText("•").assertCountEquals(4)
        onNodeWithText("3.").assertExists()
        onNodeWithText("4.").assertExists()
        val first = onNodeWithText("first guide").fetchSemanticsNode()
            .config[SemanticsProperties.Text].single()
        assertEquals("/help", assertIs<LinkAnnotation.Url>(
            first.getLinkAnnotations(0, first.length).single().item).url)
        val blocked = onNodeWithText("blocked").fetchSemanticsNode()
            .config[SemanticsProperties.Text].single()
        assertEquals(emptyList(), blocked.getLinkAnnotations(0, blocked.length))
        waitForIdle()
        assertEquals(listOf("markup.urlRefused"), diagnostics.map { it.code })
    }

    @Test fun materialTypographySwitchKeepsMarkdownParsing() = typographySwitch(materialExtendedRenderers)
    @Test fun cupertinoTypographySwitchKeepsMarkdownParsing() = typographySwitch(cupertinoExtendedRenderers)

    private fun typographySwitch(renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>) = runComposeUiTest {
        val source = "Text **bold** [link](/help)"
        val markdown = JsonPrimitive("markdown")
        setContent {
            JsonForms(
                data = kotlinx.serialization.json.JsonNull,
                schema = com.chobantonov.jsonfroms.core.models.JsonSchema(),
                uischema = VerticalLayout(elements = listOf(
                    LabelElement(source, options = mapOf("markup" to markdown,
                        "typography" to JsonPrimitive(true))),
                    LabelElement(source, options = mapOf("markup" to markdown)),
                )),
                renderers = renderers,
                config = mapOf("jsonformsExtended" to mapOf("markup" to mapOf(
                    "typography" to false,
                    "markdown" to mapOf("profile" to "unknown"),
                ))),
            )
        }
        val nodes = onAllNodesWithText("Text bold link")
        nodes.assertCountEquals(2)
        val entries = nodes.fetchSemanticsNodes()
        for (entry in entries) {
            val text = entry.config[SemanticsProperties.Text].single()
            assertEquals(1, text.getLinkAnnotations(0, text.length).size)
            assertEquals("/help", assertIs<LinkAnnotation.Url>(
                text.getLinkAnnotations(0, text.length).single().item).url)
            assertTrue(text.spanStyles.isNotEmpty())
        }
        if (renderers === materialExtendedRenderers) {
            assertTrue(entries[0].boundsInRoot.height > entries[1].boundsInRoot.height)
        }
    }

    @Test fun materialImportedMarkdownAndRefusedLink() = importedFixture(materialExtendedRenderers)
    @Test fun cupertinoImportedMarkdownAndRefusedLink() = importedFixture(cupertinoExtendedRenderers)

    private fun importedFixture(renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>) = runComposeUiTest {
        val fixture = getBuiltInExamples().first { it.name == "spec-markup-label" }
        val categories = assertIs<VerticalLayout>(fixture.uischema).elements
            .filterIsInstance<CategorizationElement>().single().elements.filterIsInstance<CategoryElement>()
        val first = categories.first { it.label == "Markdown content" }
        val legacy = categories.first { it.label == "Headings and literal text" }.elements
            .filterIsInstance<LabelElement>().first { it.i18n == "legacy" }
        val diagnostics = mutableListOf<UiDiagnostic>()
        setContent {
            JsonForms(
                data = fixture.data,
                schema = fixture.schema!!,
                uischema = VerticalLayout(elements = first.elements + legacy),
                renderers = renderers,
                config = fixture.config + (UiDiagnosticReporterConfigKey to UiDiagnosticReporter(diagnostics::add)),
            )
        }
        val introduction = onNodeWithText("Joining instructions.", substring = true)
        introduction.assertExists()
        onNodeWithText("**Joining instructions.**", substring = true).assertDoesNotExist()
        val text = introduction.fetchSemanticsNode().config[SemanticsProperties.Text].single()
        val destinations = text.getLinkAnnotations(0, text.length).map {
            assertIs<LinkAnnotation.Url>(it.item).url
        }
        assertEquals(listOf("https://example.com/venue", "mailto:workshops@example.com"), destinations)
        onNodeWithText("old policy page", substring = true).assertExists()
        onNodeWithText("Link blocked by URL policy").assertExists()
        waitForIdle()
        assertEquals(listOf("markup.urlRefused"), diagnostics.map { it.code })
    }

    @Test fun materialImportedOptionsTranslateAndKeepLiteralText() = importedOptions(materialExtendedRenderers)
    @Test fun cupertinoImportedOptionsTranslateAndKeepLiteralText() = importedOptions(cupertinoExtendedRenderers)

    private fun importedOptions(renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>) = runComposeUiTest {
        val fixture = getBuiltInExamples().first { it.name == "spec-markup-label" }
        val categories = assertIs<VerticalLayout>(fixture.uischema).elements
            .filterIsInstance<CategorizationElement>().single().elements.filterIsInstance<CategoryElement>()
        val headings = categories.first { it.label == "Headings and literal text" }
        val options = categories.first { it.label == "Markup options" }
        val locale = mutableStateOf("en")
        val diagnostics = mutableListOf<UiDiagnostic>()
        val reporter = UiDiagnosticReporter(diagnostics::add)
        setContent {
            JsonForms(
                data = fixture.data,
                schema = fixture.schema!!,
                uischema = VerticalLayout(elements = headings.elements + options.elements),
                renderers = renderers,
                config = fixture.config + (UiDiagnosticReporterConfigKey to reporter),
                i18n = JsonFormsI18nState(locale = locale.value, translate = { key, fallback, _ ->
                    fixture.localizedTranslations[locale.value]?.get(key) ?: fallback
                }),
            )
        }
        onNodeWithText("# Cancellation", substring = true).assertExists()
        onNodeWithText("Rates are 120 EUR* per seat (*excluding VAT).").assertExists()
        onNodeWithText("This notice was authored for a markup language", substring = true).assertExists()
        onNodeWithText("Seats are held for 15 minutes", substring = true).assertExists()
        onNodeWithText("**15 minutes**", substring = true).assertDoesNotExist()
        val fineprint = onNodeWithText("Seats are held for 15 minutes", substring = true)
            .fetchSemanticsNode().config[SemanticsProperties.Text].single()
        assertEquals(listOf("https://example.com/terms"), fineprint.getLinkAnnotations(0, fineprint.length).map {
            assertIs<LinkAnnotation.Url>(it.item).url
        })
        waitForIdle()
        assertEquals(setOf("markup.urlRefused", "markup.unsupported"), diagnostics.map { it.code }.toSet())

        runOnIdle { locale.value = "bg" }
        onNodeWithText("# Отказ от участие", substring = true).assertExists()
        onNodeWithText("Цената е 120 EUR* на място (*без ДДС).").assertExists()
        onNodeWithText("Местата се запазват за 15 минути", substring = true).assertExists()
        onNodeWithText("**15 минути**", substring = true).assertDoesNotExist()
        waitForIdle()
        assertEquals(2, diagnostics.size)
    }

    @Test fun materialDisabledAndUnknownMarkupKeepText() = refusedMarkup(materialExtendedRenderers)
    @Test fun cupertinoDisabledAndUnknownMarkupKeepText() = refusedMarkup(cupertinoExtendedRenderers)

    @Test fun materialMarkupConfigUsesLiteralNamespace() = markupLiteralNamespace(materialExtendedRenderers)
    @Test fun cupertinoMarkupConfigUsesLiteralNamespace() = markupLiteralNamespace(cupertinoExtendedRenderers)

    private fun markupLiteralNamespace(renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>) = runComposeUiTest {
        val diagnostics = mutableListOf<UiDiagnostic>()
        setContent {
            JsonForms(
                data = kotlinx.serialization.json.JsonNull,
                schema = com.chobantonov.jsonfroms.core.models.JsonSchema(),
                uischema = LabelElement("**Readable**", options = mapOf("markup" to JsonPrimitive("markdown"))),
                renderers = renderers,
                config = mapOf(
                    "markup" to mapOf("markdown" to mapOf("enabled" to false)),
                    "jsonFormsExtended" to mapOf("markup" to mapOf("markdown" to mapOf("enabled" to false))),
                    "jsonformsExtended" to mapOf("markup" to mapOf("markdown" to mapOf("enabled" to true))),
                    UiDiagnosticReporterConfigKey to UiDiagnosticReporter(diagnostics::add),
                ),
            )
        }
        onNodeWithText("Readable").assertExists()
        onNodeWithText("**Readable**").assertDoesNotExist()
        onNodeWithText("Markdown disabled").assertDoesNotExist()
        waitForIdle()
        assertEquals(emptyList(), diagnostics)
    }

    private fun refusedMarkup(renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>) = runComposeUiTest {
        val diagnostics = mutableListOf<UiDiagnostic>()
        val disabled = LabelElement("**Still readable**", options = mapOf("markup" to JsonPrimitive("markdown")))
        val unknown = LabelElement("Plain notice", options = mapOf("markup" to JsonPrimitive("asciidoc")))
        setContent {
            JsonForms(
                data = kotlinx.serialization.json.JsonNull,
                schema = com.chobantonov.jsonfroms.core.models.JsonSchema(),
                uischema = VerticalLayout(elements = listOf(disabled, unknown)),
                renderers = renderers,
                config = mapOf(
                    "jsonformsExtended" to mapOf("markup" to mapOf("markdown" to mapOf("enabled" to false))),
                    UiDiagnosticReporterConfigKey to UiDiagnosticReporter(diagnostics::add),
                ),
            )
        }
        onNodeWithText("**Still readable**").assertExists()
        onNodeWithText("Plain notice").assertExists()
        onNodeWithText("Markdown disabled").assertExists()
        onNodeWithText("Markup format unsupported").assertExists()
        waitForIdle()
        assertEquals(setOf("markup.markdownDisabled", "markup.unsupported"), diagnostics.map { it.code }.toSet())
    }

    @Test fun materialNoCatalogUsesRegionalMarkupBundle() = noCatalogBundle(materialExtendedRenderers)
    @Test fun cupertinoNoCatalogUsesRegionalMarkupBundle() = noCatalogBundle(cupertinoExtendedRenderers)

    private fun noCatalogBundle(renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>) = runComposeUiTest {
        val locale = mutableStateOf("en-US")
        setContent {
            JsonForms(
                data = kotlinx.serialization.json.JsonNull,
                schema = com.chobantonov.jsonfroms.core.models.JsonSchema(),
                uischema = LabelElement("Notice", options = mapOf("markup" to JsonPrimitive("asciidoc"))),
                renderers = renderers,
                i18n = JsonFormsI18nState(locale = locale.value),
            )
        }
        onNodeWithText("Markup format unsupported").assertExists()
        runOnIdle { locale.value = "bg-BG" }
        onNodeWithText("Форматът за маркиране не се поддържа").assertExists()
    }

    @Test fun materialPartialCatalogOverridesBundleDefault() = partialCatalogBundle(materialExtendedRenderers)
    @Test fun cupertinoPartialCatalogOverridesBundleDefault() = partialCatalogBundle(cupertinoExtendedRenderers)

    private fun partialCatalogBundle(renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>) = runComposeUiTest {
        val defaults = mutableListOf<Pair<String, String?>>()
        setContent {
            JsonForms(
                data = kotlinx.serialization.json.JsonNull,
                schema = com.chobantonov.jsonfroms.core.models.JsonSchema(),
                uischema = VerticalLayout(elements = listOf(
                    LabelElement("Notice", options = mapOf("markup" to JsonPrimitive("asciidoc"))),
                    LabelElement("**Readable**", options = mapOf("markup" to JsonPrimitive("markdown"))),
                )),
                renderers = renderers,
                config = mapOf("jsonformsExtended" to mapOf("markup" to mapOf("markdown" to mapOf("enabled" to false)))),
                i18n = JsonFormsI18nState(locale = "en-GB", translate = { key, fallback, _ ->
                    defaults += key to fallback
                    if (key == "markup.unsupported") "Form notice" else fallback
                }),
            )
        }
        onNodeWithText("Form notice").assertExists()
        onNodeWithText("Markdown disabled").assertExists()
        waitForIdle()
        assertTrue(defaults.contains("markup.unsupported" to "Markup format unsupported"))
        assertTrue(defaults.contains("markup.markdownDisabled" to "Markdown disabled"))
    }
}
