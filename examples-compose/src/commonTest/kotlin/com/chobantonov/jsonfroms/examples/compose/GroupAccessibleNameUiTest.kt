package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.toUiSchemaElement
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.extended.renderers.cupertinoExtendedRenderers
import com.chobantonov.jsonfroms.extended.renderers.materialExtendedRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class GroupAccessibleNameUiTest {
    @Test fun materialHeaderLabelNamesDisclosureAcrossLocales() = headerName(false)
    @Test fun cupertinoHeaderLabelNamesDisclosureAcrossLocales() = headerName(true)
    @Test fun materialHeaderLabelNamesStaticGroupAcrossLocales() = headerName(false, false)
    @Test fun cupertinoHeaderLabelNamesStaticGroupAcrossLocales() = headerName(true, false)
    @Test fun materialInterpolatedHeaderNamesDisclosure() = interpolatedHeaderName(false, true)
    @Test fun cupertinoInterpolatedHeaderNamesDisclosure() = interpolatedHeaderName(true, true)
    @Test fun materialInterpolatedHeaderNamesStaticGroup() = interpolatedHeaderName(false, false)
    @Test fun cupertinoInterpolatedHeaderNamesStaticGroup() = interpolatedHeaderName(true, false)
    @Test fun materialHiddenAndReorderedHeaderKeepsAuthoredName() = headerTransitions(false)
    @Test fun cupertinoHiddenAndReorderedHeaderKeepsAuthoredName() = headerTransitions(true)

    private fun headerTransitions(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"name":{"type":"string"},"hide":{"type":"boolean"}}
        }""").jsonObject)
        val primary = """{"type":"Label","text":"Primary {person}",
          "options":{"interpolate":true,"textParams":{"person":"{data.name}"}},
          "rule":{"effect":"HIDE","condition":{"scope":"#/properties/hide","expectedValue":true}}}"""
        val secondary = """{"type":"Label","text":"Secondary"}"""
        fun group(primaryFirst: Boolean) = Json.parseToJsonElement("""{
          "type":"Group","label":"Unused title",
          "options":{"collapsible":true,"collapsed":true},
          "header":[${if (primaryFirst) primary else secondary},
            ${if (primaryFirst) secondary else primary}],
          "elements":[{"type":"Control","scope":"#/properties/name"}]
        }""").jsonObject.toUiSchemaElement()
        var data by mutableStateOf(Json.parseToJsonElement("""{"name":"Ada","hide":false}"""))
        var ui by mutableStateOf(group(true))
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf("jsonformsExtended" to mapOf("dynamicValues" to mapOf("enabled" to true))),
                onChange = { writes++ })
        }
        onNodeWithText("Primary Ada").assertExists()
        onNodeWithContentDescription("Primary Ada").assertExists()
        runOnIdle { data = Json.parseToJsonElement("""{"name":"Ada","hide":true}""") }
        onNodeWithText("Primary Ada").assertDoesNotExist()
        onNodeWithText("Secondary").assertExists()
        onNodeWithContentDescription("Primary Ada").assertExists()
        runOnIdle { ui = group(false) }
        onNodeWithContentDescription("Primary Ada").assertDoesNotExist()
        onNodeWithContentDescription("Secondary").assertExists()
        runOnIdle { data = Json.parseToJsonElement("""{"name":"Mila","hide":false}""") }
        onNodeWithText("Primary Mila").assertExists()
        onNodeWithContentDescription("Secondary").assertExists()
        runOnIdle { ui = group(true) }
        onNodeWithContentDescription("Primary Mila").assertExists()
        assertEquals(0, writes)
    }

    private fun interpolatedHeaderName(cupertino: Boolean, collapsible: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"name":{"type":"string"}}
        }""").jsonObject)
        val ui = Json.parseToJsonElement("""{
          "type":"Group","label":"Unused title",
          "options":{"collapsible":$collapsible,"collapsed":true},
          "header":[
            {"type":"Button","label":"Help"},
            {"type":"Label","text":"Contact {person}","i18n":"contact.dynamic",
              "options":{"interpolate":true,"textParams":{"person":"{data.name}"}}},
            {"type":"Label","text":"Supporting text"}
          ],
          "elements":[{"type":"Control","scope":"#/properties/name"}]
        }""").jsonObject.toUiSchemaElement()
        var data by mutableStateOf(Json.parseToJsonElement("""{"name":"Ada"}"""))
        var locale by mutableStateOf("en")
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf("jsonformsExtended" to mapOf("dynamicValues" to mapOf("enabled" to true))),
                i18n = JsonFormsI18nState(locale = locale, translate = { key, fallback, _ ->
                    if (locale == "bg" && key == "contact.dynamic.text") "Контакт {person}" else fallback
                }),
                onChange = { writes++ })
        }
        onNodeWithText("Contact Ada").assertExists()
        onNodeWithContentDescription("Contact Ada").assertExists()
        runOnIdle { data = Json.parseToJsonElement("""{"name":"Mila"}""") }
        onNodeWithText("Contact Mila").assertExists()
        onNodeWithContentDescription("Contact Ada").assertDoesNotExist()
        onNodeWithContentDescription("Contact Mila").assertExists()
        runOnIdle { locale = "bg" }
        onNodeWithText("Контакт Mila").assertExists()
        onNodeWithContentDescription("Контакт Mila").assertExists()
        if (collapsible) onNodeWithContentDescription("Контакт Mila").performClick()
        assertEquals(0, writes)
    }

    private fun headerName(cupertino: Boolean, collapsible: Boolean = true) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"name":{"type":"string"}}
        }""").jsonObject)
        val ui = Json.parseToJsonElement("""{
          "type":"Group","label":"Contact","options":{"collapsible":$collapsible,"collapsed":true},
          "header":[
            {"type":"Button","label":"Help"},
            {"type":"Label","text":"Verified","i18n":"contact.heading"},
            {"type":"Label","text":"Supporting text"}
          ],
          "elements":[{"type":"Control","scope":"#/properties/name"}]
        }""").jsonObject.toUiSchemaElement()
        var locale by mutableStateOf("en")
        var writes = 0
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"name":"Ada"}"""), schema = schema,
                uischema = ui,
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                i18n = JsonFormsI18nState(locale = locale, translate = { key, fallback, _ ->
                    if (locale == "bg" && key == "contact.heading.text") "Потвърдено" else fallback
                }),
                onChange = { writes++ })
        }
        onNodeWithText("Help").assertExists()
        onNodeWithText("Contact").assertDoesNotExist()
        onNodeWithContentDescription("Verified").assertExists()
        runOnIdle { locale = "bg" }
        onNodeWithContentDescription("Verified").assertDoesNotExist()
        if (collapsible) onNodeWithContentDescription("Потвърдено").performClick()
        else onNodeWithContentDescription("Потвърдено").assertExists()
        onNodeWithText("Ada").assertExists()
        assertEquals(0, writes)
    }
}
