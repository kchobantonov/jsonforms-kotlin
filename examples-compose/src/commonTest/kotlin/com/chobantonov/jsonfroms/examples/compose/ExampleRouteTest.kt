package com.chobantonov.jsonfroms.examples.compose

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExampleRouteTest {
    @Test
    fun readsExampleNameFromHashRoute() {
        assertEquals("list-with-detail", exampleNameFromHash("#/examples/list-with-detail"))
        assertEquals("array", exampleNameFromHash("#/examples/array?form-only=true"))
    }

    @Test
    fun ignoresUnrelatedOrMalformedRoutes() {
        assertNull(exampleNameFromHash(""))
        assertNull(exampleNameFromHash("#/"))
        assertNull(exampleNameFromHash("#/examples/"))
        assertNull(exampleNameFromHash("#/examples/array/data"))
    }

    @Test
    fun createsExampleHashRoute() {
        assertEquals("#/examples/i18n", exampleHash("i18n"))
    }
}
