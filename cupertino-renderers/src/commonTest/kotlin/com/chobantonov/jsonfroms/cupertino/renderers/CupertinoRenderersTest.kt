package com.chobantonov.jsonfroms.cupertino.renderers

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.testers.TesterContext
import com.chobantonov.jsonfroms.cupertino.renderers.controls.cupertinoDateControlTester
import com.chobantonov.jsonfroms.cupertino.renderers.controls.cupertinoDateTimeControlTester
import com.chobantonov.jsonfroms.cupertino.renderers.controls.cupertinoEnumControlTester
import com.chobantonov.jsonfroms.cupertino.renderers.controls.cupertinoNumberControlTester
import com.chobantonov.jsonfroms.cupertino.renderers.controls.cupertinoOneOfEnumControlTester
import com.chobantonov.jsonfroms.cupertino.renderers.controls.cupertinoOneOfRadioGroupControlTester
import com.chobantonov.jsonfroms.cupertino.renderers.controls.cupertinoRadioGroupControlTester
import com.chobantonov.jsonfroms.cupertino.renderers.controls.cupertinoSliderControlTester
import com.chobantonov.jsonfroms.cupertino.renderers.controls.cupertinoTextControlTester
import com.chobantonov.jsonfroms.cupertino.renderers.controls.cupertinoTimeControlTester
import com.chobantonov.jsonfroms.cupertino.renderers.util.cupertinoColors
import com.chobantonov.jsonfroms.cupertino.renderers.util.cupertinoMaterialColorScheme
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CupertinoRenderersTest {
    @Test
    fun materialCompatibilitySurfacesUseCupertinoNeutralHierarchy() {
        listOf(false, true).forEach { darkTheme ->
            val colors = cupertinoColors(darkTheme)
            val scheme = cupertinoMaterialColorScheme(darkTheme, colors)

            assertEquals(colors.background, scheme.background)
            assertEquals(colors.secondaryBackground, scheme.surface)
            assertEquals(colors.secondaryBackground, scheme.surfaceContainerHigh)
            assertEquals(colors.tertiaryBackground, scheme.surfaceContainerHighest)
        }
    }

    @Test
    fun cupertinoPackMirrorsCompleteMaterialRegistry() {
        assertEquals(materialRenderers.size, cupertinoRenderers.size)
    }

    @Test
    fun cupertinoTextRendererOutranksMaterialTextRenderer() {
        val control = ControlElement(scope = "#/properties/name")
        val schema = JsonSchema(buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("name", buildJsonObject { put("type", "string") })
            })
        })
        val context = TesterContext(rootSchema = schema)

        val cupertinoRank = cupertinoRenderers.maxOf { it.tester(control, schema, context) }
        val materialRank = materialRenderers.maxOf { it.tester(control, schema, context) }

        assertTrue(cupertinoRank > materialRank)
    }

    @Test
    fun specializedCupertinoControlsOutrankGenericInputs() {
        fun ranks(property: kotlinx.serialization.json.JsonObject, options: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap()): Pair<ControlElement, TesterContext> {
            val schema = JsonSchema(buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject { put("value", property) })
            })
            return ControlElement(scope = "#/properties/value", options = options) to TesterContext(rootSchema = schema)
        }

        val (date, dateContext) = ranks(buildJsonObject { put("type", "string"); put("format", "date") })
        assertTrue(cupertinoDateControlTester(date, null, dateContext) > cupertinoTextControlTester(date, null, dateContext))

        val (time, timeContext) = ranks(buildJsonObject { put("type", "string"); put("format", "time") })
        assertTrue(cupertinoTimeControlTester(time, null, timeContext) > cupertinoTextControlTester(time, null, timeContext))

        val (dateTime, dateTimeContext) = ranks(buildJsonObject { put("type", "string"); put("format", "date-time") })
        assertTrue(cupertinoDateTimeControlTester(dateTime, null, dateTimeContext) > cupertinoTextControlTester(dateTime, null, dateTimeContext))

        val (enum, enumContext) = ranks(buildJsonObject {
            put("type", "string")
            put("enum", JsonArray(listOf(JsonPrimitive("One"), JsonPrimitive("Two"))))
        })
        assertTrue(cupertinoEnumControlTester(enum, null, enumContext) > cupertinoTextControlTester(enum, null, enumContext))

        val (oneOfEnum, oneOfEnumContext) = ranks(buildJsonObject {
            put("type", "string")
            put("oneOf", JsonArray(listOf(
                buildJsonObject { put("const", "foo") },
                buildJsonObject { put("const", "bar") },
            )))
        })
        assertTrue(
            cupertinoOneOfEnumControlTester(oneOfEnum, null, oneOfEnumContext) >
                cupertinoTextControlTester(oneOfEnum, null, oneOfEnumContext),
        )

        val radioOptions = mapOf("format" to JsonPrimitive("radio"))
        val (enumRadio, enumRadioContext) = ranks(buildJsonObject {
            put("type", "string")
            put("enum", JsonArray(listOf(JsonPrimitive("One"), JsonPrimitive("Two"))))
        }, radioOptions)
        assertTrue(
            cupertinoRadioGroupControlTester(enumRadio, null, enumRadioContext) >
                cupertinoEnumControlTester(enumRadio, null, enumRadioContext),
        )
        assertSame(
            com.chobantonov.jsonfroms.cupertino.renderers.controls.CupertinoRadioGroupControl,
            cupertinoRenderers.maxBy { it.tester(enumRadio, null, enumRadioContext) }.renderer,
        )

        val (oneOfRadio, oneOfRadioContext) = ranks(buildJsonObject {
            put("type", "string")
            put("oneOf", JsonArray(listOf(
                buildJsonObject { put("const", "foo") },
                buildJsonObject { put("const", "bar") },
            )))
        }, radioOptions)
        assertTrue(
            cupertinoOneOfRadioGroupControlTester(oneOfRadio, null, oneOfRadioContext) >
                cupertinoOneOfEnumControlTester(oneOfRadio, null, oneOfRadioContext),
        )
        assertSame(
            com.chobantonov.jsonfroms.cupertino.renderers.controls.CupertinoOneOfRadioGroupControl,
            cupertinoRenderers.maxBy { it.tester(oneOfRadio, null, oneOfRadioContext) }.renderer,
        )

        val (slider, sliderContext) = ranks(
            buildJsonObject {
                put("type", "number"); put("minimum", 0); put("maximum", 100); put("default", 50)
            },
            mapOf("slider" to JsonPrimitive(true)),
        )
        assertTrue(cupertinoSliderControlTester(slider, null, sliderContext) > cupertinoNumberControlTester(slider, null, sliderContext))
    }
}
