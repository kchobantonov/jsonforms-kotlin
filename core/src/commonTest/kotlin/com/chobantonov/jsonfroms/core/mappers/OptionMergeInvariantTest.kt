package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.ControlElement
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame

class OptionMergeInvariantTest {
    @Test fun localFalseZeroAndEmptyStringWinWithoutMutatingSources() {
        val layoutDefaults = mutableMapOf<String, Any?>("gap" to 24, "wrap" to true)
        val dialogDefaults = mutableMapOf<String, Any?>("width" to "600px", "maximizable" to true)
        val extended = mutableMapOf<String, Any?>(
            "layoutDefaults" to layoutDefaults,
            "dialog" to dialogDefaults,
            "showValidationIndicator" to true,
        )
        val config = mutableMapOf<String, Any?>(
            "jsonformsExtended" to extended,
            "placeholder" to "Global hint",
        )
        val dialog = JsonObject(mapOf(
            "width" to JsonPrimitive("50%"),
            "maximizable" to JsonPrimitive(false),
        ))
        val authored = mutableMapOf<String, JsonElement>(
            "gap" to JsonPrimitive(0),
            "wrap" to JsonPrimitive(false),
            "placeholder" to JsonPrimitive(""),
            "showValidationIndicator" to JsonPrimitive(false),
            "dialog" to dialog,
        )
        val control = ControlElement("#", options = authored)
        val configBefore = config.toMap()
        val extendedBefore = extended.toMap()
        val layoutBefore = layoutDefaults.toMap()
        val dialogBefore = dialogDefaults.toMap()
        val authoredBefore = authored.toMap()

        val applied = config + control.options
        assertEquals(JsonPrimitive(""), applied["placeholder"])
        assertEquals(JsonPrimitive(false), applied["showValidationIndicator"])
        assertEquals(0f, resolveLayoutSizing(control.options, config).gap)
        assertFalse(resolveLayoutSizing(control.options, config).wrap)
        assertFalse(resolveContainerValidationOptions(control.options, config).showIndicator)
        val resolvedDialog = resolveDetailDialogOptions(dialog, config)
        assertEquals(DialogDimension(50f, DialogDimensionUnit.Percent), resolvedDialog.width)
        assertFalse(resolvedDialog.maximizable)

        assertSame(authored, control.options)
        assertSame(dialog, control.options["dialog"])
        assertSame(extended, config["jsonformsExtended"])
        assertSame(layoutDefaults, extended["layoutDefaults"])
        assertSame(dialogDefaults, extended["dialog"])
        assertEquals(authoredBefore, authored)
        assertEquals(configBefore, config)
        assertEquals(extendedBefore, extended)
        assertEquals(layoutBefore, layoutDefaults)
        assertEquals(dialogBefore, dialogDefaults)
    }
}
