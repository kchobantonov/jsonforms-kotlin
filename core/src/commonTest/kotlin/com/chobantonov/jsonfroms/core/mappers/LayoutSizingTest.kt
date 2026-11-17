package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.SpacerElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.math.abs
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class LayoutSizingTest {
    @Test fun serializedLayoutDefaultsReadOnlyLiteralExtendedNamespace() {
        val defaults = Json.parseToJsonElement(
            """{"layoutDefaults":{"gap":8,"gridColumns":12,"wrap":true}}""").jsonObject
        val expected = LayoutSizing(8f, 12, true, null)
        assertEquals(expected, resolveLayoutSizing(emptyMap(), mapOf("jsonformsExtended" to defaults)))
        assertEquals(LayoutSizing(16f, 16, false, null), resolveLayoutSizing(emptyMap(), mapOf(
            "layoutDefaults" to defaults["layoutDefaults"],
            "jsonFormsExtended" to defaults,
        )))
        assertEquals(LayoutSizing(0f, 12, false, null), resolveLayoutSizing(mapOf(
            "gap" to JsonPrimitive(0), "wrap" to JsonPrimitive(false),
        ), mapOf("jsonformsExtended" to defaults)))
    }

    @Test fun resolvesPortableDefaultsAndExplicitZero() {
        assertEquals(LayoutSizing(16f, 16, false, null), resolveLayoutSizing(emptyMap(), emptyMap()))
        val config = mapOf("jsonformsExtended" to mapOf("layoutDefaults" to mapOf(
            "gap" to 8, "gridColumns" to 12, "wrap" to true,
        )))
        assertEquals(LayoutSizing(8f, 12, true, null), resolveLayoutSizing(emptyMap(), config))
        assertEquals(
            LayoutSizing(0f, 4, false, 40f),
            resolveLayoutSizing(mapOf(
                "gap" to JsonPrimitive(0),
                "gridColumns" to JsonPrimitive(4),
                "wrap" to JsonPrimitive(false),
                "minItemWidth" to JsonPrimitive(40),
            ), config),
        )
    }

    @Test fun resolvesFixedBeforeSpanBeforeWeightAndAuto() {
        fun item(options: kotlinx.serialization.json.JsonObject) =
            resolveLayoutItemSizing(ControlElement(scope = "#", options = mapOf("layout" to options)), null)
        assertEquals(LayoutWidthMode.Fixed(90f), item(buildJsonObject {
            put("width", 90); put("span", 2); put("weight", 3)
        }).mode)
        assertEquals(LayoutWidthMode.Span(2), item(buildJsonObject {
            put("span", 2); put("weight", 3)
        }).mode)
        assertEquals(LayoutWidthMode.Weight(3f), item(buildJsonObject { put("weight", 3) }).mode)
        assertEquals(LayoutWidthMode.Weight(1f), item(buildJsonObject { }).mode)
        assertEquals(LayoutWidthMode.Weight(1f), item(buildJsonObject {
            put("span", 0); put("weight", -2)
        }).mode)
    }

    @Test fun reservedStartAndResponsiveHintsDoNotChangePortableSizing() {
        val portable = ControlElement(scope = "#", options = mapOf("layout" to buildJsonObject {
            put("span", 2)
        }))
        val reserved = ControlElement(scope = "#", options = mapOf("layout" to buildJsonObject {
            put("span", 2)
            put("start", 3)
            put("responsive", buildJsonObject { put("small", 1) })
        }))
        val sizing = LayoutSizing(10f, 4, false, null)
        assertEquals(resolveLayoutItemSizing(portable, null), resolveLayoutItemSizing(reserved, null))
        assertEquals(
            resolveHorizontalWidths(400f, sizing, listOf(resolveLayoutItemSizing(portable, null))),
            resolveHorizontalWidths(400f, sizing, listOf(resolveLayoutItemSizing(reserved, null))),
        )
        assertEquals(emptyList(), collectLayoutDiagnostics(emptyMap(), emptyMap(), listOf(reserved), horizontal = true))
    }

    @Test fun parsesLogicalDimensionStringsAndRejectsNegativeWidth() {
        fun item(width: String, weight: Int) = resolveLayoutItemSizing(
            ControlElement(scope = "#", options = mapOf("layout" to buildJsonObject {
                put("width", width); put("weight", weight)
            })),
            null,
        )
        assertEquals(LayoutWidthMode.Fixed(50f), item("50dp", 2).mode)
        assertEquals(LayoutWidthMode.Fixed(40f), item("40px", 2).mode)
        assertEquals(LayoutWidthMode.Weight(2f), item("-10dp", 2).mode)
    }

    @Test fun usesFullGridForSpanAndRemainderForWeights() {
        val sizing = LayoutSizing(gap = 16f, gridColumns = 4, wrap = false, minItemWidth = null)
        val widths = resolveHorizontalWidths(600f, sizing, listOf(
            LayoutItemSizing(LayoutWidthMode.Fixed(100f), null, null),
            LayoutItemSizing(LayoutWidthMode.Span(2), null, null),
            LayoutItemSizing(LayoutWidthMode.Weight(2f), null, null),
            LayoutItemSizing(LayoutWidthMode.Weight(1f), null, null),
        ))
        assertEquals(100f, widths[0])
        assertEquals(292f, widths[1])
        assertTrue(abs(widths[2] - 160f / 3f * 2f) < 0.001f)
        assertTrue(abs(widths[3] - 160f / 3f) < 0.001f)
    }

    @Test fun invalidLocalValuesFallBackToNamespacedDefaults() {
        val config = mapOf("jsonformsExtended" to mapOf("layoutDefaults" to mapOf(
            "gap" to 7, "gridColumns" to 5, "wrap" to true,
        )))
        assertEquals(
            LayoutSizing(7f, 5, true, null),
            resolveLayoutSizing(mapOf(
                "gap" to JsonPrimitive(-1),
                "gridColumns" to JsonPrimitive(2.5),
                "wrap" to JsonPrimitive("yes"),
            ), config),
        )
    }

    @Test fun clampsSpanAndLetsChildOverrideParentMinimum() {
        val sizing = LayoutSizing(16f, 4, false, 80f)
        val child = ControlElement(scope = "#", options = mapOf("layout" to buildJsonObject {
            put("span", 5); put("minWidth", 0)
        }))
        val item = resolveLayoutItemSizing(child, sizing.minItemWidth)
        assertEquals(0f, item.minWidth)
        assertEquals(listOf(600f), resolveHorizontalWidths(600f, sizing, listOf(item)))
        val auto = resolveLayoutItemSizing(ControlElement(scope = "#"), sizing.minItemWidth)
        assertEquals(80f, auto.minWidth)
    }

    @Test fun spacerUsesParentAxisAndWeightOverridesIntrinsicWidth() {
        val default = SpacerElement()
        assertEquals(32f, resolveSpacerSize(default))
        assertEquals(LayoutWidthMode.Fixed(32f), resolveLayoutItemSizing(default, null).mode)
        assertEquals(32f, resolveColumnSpacerHeight(default))
        val sized = SpacerElement(size = JsonPrimitive(48))
        assertEquals(LayoutWidthMode.Fixed(48f), resolveLayoutItemSizing(sized, null).mode)
        assertEquals(48f, resolveColumnSpacerHeight(sized))
        val weighted = SpacerElement(
            size = JsonPrimitive(0),
            options = mapOf("layout" to buildJsonObject { put("weight", 1); put("height", 12) }),
        )
        assertEquals(LayoutWidthMode.Weight(1f), resolveLayoutItemSizing(weighted, null).mode)
        assertEquals(12f, resolveColumnSpacerHeight(weighted))
    }

    @Test fun verticalControlHeightUsesOnlyPortableLayoutHint() {
        val fixed = ControlElement(scope = "#", options = mapOf(
            "columns" to JsonPrimitive(4),
            "layout" to buildJsonObject { put("height", 120) },
        ))
        assertEquals(120f, resolveColumnChildHeight(fixed))
        assertEquals(LayoutHeightBounds(null, null), resolveColumnChildHeightBounds(fixed))
        assertEquals(null, resolveColumnChildHeight(ControlElement(scope = "#")))
        assertEquals(
            emptyList(),
            collectLayoutDiagnostics(emptyMap(), emptyMap(), listOf(fixed), horizontal = false)
                .filter { it.message.contains("options.layout.height") },
        )
    }

    @Test fun verticalHeightBoundsClampAnInvertedMaximum() {
        val child = ControlElement(scope = "#", options = mapOf(
            "layout" to buildJsonObject { put("height", 120); put("minHeight", 160); put("maxHeight", 90) },
        ))
        assertEquals(LayoutHeightBounds(160f, 160f), resolveColumnChildHeightBounds(child))
        assertEquals(emptyList(), collectLayoutDiagnostics(emptyMap(), emptyMap(), listOf(child), horizontal = false))
    }

    @Test fun spacerSharesVerticalHeightBounds() {
        val spacer = SpacerElement(size = JsonPrimitive(100), options = mapOf(
            "layout" to buildJsonObject { put("maxHeight", 40) },
        ))
        assertEquals(LayoutHeightBounds(null, 40f), resolveColumnChildHeightBounds(spacer))
        assertEquals(100f, resolveColumnSpacerHeight(spacer))
    }

    @Test fun verticalWidthHintsKeepBoundsAsConstraints() {
        val child = ControlElement(scope = "#", options = mapOf("layout" to buildJsonObject {
            put("width", 250); put("minWidth", 100); put("maxWidth", 180)
        }))
        assertEquals(250f, resolveColumnChildWidth(child))
        assertEquals(LayoutWidthBounds(100f, 180f), resolveColumnChildWidthBounds(child))
        assertEquals(emptyList(), collectLayoutDiagnostics(emptyMap(), emptyMap(), listOf(child), horizontal = false))
    }

    @Test fun horizontalCrossHeightIsAValidBoundedChildHint() {
        val child = ControlElement(scope = "#", options = mapOf("layout" to buildJsonObject {
            put("height", 120); put("minHeight", 60); put("maxHeight", 90)
        }))
        assertEquals(120f, resolveColumnChildHeight(child))
        assertEquals(LayoutHeightBounds(60f, 90f), resolveColumnChildHeightBounds(child))
        assertEquals(emptyList(), collectLayoutDiagnostics(emptyMap(), emptyMap(), listOf(child), horizontal = true))
    }

    @Test fun verticalWeightRetainsContentBasisAndUsesOnlyBoundedRemainder() {
        val natural = listOf(40f, 80f, 30f)
        val weights = listOf(1f, 1f, null)
        val bounds = List(3) { LayoutHeightBounds(null, null) }
        assertEquals(natural, resolveVerticalHeights(null, 10f, natural, weights, bounds))
        assertEquals(listOf(105f, 145f, 30f),
            resolveVerticalHeights(300f, 10f, natural, weights, bounds))
    }

    @Test fun verticalWeightRedistributesAfterMaximum() {
        assertEquals(listOf(60f, 210f, 10f), resolveVerticalHeights(
            availableHeight = 300f,
            gap = 10f,
            naturalHeights = listOf(40f, 50f, 10f),
            weights = listOf(1f, 1f, null),
            bounds = listOf(LayoutHeightBounds(null, 60f), LayoutHeightBounds(null, null), LayoutHeightBounds(null, null)),
        ))
    }

    @Test fun wrapMovesNonfittingFixedAndMinimumWidthChildrenInAuthoredOrder() {
        val sizing = LayoutSizing(gap = 10f, gridColumns = 4, wrap = true, minItemWidth = 120f)
        val items = listOf(
            LayoutItemSizing(LayoutWidthMode.Fixed(180f), null, null),
            LayoutItemSizing(LayoutWidthMode.Weight(1f), 120f, null),
            LayoutItemSizing(LayoutWidthMode.Weight(1f), 120f, null),
        )
        assertEquals(listOf(0..0, 1..2), resolveWrappedRows(300f, sizing, items))
        assertEquals(listOf(0..2), resolveWrappedRows(300f, sizing.copy(wrap = false), items))
    }

    @Test fun wrapUsesFullGridSpanWidthAndOnlyInterchildGaps() {
        val sizing = LayoutSizing(gap = 10f, gridColumns = 4, wrap = true, minItemWidth = null)
        val spans = List(3) { LayoutItemSizing(LayoutWidthMode.Span(2), null, null) }
        assertEquals(listOf(0..1, 2..2), resolveWrappedRows(400f, sizing, spans))
        assertEquals(emptyList(), resolveWrappedRows(400f, sizing, emptyList()))
    }

    @Test fun percentageSpanExpressionEqualsMeasuredGridCalculation() {
        for (rowWidth in listOf(120f, 400f, 600f)) {
            for (gap in listOf(0f, 10f, 24f)) {
                for (span in 1..4) {
                    val measuredColumn = (rowWidth - 3 * gap) / 4
                    val measured = maxOf(0f, span * measuredColumn + (span - 1) * gap)
                    assertEquals(measured, gridSpanWidth(rowWidth, gap, 4, span), 0.0001f)
                }
            }
        }
        assertEquals(295f, gridSpanWidth(600f, 10f, 4, 2))
    }

    @Test fun redistributesFlexibleWidthsAfterBounds() {
        val sizing = LayoutSizing(0f, 16, false, null)
        val widths = resolveHorizontalWidths(300f, sizing, listOf(
            LayoutItemSizing(LayoutWidthMode.Weight(1f), null, 60f),
            LayoutItemSizing(LayoutWidthMode.Weight(1f), null, null),
        ))
        assertEquals(listOf(60f, 240f), widths)
        val minimum = resolveHorizontalWidths(300f, sizing, listOf(
            LayoutItemSizing(LayoutWidthMode.Weight(1f), 200f, null),
            LayoutItemSizing(LayoutWidthMode.Weight(1f), null, null),
        ))
        assertEquals(listOf(200f, 100f), minimum)
    }

    @Test fun mixedRowRedistributesWeightAfterFixedAndFullGridSpan() {
        val sizing = LayoutSizing(gap = 10f, gridColumns = 4, wrap = false, minItemWidth = null)
        val widths = resolveHorizontalWidths(600f, sizing, listOf(
            LayoutItemSizing(LayoutWidthMode.Fixed(100f), null, null),
            LayoutItemSizing(LayoutWidthMode.Span(2), null, null),
            LayoutItemSizing(LayoutWidthMode.Weight(1f), null, 60f),
            LayoutItemSizing(LayoutWidthMode.Weight(1f), null, null),
        ))
        assertEquals(listOf(100f, 295f, 60f, 115f), widths)
        assertEquals(600f, widths.sum() + 3 * sizing.gap)
    }

    @Test fun narrowRowShrinksFlexibleBeforeFixedThenStopsAtAuthoredMinimum() {
        val sizing = LayoutSizing(10f, 4, false, null)
        val items = listOf(
            LayoutItemSizing(LayoutWidthMode.Fixed(200f), 150f, null),
            LayoutItemSizing(LayoutWidthMode.Weight(1f), 80f, null),
        )
        assertEquals(listOf(170f, 80f), resolveHorizontalWidths(260f, sizing, items))
        assertEquals(listOf(150f, 80f), resolveHorizontalWidths(220f, sizing, items))
        assertEquals(listOf(0..0, 1..1), resolveWrappedRows(260f, sizing.copy(wrap = true), items))
    }

    @Test fun constrainedSpansShrinkAndKeepMinimumWhenTheyOverflow() {
        val sizing = LayoutSizing(10f, 4, false, null)
        val spans = List(2) { LayoutItemSizing(LayoutWidthMode.Span(3), 100f, null) }
        assertEquals(listOf(100f, 100f), resolveHorizontalWidths(200f, sizing, spans))
        assertEquals(listOf(0..0, 1..1), resolveWrappedRows(200f, sizing.copy(wrap = true), spans))
    }
}
