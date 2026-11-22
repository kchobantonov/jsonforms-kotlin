package com.chobantonov.jsonfroms.core.util

import com.chobantonov.jsonfroms.core.store.Translator
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class InputAdornmentsTest {
    @Test fun normalizesStringsObjectsAndOrderedArrays() {
        val options = Json.parseToJsonElement("""{
          "startAdornment":"#",
          "endAdornment":[{"kind":"text","text":"unit","tooltip":"Unit help"},
            {"kind":"icon","icon":"unknown","label":"Info"}]
        }""").jsonObject
        assertEquals(listOf(InputAdornment("#")), resolveInputAdornments(options, "startAdornment", null))
        assertEquals(listOf(InputAdornment("unit", "Unit help"), InputAdornment("Info", icon = "unknown")),
            resolveInputAdornments(options, "endAdornment", null))
    }

    @Test fun resolvesExplicitTranslationKeysAndRetainsDecorativeIdentifiers() {
        val options = Json.parseToJsonElement("""{
          "startAdornment":[{"kind":"text","text":"Fallback","tooltip":"Help","i18n":"amount.unit"},
            {"kind":"icon","icon":"decoration","decorative":true}]
        }""").jsonObject
        val translator = Translator { key, fallback, _ -> when (key) {
            "amount.unit.text" -> "Лева"
            "amount.unit.tooltip" -> "Помощ"
            else -> fallback
        } }
        assertEquals(listOf(InputAdornment("Лева", "Помощ"),
            InputAdornment("", icon = "decoration", decorative = true)),
            resolveInputAdornments(options, "startAdornment", translator))
    }

    @Test fun rejectsStringsInsideArraysAndEmptyValues() {
        val options = Json.parseToJsonElement("""{
          "startAdornment":["ignored",{"kind":"text","text":"kept"},
            {"kind":"icon","label":"No identifier"}],
          "endAdornment":""
        }""").jsonObject
        assertEquals(listOf(InputAdornment("kept")), resolveInputAdornments(options, "startAdornment", null))
        assertEquals(emptyList(), resolveInputAdornments(options, "endAdornment", null))
    }

    @Test fun rejectsNonStringFieldsAndQuotedDecorativeFlags() {
        val options = Json.parseToJsonElement("""{
          "startAdornment":[
            {"kind":"text","text":42},
            {"kind":"text","text":"valid","tooltip":7},
            {"kind":"icon","icon":123,"label":"bad icon"},
            {"kind":"icon","icon":"help","label":55},
            {"kind":"icon","icon":"help","label":"quoted flag","decorative":"true"},
            {"kind":"text","text":"typed text"},
            {"kind":"icon","icon":"help","label":"typed icon","decorative":false}
          ]
        }""").jsonObject
        assertEquals(listOf(InputAdornment("typed text"), InputAdornment("typed icon", icon = "help")),
            resolveInputAdornments(options, "startAdornment", null))
        assertEquals(listOf(0, 1, 2, 3, 4),
            invalidInputAdornments(options).map(InvalidInputAdornment::index))
    }

    @Test fun preservesSemanticIconIdentifierWithTranslatedAccessibleName() {
        val options = Json.parseToJsonElement("""{
          "endAdornment":{"kind":"icon","icon":"information","label":"Help",
            "tooltip":"More information","i18n":"field.info"}
        }""").jsonObject
        val translator = Translator { key, fallback, _ -> when (key) {
            "field.info.label" -> "Помощ"
            "field.info.tooltip" -> "Повече информация"
            else -> fallback
        } }
        assertEquals(listOf(InputAdornment("Помощ", "Повече информация", "information")),
            resolveInputAdornments(options, "endAdornment", translator))
    }

    @Test fun diagnosesMalformedLocationsWithoutFlaggingInertOrDecorativeContent() {
        val options = Json.parseToJsonElement("""{
          "startAdornment":[{"kind":"icon","icon":"star","decorative":true},
            {"kind":"icon","icon":"star","decorative":true,"label":"not decorative"},
            "array strings are invalid",{"kind":"text","text":"valid"},
            {"kind":"text","text":"unknown field","onClick":"ignored"}],
          "endAdornment":42
        }""").jsonObject
        assertEquals(listOf(
            InvalidInputAdornment("startAdornment", 1),
            InvalidInputAdornment("startAdornment", 2),
            InvalidInputAdornment("startAdornment", 4),
            InvalidInputAdornment("endAdornment"),
        ), invalidInputAdornments(options))
        assertEquals(listOf(InputAdornment("", icon = "star", decorative = true),
            InputAdornment("valid")),
            resolveInputAdornments(options, "startAdornment", null))
        assertEquals(emptyList(), invalidInputAdornments(Json.parseToJsonElement("""{
          "startAdornment":"","endAdornment":[]
        }""").jsonObject))
    }
}
