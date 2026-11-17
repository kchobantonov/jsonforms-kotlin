package com.chobantonov.jsonfroms.core.mappers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class DetailDialogOptionsTest {
    @Test fun defaultsKeepNormalSizeAndAllowMaximize() {
        val options = resolveDetailDialogOptions(null, emptyMap())
        assertNull(options.width)
        assertNull(options.height)
        assertTrue(options.maximizable)
        assertFalse(options.draggable)
        assertFalse(options.resizable)
    }

    @Test fun localPropertiesOverrideFormDefaultsIndividually() {
        val local = Json.parseToJsonElement(
            """{"width":900,"height":"75vh","maximizable":false,"resizable":false}"""
        ).jsonObject
        val options = resolveDetailDialogOptions(local, mapOf("jsonformsExtended" to mapOf(
            "dialog" to mapOf("width" to "50vw", "draggable" to true,
                "resizable" to true, "maximizable" to true),
        )))
        assertEquals(DialogDimension(900f, DialogDimensionUnit.Px), options.width)
        assertEquals(DialogDimension(75f, DialogDimensionUnit.Vh), options.height)
        assertFalse(options.maximizable)
        assertTrue(options.draggable)
        assertFalse(options.resizable)
    }

    @Test fun dimensionsResolveAgainstViewportAndDensity() {
        val options = resolveDetailDialogOptions(Json.parseToJsonElement(
            """{"width":"50vw","height":"75%"}"""
        ).jsonObject, emptyMap())
        assertEquals(600f, options.width?.pixels(1200f, 800f, 2f, DialogDimensionAxis.Width))
        assertEquals(600f, options.height?.pixels(1200f, 800f, 2f, DialogDimensionAxis.Height))
        val dp = resolveDetailDialogOptions(Json.parseToJsonElement(
            """{"width":"320dp","height":"30rem"}"""
        ).jsonObject, emptyMap())
        assertEquals(640f, dp.width?.pixels(1200f, 800f, 2f, DialogDimensionAxis.Width))
        assertEquals(480f, dp.height?.pixels(1200f, 800f, 2f, DialogDimensionAxis.Height))
    }

    @Test fun invalidDimensionsDoNotCreateUnusableSizes() {
        val options = resolveDetailDialogOptions(Json.parseToJsonElement(
            """{"width":-20,"height":"calc(100vh - 10px)"}"""
        ).jsonObject, emptyMap())
        assertNull(options.width)
        assertNull(options.height)
    }

    @Test fun quotedBooleanFlagsDoNotOverrideTypedDefaults() {
        val local = Json.parseToJsonElement(
            """{"maximizable":"false","draggable":"true","resizable":"true"}"""
        ).jsonObject
        val config = mapOf("jsonformsExtended" to mapOf("dialog" to mapOf(
            "maximizable" to "false", "draggable" to "true", "resizable" to "true",
        )))
        val resolved = resolveDetailDialogOptions(local, config)
        assertTrue(resolved.maximizable)
        assertFalse(resolved.draggable)
        assertFalse(resolved.resizable)
    }
}
