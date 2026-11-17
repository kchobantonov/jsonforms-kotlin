package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticSeverity
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.LabelElement
import com.chobantonov.jsonfroms.core.models.ListWithDetailElement
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry
import com.chobantonov.jsonfroms.core.store.JsonFormsUISchemaRegistryEntry
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class LabelInterpolationUiTest {
    @Test fun materialMarkdownUrlFollowsDataAndLocale() = markdownUrl(materialExtendedRenderers)
    @Test fun cupertinoMarkdownUrlFollowsDataAndLocale() = markdownUrl(cupertinoExtendedRenderers)
    @Test fun materialReferenceMarkdownUrlFollowsDataAndLocale() = referenceMarkdownUrl(materialExtendedRenderers)
    @Test fun cupertinoReferenceMarkdownUrlFollowsDataAndLocale() = referenceMarkdownUrl(cupertinoExtendedRenderers)
    @Test fun materialFixtureStaticAndDynamicLabels() = fixture(materialExtendedRenderers)
    @Test fun cupertinoFixtureStaticAndDynamicLabels() = fixture(cupertinoExtendedRenderers)
    @Test fun materialExplicitTranslationFallbackReachesHost() = translationFallback(materialExtendedRenderers)
    @Test fun cupertinoExplicitTranslationFallbackReachesHost() = translationFallback(cupertinoExtendedRenderers)
    @Test fun materialCollectionPredicateUpdatesWithHostData() = collectionPredicate(materialExtendedRenderers)
    @Test fun cupertinoCollectionPredicateUpdatesWithHostData() = collectionPredicate(cupertinoExtendedRenderers)

    private fun markdownUrl(
        renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
    ) = runComposeUiTest {
        val data = mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"name":"A&B"}"""))
        val locale = mutableStateOf("en")
        var writes = 0
        setContent {
            JsonForms(data = data.value, schema = JsonSchema(),
                uischema = LabelElement("[open](/lookup?q={name}&lang={locale})", options = mapOf(
                    "markup" to JsonPrimitive("markdown"),
                    "interpolate" to JsonPrimitive(true),
                    "textParams" to JsonObject(mapOf("name" to JsonPrimitive("{data.name}"))),
                )),
                renderers = renderers,
                config = mapOf("jsonformsExtended" to mapOf("dynamicValues" to mapOf("enabled" to true))),
                i18n = JsonFormsI18nState(locale = locale.value),
                onChange = { writes++ })
        }
        fun target(): String {
            val text = onNodeWithText("open", substring = true).fetchSemanticsNode()
                .config[SemanticsProperties.Text].single()
            return assertIs<LinkAnnotation.Url>(text.getLinkAnnotations(0, text.length).single().item).url
        }
        assertEquals("/lookup?q=A%26B&lang=en", target())
        runOnIdle {
            data.value = Json.parseToJsonElement("""{"name":"София (център)"}""")
            locale.value = "bg-BG"
        }
        assertEquals("/lookup?q=%D0%A1%D0%BE%D1%84%D0%B8%D1%8F%20%28%D1%86%D0%B5%D0%BD%D1%82%D1%8A%D1%80%29&lang=bg-BG", target())
        assertEquals(0, writes)
    }

    private fun referenceMarkdownUrl(
        renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
    ) = runComposeUiTest {
        val data = mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"name":"A&B"}"""))
        val locale = mutableStateOf("en")
        var writes = 0
        setContent {
            JsonForms(data = data.value, schema = JsonSchema(),
                uischema = LabelElement("[open][ref]\n\n[ref]: /lookup?q={name}&lang={locale}", options = mapOf(
                    "markup" to JsonPrimitive("markdown"),
                    "interpolate" to JsonPrimitive(true),
                    "textParams" to JsonObject(mapOf("name" to JsonPrimitive("{data.name}"))),
                )),
                renderers = renderers,
                config = mapOf("jsonformsExtended" to mapOf("dynamicValues" to mapOf("enabled" to true))),
                i18n = JsonFormsI18nState(locale = locale.value),
                onChange = { writes++ })
        }
        fun target(): String {
            val text = onNodeWithText("open", substring = true).fetchSemanticsNode()
                .config[SemanticsProperties.Text].single()
            return assertIs<LinkAnnotation.Url>(text.getLinkAnnotations(0, text.length).single().item).url
        }
        assertEquals("/lookup?q=A%26B&lang=en", target())
        runOnIdle {
            data.value = Json.parseToJsonElement("""{"name":"София (център)"}""")
            locale.value = "bg-BG"
        }
        assertEquals("/lookup?q=%D0%A1%D0%BE%D1%84%D0%B8%D1%8F%20%28%D1%86%D0%B5%D0%BD%D1%82%D1%8A%D1%80%29&lang=bg-BG", target())
        assertEquals(0, writes)
    }

    private fun collectionPredicate(
        renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
    ) = runComposeUiTest {
        val data = mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"items":[1,2,3],"label":"Alpha","seatsText":"3.9","enabledText":"TRUE"}"""))
        setContent {
            JsonForms(
                data = data.value,
                schema = JsonSchema(),
                uischema = LabelElement("All positive: {all}; any zero: {zero}; above one: {count}; method count: {methodCount}; name starts A: {nameStarts}; converted seats: {convertedSeats}; seats text: {seatsText}; decimal text: {decimalText}; enabled: {enabled}; raw path: {rawPath}; triple: {triple}; first above one: {firstFiltered}; concatenated second: {concatenated}; doubled contains four: {mapped}; literal map matches: {literalMap}; logical: {logical}; end", options = mapOf(
                    "interpolate" to JsonPrimitive(true),
                    "textParams" to JsonObject(mapOf(
                        "all" to JsonPrimitive("{data.items.all(i, i > 0)}"),
                        "zero" to JsonPrimitive("{data.items.exists(i, i == 0)}"),
                        "count" to JsonPrimitive("{size(data.items.filter(i, i > 1))}"),
                        "methodCount" to JsonPrimitive("{data.items.filter(i, i > 1).size()}"),
                        "nameStarts" to JsonPrimitive("{data.label.startsWith(\"A\")}"),
                        "convertedSeats" to JsonPrimitive("{int(double(data.seatsText))}"),
                        "seatsText" to JsonPrimitive("{string(dyn(int(double(data.seatsText))))}"),
                        "decimalText" to JsonPrimitive("{string(double(data.seatsText))}"),
                        "enabled" to JsonPrimitive("{bool(data.enabledText)}"),
                        "rawPath" to JsonPrimitive("""{r'path\to\file'}"""),
                        "triple" to JsonPrimitive("{'''a, [b]'''.size()}"),
                        "firstFiltered" to JsonPrimitive("{data.items.filter(i, i > 1)[0]}"),
                        "concatenated" to JsonPrimitive("{(data.items + [4])[1]}"),
                        "mapped" to JsonPrimitive("{data.items.map(i, i * 2).exists(v, v == 4)}"),
                        "literalMap" to JsonPrimitive("{ {\"first\": data.items[0]} == {\"first\": 1} }"),
                        "logical" to JsonPrimitive("{1 / 0 > 0 || data.items[0] > 0}"),
                    )),
                )),
                renderers = renderers,
                config = mapOf("jsonformsExtended" to mapOf("dynamicValues" to mapOf("enabled" to true))),
            )
        }
        onNodeWithText("All positive: true; any zero: false; above one: 2; method count: 2; name starts A: true; converted seats: 3; seats text: 3; decimal text: 3.9; enabled: true; raw path: path\\to\\file; triple: 6; first above one: 2; concatenated second: 2; doubled contains four: true; literal map matches: true; logical: true; end").assertExists()
        runOnIdle { data.value = Json.parseToJsonElement("""{"items":[0,0,3],"label":"Beta","seatsText":"0.9","enabledText":"FALSE"}""") }
        onNodeWithText("All positive: false; any zero: true; above one: 1; method count: 1; name starts A: false; converted seats: 0; seats text: 0; decimal text: 0.9; enabled: false; raw path: path\\to\\file; triple: 6; first above one: 3; concatenated second: 0; doubled contains four: false; literal map matches: false; logical: ; end").assertExists()
    }

    private fun translationFallback(
        renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
    ) = runComposeUiTest {
        val locale = mutableStateOf("en")
        val calls = mutableListOf<Pair<String, String?>>()
        setContent {
            JsonForms(
                data = JsonObject(emptyMap()),
                schema = JsonSchema(),
                uischema = LabelElement("Plan {known}; missing {missing}.", options = mapOf(
                    "interpolate" to JsonPrimitive(true),
                    "textParams" to JsonObject(mapOf(
                        "known" to JsonPrimitive("{translate(\"plan.Team\", \"Generic\")}"),
                        "missing" to JsonPrimitive("{translate(\"plan.Unknown\", \"Custom\")}"),
                    )),
                )),
                renderers = renderers,
                i18n = JsonFormsI18nState(locale = locale.value, translate = { key, fallback, _ ->
                    if (key.startsWith("plan.")) calls += key to fallback
                    if (key == "plan.Team") if (locale.value == "bg") "Екип" else "Team" else fallback
                }),
            )
        }
        onNodeWithText("Plan Team; missing Custom.").assertExists()
        waitForIdle()
        assertTrue("plan.Team" to "Generic" in calls)
        assertTrue("plan.Unknown" to "Custom" in calls)
        runOnIdle { locale.value = "bg" }
        onNodeWithText("Plan Екип; missing Custom.").assertExists()
    }

    private fun fixture(renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>) = runComposeUiTest {
        val fixture = getBuiltInExamples().first { it.name == "spec-label-interpolation" }
        val categories = assertIs<CategorizationElement>(fixture.uischema).elements.filterIsInstance<CategoryElement>()
        val interpolation = categories.first { it.label == "Interpolation" }.elements.filterIsInstance<LabelElement>()
        val combined = categories.first { it.label == "Both together" }.elements.filterIsInstance<LabelElement>()
        val localized = categories.first { it.label == "Locale and translation" }.elements.filterIsInstance<LabelElement>()
        assertEquals("product", interpolation.first().i18n)
        assertEquals(JsonPrimitive(true), interpolation.first().options["interpolate"])
        assertEquals(JsonPrimitive("Atlas"),
            (interpolation.first().options["textParams"] as JsonObject)["product"])
        assertEquals(JsonPrimitive("markdown"), combined.first().options["markup"])
        assertEquals(JsonPrimitive(true), combined.first().options["interpolate"])
        assertEquals("planName", localized.first().i18n)
        val data = mutableStateOf<JsonElement>(fixture.data)
        val locale = mutableStateOf("en")
        val diagnostics = mutableListOf<UiDiagnostic>()
        val reporter = UiDiagnosticReporter(diagnostics::add)
        setContent {
            JsonForms(
                data = data.value,
                schema = fixture.schema!!,
                uischema = VerticalLayout(elements = interpolation + combined + localized),
                renderers = renderers,
                config = fixture.config + (UiDiagnosticReporterConfigKey to reporter),
                i18n = JsonFormsI18nState(locale = locale.value, translate = { key, fallback, _ ->
                    fixture.localizedTranslations[locale.value]?.get(key) ?: fallback
                }),
            )
        }
        onNodeWithText("You are subscribed to Atlas.").assertExists()
        onNodeWithText("Your plan includes 3 seats.").assertExists()
        onNodeWithText("Promotional code:", substring = true).assertExists()
        onNodeWithText("Write {seats} to show a placeholder", substring = true).assertExists()
        onNodeWithText("Account holder: Ana **Petrova**").assertExists()
        onNodeWithText("Northwind Books is on the Team plan, and the next invoice is €148.50.").assertExists()
        onNodeWithText("Your plan is Team.").assertExists()
        onNodeWithText("It renews on Oct 1, 2026.").assertExists()
        onNodeWithText("Seat price €49.50, or 49.50 before tax.").assertExists()
        onNodeWithText("Reference 2026 is not a quantity", substring = true).assertExists()
        waitForIdle()
        assertEquals(emptyList(), diagnostics)

        runOnIdle { data.value = JsonObject((data.value as JsonObject) + ("seats" to JsonPrimitive(1))) }
        onNodeWithText("Your plan includes 1 seat.").assertExists()
        runOnIdle { locale.value = "bg" }
        onNodeWithText("Абонирани сте за Atlas.").assertExists()
        onNodeWithText("Планът ви включва 1 място.").assertExists()
        onNodeWithText("Вашият план е Екип.").assertExists()
        onNodeWithText("следващата фактура е 148,50", substring = true).assertExists()
        onNodeWithText("Дата на подновяване: 1.10.2026", substring = true).assertExists()
        onNodeWithText("Цена на място 49,50", substring = true).assertExists()
        waitForIdle()
        assertEquals(emptyList(), diagnostics)
    }

    @Test fun materialUnknownExpressionShowsDiagnosticAndKeepsText() = authorError(materialExtendedRenderers)
    @Test fun cupertinoUnknownExpressionShowsDiagnosticAndKeepsText() = authorError(cupertinoExtendedRenderers)

    private fun authorError(renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>) = runComposeUiTest {
        val diagnostics = mutableListOf<UiDiagnostic>()
        val reporter = UiDiagnosticReporter(diagnostics::add)
        setContent {
            JsonForms(
                data = JsonObject(mapOf("name" to JsonPrimitive("Ada"))),
                schema = com.chobantonov.jsonfroms.core.models.JsonSchema(),
                uischema = LabelElement("Hello {data.name}, {person}!", options = mapOf(
                    "interpolate" to JsonPrimitive(true),
                    "textParams" to JsonObject(mapOf("person" to JsonPrimitive("Ada"))),
                )),
                renderers = renderers,
                config = mapOf(
                    "jsonformsExtended" to mapOf("dynamicValues" to mapOf("enabled" to true)),
                    UiDiagnosticReporterConfigKey to reporter,
                ),
            )
        }
        onNodeWithText("Hello , Ada!").assertExists()
        onNodeWithText("Some label values could not be resolved").assertExists()
        waitForIdle()
        assertEquals(listOf("markup.interpolationFailed"), diagnostics.map { it.code })
    }

    @Test fun materialEscapedDynamicPathReportsWarningAndKeepsLiteral() = escapedDynamicPath(materialExtendedRenderers)
    @Test fun cupertinoEscapedDynamicPathReportsWarningAndKeepsLiteral() = escapedDynamicPath(cupertinoExtendedRenderers)

    private fun escapedDynamicPath(renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>) = runComposeUiTest {
        val diagnostics = mutableListOf<UiDiagnostic>()
        setContent {
            JsonForms(
                data = JsonObject(mapOf("name" to JsonPrimitive("Ada"))),
                schema = JsonSchema(),
                uischema = LabelElement("Literal {{data.name}}", options = mapOf(
                    "interpolate" to JsonPrimitive(true),
                )),
                renderers = renderers,
                config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter(diagnostics::add)),
            )
        }
        onNodeWithText("Literal {data.name}").assertExists()
        onNodeWithText("Double braces show expressions literally").assertDoesNotExist()
        waitForIdle()
        assertEquals(listOf("markup.escapedExpression"), diagnostics.map { it.code })
        assertEquals(UiDiagnosticSeverity.Warning, diagnostics.single().severity)
    }

    @Test fun materialUnicodeStringSizeUpdatesWithData() = unicodeStringSize(materialExtendedRenderers)
    @Test fun cupertinoUnicodeStringSizeUpdatesWithData() = unicodeStringSize(cupertinoExtendedRenderers)

    private fun unicodeStringSize(renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>) = runComposeUiTest {
        val data = mutableStateOf<JsonElement>(JsonObject(mapOf("name" to JsonPrimitive("😀"))))
        setContent {
            JsonForms(
                data = data.value,
                schema = JsonSchema(),
                uischema = LabelElement("Characters: {count}", options = mapOf(
                    "interpolate" to JsonPrimitive(true),
                    "textParams" to JsonObject(mapOf("count" to JsonPrimitive("{size(data.name)}"))),
                )),
                renderers = renderers,
                config = mapOf("jsonformsExtended" to mapOf("dynamicValues" to mapOf("enabled" to true))),
            )
        }
        onNodeWithText("Characters: 1").assertExists()
        runOnIdle { data.value = JsonObject(mapOf("name" to JsonPrimitive("e\u0301"))) }
        onNodeWithText("Characters: 2").assertExists()
    }

    @Test fun materialUnicodeStringOrderingUpdatesWithData() = unicodeStringOrdering(materialExtendedRenderers)
    @Test fun cupertinoUnicodeStringOrderingUpdatesWithData() = unicodeStringOrdering(cupertinoExtendedRenderers)

    private fun unicodeStringOrdering(renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>) = runComposeUiTest {
        val data = mutableStateOf<JsonElement>(JsonObject(mapOf(
            "first" to JsonPrimitive("\uE000"), "second" to JsonPrimitive("😀"),
        )))
        setContent {
            JsonForms(
                data = data.value,
                schema = JsonSchema(),
                uischema = LabelElement("Order: {result}", options = mapOf(
                    "interpolate" to JsonPrimitive(true),
                    "textParams" to JsonObject(mapOf(
                        "result" to JsonPrimitive("{data.first < data.second ? 'before' : 'after'}"),
                    )),
                )),
                renderers = renderers,
                config = mapOf("jsonformsExtended" to mapOf("dynamicValues" to mapOf("enabled" to true))),
            )
        }
        onNodeWithText("Order: before").assertExists()
        runOnIdle { data.value = JsonObject(mapOf(
            "first" to JsonPrimitive("😀"), "second" to JsonPrimitive("\uE000"),
        )) }
        onNodeWithText("Order: after").assertExists()
    }

    @Test fun materialLargeMissingIndexStaysQuietUntilDataSelectsRow() = largeMissingIndex(materialExtendedRenderers)
    @Test fun cupertinoLargeMissingIndexStaysQuietUntilDataSelectsRow() = largeMissingIndex(cupertinoExtendedRenderers)

    private fun largeMissingIndex(renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>) = runComposeUiTest {
        val data = mutableStateOf<JsonElement>(kotlinx.serialization.json.Json.parseToJsonElement(
            """{"items":[{"name":"Ada"}],"selected":2147483648}"""))
        val diagnostics = mutableListOf<UiDiagnostic>()
        setContent {
            JsonForms(
                data = data.value,
                schema = JsonSchema(),
                uischema = LabelElement("Row: {name}", options = mapOf(
                    "interpolate" to JsonPrimitive(true),
                    "textParams" to JsonObject(mapOf("name" to JsonPrimitive("{data.items[data.selected].name}"))),
                )),
                renderers = renderers,
                config = mapOf(
                    "jsonformsExtended" to mapOf("dynamicValues" to mapOf("enabled" to true)),
                    UiDiagnosticReporterConfigKey to UiDiagnosticReporter(diagnostics::add),
                ),
            )
        }
        onNodeWithText("Row: ").assertExists()
        waitForIdle()
        assertEquals(emptyList(), diagnostics)
        runOnIdle { data.value = kotlinx.serialization.json.Json.parseToJsonElement(
            """{"items":[{"name":"Ada"}],"selected":0}""") }
        onNodeWithText("Row: Ada").assertExists()
        waitForIdle()
        assertEquals(emptyList(), diagnostics)
    }

    @Test fun materialHasMacroKeepsPresentNullDistinctFromMissing() = hasPresentNull(materialExtendedRenderers)
    @Test fun cupertinoHasMacroKeepsPresentNullDistinctFromMissing() = hasPresentNull(cupertinoExtendedRenderers)

    private fun hasPresentNull(renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>) = runComposeUiTest {
        val data = mutableStateOf<JsonElement>(kotlinx.serialization.json.Json.parseToJsonElement(
            """{"outer":{"value":null}}"""))
        val diagnostics = mutableListOf<UiDiagnostic>()
        setContent {
            JsonForms(
                data = data.value,
                schema = JsonSchema(),
                uischema = LabelElement("Value is {status}", options = mapOf(
                    "interpolate" to JsonPrimitive(true),
                    "textParams" to JsonObject(mapOf(
                        "status" to JsonPrimitive("{has(data.outer.value) ? 'present' : 'missing'}"),
                    )),
                )),
                renderers = renderers,
                config = mapOf(
                    "jsonformsExtended" to mapOf("dynamicValues" to mapOf("enabled" to true)),
                    UiDiagnosticReporterConfigKey to UiDiagnosticReporter(diagnostics::add),
                ),
            )
        }
        onNodeWithText("Value is present").assertExists()
        runOnIdle { data.value = kotlinx.serialization.json.Json.parseToJsonElement("""{"outer":{}}""") }
        onNodeWithText("Value is missing").assertExists()
        waitForIdle()
        assertEquals(emptyList(), diagnostics)
    }

    @Test fun materialTranslatedTextCannotReadDataPath() = translatedTextScope(materialExtendedRenderers)
    @Test fun cupertinoTranslatedTextCannotReadDataPath() = translatedTextScope(cupertinoExtendedRenderers)

    private fun translatedTextScope(renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>) = runComposeUiTest {
        val locale = mutableStateOf("en")
        val diagnostics = mutableListOf<UiDiagnostic>()
        setContent {
            JsonForms(
                data = JsonObject(mapOf("name" to JsonPrimitive("Ada"))),
                schema = JsonSchema(),
                uischema = LabelElement("Hello {person}!", i18n = "greeting", options = mapOf(
                    "interpolate" to JsonPrimitive(true),
                    "textParams" to JsonObject(mapOf("person" to JsonPrimitive("{data.name}"))),
                )),
                renderers = renderers,
                config = mapOf(
                    "jsonformsExtended" to mapOf("dynamicValues" to mapOf("enabled" to true)),
                    UiDiagnosticReporterConfigKey to UiDiagnosticReporter(diagnostics::add),
                ),
                i18n = JsonFormsI18nState(locale = locale.value, translate = { key, fallback, _ ->
                    if (key == "greeting.text" && locale.value == "bg")
                        "Здравей {data.name}, {person}!" else fallback
                }),
            )
        }
        onNodeWithText("Hello Ada!").assertExists()
        waitForIdle()
        assertEquals(emptyList(), diagnostics)

        runOnIdle { locale.value = "bg" }
        onNodeWithText("Здравей , Ada!").assertExists()
        onNodeWithText("Някои стойности на етикета не могат да бъдат определени").assertExists()
        waitForIdle()
        assertEquals(listOf("markup.interpolationFailed"), diagnostics.map { it.code })
    }

    @Test fun materialHostNamespacesUpdateWithoutDataEdits() = hostNamespaces(materialExtendedRenderers)
    @Test fun cupertinoHostNamespacesUpdateWithoutDataEdits() = hostNamespaces(cupertinoExtendedRenderers)

    private fun hostNamespaces(renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>) = runComposeUiTest {
        val config = mutableStateOf(mapOf<String, Any?>(
            "brand" to "Atlas",
            "jsonformsExtended" to mapOf("dynamicValues" to mapOf("enabled" to true)),
        ))
        val context = mutableStateOf<JsonElement>(JsonObject(mapOf(
            "external" to JsonObject(mapOf("person" to JsonPrimitive("Ada"))),
        )))
        val diagnostics = mutableListOf<UiDiagnostic>()
        val reporter = UiDiagnosticReporter(diagnostics::add)
        val label = LabelElement("{brand}: {person}", options = mapOf(
            "interpolate" to JsonPrimitive(true),
            "textParams" to JsonObject(mapOf(
                "brand" to JsonPrimitive("{config.brand}"),
                "person" to JsonPrimitive("{context.external.person}"),
            )),
        ))
        setContent {
            JsonForms(
                data = JsonObject(emptyMap()),
                schema = com.chobantonov.jsonfroms.core.models.JsonSchema(),
                uischema = label,
                renderers = renderers,
                config = config.value + (UiDiagnosticReporterConfigKey to reporter),
                context = context.value,
            )
        }
        onNodeWithText("Atlas: Ada").assertExists()
        runOnIdle {
            config.value = config.value + ("brand" to "Orbit")
            context.value = JsonObject(mapOf("external" to JsonObject(mapOf("person" to JsonPrimitive("Bea")))))
        }
        onNodeWithText("Orbit: Bea").assertExists()
        waitForIdle()
        assertEquals(emptyList(), diagnostics)
    }

    @Test fun materialSelectedListItemIsAvailableToParameters() = selectedItem(materialExtendedRenderers)
    @Test fun cupertinoSelectedListItemIsAvailableToParameters() = selectedItem(cupertinoExtendedRenderers)

    private fun selectedItem(renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>) = runComposeUiTest {
        val schema = com.chobantonov.jsonfroms.core.models.JsonSchema(
            kotlinx.serialization.json.Json.parseToJsonElement("""{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"}}}}}}""") as JsonObject,
        )
        val detail = LabelElement("Current {name}", options = mapOf(
            "interpolate" to JsonPrimitive(true),
            "textParams" to JsonObject(mapOf("name" to JsonPrimitive("{item.name}"))),
        ))
        val diagnostics = mutableListOf<UiDiagnostic>()
        setContent {
            JsonForms(
                data = kotlinx.serialization.json.Json.parseToJsonElement("""{"rows":[{"name":"Ada"},{"name":"Bea"}]}"""),
                schema = schema,
                uischema = ListWithDetailElement("#/properties/rows", options = mapOf(
                    "detail" to JsonPrimitive("REGISTERED"),
                    "elementLabelProp" to JsonPrimitive("name"),
                )),
                uischemas = listOf(JsonFormsUISchemaRegistryEntry(tester = { _, _, _ -> 10 }, uischema = detail)),
                renderers = renderers,
                config = mapOf(
                    "jsonformsExtended" to mapOf("dynamicValues" to mapOf("enabled" to true)),
                    UiDiagnosticReporterConfigKey to UiDiagnosticReporter(diagnostics::add),
                ),
            )
        }
        onNodeWithText("Current Ada").assertDoesNotExist()
        onNodeWithText("Ada").performClick()
        onNodeWithText("Current Ada").assertExists()
        onNodeWithText("Bea").performClick()
        onNodeWithText("Current Bea").assertExists()
        waitForIdle()
        assertEquals(emptyList(), diagnostics)
    }

    @Test fun materialTableDialogUsesLiveDraftItem() = tableDraftItem(materialExtendedRenderers)
    @Test fun cupertinoTableDialogUsesLiveDraftItem() = tableDraftItem(cupertinoExtendedRenderers)

    private fun tableDraftItem(renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>) = runComposeUiTest {
        val schema = JsonSchema(kotlinx.serialization.json.Json.parseToJsonElement(
            """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"}}}}}}"""
        ).jsonObject)
        val options = mapOf(
            "table" to JsonPrimitive(true),
            "columnDefs" to kotlinx.serialization.json.Json.parseToJsonElement("""[{"field":"name"}]"""),
            "rowDetail" to kotlinx.serialization.json.Json.parseToJsonElement("""{
              "presentation":"dialog",
              "detail":{"type":"VerticalLayout","elements":[
                {"type":"Label","text":"Draft {who}","options":{"interpolate":true,
                  "textParams":{"who":"{item.name}"}}},
                {"type":"Control","scope":"#/properties/name"}
              ]}
            }"""),
        )
        val original = kotlinx.serialization.json.Json.parseToJsonElement("""{"rows":[{"name":"Ada"}]}""")
        val data = mutableStateOf<JsonElement>(original)
        setContent {
            JsonForms(
                data = data.value, schema = schema,
                uischema = ControlElement("#/properties/rows", options = options),
                renderers = renderers,
                config = mapOf("jsonformsExtended" to mapOf("dynamicValues" to mapOf("enabled" to true))),
                onChange = { data.value = it.data },
            )
        }
        onNodeWithText("Edit details").performClick()
        onNodeWithText("Draft Ada").assertExists()
        onAllNodes(hasSetTextAction() and hasText("Ada")).onLast().performTextReplacement("Bea")
        onNodeWithText("Draft Bea").assertExists()
        assertEquals(original, data.value)
        onNodeWithText("Cancel").performClick()
        onNodeWithText("Draft Bea").assertDoesNotExist()
        assertEquals(original, data.value)
    }

    @Test fun materialTupleDialogUsesLiveDraftItem() = tupleDraftItem(materialExtendedRenderers)
    @Test fun cupertinoTupleDialogUsesLiveDraftItem() = tupleDraftItem(cupertinoExtendedRenderers)

    private fun tupleDraftItem(renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>) = runComposeUiTest {
        val schema = JsonSchema(kotlinx.serialization.json.Json.parseToJsonElement(
            """{"type":"array","items":[{"type":"object","title":"Address","properties":{"name":{"type":"string"}}}],"additionalItems":false}"""
        ).jsonObject)
        val detail = kotlinx.serialization.json.Json.parseToJsonElement("""{
          "type":"VerticalLayout","elements":[
            {"type":"Label","text":"Tuple {who}","options":{"interpolate":true,
              "textParams":{"who":"{item.name}"}}},
            {"type":"Control","scope":"#/properties/name"}
          ]}
        """)
        val registry = listOf(JsonFormsUISchemaRegistryEntry(
            tester = { _, schemaPath, _ -> if (schemaPath == "#/items/0") 10 else -1 },
            uischema = ControlElement(scope = "#", options = mapOf("detail" to detail)),
        ))
        val original = kotlinx.serialization.json.Json.parseToJsonElement("""[{"name":"Ada"}]""")
        val data = mutableStateOf<JsonElement>(original)
        setContent {
            JsonForms(
                data = data.value, schema = schema, uischema = ControlElement(scope = "#"),
                renderers = renderers, uischemas = registry,
                config = mapOf("jsonformsExtended" to mapOf("dynamicValues" to mapOf("enabled" to true))),
                onChange = { data.value = it.data },
            )
        }
        onNodeWithContentDescription("Edit Address").performClick()
        onNodeWithText("Tuple Ada").assertExists()
        onAllNodes(hasSetTextAction() and hasText("Ada")).onLast().performTextReplacement("Bea")
        onNodeWithText("Tuple Bea").assertExists()
        assertEquals(original, data.value)
        onNodeWithText("Cancel").performClick()
        assertEquals(original, data.value)
    }

    @Test fun materialCellDialogUsesRowDraftItem() = cellDraftItem(materialExtendedRenderers)
    @Test fun cupertinoCellDialogUsesRowDraftItem() = cellDraftItem(cupertinoExtendedRenderers)

    private fun cellDraftItem(renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>) = runComposeUiTest {
        val schema = JsonSchema(kotlinx.serialization.json.Json.parseToJsonElement(
            """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"},"contact":{"type":"object","properties":{"city":{"type":"string"}}}}}}}}"""
        ).jsonObject)
        val options = mapOf(
            "table" to JsonPrimitive(true),
            "columnDefs" to kotlinx.serialization.json.Json.parseToJsonElement("""[{"field":"name"},{"field":"contact"}]"""),
            "cells" to kotlinx.serialization.json.Json.parseToJsonElement("""{
              "contact":{"detail":{"type":"VerticalLayout","elements":[
                {"type":"Label","text":"{who} in {where}","options":{"interpolate":true,
                  "textParams":{"who":"{item.name}","where":"{item.contact.city}"}}},
                {"type":"Control","scope":"#/properties/city"}
              ]}}
            }"""),
        )
        val original = kotlinx.serialization.json.Json.parseToJsonElement("""{"rows":[{"name":"Ada","contact":{"city":"Boston"}}]}""")
        val data = mutableStateOf<JsonElement>(original)
        setContent {
            JsonForms(
                data = data.value, schema = schema,
                uischema = ControlElement("#/properties/rows", options = options),
                renderers = renderers,
                config = mapOf("jsonformsExtended" to mapOf("dynamicValues" to mapOf("enabled" to true))),
                onChange = { data.value = it.data },
            )
        }
        onNodeWithContentDescription("Edit Contact").performClick()
        onNodeWithText("Ada in Boston").assertExists()
        onAllNodes(hasSetTextAction() and hasText("Boston")).onLast().performTextReplacement("Paris")
        onNodeWithText("Ada in Paris").assertExists()
        assertEquals(original, data.value)
        onNodeWithText("Cancel").performClick()
        assertEquals(original, data.value)
    }

    @Test fun materialImportedJavaScriptKeysAndAuthorErrors() = importedErrors(materialExtendedRenderers)
    @Test fun cupertinoImportedJavaScriptKeysAndAuthorErrors() = importedErrors(cupertinoExtendedRenderers)

    private fun importedErrors(renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>) = runComposeUiTest {
        val fixture = getBuiltInExamples().first { it.name == "spec-label-interpolation" }
        val categories = assertIs<CategorizationElement>(fixture.uischema).elements.filterIsInstance<CategoryElement>()
        val names = categories.first { it.label == "JavaScript names" }.elements.filterIsInstance<LabelElement>()
        val errors = categories.first { it.label == "Incorrect usage" }.elements.filterIsInstance<LabelElement>()
        val markdown = categories.first { it.label == "Markdown" }.elements.filterIsInstance<LabelElement>()
        val off = categories.first { it.label == "Interpolation off" }.elements.filterIsInstance<LabelElement>()
        val data = mutableStateOf<JsonElement>(fixture.data)
        val locale = mutableStateOf("en")
        val diagnostics = mutableListOf<UiDiagnostic>()
        val reporter = UiDiagnosticReporter(diagnostics::add)
        setContent {
            JsonForms(
                data = data.value,
                schema = fixture.schema!!,
                uischema = VerticalLayout(elements = names + errors + markdown + off),
                renderers = renderers,
                config = fixture.config + (UiDiagnosticReporterConfigKey to reporter),
                i18n = JsonFormsI18nState(locale = locale.value, translate = { key, fallback, _ ->
                    fixture.localizedTranslations[locale.value]?.get(key) ?: fallback
                }),
            )
        }
        onNodeWithText("A property called constructor: \"a perfectly ordinary value\".").assertExists()
        onNodeWithText("Its siblings are still readable: \"Northwind Books\".").assertExists()
        onNodeWithText("And a property called toString: \"also ordinary\".").assertExists()
        onNodeWithText("A data path in the text: \"\".").assertExists()
        onNodeWithText("A parameter that was never declared: \"\".").assertExists()
        onNodeWithText("A function that does not exist: \"\".").assertExists()
        onNodeWithText("A namespace that does not exist: \"\".").assertExists()
        onNodeWithText("A parameter named after a reserved name: \"en\".").assertExists()
        onNodeWithText("first working day", substring = true).assertExists()
        onNodeWithText("**first working day**", substring = true).assertDoesNotExist()
        onNodeWithText("Without interpolate, {product} and {data.customerName} are just characters.").assertExists()
        onNodeWithText("Declared parameters are inert too: {product}.").assertExists()
        onAllNodesWithText("Some label values could not be resolved").assertCountEquals(5)
        waitForIdle()
        assertEquals(5, diagnostics.count { it.code == "markup.interpolationFailed" })

        runOnIdle {
            data.value = JsonObject((data.value as JsonObject) +
                ("constructor" to JsonPrimitive("updated")) + ("toString" to JsonPrimitive("changed")))
            locale.value = "bg"
        }
        onNodeWithText("Без interpolate {product} и {data.customerName} са просто знаци.").assertExists()
        onNodeWithText("Декларираните параметри също не се използват: {product}.").assertExists()
        onNodeWithText("Свойство с име constructor: „updated“.").assertExists()
        onNodeWithText("Съседните свойства се четат нормално: „Northwind Books“.").assertExists()
        onNodeWithText("И свойство с име toString: „changed“.").assertExists()
        onNodeWithText("Параметър със запазено име: „bg“.").assertExists()
        onAllNodesWithText("Някои стойности на етикета не могат да бъдат определени").assertCountEquals(5)
    }
}
