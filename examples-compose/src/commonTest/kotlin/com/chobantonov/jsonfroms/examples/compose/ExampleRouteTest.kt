package com.chobantonov.jsonfroms.examples.compose

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExampleRouteTest {
    @Test
    fun readsExampleNameFromHashRoute() {
        assertEquals("list-with-detail", exampleNameFromHash("#/examples/list-with-detail"))
        assertEquals("array", exampleNameFromHash("#/examples/array?form-only=true"))
        assertEquals("space & / бг", exampleNameFromHash("#space%20%26%20%2F%20%D0%B1%D0%B3"))
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
        assertEquals("#i18n", exampleHash("i18n"))
        assertEquals("#space%20%26%20%2F%20%D0%B1%D0%B3", exampleHash("space & / бг"))
    }

    @Test
    fun hashQueryOverridesNormalQueryAndOnlyLiteralTrueEnablesBooleanFlags() {
        val route = parseDemoUrl(
            "?read-only=true&form-only=true&active-tab=schema&drawer=true&use-webcomponent=true",
            "#mixed?read-only=false&form-only=TRUE&active-tab=uiSchemas&drawer=false&use-webcomponent=1",
        )
        assertEquals("mixed", route.exampleName)
        assertEquals(DemoUrlViewState(activePane = DemoPane.UiSchemas, drawer = false), route.view)
        assertEquals(DemoPane.UiSchema, parseDemoUrl("?active-tab=uiSchema", "#mixed").view.activePane)
        assertEquals(DemoPane.Internationalization,
            parseDemoUrl("?active-tab=internationalization", "#mixed").view.activePane)
    }

    @Test
    fun writerNormalizesSupportedKeysAndKeepsUnrelatedParameters() {
        val state = DemoUrlState("space & / бг", DemoUrlViewState(
            readonly = true, formOnly = true, activePane = DemoPane.Schema,
            webComponent = true, drawer = false,
        ))
        val (search, hash) = writeDemoUrl(
            "?campaign=hello+world&form-only=false&drawer=true",
            "#/examples/old?source=spec&active-tab=data&read-only=false",
            state,
        )
        assertEquals("?campaign=hello%20world", search)
        assertEquals("#space%20%26%20%2F%20%D0%B1%D0%B3?source=spec&read-only=true&form-only=true&active-tab=schema&use-webcomponent=true&drawer=false", hash)
        assertEquals(state, parseDemoUrl(search, hash))
        assertEquals("#mixed", writeDemoUrl("", "#mixed?form-only=true", DemoUrlState("mixed")).second)
    }
}
