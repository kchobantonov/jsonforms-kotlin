package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.compose.JsonFormsChange
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticSeverity
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.ImageViewElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.SeparatorElement
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalTestApi::class)
class ImageViewUiTest {
    private val imageData = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Y9c0kcAAAAASUVORK5CYII="
    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"photo":{"type":"string"}}}"""
    ) as JsonObject)

    @Test fun materialRendersScopedImageAndClearsOnWrongData() = scopedImage(materialExtendedRenderers)
    @Test fun cupertinoRendersScopedImageAndClearsOnWrongData() = scopedImage(cupertinoExtendedRenderers)

    private fun scopedImage(renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>) = runComposeUiTest {
        val diagnostics = mutableListOf<UiDiagnostic>()
        val data = mutableStateOf<JsonElement>(buildJsonObject { put("photo", imageData) })
        setContent {
            JsonForms(
                data = data.value,
                schema = schema,
                uischema = ImageViewElement(scope = "#/properties/photo", alt = "Profile portrait"),
                renderers = renderers,
                config = mapOf(
                    "jsonformsExtended" to mapOf("security" to mapOf("urlPolicy" to mapOf(
                        "allowImageDataUrls" to true,
                    ))),
                    UiDiagnosticReporterConfigKey to UiDiagnosticReporter(diagnostics::add),
                ),
            )
        }
        onNodeWithContentDescription("Profile portrait").assertExists()
        assertEquals(emptyList(), diagnostics)
        runOnIdle { data.value = buildJsonObject { put("photo", 42) } }
        waitForIdle()
        onNodeWithContentDescription("Profile portrait").assertDoesNotExist()
        onNodeWithText("Image unavailable").assertExists()
        assertEquals(listOf("image.nonStringSource"), diagnostics.map { it.code })
        assertEquals(UiDiagnosticSeverity.Error, diagnostics.single().severity)
    }

    @Test fun materialRefusesDirectUrlWithoutScopeFallback() = refusedDirectUrl(materialExtendedRenderers)
    @Test fun cupertinoRefusesDirectUrlWithoutScopeFallback() = refusedDirectUrl(cupertinoExtendedRenderers)

    @Test fun materialBlockedImageUsesLocaleDefault() = localizedBlocked(materialExtendedRenderers)
    @Test fun cupertinoBlockedImageUsesLocaleDefault() = localizedBlocked(cupertinoExtendedRenderers)
    @Test fun materialImageDiagnosticsFollowLocaleChange() = diagnosticLocaleChange(materialExtendedRenderers)
    @Test fun cupertinoImageDiagnosticsFollowLocaleChange() = diagnosticLocaleChange(cupertinoExtendedRenderers)

    private fun diagnosticLocaleChange(
        renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
    ) = runComposeUiTest {
        val diagnostics = mutableListOf<UiDiagnostic>()
        val reporter = UiDiagnosticReporter(diagnostics::add)
        val locale = mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(data = buildJsonObject {}, schema = schema,
                uischema = ImageViewElement(src = "javascript:alert(1)"),
                renderers = renderers,
                config = mapOf(UiDiagnosticReporterConfigKey to reporter),
                i18n = JsonFormsI18nState(locale = locale.value),
                onChange = { changes++ })
        }
        waitForIdle()
        assertEquals(2, diagnostics.size)
        assertEquals("Image source refused by URL policy",
            diagnostics.last { it.code == "image.urlRefused" }.message)
        assertEquals("Image alternative text is missing",
            diagnostics.last { it.code == "image.missingAlt" }.message)
        runOnIdle { locale.value = "bg-BG" }
        waitForIdle()
        assertEquals(4, diagnostics.size)
        assertEquals("Източникът на изображението е отказан от правилата за URL адреси",
            diagnostics.last { it.code == "image.urlRefused" }.message)
        assertEquals("Липсва алтернативен текст за изображението",
            diagnostics.last { it.code == "image.missingAlt" }.message)
        assertEquals(0, changes)
    }

    private fun localizedBlocked(renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>) = runComposeUiTest {
        val diagnostics = mutableListOf<UiDiagnostic>()
        setContent {
            JsonForms(
                data = buildJsonObject {}, schema = schema,
                uischema = ImageViewElement(src = "javascript:alert(1)", alt = "Portrait"),
                renderers = renderers,
                config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter(diagnostics::add)),
                i18n = JsonFormsI18nState(locale = "bg-BG"),
            )
        }
        waitForIdle()
        onNodeWithText("Изображението е блокирано").assertExists()
        assertEquals("Източникът на изображението е отказан от правилата за URL адреси",
            diagnostics.single().message)
    }

    @Test fun materialUnavailableImageUsesLocaleDefault() = localizedUnavailable(materialExtendedRenderers)
    @Test fun cupertinoUnavailableImageUsesLocaleDefault() = localizedUnavailable(cupertinoExtendedRenderers)

    private fun localizedUnavailable(renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>) = runComposeUiTest {
        val diagnostics = mutableListOf<UiDiagnostic>()
        setContent {
            JsonForms(
                data = buildJsonObject {}, schema = schema,
                uischema = ImageViewElement(alt = "Portrait"),
                renderers = renderers,
                config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter(diagnostics::add)),
                i18n = JsonFormsI18nState(locale = "en-US"),
            )
        }
        waitForIdle()
        onNodeWithText("Image unavailable").assertExists()
        assertEquals("ImageView needs src or scope", diagnostics.single().message)
    }

    private fun refusedDirectUrl(renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>) = runComposeUiTest {
        val diagnostics = mutableListOf<UiDiagnostic>()
        val locale = mutableStateOf("en")
        val reporter = UiDiagnosticReporter(diagnostics::add)
        setContent {
            JsonForms(
                data = buildJsonObject { put("photo", imageData) },
                schema = schema,
                uischema = ImageViewElement(src = "javascript:alert(1)", scope = "#/properties/photo", alt = "Portrait"),
                renderers = renderers,
                config = mapOf(
                    "jsonformsExtended" to mapOf("security" to mapOf("urlPolicy" to mapOf(
                        "allowImageDataUrls" to true,
                    ))),
                    UiDiagnosticReporterConfigKey to reporter,
                ),
                i18n = JsonFormsI18nState(locale = locale.value, translate = { key, fallback, _ ->
                    if (locale.value == "bg" && key == "image.blocked") "Изображението е блокирано" else fallback
                }),
            )
        }
        waitForIdle()
        onNodeWithContentDescription("Portrait").assertDoesNotExist()
        onNodeWithText("Image blocked").assertExists()
        assertEquals(listOf("image.urlRefused"), diagnostics.map { it.code })
        assertEquals(UiDiagnosticSeverity.Error, diagnostics.single().severity)
        runOnIdle { locale.value = "bg" }
        onNodeWithText("Изображението е блокирано").assertExists()
        assertEquals(2, diagnostics.size)
        assertEquals("Източникът на изображението е отказан от правилата за URL адреси",
            diagnostics.last().message)
    }

    @Test fun materialReportsMissingImageFields() = missingFields(materialExtendedRenderers)
    @Test fun cupertinoReportsMissingImageFields() = missingFields(cupertinoExtendedRenderers)

    private fun missingFields(renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>) = runComposeUiTest {
        val diagnostics = mutableListOf<UiDiagnostic>()
        setContent {
            JsonForms(
                data = buildJsonObject {},
                schema = schema,
                uischema = VerticalLayout(elements = listOf(
                    ImageViewElement(src = imageData),
                    ImageViewElement(alt = "No source"),
                    ImageViewElement(scope = "#/properties/missing", alt = "Missing field"),
                )),
                renderers = renderers,
                config = mapOf(
                    "jsonformsExtended" to mapOf("security" to mapOf("urlPolicy" to mapOf(
                        "allowImageDataUrls" to true,
                    ))),
                    UiDiagnosticReporterConfigKey to UiDiagnosticReporter(diagnostics::add),
                ),
            )
        }
        waitForIdle()
        assertEquals(
            mapOf(
                "image.missingAlt" to UiDiagnosticSeverity.Warning,
                "image.noSource" to UiDiagnosticSeverity.Error,
                "image.scopeNotString" to UiDiagnosticSeverity.Error,
            ),
            diagnostics.associate { it.code to it.severity },
        )
        onNodeWithContentDescription("No source").assertDoesNotExist()
        onAllNodesWithText("Image unavailable").assertCountEquals(2)
    }

    @Test fun materialImportedPresentationImagesFollowDataAndRule() = importedPresentation(materialExtendedRenderers)
    @Test fun cupertinoImportedPresentationImagesFollowDataAndRule() = importedPresentation(cupertinoExtendedRenderers)

    @Test fun materialReportsLoadFailureAndRecoversForNewSource() = loadFailure(materialExtendedRenderers)
    @Test fun cupertinoReportsLoadFailureAndRecoversForNewSource() = loadFailure(cupertinoExtendedRenderers)

    private fun loadFailure(
        renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
    ) = runComposeUiTest {
        val diagnostics = mutableListOf<UiDiagnostic>()
        val reporter = UiDiagnosticReporter(diagnostics::add)
        val source = mutableStateOf("data:image/png;base64,%%%")
        val locale = mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(
                data = buildJsonObject {},
                schema = schema,
                uischema = ImageViewElement(src = source.value, alt = "Preview"),
                renderers = renderers,
                config = mapOf(
                    "jsonformsExtended" to mapOf("security" to mapOf("urlPolicy" to mapOf(
                        "allowImageDataUrls" to true,
                    ))),
                    UiDiagnosticReporterConfigKey to reporter,
                ),
                i18n = JsonFormsI18nState(locale = locale.value, translate = { key, fallback, _ ->
                    if (locale.value != "bg") fallback else when (key) {
                        "image.unavailable" -> "Изображението не е достъпно"
                        "image.retry" -> "Опитай отново"
                        else -> fallback
                    }
                }),
                onChange = { changes++ },
            )
        }
        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithText("Image unavailable").fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithContentDescription("Preview").assertDoesNotExist()
        assertEquals(listOf("image.loadFailed"), diagnostics.map { it.code })
        assertEquals(UiDiagnosticSeverity.Error, diagnostics.single().severity)
        runOnIdle { locale.value = "bg" }
        onNodeWithText("Изображението не е достъпно").assertExists()
        waitUntil(timeoutMillis = 5_000) { diagnostics.size == 2 }
        assertEquals("Източникът на изображението не може да се зареди",
            diagnostics.last().message)
        onNodeWithText("Опитай отново").performClick()
        waitUntil(timeoutMillis = 5_000) { diagnostics.size == 3 }
        onNodeWithText("Изображението не е достъпно").assertExists()
        val fixture = getBuiltInExamples().first { it.name == "spec-presentation" }
        val imageCategory = assertIs<CategorizationElement>(
            assertIs<VerticalLayout>(fixture.uischema).elements[1],
        ).elements.filterIsInstance<CategoryElement>().first { it.name == "image" }
        val valid = imageCategory.elements.filterIsInstance<ImageViewElement>().first().src!!
        runOnIdle { source.value = valid }
        onNodeWithText("Изображението не е достъпно").assertDoesNotExist()
        onNodeWithContentDescription("Preview").assertExists()
        assertEquals(3, diagnostics.size)
        assertEquals(0, changes)
    }

    private fun importedPresentation(
        renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
    ) = runComposeUiTest {
        val fixture = getBuiltInExamples().first { it.name == "spec-presentation" }
        val sections = assertIs<VerticalLayout>(fixture.uischema).elements
        val imageCategory = assertIs<CategorizationElement>(sections[1]).elements
            .filterIsInstance<CategoryElement>().first { it.name == "image" }
        val images = imageCategory.elements.filterIsInstance<ImageViewElement>()
        val separators = imageCategory.elements.filterIsInstance<SeparatorElement>()
        assertEquals(3, images.size)
        assertEquals(2, separators.size)
        assertEquals(null, images[0].scope)
        assertEquals("#/properties/badgeImage", images[1].scope)
        assertEquals("", images[2].alt)
        val data = mutableStateOf(fixture.data)
        val changes = mutableListOf<JsonFormsChange>()
        setContent {
            JsonForms(
                data = data.value,
                schema = fixture.schema!!,
                uischema = VerticalLayout(elements = imageCategory.elements.filter {
                    it is ImageViewElement || it is SeparatorElement
                }),
                renderers = renderers,
                config = fixture.config,
                onChange = changes::add,
            )
        }
        onNodeWithContentDescription("Blue circle beside the words Course handbook").assertExists()
        onNodeWithContentDescription("Course completion badge").assertExists()
        onAllNodesWithContentDescription("Horizontal separator").assertCountEquals(2)
        runOnIdle {
            data.value = JsonObject((data.value as JsonObject).toMutableMap().apply {
                put("badgeImage", JsonPrimitive(""))
                put("showBanner", JsonPrimitive(false))
            })
        }
        waitForIdle()
        onNodeWithContentDescription("Blue circle beside the words Course handbook").assertDoesNotExist()
        onNodeWithContentDescription("Course completion badge").assertDoesNotExist()
        onNodeWithText("Image unavailable").assertDoesNotExist()
        onNodeWithText("Image blocked").assertDoesNotExist()
        onAllNodesWithContentDescription("Horizontal separator").assertCountEquals(2)
        assertEquals(emptyList(), changes)
    }
}
