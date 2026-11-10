package com.chobantonov.jsonfroms.examples

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ExampleCatalogTest {
    @Test
    fun builtInCatalogContainsUpstreamAndExtendedRendererExamples() {
        val examples = getBuiltInExamples()
        val extendedNames = setOf(
            "button", "color", "duration", "extended-control-options",
            "file", "null-control", "split-layout",
        )

        assertEquals(upstreamSnapshotExampleNames.size + extendedNames.size, examples.size)
        assertEquals(upstreamSnapshotExampleNames + extendedNames, examples.map(ExampleDescription::name).toSet())
    }

    @Test
    fun everyExampleHasUniqueNameAndLabel() {
        val examples = getBuiltInExamples()

        assertEquals(examples.size, examples.map(ExampleDescription::name).distinct().size)
        assertEquals(examples.size, examples.map(ExampleDescription::label).distinct().size)
    }

    @Test
    fun extendedExamplesPreserveCustomUiSchemaElements() {
        val examples = getBuiltInExamples().associateBy(ExampleDescription::name)
        val button = examples.getValue("button").uischema

        assertNotNull(button)
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
