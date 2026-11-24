package com.chobantonov.jsonfroms.examples.compose

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.ImageViewElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.LinkElement
import com.chobantonov.jsonfroms.core.models.SeparatorElement
import com.chobantonov.jsonfroms.core.models.SpacerElement
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.store.ValidationMode
import com.chobantonov.jsonfroms.core.store.JsonFormsUISchemaRegistryEntry
import com.chobantonov.jsonfroms.core.testers.NOT_APPLICABLE
import com.chobantonov.jsonfroms.core.testers.UiSchemaTester
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import com.chobantonov.jsonfroms.examples.StateProps
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import com.chobantonov.jsonfroms.examples.upstream.behavior.ruleUiSchema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class EditorDraftTest {
    private val originalSchema = JsonSchema(buildJsonObject { put("type", "object") })
    private val originalUiSchema = VerticalLayout(
        elements = listOf(ControlElement(scope = "#/properties/name")),
    )
    private val state = StateProps(
        data = buildJsonObject { put("name", "Ada") },
        schema = originalSchema,
        uischema = originalUiSchema,
    )

    @Test
    fun `reserved child layout hints survive UI schema editor round trip`() {
        val authored = """{"type":"Control","scope":"#/properties/name","options":{"layout":{"span":2,"start":3,"responsive":{"small":1}}}}"""
        val source = Json.parseToJsonElement(authored).jsonObject
        val parsed = applyEditorDraft(DemoPane.UiSchema, authored, state).getOrThrow().uischema

        assertEquals(source["options"], parsed?.toJsonObject()?.get("options"))
    }

    @Test
    fun `group card regions and input adornments survive UI schema editor round trip`() {
        val source = Json.parseToJsonElement("""{
          "type":"Group","label":"Fallback","options":{"collapsible":true},
          "cover":{"type":"ImageView","src":"/cover.png","alt":"Cover"},
          "header":[{"type":"Label","text":"Heading"}],
          "elements":[{"type":"Control","scope":"#/properties/name","options":{
            "startAdornment":"#","endAdornment":{"kind":"text","text":"internal"}}}],
          "footer":[{"type":"Label","text":"Footer"}]
        }""").jsonObject
        val parsed = applyEditorDraft(DemoPane.UiSchema, source.toString(), state).getOrThrow()

        assertEquals(source, parsed.uischema?.toJsonObject())
        assertEquals(source, Json.parseToJsonElement(editorText(DemoPane.UiSchema, parsed)).jsonObject)
    }

    @Test
    fun `unknown renderer namespace survives UI schema editor round trip`() {
        val authored = """{"type":"VendorWidget","name":"stable","options":{"vendor":{"mode":"wide"}},"payload":{"enabled":true},"elements":[]}"""
        val source = Json.parseToJsonElement(authored).jsonObject
        val parsed = applyEditorDraft(DemoPane.UiSchema, authored, state).getOrThrow().uischema

        assertEquals(source, parsed?.toJsonObject())

        val explicitEmpty = """{"type":"VendorWidget","options":{},"elements":[]}"""
        val emptySource = Json.parseToJsonElement(explicitEmpty).jsonObject
        val emptyParsed = applyEditorDraft(DemoPane.UiSchema, explicitEmpty, state).getOrThrow().uischema
        assertEquals(emptySource, emptyParsed?.toJsonObject())
    }

    @Test
    fun `script and renderer escape hatches survive editor without interpretation`() {
        val authored = """{"type":"Button","name":"run","label":"Run","script":"throw new Error('not permitted')","options":{"reactAntd":{"danger":true},"svelteFlowbite":{"color":"red"},"vendor":{"nested":{"constructor":"value"}}}}"""
        val source = Json.parseToJsonElement(authored).jsonObject
        val parsed = applyEditorDraft(DemoPane.UiSchema, authored, state).getOrThrow().uischema

        assertEquals(source, parsed?.toJsonObject())
        assertEquals(source, Json.parseToJsonElement(editorText(
            DemoPane.UiSchema, state.copy(uischema = parsed),
        )).jsonObject)
    }

    @Test
    fun `canonical presentation options survive editor without changing schema or data`() {
        val source = Json.parseToJsonElement("""{
          "type":"VerticalLayout","elements":[
            {"type":"Control","scope":"#/properties/tags","options":{"variant":"chips"}},
            {"type":"Control","scope":"#/properties/source","options":{"format":"code"}},
            {"type":"Control","scope":"#/properties/name","options":{
              "material":{"inputVariant":"filled"}}}
          ]
        }""").jsonObject
        val original = state.copy(schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{
            "tags":{"type":"array","items":{"type":"string"}},
            "source":{"type":"string"},"name":{"type":"string"}
          }
        }""").jsonObject))
        val next = applyEditorDraft(DemoPane.UiSchema, source.toString(), original).getOrThrow()

        assertEquals(source, next.uischema?.toJsonObject())
        assertEquals(original.schema?.toJsonObject(), next.schema?.toJsonObject())
        assertEquals(original.data, next.data)
        assertEquals(source, Json.parseToJsonElement(editorText(DemoPane.UiSchema, next)).jsonObject)
    }

    @Test
    fun `mixed portable and template elements keep their wire fields through editor`() {
        val authored = """{
            "type":"VerticalLayout","name":"root","options":{"gap":0},"elements":[
              {"type":"Group","name":"details","label":"Details","elements":[
                {"type":"Control","name":"choice","scope":"#/properties/choice",
                  "options":{"toggle":true},"rule":{"effect":"SHOW","condition":{
                    "type":"LEAF","scope":"#/properties/enabled","expectedValue":true}}},
                {"type":"Label","name":"summary","text":"Summary","i18n":"summary"},
                {"type":"Spacer","name":"space","size":8},
                {"type":"Separator","name":"divider"},
                {"type":"Link","name":"help","href":"/help","label":"Help","target":"_blank","rel":"noopener"},
                {"type":"ImageView","name":"photo","src":"/photo.png","alt":"Portrait"},
                {"type":"ListWithDetail","name":"rows","scope":"#/properties/rows"}
              ]},
              {"type":"Categorization","name":"pages","label":"Pages","elements":[
                {"type":"Category","name":"first","label":"First","elements":[
                  {"type":"Slot","name":"body","elements":[{"type":"Label","text":"Fallback"}]}
                ]}
              ]},
              {"type":"TemplateLayout","name":"shell","language":"ractive",
                "template":"<section>{{title}}</section>","options":{"vendor":{"mode":"compact"}}}
            ]
        }"""
        val source = Json.parseToJsonElement(authored).jsonObject
        val parsed = applyEditorDraft(DemoPane.UiSchema, authored, state).getOrThrow().uischema

        assertEquals(source, parsed?.toJsonObject())
        assertEquals(source, Json.parseToJsonElement(editorText(
            DemoPane.UiSchema, state.copy(uischema = parsed),
        )).jsonObject)
    }

    @Test
    fun `presentation elements and names survive UI schema editor round trip`() {
        val ui = VerticalLayout(name = "root", elements = listOf(
            SpacerElement(size = Json.parseToJsonElement("12"), name = "space"),
            SeparatorElement(name = "divider"),
            LinkElement(href = "https://example.test", label = "Help", target = "_blank",
                rel = "noopener", i18n = "help", name = "link"),
            ImageViewElement(src = "https://example.test/picture.png", alt = "Portrait",
                i18n = "portrait", name = "image"),
            ControlElement(scope = "#/properties/name", name = "nameInput"),
        ))
        val original = state.copy(uischema = ui)
        val text = editorText(DemoPane.UiSchema, original)
        val result = applyEditorDraft(DemoPane.UiSchema, text, original).getOrThrow()

        assertEquals(ui.toJsonObject(), result.uischema?.toJsonObject())
    }

    @Test
    fun `demo validation count follows committed data and validation mode`() {
        val constrained = state.copy(schema = JsonSchema(buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("name", buildJsonObject { put("type", "string"); put("minLength", 2) })
            })
        }), data = buildJsonObject { put("name", "x") })
        assertEquals(1, demoValidationErrorCount(constrained, ValidationMode.ValidateAndShow))
        assertEquals(1, demoValidationErrorCount(constrained, ValidationMode.ValidateAndHide))
        assertEquals(0, demoValidationErrorCount(constrained, ValidationMode.NoValidation))
        assertEquals(0, demoValidationErrorCount(constrained.copy(
            data = buildJsonObject { put("name", "ok") }), ValidationMode.ValidateAndShow))
    }

    @Test
    fun `demo validation summary keeps schema and host paths and sources`() {
        val constrained = state.copy(
            schema = JsonSchema(buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("name", buildJsonObject { put("type", "string"); put("minLength", 2) })
                })
            }),
            data = buildJsonObject { put("name", "x") },
            additionalErrors = listOf(ErrorObject(instancePath = "/name", message = "Rejected by host")),
        )
        val issues = demoValidationIssues(constrained, ValidationMode.ValidateAndShow)
        assertEquals(2, issues.size)
        assertEquals(DemoErrorSource.Schema, issues[0].source)
        assertEquals("/name", issues[0].path)
        assertEquals(DemoErrorSource.Host, issues[1].source)
        assertEquals("/name", issues[1].path)
        assertEquals("Schema /name: ${issues[0].message}",
            demoValidationIssueLabel(issues[0], DemoLocale.English))
        assertEquals("Хост /name: Rejected by host",
            demoValidationIssueLabel(issues[1], DemoLocale.Bulgarian))
        assertEquals(issues.size, demoValidationErrorCount(constrained, ValidationMode.ValidateAndShow))
    }

    @Test
    fun `invalid schema draft does not produce replacement state`() {
        val result = applyEditorDraft(DemoPane.Schema, "{ invalid", state)

        assertTrue(result.isFailure)
        assertEquals(originalSchema, state.schema)
    }

    @Test
    fun `data draft replaces data only after valid JSON parses`() {
        val invalid = applyEditorDraft(DemoPane.Data, "{ invalid", state)
        val valid = applyEditorDraft(DemoPane.Data, """{"name":"Grace"}""", state)

        assertTrue(invalid.isFailure)
        assertEquals("Ada", state.data.jsonObject["name"]?.jsonPrimitive?.content)
        assertEquals("Grace", valid.getOrThrow().data.jsonObject["name"]?.jsonPrimitive?.content)
        assertEquals(state.schema, valid.getOrThrow().schema)
        assertEquals(state.uischema, valid.getOrThrow().uischema)
        assertEquals(state.config, valid.getOrThrow().config)
    }

    @Test
    fun `blank data draft represents absent data while null remains explicit`() {
        val absent = applyEditorDraft(DemoPane.Data, "", state).getOrThrow()
        val explicitNull = applyEditorDraft(DemoPane.Data, "null", state).getOrThrow()

        assertEquals(JsonNull, absent.data)
        assertFalse(absent.dataProvided)
        assertEquals(JsonNull, explicitNull.data)
        assertTrue(explicitNull.dataProvided)
    }

    @Test
    fun `ui schema must parse and convert before replacement state is produced`() {
        val invalid = applyEditorDraft(DemoPane.UiSchema, """{"type":"Control"}""", state)
        val valid = applyEditorDraft(
            DemoPane.UiSchema,
            """{"type":"Control","scope":"#/properties/name"}""",
            state,
        )

        assertTrue(invalid.isFailure)
        assertEquals(originalUiSchema, state.uischema)
        assertIs<ControlElement>(valid.getOrThrow().uischema)
    }

    @Test
    fun `registered UI schemas preserve tester identity and reject changed entry count`() {
        val tester: UiSchemaTester = { _, _, _ -> NOT_APPLICABLE }
        val registered = state.copy(uischemas = listOf(
            JsonFormsUISchemaRegistryEntry(tester, originalUiSchema)))
        val invalid = applyEditorDraft(DemoPane.UiSchemas, "[]", registered)
        val malformed = applyEditorDraft(DemoPane.UiSchemas, "[42]", registered)
        val valid = applyEditorDraft(DemoPane.UiSchemas,
            """[{"type":"Control","scope":"#/properties/name"}]""", registered).getOrThrow()

        assertTrue(invalid.isFailure)
        assertTrue(malformed.isFailure)
        assertEquals(originalUiSchema, registered.uischemas.single().uischema)
        assertTrue(valid.uischemas.single().tester === tester)
        assertIs<ControlElement>(valid.uischemas.single().uischema)
        assertEquals(state.data, valid.data)
        assertEquals(state.schema, valid.schema)
    }

    @Test
    fun `rule example UI schema can be serialized and applied by the editor`() {
        val ruleState = state.copy(uischema = ruleUiSchema)
        val text = Json.encodeToString(ruleUiSchema.toJsonObject())

        val result = applyEditorDraft(DemoPane.UiSchema, text, ruleState)

        assertTrue(result.isSuccess)
        assertEquals(ruleUiSchema.toJsonObject(), result.getOrThrow().uischema?.toJsonObject())
    }

    @Test
    fun `config draft converts JSON primitives to renderer values`() {
        val result = applyEditorDraft(
            DemoPane.Config,
            """{"restrict":false,"nested":{"count":2}}""",
            state,
        ).getOrThrow()

        assertEquals(false, result.config["restrict"])
        assertEquals(mapOf("count" to 2L), result.config["nested"])
    }

    @Test
    fun `config draft does not insert schema defaults`() {
        val result = applyEditorDraft(DemoPane.Config, "{}", state).getOrThrow()

        assertEquals(emptyMap(), result.config)
        assertEquals("{}", editorText(DemoPane.Config, result).trim())
    }

    @Test
    fun `config editor preserves UI schema values as structured objects`() {
        val configuredState = state.copy(config = mapOf("withSort" to originalUiSchema))
        val text = editorText(DemoPane.Config, configuredState)
        val json = Json.parseToJsonElement(text).jsonObject

        assertEquals("VerticalLayout", json["withSort"]?.jsonObject?.get("type")?.jsonPrimitive?.content)
        assertEquals(originalUiSchema, applyEditorDraft(DemoPane.Config, text, configuredState)
            .getOrThrow().config["withSort"])
        assertTrue(applyEditorDraft(DemoPane.Config, """{"withSort":"not a schema"}""", configuredState).isFailure)
    }

    @Test
    fun `internationalization editor groups translations by locale`() {
        val translatedState = state.copy(
            translations = mapOf("name.label" to "Name"),
            localizedTranslations = mapOf(
                "en" to mapOf("name.label" to "Name"),
                "bg" to mapOf("name.label" to "Име"),
            ),
        )

        val text = editorText(DemoPane.Internationalization, translatedState)
        val json = Json.parseToJsonElement(text).jsonObject

        assertEquals(setOf("en", "bg"), json.keys)
        assertEquals("Име", json["bg"]?.jsonObject?.get("name.label")?.jsonPrimitive?.content)
    }

    @Test
    fun `additional properties example exposes its available locales`() {
        val example = getBuiltInExamples().first { it.name == "additional-properties" }
        val exampleState = state.copy(
            translations = example.translations,
            localizedTranslations = example.localizedTranslations,
        )

        val json = Json.parseToJsonElement(editorText(DemoPane.Internationalization, exampleState)).jsonObject

        assertEquals(setOf("en", "bg"), json.keys)
        assertEquals("Име на свойство", json["bg"]?.jsonObject?.get("root.additionalProperties.propertyNameLabel")?.jsonPrimitive?.content)
    }

    @Test
    fun `internationalization draft updates each locale and keeps the English fallback`() {
        val result = applyEditorDraft(
            DemoPane.Internationalization,
            """{"en":{"name.label":"Full name"},"bg":{"name.label":"Пълно име"}}""",
            state,
        ).getOrThrow()

        assertEquals("Full name", result.translations["name.label"])
        assertEquals("Пълно име", result.localizedTranslations["bg"]?.get("name.label"))
        assertEquals("Пълно име", exampleI18nForLocale(result, "bg").translate?.translate("name.label", null, emptyMap()))
        assertEquals("Full name", exampleI18nForLocale(result, "en").translate?.translate("name.label", null, emptyMap()))
    }

    @Test
    fun `internationalization draft rejects flat and non-string values`() {
        assertTrue(applyEditorDraft(DemoPane.Internationalization, """{"name.label":"Name"}""", state).isFailure)
        assertTrue(applyEditorDraft(DemoPane.Internationalization, """{"bg":{"name.label":42}}""", state).isFailure)
    }
}
