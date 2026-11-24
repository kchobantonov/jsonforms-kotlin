package com.chobantonov.jsonfroms.extended.renderers

import com.chobantonov.jsonfroms.core.validation.RgbaColor
import kotlin.test.Test
import kotlin.test.assertEquals

class ColorSpectrumTest {
    @Test fun spectrumCornersAndCenterFollowHsbCoordinates() {
        val red = RgbaColor(255, 0, 0)
        assertEquals(RgbaColor(255, 255, 255), colorAtSpectrumPosition(red, 0f, 0f))
        assertEquals(red, colorAtSpectrumPosition(red, 1f, 0f))
        assertEquals(RgbaColor(0, 0, 0), colorAtSpectrumPosition(red, 1f, 1f))
        assertEquals(RgbaColor(128, 64, 64), colorAtSpectrumPosition(red, .5f, .5f))
    }

    @Test fun spectrumPreservesAlphaAndClampsPointerCoordinates() {
        val translucentBlue = RgbaColor(0, 0, 255, .5)
        assertEquals(RgbaColor(0, 0, 255, .5),
            colorAtSpectrumPosition(translucentBlue, 2f, -.5f))
        assertEquals(RgbaColor(0, 0, 0, .5),
            colorAtSpectrumPosition(translucentBlue, -1f, 2f))
    }
}
