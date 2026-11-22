package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.compose.JsonFormsChange
import com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry
import com.chobantonov.jsonfroms.core.store.JsonFormsUISchemaRegistryEntry
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.LabelElement
import com.chobantonov.jsonfroms.core.models.UnknownUiSchemaElement
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class TemplateSlotUiTest {
    @Test fun materialTemplateLookupIsCaseSensitive() = caseSensitiveTemplateLookup(materialExtendedRenderers)
    @Test fun cupertinoTemplateLookupIsCaseSensitive() = caseSensitiveTemplateLookup(cupertinoExtendedRenderers)

    private fun caseSensitiveTemplateLookup(
        renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
    ) = runComposeUiTest {
        var requestedName by mutableStateOf("Card")
        val diagnostics = mutableListOf<TemplateDiagnostic>()
        val changes = mutableListOf<JsonFormsChange>()
        val registry = listOf(
            LabelElement("Unrelated template", name = "other"),
            LabelElement("Exact lowercase template", name = "card"),
        ).map { JsonFormsUISchemaRegistryEntry({ _, _, _ -> -1 }, it) }
        setContent {
            JsonForms(data = JsonNull, schema = JsonSchema(),
                uischema = namedElement("Template", requestedName),
                uischemas = registry,
                renderers = renderers,
                config = mapOf(ExtendedTemplateDiagnosticsConfigKey to ExtendedTemplateDiagnosticReporter(diagnostics::add)),
                onChange = changes::add)
        }
        waitForIdle()
        onAllNodesWithText("Exact lowercase template").assertCountEquals(0)
        onAllNodesWithText("Unrelated template").assertCountEquals(0)
        assertTrue(diagnostics.any { it.code == TemplateDiagnosticCode.MissingTemplate && it.name == "Card" })
        runOnIdle { requestedName = "card" }
        onNodeWithText("Exact lowercase template").assertExists()
        onAllNodesWithText("Unrelated template").assertCountEquals(0)
        assertTrue(changes.isEmpty())
    }

    @Test fun materialNestedTemplatesInheritAndOverrideSlots() = nestedSlotInheritance(materialExtendedRenderers)
    @Test fun cupertinoNestedTemplatesInheritAndOverrideSlots() = nestedSlotInheritance(cupertinoExtendedRenderers)

    private fun nestedSlotInheritance(
        renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
    ) = runComposeUiTest {
        var inherited by mutableStateOf(LabelElement("Inherited heading", name = "heading"))
        val diagnostics = mutableListOf<TemplateDiagnostic>()
        val changes = mutableListOf<JsonFormsChange>()
        val slot = UnknownUiSchemaElement("Slot",
            properties = buildJsonObject { put("name", "heading") },
            elements = listOf(LabelElement("Fallback heading")))
        val outer = VerticalLayout(name = "outer", elements = listOf(
            slot,
            namedElement("Template", "inner"),
            UnknownUiSchemaElement("Template",
                properties = buildJsonObject { put("name", "inner") },
                elements = listOf(LabelElement("Local heading", name = "heading"))),
        ))
        val inner = VerticalLayout(name = "inner", elements = listOf(slot))
        setContent {
            JsonForms(data = JsonNull, schema = JsonSchema(),
                uischema = UnknownUiSchemaElement("Template",
                    properties = buildJsonObject { put("name", "outer") },
                    elements = listOf(inherited)),
                uischemas = listOf(outer, inner).map {
                    JsonFormsUISchemaRegistryEntry({ _, _, _ -> -1 }, it)
                },
                renderers = renderers,
                config = mapOf(ExtendedTemplateDiagnosticsConfigKey to ExtendedTemplateDiagnosticReporter(diagnostics::add)),
                onChange = changes::add)
        }
        onAllNodesWithText("Inherited heading").assertCountEquals(2)
        onNodeWithText("Local heading").assertExists()
        onAllNodesWithText("Fallback heading").assertCountEquals(0)
        runOnIdle { inherited = LabelElement("Updated heading", name = "heading") }
        onAllNodesWithText("Inherited heading").assertCountEquals(0)
        onAllNodesWithText("Updated heading").assertCountEquals(2)
        onNodeWithText("Local heading").assertExists()
        assertTrue(diagnostics.isEmpty())
        assertTrue(changes.isEmpty())
    }

    @Test fun materialNestedTemplateEditKeepsCallerPath() = nestedTemplateEdit(materialExtendedRenderers)
    @Test fun cupertinoNestedTemplateEditKeepsCallerPath() = nestedTemplateEdit(cupertinoExtendedRenderers)

    private fun nestedTemplateEdit(
        renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
    ) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"person":{"type":"object","properties":{"name":{"type":"string"},"locked":{"type":"boolean"}}},"other":{"type":"object","properties":{"name":{"type":"string"},"locked":{"type":"boolean"}}}}}"""
        ).jsonObject)
        val detail = Json.parseToJsonElement(
            """{"type":"Template","name":"card","elements":[{"type":"Control","scope":"#/properties/name","name":"body","rule":{"effect":"DISABLE","condition":{"scope":"#/properties/locked","schema":{"const":true},"failWhenUndefined":true}}}]}"""
        )
        var data by mutableStateOf(Json.parseToJsonElement(
            """{"person":{"name":"Ada","locked":false},"other":{"name":"Keep","locked":true}}"""
        ))
        val changes = mutableListOf<JsonFormsChange>()
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/person", options = mapOf("detail" to detail)),
                uischemas = listOf(JsonFormsUISchemaRegistryEntry({ _, _, _ -> -1 },
                    VerticalLayout(name = "card", elements = listOf(namedElement("Slot", "body"))))),
                renderers = renderers,
                additionalErrors = listOf(
                    ErrorObject(instancePath = "/person/name", message = "person problem"),
                    ErrorObject(instancePath = "/other/name", message = "other problem"),
                ),
                onChange = { data = it.data; changes += it })
        }
        assertTrue(changes.isEmpty())
        onNodeWithText("person problem").assertExists()
        onAllNodesWithText("other problem").assertCountEquals(0)
        onNodeWithText("Ada").assertIsEnabled()
        onAllNodesWithText("Ada").onFirst().performTextReplacement("Grace")
        waitForIdle()
        assertEquals(Json.parseToJsonElement(
            """{"person":{"name":"Grace","locked":false},"other":{"name":"Keep","locked":true}}"""
        ), data)
        assertEquals(1, changes.size)
        runOnIdle { data = Json.parseToJsonElement(
            """{"person":{"name":"Marie","locked":true},"other":{"name":"Keep","locked":false}}"""
        ) }
        onNodeWithText("Marie").assertIsNotEnabled()
        assertEquals(1, changes.size)
    }

    @Test fun materialUnfilledSlotUsesOnlyFirstFallback() = unfilledSlotFallback(materialExtendedRenderers)
    @Test fun cupertinoUnfilledSlotUsesOnlyFirstFallback() = unfilledSlotFallback(cupertinoExtendedRenderers)

    private fun unfilledSlotFallback(
        renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
    ) = runComposeUiTest {
        var fallbacks by mutableStateOf(listOf(
            LabelElement("First fallback"), LabelElement("Second fallback"),
        ))
        val changes = mutableListOf<JsonFormsChange>()
        setContent {
            JsonForms(data = JsonNull, schema = JsonSchema(),
                uischema = UnknownUiSchemaElement("Slot",
                    properties = buildJsonObject { put("name", "empty") },
                    elements = fallbacks),
                renderers = renderers, onChange = changes::add)
        }
        onNodeWithText("First fallback").assertExists()
        onAllNodesWithText("Second fallback").assertCountEquals(0)
        runOnIdle { fallbacks = emptyList() }
        onAllNodesWithText("First fallback").assertCountEquals(0)
        onAllNodesWithText("Second fallback").assertCountEquals(0)
        runOnIdle { fallbacks = listOf(LabelElement("Replacement fallback")) }
        onNodeWithText("Replacement fallback").assertExists()
        assertTrue(changes.isEmpty())
    }

    @Test
    fun materialRendersImportedTemplateOverridesAndFallbacks() = renderFixture(materialExtendedRenderers)

    @Test
    fun cupertinoRendersImportedTemplateOverridesAndFallbacks() = renderFixture(cupertinoExtendedRenderers)

    @Test
    fun editingInsideMaterialTemplateUpdatesCallerData() = editFixture(materialExtendedRenderers)

    @Test
    fun editingInsideCupertinoTemplateUpdatesCallerData() = editFixture(cupertinoExtendedRenderers)

    private fun editFixture(renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>) = runComposeUiTest {
        val fixture = getBuiltInExamples().first { it.name == "spec-template-slots" }
        val changes = mutableListOf<JsonFormsChange>()
        setContent {
            JsonForms(
                data = fixture.data,
                schema = fixture.schema,
                uischema = fixture.uischema,
                uischemas = fixture.uischemas,
                renderers = renderers,
                onChange = changes::add,
            )
        }
        onAllNodesWithText("Ada").onFirst().performTextReplacement("Grace")
        waitForIdle()
        assertTrue(changes.isNotEmpty())
        assertTrue((changes.last().data as JsonObject)["name"] == JsonPrimitive("Grace"))
    }

    @Test
    fun recursiveTemplateIsGuardedAndReported() = runComposeUiTest {
        val diagnostics = mutableListOf<TemplateDiagnostic>()
        val recursive = UnknownUiSchemaElement("Template", properties = buildJsonObject { put("name", "loop") })
        setContent {
            JsonForms(
                data = JsonNull,
                schema = JsonSchema(),
                uischema = recursive,
                uischemas = listOf(JsonFormsUISchemaRegistryEntry({ _, _, _ -> -1 }, recursive)),
                renderers = materialExtendedRenderers,
                config = mapOf(ExtendedTemplateDiagnosticsConfigKey to ExtendedTemplateDiagnosticReporter(diagnostics::add)),
            )
        }
        waitForIdle()
        assertTrue(diagnostics.any { it.code == TemplateDiagnosticCode.RecursiveTemplate && it.name == "loop" })
    }

    @Test
    fun materialReportsMissingAndDuplicateTemplates() = missingAndDuplicateTemplates(materialExtendedRenderers)

    @Test
    fun cupertinoReportsMissingAndDuplicateTemplates() = missingAndDuplicateTemplates(cupertinoExtendedRenderers)

    private fun missingAndDuplicateTemplates(
        renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
    ) = runComposeUiTest {
        val diagnostics = mutableListOf<TemplateDiagnostic>()
        val first = LabelElement("First matching template", name = "shared")
        val second = LabelElement("Later matching template", name = "shared")
        val callers = VerticalLayout(elements = listOf(
            UnknownUiSchemaElement("Template"),
            namedElement("Template", "absent"),
            namedElement("Template", "shared"),
        ))
        val data = buildJsonObject { put("value", "preserved") }
        val changes = mutableListOf<JsonFormsChange>()
        setContent {
            JsonForms(
                data = data,
                schema = JsonSchema(),
                uischema = callers,
                uischemas = listOf(first, second).map {
                    JsonFormsUISchemaRegistryEntry({ _, _, _ -> -1 }, it)
                },
                renderers = renderers,
                config = mapOf(ExtendedTemplateDiagnosticsConfigKey to ExtendedTemplateDiagnosticReporter(diagnostics::add)),
                onChange = changes::add,
            )
        }
        waitForIdle()
        onAllNodesWithText("First matching template").assertCountEquals(0)
        onAllNodesWithText("Later matching template").assertCountEquals(0)
        assertEquals(
            setOf(
                TemplateDiagnostic(TemplateDiagnosticCode.MissingName, null, ""),
                TemplateDiagnostic(TemplateDiagnosticCode.MissingTemplate, "absent", ""),
                TemplateDiagnostic(TemplateDiagnosticCode.DuplicateName, "shared", ""),
            ),
            diagnostics.toSet(),
        )
        assertTrue(changes.isEmpty())
    }

    @Test fun materialLaterSuppliedSlotWins() = laterSuppliedSlot(materialExtendedRenderers)
    @Test fun cupertinoLaterSuppliedSlotWins() = laterSuppliedSlot(cupertinoExtendedRenderers)
    @Test fun materialTemplateRegistryReplacementUpdatesAmbiguity() = templateRegistryReplacement(materialExtendedRenderers)
    @Test fun cupertinoTemplateRegistryReplacementUpdatesAmbiguity() = templateRegistryReplacement(cupertinoExtendedRenderers)
    @Test fun materialTemplateNamesIgnoreRankedTesters() = namesIgnoreRankedTesters(materialExtendedRenderers)
    @Test fun cupertinoTemplateNamesIgnoreRankedTesters() = namesIgnoreRankedTesters(cupertinoExtendedRenderers)
    @Test fun materialTemplateAndSlotNamesSurviveLocaleChanges() = namesSurviveLocaleChanges(materialExtendedRenderers)
    @Test fun cupertinoTemplateAndSlotNamesSurviveLocaleChanges() = namesSurviveLocaleChanges(cupertinoExtendedRenderers)

    private fun namesIgnoreRankedTesters(
        renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
    ) = runComposeUiTest {
        var duplicate by mutableStateOf(false)
        var testerCalls = 0
        val diagnostics = mutableListOf<TemplateDiagnostic>()
        val changes = mutableListOf<JsonFormsChange>()
        val unrelated = JsonFormsUISchemaRegistryEntry(
            { _, _, _ -> testerCalls++; 1000 }, LabelElement("Ranked detail", name = "other"))
        val named = JsonFormsUISchemaRegistryEntry(
            { _, _, _ -> testerCalls++; -1 }, LabelElement("Named template", name = "card"))
        val conflicting = JsonFormsUISchemaRegistryEntry(
            { _, _, _ -> testerCalls++; 2000 }, LabelElement("Conflicting template", name = "card"))
        setContent {
            JsonForms(data = JsonNull, schema = JsonSchema(), uischema = namedElement("Template", "card"),
                uischemas = listOf(unrelated, named) + if (duplicate) listOf(conflicting) else emptyList(),
                renderers = renderers,
                config = mapOf(ExtendedTemplateDiagnosticsConfigKey to ExtendedTemplateDiagnosticReporter(diagnostics::add)),
                onChange = changes::add)
        }
        onNodeWithText("Named template").assertExists()
        onAllNodesWithText("Ranked detail").assertCountEquals(0)
        assertEquals(0, testerCalls)
        runOnIdle { duplicate = true }
        onAllNodesWithText("Named template").assertCountEquals(0)
        onAllNodesWithText("Conflicting template").assertCountEquals(0)
        assertTrue(diagnostics.any { it.code == TemplateDiagnosticCode.DuplicateName && it.name == "card" })
        assertEquals(0, testerCalls)
        runOnIdle { duplicate = false }
        onNodeWithText("Named template").assertExists()
        assertEquals(0, testerCalls)
        assertTrue(changes.isEmpty())
    }

    private fun namesSurviveLocaleChanges(
        renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
    ) = runComposeUiTest {
        var locale by mutableStateOf("en")
        val changes = mutableListOf<JsonFormsChange>()
        val caller = UnknownUiSchemaElement("Template",
            properties = buildJsonObject { put("name", "card") },
            elements = listOf(LabelElement("Heading", i18n = "heading", name = "heading")))
        val slot = namedElement("Slot", "heading")
        setContent {
            JsonForms(data = JsonNull, schema = JsonSchema(), uischema = caller,
                uischemas = listOf(JsonFormsUISchemaRegistryEntry({ _, _, _ -> -1 },
                    VerticalLayout(elements = listOf(slot), name = "card"))),
                renderers = renderers,
                i18n = JsonFormsI18nState(locale = locale,
                    translate = { key, fallback, _ ->
                        if (locale == "bg" && key == "heading.text") "Заглавие" else fallback
                    }),
                onChange = changes::add)
        }
        onNodeWithText("Heading").assertExists()
        runOnIdle { locale = "bg" }
        onNodeWithText("Заглавие").assertExists()
        onAllNodesWithText("Heading").assertCountEquals(0)
        runOnIdle { locale = "en" }
        onNodeWithText("Heading").assertExists()
        assertEquals("card", caller.name)
        assertEquals("heading", caller.elements.single().name)
        assertTrue(changes.isEmpty())
    }

    private fun templateRegistryReplacement(
        renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
    ) = runComposeUiTest {
        val first = LabelElement("First template", name = "card")
        val second = LabelElement("Second template", name = "card")
        var templates by mutableStateOf(listOf(first, second))
        val diagnostics = mutableListOf<TemplateDiagnostic>()
        val changes = mutableListOf<JsonFormsChange>()
        setContent {
            JsonForms(data = JsonNull, schema = JsonSchema(), uischema = namedElement("Template", "card"),
                uischemas = templates.map { JsonFormsUISchemaRegistryEntry({ _, _, _ -> -1 }, it) },
                renderers = renderers,
                config = mapOf(ExtendedTemplateDiagnosticsConfigKey to ExtendedTemplateDiagnosticReporter(diagnostics::add)),
                onChange = changes::add)
        }
        waitForIdle()
        onAllNodesWithText("First template").assertCountEquals(0)
        assertEquals(TemplateDiagnosticCode.DuplicateName, diagnostics.last().code)
        runOnIdle { templates = listOf(first) }
        onNodeWithText("First template").assertExists()
        runOnIdle { templates = listOf(first, second) }
        onAllNodesWithText("First template").assertCountEquals(0)
        assertEquals(0, changes.size)
    }

    private fun laterSuppliedSlot(
        renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
    ) = runComposeUiTest {
        val diagnostics = mutableListOf<TemplateDiagnostic>()
        val changes = mutableListOf<JsonFormsChange>()
        val slot = UnknownUiSchemaElement("Slot",
            properties = buildJsonObject { put("name", "heading") },
            elements = listOf(LabelElement("Fallback heading")))
        val first = LabelElement("First supplied heading", name = "heading")
        val unnamed = LabelElement("Unnamed supplied heading")
        val second = LabelElement("Second supplied heading", name = "heading")
        var supplied by mutableStateOf(listOf(first, unnamed, second))
        setContent {
            JsonForms(data = JsonNull, schema = JsonSchema(),
                uischema = UnknownUiSchemaElement("Template",
                    properties = buildJsonObject { put("name", "card") }, elements = supplied),
                uischemas = listOf(JsonFormsUISchemaRegistryEntry({ _, _, _ -> -1 },
                    VerticalLayout(elements = listOf(slot), name = "card"))),
                renderers = renderers,
                config = mapOf(ExtendedTemplateDiagnosticsConfigKey to ExtendedTemplateDiagnosticReporter(diagnostics::add)),
                onChange = changes::add)
        }
        waitForIdle()
        onAllNodesWithText("Fallback heading").assertCountEquals(0)
        onAllNodesWithText("First supplied heading").assertCountEquals(0)
        onAllNodesWithText("Unnamed supplied heading").assertCountEquals(0)
        onNodeWithText("Second supplied heading").assertExists()
        runOnIdle { supplied = listOf(second, unnamed, first) }
        onNodeWithText("First supplied heading").assertExists()
        onAllNodesWithText("Second supplied heading").assertCountEquals(0)
        assertTrue(diagnostics.isEmpty())
        assertTrue(changes.isEmpty())
    }

    @Test
    fun materialGuardsRecursiveSuppliedSlot() = recursiveSuppliedSlot(materialExtendedRenderers)

    @Test
    fun cupertinoGuardsRecursiveSuppliedSlot() = recursiveSuppliedSlot(cupertinoExtendedRenderers)

    private fun recursiveSuppliedSlot(
        renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
    ) = runComposeUiTest {
        val diagnostics = mutableListOf<TemplateDiagnostic>()
        val data = buildJsonObject { put("value", "preserved") }
        val changes = mutableListOf<JsonFormsChange>()
        val slot = namedElement("Slot", "heading")
        setContent {
            JsonForms(
                data = data,
                schema = JsonSchema(),
                uischema = UnknownUiSchemaElement(
                    "Template",
                    properties = buildJsonObject { put("name", "card") },
                    elements = listOf(slot),
                ),
                uischemas = listOf(JsonFormsUISchemaRegistryEntry(
                    { _, _, _ -> -1 },
                    VerticalLayout(elements = listOf(slot), name = "card"),
                )),
                renderers = renderers,
                config = mapOf(ExtendedTemplateDiagnosticsConfigKey to ExtendedTemplateDiagnosticReporter(diagnostics::add)),
                onChange = changes::add,
            )
        }
        waitForIdle()
        assertEquals(
            listOf(TemplateDiagnostic(TemplateDiagnosticCode.RecursiveSlot, "heading", "")),
            diagnostics,
        )
        assertTrue(changes.isEmpty())
    }

    private fun namedElement(type: String, name: String) = UnknownUiSchemaElement(
        type,
        properties = buildJsonObject { put("name", name) },
    )

    @Test
    fun materialReplacesRecursiveTemplateWithSafeContent() = replaceRecursiveTemplate(materialExtendedRenderers)

    @Test
    fun cupertinoReplacesRecursiveTemplateWithSafeContent() = replaceRecursiveTemplate(cupertinoExtendedRenderers)

    private fun replaceRecursiveTemplate(
        renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
    ) = runComposeUiTest {
        val diagnostics = mutableListOf<TemplateDiagnostic>()
        val selected = mutableStateOf("loop")
        val recursive = namedElement("Template", "loop")
        val safe = LabelElement("Safe replacement", name = "safe")
        setContent {
            JsonForms(
                data = JsonNull,
                schema = JsonSchema(),
                uischema = namedElement("Template", selected.value),
                uischemas = listOf(recursive, safe).map {
                    JsonFormsUISchemaRegistryEntry({ _, _, _ -> -1 }, it)
                },
                renderers = renderers,
                config = mapOf(ExtendedTemplateDiagnosticsConfigKey to ExtendedTemplateDiagnosticReporter(diagnostics::add)),
            )
        }
        waitForIdle()
        assertTrue(diagnostics.any { it.code == TemplateDiagnosticCode.RecursiveTemplate })
        runOnIdle { selected.value = "safe" }
        waitForIdle()
        onNodeWithText("Safe replacement").assertExists()
        assertEquals(1, diagnostics.count { it.code == TemplateDiagnosticCode.RecursiveTemplate })
    }

    private fun renderFixture(renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>) = runComposeUiTest {
        val fixture = getBuiltInExamples().first { it.name == "spec-template-slots" }
        setContent {
            JsonForms(
                data = fixture.data,
                schema = fixture.schema,
                uischema = fixture.uischema,
                uischemas = fixture.uischemas,
                renderers = renderers,
            )
        }
        onNodeWithText("Custom heading").assertExists()
        onAllNodesWithText("Default heading").assertCountEquals(1)
        onNodeWithText("Local heading overrides inherited heading").assertExists()
    }
}
