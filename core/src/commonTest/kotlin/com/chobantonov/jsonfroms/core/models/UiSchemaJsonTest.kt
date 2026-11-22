package com.chobantonov.jsonfroms.core.models

import com.chobantonov.jsonfroms.core.mappers.LayoutSizing
import com.chobantonov.jsonfroms.core.mappers.resolveLayoutSizing
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class UiSchemaJsonTest {
    @Test
    fun parsesControlPresentationOptionsWithoutChangingTheirTypes() {
        val source = Json.parseToJsonElement("""{
          "type":"VerticalLayout","elements":[
            {"type":"Control","scope":"#/properties/tags","options":{"variant":"chips"}},
            {"type":"Control","scope":"#/properties/choices","options":{"variant":"multi-select"}},
            {"type":"Control","scope":"#/properties/rows","options":{"table":true}},
            {"type":"Control","scope":"#/properties/enabled","options":{"toggle":true}},
            {"type":"Control","scope":"#/properties/amount","options":{"slider":true}},
            {"type":"Control","scope":"#/properties/source","options":{"format":"code","language":"json"}}
          ]
        }""") as JsonObject
        val controls = assertIs<VerticalLayout>(source.toUiSchemaElement()).elements
            .map { assertIs<ControlElement>(it) }

        assertEquals(listOf("tags", "choices", "rows", "enabled", "amount", "source"),
            controls.map { it.scope.substringAfterLast('/') })
        assertEquals(JsonPrimitive("chips"), controls[0].options["variant"])
        assertEquals(JsonPrimitive("multi-select"), controls[1].options["variant"])
        assertEquals(JsonPrimitive(true), controls[2].options["table"])
        assertEquals(JsonPrimitive(true), controls[3].options["toggle"])
        assertEquals(JsonPrimitive(true), controls[4].options["slider"])
        assertEquals(JsonPrimitive("code"), controls[5].options["format"])
        assertEquals(JsonPrimitive("json"), controls[5].options["language"])
    }

    @Test
    fun keepsAuthoredLayoutOptionsAbsentWhileRuntimeResolvesDefaults() {
        val source = Json.parseToJsonElement(
            """{"type":"HorizontalLayout","elements":[{"type":"Control","scope":"#/properties/name"}]}"""
        ) as JsonObject
        val authored = source.toString()
        val layout = assertIs<HorizontalLayout>(source.toUiSchemaElement())
        assertEquals(emptyMap(), layout.options)
        assertEquals(emptyMap(), assertIs<ControlElement>(layout.elements.single()).options)
        assertEquals(LayoutSizing(16f, 16, false, null), resolveLayoutSizing(layout.options, emptyMap()))
        assertEquals(authored, source.toString())
    }


    @Test
    fun parsesCategoriesLabelsAndRules() {
        val schema = Json.parseToJsonElement("""
            {
              "type": "Categorization",
              "elements": [{
                "type": "Category", "label": "Contact", "elements": [{
                  "type": "Control", "scope": "#/properties/email",
                  "label": {"text": "Email", "show": true},
                  "rule": {"effect": "SHOW", "condition": {
                    "scope": "#/properties/newsletter", "expectedValue": true
                  }}
                }]
              }]
            }
        """) as JsonObject

        val category = assertIs<CategoryElement>(assertIs<CategorizationElement>(schema.toUiSchemaElement()).elements.single())
        val control = assertIs<ControlElement>(category.elements.single())
        assertEquals("#/properties/email", control.scope)
        assertEquals("Email", assertIs<DescriptionControlLabel>(control.label).value.text)
        assertEquals(RuleEffect.SHOW, control.rule?.effect)
        assertIs<LeafCondition>(control.rule?.condition)
    }

    @Test
    fun quotedBooleanFieldsDoNotEnableLabelOrUndefinedRuleBehavior() {
        fun control(show: String, failWhenUndefined: String) = assertIs<ControlElement>(
            (Json.parseToJsonElement("""{
              "type":"Control","scope":"#/properties/name",
              "label":{"text":"Name","show":$show},
              "rule":{"effect":"SHOW","condition":{
                "scope":"#/properties/flag","schema":{"const":true},
                "failWhenUndefined":$failWhenUndefined
              }}
            }""") as JsonObject).toUiSchemaElement()
        )

        val canonical = control("true", "true")
        assertEquals(true, assertIs<DescriptionControlLabel>(canonical.label).value.show)
        assertEquals(true, assertIs<SchemaBasedCondition>(canonical.rule?.condition).failWhenUndefined)

        val quoted = control("\"true\"", "\"true\"")
        assertEquals(null, assertIs<DescriptionControlLabel>(quoted.label).value.show)
        assertEquals(false, assertIs<SchemaBasedCondition>(quoted.rule?.condition).failWhenUndefined)
    }

    @Test
    fun preservesNamesForTemplateRegistryAndSlotContent() {
        val group = Json.parseToJsonElement("""
            {"type":"Group","name":"contact-card","elements":[
              {"type":"Slot","name":"heading","elements":[{"type":"Label","text":"Default"}]},
              {"type":"Control","scope":"#/properties/name","name":"body"}
            ]}
        """) as JsonObject
        val parsed = assertIs<GroupElement>(group.toUiSchemaElement())
        assertEquals("contact-card", parsed.name)
        assertEquals("heading", assertIs<UnknownUiSchemaElement>(parsed.elements[0]).name)
        assertEquals("body", assertIs<ControlElement>(parsed.elements[1]).name)
        val namedLayout = Json.parseToJsonElement("""{"type":"VerticalLayout","name":"content"}""") as JsonObject
        assertEquals("content", assertIs<VerticalLayout>(namedLayout.toUiSchemaElement()).name)
    }

    @Test
    fun parsesGroupCardRegionsOutsideBody() {
        val group = Json.parseToJsonElement("""{
          "type":"Group","cover":{"type":"ImageView","src":"/cover.png","alt":"Cover"},
          "header":[{"type":"Label","text":"Heading"}],
          "elements":[{"type":"Control","scope":"#/properties/name"}],
          "footer":[{"type":"Label","text":"After body"}]
        }""") as JsonObject
        val parsed = assertIs<GroupElement>(group.toUiSchemaElement())
        assertIs<ImageViewElement>(parsed.cover)
        assertEquals("Heading", assertIs<LabelElement>(parsed.header.single()).text)
        assertIs<ControlElement>(parsed.elements.single())
        assertEquals("After body", assertIs<LabelElement>(parsed.footer.single()).text)
    }

    @Test
    fun preservesUnknownNamespaceAndExplicitEmptyChildren() {
        val authored = Json.parseToJsonElement(
            """{"type":"VendorWidget","name":"stable","options":{"vendor":{"mode":"wide"}},"payload":{"enabled":true},"elements":[]}"""
        ) as JsonObject
        val parsed = assertIs<UnknownUiSchemaElement>(authored.toUiSchemaElement())

        assertEquals("stable", parsed.name)
        assertEquals(authored["payload"], parsed.properties["payload"])
        assertEquals(authored["options"]?.let { (it as JsonObject)["vendor"] }, parsed.options["vendor"])
        assertEquals(emptyList(), parsed.elements)
        assertEquals(true, parsed.elementsPresent)
        assertEquals(true, parsed.optionsPresent)

        val explicitEmpty = assertIs<UnknownUiSchemaElement>((Json.parseToJsonElement(
            """{"type":"VendorWidget","options":{},"elements":[]}"""
        ) as JsonObject).toUiSchemaElement())
        assertEquals(emptyMap(), explicitEmpty.options)
        assertEquals(true, explicitEmpty.optionsPresent)
        assertEquals(true, explicitEmpty.elementsPresent)
    }

    @Test
    fun parsesSpacerWithoutSchemaBinding() {
        val ui = Json.parseToJsonElement(
            """{"type":"HorizontalLayout","elements":[{"type":"Spacer","size":48,"name":"push","options":{"layout":{"weight":1}}}]}"""
        ) as JsonObject
        val spacer = assertIs<SpacerElement>(assertIs<HorizontalLayout>(ui.toUiSchemaElement()).elements.single())
        assertEquals("push", spacer.name)
        assertEquals(kotlinx.serialization.json.JsonPrimitive(48), spacer.size)
        assertEquals(kotlinx.serialization.json.JsonPrimitive(1),
            (spacer.options["layout"] as JsonObject)["weight"])
    }

    @Test
    fun parsesSeparatorWithoutSchemaBinding() {
        val ui = Json.parseToJsonElement(
            """{"type":"Separator","name":"divider","options":{"vertical":true}}"""
        ) as JsonObject
        val separator = assertIs<SeparatorElement>(ui.toUiSchemaElement())
        assertEquals("divider", separator.name)
        assertEquals(kotlinx.serialization.json.JsonPrimitive(true), separator.options["vertical"])
    }

    @Test
    fun parsesLinkTopLevelFieldsAndRequiresHref() {
        val ui = Json.parseToJsonElement(
            """{"type":"Link","label":"Help","href":"/help","target":"_blank","rel":"nofollow","i18n":"help"}"""
        ) as JsonObject
        val link = assertIs<LinkElement>(ui.toUiSchemaElement())
        assertEquals("Help", link.label)
        assertEquals("/help", link.href)
        assertEquals("_blank", link.target)
        assertEquals("nofollow", link.rel)
        assertEquals("help", link.i18n)
        assertFailsWith<IllegalArgumentException> {
            (Json.parseToJsonElement("""{"type":"Link","label":"No destination"}""") as JsonObject)
                .toUiSchemaElement()
        }
    }

    @Test
    fun parsesImageViewTopLevelFieldsAndPreservesMissingAnnotationsForDiagnostics() {
        val image = assertIs<ImageViewElement>((Json.parseToJsonElement(
            """{"type":"ImageView","src":"","scope":"#/properties/photo","alt":"","i18n":"avatar","name":"portrait"}"""
        ) as JsonObject).toUiSchemaElement())
        assertEquals("", image.src)
        assertEquals("#/properties/photo", image.scope)
        assertEquals("", image.alt)
        assertEquals("avatar", image.i18n)
        assertEquals("portrait", image.name)
        assertEquals(null, assertIs<ImageViewElement>((Json.parseToJsonElement(
            """{"type":"ImageView","src":"/photo.png"}"""
        ) as JsonObject).toUiSchemaElement()).alt)
        assertEquals(null, assertIs<ImageViewElement>((Json.parseToJsonElement(
            """{"type":"ImageView","alt":"Portrait"}"""
        ) as JsonObject).toUiSchemaElement()).src)
        for (invalid in listOf(
            """{"type":"ImageView","src":7,"alt":"Portrait"}""",
            """{"type":"ImageView","scope":null,"alt":"Portrait"}""",
            """{"type":"ImageView","src":"/photo.png","alt":false}""",
        )) {
            assertFailsWith<IllegalArgumentException> {
                (Json.parseToJsonElement(invalid) as JsonObject).toUiSchemaElement()
            }
        }
    }

    @Test
    fun rejectsMalformedNestedElements() {
        val schema = Json.parseToJsonElement("""
            {"type":"VerticalLayout","elements":[{"type":"Control"}]}
        """) as JsonObject
        assertFailsWith<IllegalArgumentException> { schema.toUiSchemaElement() }
    }
}
