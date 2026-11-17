package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.ControlElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class ArrayRowDetailOptionsTest {
    @Test fun absentAndInvalidPresentationDoNotEnableRowDetail() {
        assertNull(resolveArrayRowDetailOptions(emptyMap()))
        assertNull(resolveArrayRowDetailOptions(Json.parseToJsonElement(
            """{"rowDetail":{"detail":{"type":"Control","scope":"#"}}}"""
        ).jsonObject))
    }

    @Test fun dialogParsesAuthoredItemRelativeDetail() {
        val options = resolveArrayRowDetailOptions(Json.parseToJsonElement("""{
          "rowDetail":{"presentation":"dialog","detail":{"type":"Control","scope":"#/properties/name"},
            "dialog":{"width":900}}
        }""").jsonObject)
        assertEquals(ArrayRowDetailPresentation.Dialog, options?.presentation)
        assertEquals("#/properties/name", (options?.detail as? ControlElement)?.scope)
        assertEquals(900, options?.dialog?.get("width")?.toString()?.toInt())
    }

    @Test fun panelDefaultsAndExplicitLayoutControls() {
        val defaults = resolveArrayRowDetailOptions(Json.parseToJsonElement(
            """{"rowDetail":{"presentation":"panel"}}"""
        ).jsonObject)
        assertEquals(ArrayRowDetailPlacement.Right, defaults?.placement)
        assertEquals(false, defaults?.collapsed)
        assertEquals(true, defaults?.resizable)
        val bottom = resolveArrayRowDetailOptions(Json.parseToJsonElement(
            """{"rowDetail":{"presentation":"panel","placement":"bottom","collapsed":true,"resizable":false}}"""
        ).jsonObject)
        assertEquals(ArrayRowDetailPlacement.Bottom, bottom?.placement)
        assertEquals(true, bottom?.collapsed)
        assertEquals(false, bottom?.resizable)
    }

    @Test fun quotedBooleanFlagsKeepPanelDefaults() {
        val options = resolveArrayRowDetailOptions(Json.parseToJsonElement(
            """{"rowDetail":{"presentation":"panel","collapsed":"true","resizable":"false"}}"""
        ).jsonObject)
        assertEquals(false, options?.collapsed)
        assertEquals(true, options?.resizable)
    }
}
