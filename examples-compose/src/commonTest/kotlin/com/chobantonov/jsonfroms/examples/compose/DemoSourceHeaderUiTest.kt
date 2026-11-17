package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.examples.ExampleDescription
import com.chobantonov.jsonfroms.examples.ExampleSource
import kotlinx.serialization.json.JsonNull
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class DemoSourceHeaderUiTest {
    @Test
    fun sourceActionOpensTheExactExampleUrlAndUpdatesItsLocale() = runComposeUiTest {
        var locale by mutableStateOf(DemoLocale.English)
        var opened: String? = null
        val url = "https://example.test/pinned/source"
        val example = ExampleDescription("spec-example", "Spec: Example", JsonNull,
            source = ExampleSource.Spec, sourceUrl = url,
            capabilityNotice = mapOf("en" to "AG Grid section omitted",
                "bg" to "Разделът AG Grid е изключен"))
        val handler = object : UriHandler {
            override fun openUri(uri: String) { opened = uri }
        }
        setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalDemoLocale provides locale, LocalUriHandler provides handler) {
                    ExampleHeader(example)
                }
            }
        }
        onNodeWithText("Specification fixture").assertExists()
        onNodeWithText("Capability notice: AG Grid section omitted").assertExists()
        onNodeWithText("View source").performClick()
        assertEquals(url, opened)
        runOnIdle { locale = DemoLocale.Bulgarian }
        onNodeWithText("Пример от спецификацията").assertExists()
        onNodeWithText("Бележка за възможностите: Разделът AG Grid е изключен").assertExists()
        onNodeWithText("Виж източника").performClick()
        assertEquals(url, opened)
    }

    @Test
    fun unlinkedHostExampleDoesNotInventProvenance() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ExampleHeader(ExampleDescription("local", "Local example", JsonNull))
            }
        }
        onNodeWithText("Host example").assertExists()
        onNodeWithText("View source").assertDoesNotExist()
    }
}
