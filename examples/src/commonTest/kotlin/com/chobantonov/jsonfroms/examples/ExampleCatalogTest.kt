package com.chobantonov.jsonfroms.examples

import com.chobantonov.jsonfroms.core.models.Categorization
import com.chobantonov.jsonfroms.core.models.Layout
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ExampleCatalogTest {
    @Test
    fun builtInCatalogContainsUpstreamAndPortableSpecExamples() {
        val examples = getBuiltInExamples()

        assertEquals(79, upstreamExampleNames.size)
        assertEquals(upstreamExampleNames.size + specSnapshotExampleNames.size, examples.size)
        assertEquals(upstreamExampleNames + specSnapshotExampleNames,
            examples.map(ExampleDescription::name).toSet())
        assertEquals(ExampleSource.JsonForms, examples.first { it.name == "array" }.source)
        assertEquals(ExampleSource.Spec, examples.first { it.name == "spec-additional-properties" }.source)
        val nestedValidation = examples.first { it.name == "validation-nested-same-name" }
        assertEquals(ExampleSource.JsonForms, nestedValidation.source)
        assertEquals("Validation - 3x nested properties with same name", nestedValidation.label)
        assertNotNull(nestedValidation.schema)
        assertNotNull(nestedValidation.uischema)
        assertEquals("http://json-schema.org/draft-07/schema#",
            examples.first { it.name == "mixed-object" }.schema?.schemaUri)
        assertEquals(39, specSnapshotExampleNames.size)
        assertTrue(specSnapshotExampleNames.none { it in setOf(
            "spec-ag-grid", "spec-code-editor", "spec-template-layout", "spec-template-slots",
            "spec-job-application", "spec-kitchen-sink", "spec-array-controls",
        ) })
    }

    @Test
    fun everyExampleHasUniqueNameAndLabel() {
        val examples = getBuiltInExamples()

        assertEquals(examples.size, examples.map(ExampleDescription::name).distinct().size)
        assertEquals(examples.size, examples.map(ExampleDescription::label).distinct().size)
    }

    @Test
    fun portableFileExampleUsesNormalTable() {
        val examples = getBuiltInExamples().associateBy(ExampleDescription::name)
        val fileUi = assertNotNull(examples.getValue("spec-file-control").uischema)
        fun flatten(element: UiSchemaElement): List<UiSchemaElement> {
            val children = when (element) {
                is Layout -> element.elements
                is Categorization -> element.elements
                else -> emptyList()
            }
            return listOf(element) + children.flatMap(::flatten)
        }
        val elements = flatten(fileUi)
        assertTrue(elements.any { it.options["table"]?.toString() == "true" })
        assertFalse(elements.any { it.options["variant"]?.toString()?.contains("ag-grid") == true })
    }

    @Test
    fun portableInteractiveMetadataIsRetained() {
        val examples = getBuiltInExamples().associateBy(ExampleDescription::name)

        assertTrue(examples.getValue("dynamic").actions.isNotEmpty())
        assertEquals(2, examples.getValue("jsonschema").actions.size)
        assertEquals(2, examples.getValue("nestedArray").actions.size)
        assertNotNull(examples.getValue("enum-i18n").i18n?.translate)
        assertNotNull(examples.getValue("array-i18n").i18n?.translate)
        assertNotNull(examples.getValue("array-with-translated-custom-element-label").i18n?.translate)
        assertEquals(3, examples.getValue("array").actions.size)
        assertEquals(2, examples.getValue("array-with-sorting").actions.size)
        assertEquals(2, examples.getValue("rule-enable").actions.size)
        assertTrue(examples.getValue("additional-errors").actions.isNotEmpty())
        assertNotNull(examples.getValue("categorization").i18n?.translate)
    }

    @Test
    fun internationalizedExamplesProvideDemoLocales() {
        val examples = getBuiltInExamples().associateBy(ExampleDescription::name)

        listOf("i18n", "enum-i18n", "array-i18n", "additional-properties").forEach { name ->
            assertEquals(setOf("en", "de", "bg"), examples.getValue(name).localizedTranslations.keys)
        }
    }

    @Test
    fun jsonEditorPreservesUpstreamAbsentInitialData() {
        val example = getBuiltInExamples().first { it.name == "json-editor" }

        assertFalse(example.dataProvided)
    }

    @Test
    fun jsonSchemaActionsRegisterAndUnregisterTheUpstreamUiSchemas() {
        val example = getBuiltInExamples().first { it.name == "jsonschema" }
        val state = StateProps(
            data = example.data,
            schema = example.schema,
            uischema = example.uischema,
            actions = example.actions,
        )

        assertEquals(listOf("Register UISchema", "Unregister UISchema"), example.actions.map(ExampleAction::label))

        val registered = example.actions[0].apply(state)
        assertEquals(5, registered.uischemas.size)

        val unregistered = example.actions[1].apply(registered)
        assertTrue(unregistered.uischemas.isEmpty())
    }
}
