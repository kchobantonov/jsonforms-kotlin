package com.chobantonov.jsonfroms.material.renderers.layouts

import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonPrimitive

class MaterialHorizontalLayoutTest {
    @Test
    fun staysHorizontalOnSmallScreensByDefault() {
        assertEquals(
            LayoutDirection.Row,
            horizontalLayoutDirection(HorizontalLayout(), 320.dp),
        )
    }

    @Test
    fun stacksOnSmallScreensWhenRequested() {
        val layout = HorizontalLayout(
            options = mapOf("stackOnSmallScreen" to JsonPrimitive(true)),
        )

        assertEquals(LayoutDirection.Column, horizontalLayoutDirection(layout, 320.dp))
        assertEquals(LayoutDirection.Row, horizontalLayoutDirection(layout, 600.dp))
    }
}
