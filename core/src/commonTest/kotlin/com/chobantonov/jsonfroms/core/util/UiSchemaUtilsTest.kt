package com.chobantonov.jsonfroms.core.util

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.models.LabelElement
import com.chobantonov.jsonfroms.core.models.RuleEffect
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

class UiSchemaUtilsTest {
    @Test
    fun detailModeControlsRegistryLookup() {
        val registered = LabelElement("Registered")
        val generated = LabelElement("Generated")
        var lookups = 0
        fun resolve(detail: kotlinx.serialization.json.JsonElement?) = resolveDetailUiSchema(
            ControlElement("#", options = detail?.let { mapOf("detail" to it) } ?: emptyMap()),
            registered = { lookups++; registered },
            generated = { generated },
        )
        assertEquals(generated, resolve(JsonPrimitive("gEnErAtE")))
        assertEquals(0, lookups)
        assertEquals(registered, resolve(JsonPrimitive("REGISTERED")))
        assertEquals(registered, resolve(JsonPrimitive("GENERATED")))
        assertEquals(registered, resolve(JsonPrimitive("DEFAULT")))
        assertEquals(registered, resolve(null))
        assertEquals(4, lookups)
        val inline = Json.parseToJsonElement("""{"type":"Label","text":"Inline"}""")
        assertEquals(LabelElement("Inline"), resolve(inline))
        assertEquals(4, lookups)
        assertEquals(registered, resolve(Json.parseToJsonElement("""{"label":"No type"}""")))
        assertEquals(5, lookups)
        val controlFallback = ControlElement("#", options = mapOf("readonly" to JsonPrimitive(true)))
        val delegated = resolveDetailUiSchema(
            ControlElement("#", options = mapOf("detail" to JsonPrimitive("generate"))),
            registered = { error("Registry must be bypassed") },
            generated = { controlFallback },
        ) as ControlElement
        assertEquals(JsonPrimitive("GENERATE"), delegated.options["detail"])
        assertEquals(JsonPrimitive(true), delegated.options["readonly"])
        assertEquals(null, controlFallback.options["detail"])
    }

    @Test
    fun parsesNestedDetailLayoutFromControlOptions() {
        val control = ControlElement(
            scope = "#/properties/comments",
            options = mapOf(
                "detail" to buildJsonObject {
                    put("type", "VerticalLayout")
                    putJsonArray("elements") {
                        add(
                            buildJsonObject {
                                put("type", "Control")
                                put("scope", "#/properties/message")
                                put("options", buildJsonObject { put("trim", true) })
                            },
                        )
                        add(
                            buildJsonObject {
                                put("type", "Control")
                                put("scope", "#/properties/date")
                            },
                        )
                    }
                },
            ),
        )

        val detail = assertIs<VerticalLayout>(getDetailUiSchema(control))

        assertEquals(2, detail.elements.size)
        assertEquals("#/properties/message", assertIs<ControlElement>(detail.elements[0]).scope)
        assertEquals(JsonPrimitive(true), detail.elements[0].options["trim"])
        assertEquals("#/properties/date", assertIs<ControlElement>(detail.elements[1]).scope)
    }

    @Test
    fun preservesGroupDetailOptionsRuleAndNestedRootScope() {
        val detailJson = Json.parseToJsonElement(
            """{"type":"Group","label":"Item details","i18n":"detail.title","options":{"collapsible":true,"showDataIndicator":true},"rule":{"effect":"SHOW","condition":{"scope":"#/properties/name","expectedValue":"Ada"}},"elements":[{"type":"Control","scope":"#","options":{"readonly":true}}]}"""
        ).jsonObject
        val control = ControlElement(
            scope = "#/properties/rows",
            options = mapOf("detail" to detailJson),
        )

        val detail = assertIs<GroupElement>(getDetailUiSchema(control))

        assertEquals("detail.title", detail.i18n)
        assertEquals(JsonPrimitive(true), detail.options["collapsible"])
        assertEquals(JsonPrimitive(true), detail.options["showDataIndicator"])
        assertEquals(RuleEffect.SHOW, detail.rule?.effect)
        val nestedControl = assertIs<ControlElement>(detail.elements.single())
        assertEquals("#", nestedControl.scope)
        assertEquals(JsonPrimitive(true), nestedControl.options["readonly"])
    }
}
