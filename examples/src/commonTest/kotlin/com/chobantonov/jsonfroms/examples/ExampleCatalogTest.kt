package com.chobantonov.jsonfroms.examples

import com.chobantonov.jsonfroms.core.models.Categorization
import com.chobantonov.jsonfroms.core.models.Layout
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.models.LabelElement
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import com.chobantonov.jsonfroms.core.models.UnknownUiSchemaElement
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.validation.OptimumCodeJsonSchemaValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertIs

class ExampleCatalogTest {
    @Test
    fun updatedSpecExamplesExposeCardRegionsAndAdornmentOptions() {
        val examples = getBuiltInExamples().associateBy(ExampleDescription::name)
        fun visit(element: UiSchemaElement): List<UiSchemaElement> = listOf(element) + when (element) {
            is Layout -> element.elements.flatMap(::visit)
            is Categorization -> element.elements.flatMap(::visit)
            else -> emptyList()
        }
        val groupNodes = visit(assertNotNull(examples["spec-group-layout"]?.uischema))
        val card = groupNodes.filterIsInstance<GroupElement>().first { it.cover != null }
        assertTrue(card.header.isNotEmpty())
        assertTrue(card.footer.isNotEmpty())

        val adornmentNodes = visit(assertNotNull(examples["spec-input-adornments"]?.uischema))
        val controls = adornmentNodes.filterIsInstance<ControlElement>()
        assertTrue(controls.any { "startAdornment" in it.options })
        assertTrue(controls.any { "endAdornment" in it.options })
    }

    @Test
    fun bundledExampleParserPreservesGroupCardRegions() {
        val source = Json.parseToJsonElement("""{
          "type":"Group","cover":{"type":"Label","text":"Cover"},
          "header":[{"type":"Label","text":"Heading"}],
          "elements":[{"type":"Control","scope":"#/properties/name"}],
          "footer":[{"type":"Label","text":"Footer"}]
        }""").jsonObject
        val group = assertIs<GroupElement>(source.toUiSchemaElement())
        assertEquals("Cover", assertIs<LabelElement>(group.cover).text)
        assertEquals("Heading", assertIs<LabelElement>(group.header.single()).text)
        assertEquals("Footer", assertIs<LabelElement>(group.footer.single()).text)
        assertEquals(1, group.elements.size)
    }

    @Test
    fun builtInExamplesHaveFeatureGroupsWithoutChangingTheirStableIds() {
        val examples = getBuiltInExamples()
        assertEquals(upstreamExampleNames + specSnapshotExampleNames,
            examples.map(ExampleDescription::name).toSet())
        assertTrue(examples.none { it.featureGroup == ExampleFeatureGroup.Other },
            examples.filter { it.featureGroup == ExampleFeatureGroup.Other }.joinToString { it.name })
        assertEquals(ExampleFeatureGroup.Collections,
            examples.first { it.name == "spec-collection-pagination" }.featureGroup)
        assertEquals(ExampleFeatureGroup.Collections,
            examples.first { it.name == "array-with-detail" }.featureGroup)
        assertEquals(ExampleFeatureGroup.Internationalization,
            examples.first { it.name == "spec-label-interpolation" }.featureGroup)
    }

    @Test
    fun builtInExamplesLinkToTheirPinnedSourceFiles() {
        val examples = getBuiltInExamples()
        assertTrue(examples.all { it.sourceUrl != null })
        assertEquals(
            "https://github.com/kchobantonov/jsonforms/blob/$upstreamExamplesCommit/" +
                "packages/examples/src/examples/arrays.ts",
            examples.first { it.name == "array" }.sourceUrl,
        )
        assertEquals(
            "https://github.com/kchobantonov/jsonforms-extended-spec/tree/$specExamplesCommit/" +
                "examples/additional-properties",
            examples.first { it.name == "spec-additional-properties" }.sourceUrl,
        )
    }

    @Test
    fun adaptedSpecExamplesDeclareEnglishAndBulgarianCapabilityNotices() {
        val examples = getBuiltInExamples().associateBy(ExampleDescription::name)
        val adapted = setOf("additional-errors", "destructive-confirmation", "array-controls",
            "kitchen-sink", "job-application", "file-control", "table-row-details")
        adapted.forEach { id ->
            val notice = examples.getValue("spec-$id").capabilityNotice
            assertEquals(setOf("en", "bg"), notice.keys)
            assertTrue(notice.values.all(String::isNotBlank))
        }
        assertTrue(examples.getValue("spec-additional-properties").capabilityNotice.isEmpty())
        assertTrue(examples.getValue("array").capabilityNotice.isEmpty())
    }

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
        assertEquals(47, specSnapshotExampleNames.size)
        assertEquals(setOf("en", "bg"), examples.first { it.name == "spec-input-adornments" }
            .localizedTranslations.keys)
        assertEquals(setOf("en", "bg"), examples.first { it.name == "spec-array-controls" }.localizedTranslations.keys)
        assertEquals(setOf("en", "bg"), examples.first { it.name == "spec-kitchen-sink" }.localizedTranslations.keys)
        assertEquals(setOf("en", "bg"), examples.first { it.name == "spec-job-application" }.localizedTranslations.keys)
        assertEquals("Personal Information", examples.first { it.name == "spec-job-application" }
            .localizedTranslations.getValue("en").getValue("static-text.category.personal-information"))
        assertEquals("Лична информация", examples.first { it.name == "spec-job-application" }
            .localizedTranslations.getValue("bg").getValue("static-text.category.personal-information"))
        val kitchen = examples.first { it.name == "spec-kitchen-sink" }
        assertNotNull(kitchen.invalidData)
        assertEquals(kitchen.data.jsonObject.getValue("gridTeam"),
            kitchen.invalidData.jsonObject.getValue("gridTeam"))
        assertTrue(specSnapshotExampleNames.none { it in setOf(
            "spec-ag-grid", "spec-code-editor", "spec-template-layout",
        ) })
    }

    @Test
    fun portableAdditionalErrorsFixtureRetainsHostActionsAndTwoLocales() {
        val example = getBuiltInExamples().first { it.name == "spec-additional-errors" }
        assertEquals(setOf("en", "bg"), example.localizedTranslations.keys)
        val layout = example.uischema as VerticalLayout
        val buttons = (layout.elements[1] as HorizontalLayout).elements.map { it as UnknownUiSchemaElement }
        assertEquals(listOf("reportServer", "clearServer"), buttons.map {
            (it.properties["action"] as JsonPrimitive).content
        })
        assertFalse(example.data.toString().contains("policy"))
    }

    @Test
    fun nestedSpecTranslationsHonorLiteralDottedKeys() {
        val example = parseSpecExamples("""[{"name":"spec-nested","label":"Nested","data":{},"translations":{"en":{"group":{"title":"Nested","deep":{"label":"Deep"}},"group.title":"Literal","group.deep.label":"Direct"}}}]""").single()
        assertEquals("Literal", example.localizedTranslations.getValue("en").getValue("group.title"))
        assertEquals("Direct", example.localizedTranslations.getValue("en").getValue("group.deep.label"))
        assertFailsWith<IllegalArgumentException> {
            parseSpecExamples("""[{"name":"bad","label":"Bad","translations":{"en":{"label":true}}}]""")
        }
    }

    @Test
    fun schemaPathTranslationsExposeFieldAliasesWithoutOverridingAuthoredKeys() {
        val example = getBuiltInExamples().first { it.name == "spec-container-validation-indicator" }
        val bulgarian = example.localizedTranslations.getValue("bg")
        assertEquals("Сертификат", bulgarian["certifications.name.label"])
        assertEquals("Валиден до", bulgarian["certifications.expiresOn.label"])
        assertEquals("Сертификати", bulgarian["certifications.label"])

        val authored = parseSpecExamples("""[{"name":"alias","label":"Alias","translations":{"en":{
          "form.schema.properties.rows.items.properties.name.label":"Schema name",
          "rows.name.label":"Authored name"
        }}}]""").single()
        assertEquals("Authored name", authored.localizedTranslations.getValue("en")["rows.name.label"])
    }

    @Test
    fun portableKitchenSinkInvalidSampleReportsItsNinePaths() {
        val example = getBuiltInExamples().first { it.name == "spec-kitchen-sink" }
        val errors = OptimumCodeJsonSchemaValidator().compile(example.schema!!)
            .validate(example.invalidData!!)
        assertEquals(setOf(
            "/email", "/phone", "/otp", "/address/city", "/age", "/budget",
            "/startDate", "/team/4/email", "/experience/0/company",
        ), errors.map { it.instancePath }.toSet())
    }

    @Test
    fun importedDetailRegistriesEvaluateDeclaredSchemaMatches() {
        val examples = getBuiltInExamples().associateBy(ExampleDescription::name)
        val address = examples.getValue("spec-object-control").uischemas.first().tester
        val addressSchema = JsonSchema(Json.parseToJsonElement("""{"title":"Address"}""") as JsonObject)
        val otherSchema = JsonSchema(Json.parseToJsonElement("""{"title":"Other"}""") as JsonObject)
        assertEquals(10, address(addressSchema, "", ""))
        assertEquals(-1, address(otherSchema, "", ""))

        val choice = examples.getValue("spec-choice-controls").uischemas.first().tester
        val emailSchema = JsonSchema(Json.parseToJsonElement("""{"properties":{"address":{"format":"email"}}}""") as JsonObject)
        assertEquals(10, choice(emailSchema, "#/properties/cardContact", ""))
        assertEquals(-1, choice(emailSchema, "#/properties/other", ""))
    }

    @Test
    fun unknownDetailRegistryMatchFieldIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            parseSpecExamples("""[{"name":"spec-unknown-match","label":"Unknown","data":{},"uischemas":[{"match":{"rank":10,"surprise":true},"uischema":{"type":"VerticalLayout"}}]}]""")
        }
    }

    @Test
    fun importedTemplateFixtureRetainsItsNamedRegistry() {
        val example = getBuiltInExamples().first { it.name == "spec-template-slots" }
        assertEquals(listOf("contact-card", "nested-card"), example.uischemas.map { it.uischema.name })
        assertEquals(-1, example.uischemas.first().tester(null, "", ""))
        assertNotNull(example.uischema)
    }

    @Test
    fun specSnapshotRetainsNamedTemplateRegistryWithoutRankingIt() {
        val example = parseSpecExamples("""[{"name":"spec-template","label":"Template","data":{},"uischema":{"type":"Template","name":"card"},"uischemas":[{"uischema":{"type":"Group","name":"card","elements":[{"type":"Slot","name":"body"}]}}]}]""").single()
        assertEquals("card", example.uischemas.single().uischema.name)
        assertEquals(-1, example.uischemas.single().tester(null, "", ""))
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
            assertEquals(setOf("en", "bg"), examples.getValue(name).localizedTranslations.keys)
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
