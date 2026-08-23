package com.chobantonov.jsonfroms.material.renderers.controls

import kotlin.test.Test
import kotlin.test.assertEquals

class MaterialSliderControlTest {
    @Test
    fun formatsWholeNumberEndpointsWithoutDecimalSuffix() {
        assertEquals("1", formatSliderEndpoint(1.0))
        assertEquals("5", formatSliderEndpoint(5.0))
    }

    @Test
    fun preservesFractionalEndpoints() {
        assertEquals("1.5", formatSliderEndpoint(1.5))
    }
}
