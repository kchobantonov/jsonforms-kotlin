package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.ComposeUnknownRenderer
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.toUiSchemaElement
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class UnknownRendererFamilyUiTest {
    @Test fun customRegistryWithoutFamilyProviderRetainsVisibleFallback() = runComposeUiTest {
        val plainRenderer = ComposeJsonFormsRenderer { }
        val registry = materialRenderers.take(1).map { it.copy(renderer = plainRenderer) }
        val unknown = Json.parseToJsonElement("""{"type":"VendorWidget"}""").jsonObject.toUiSchemaElement()
        var locale by mutableStateOf("en")
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("{}"),
                schema = JsonSchema(Json.parseToJsonElement("""{"type":"object"}""").jsonObject),
                uischema = unknown,
                renderers = registry,
                i18n = JsonFormsI18nState(locale = locale),
            )
        }
        onNodeWithText("No applicable renderer found.").assertExists()
        runOnIdle { locale = "bg" }
        onNodeWithText("Няма приложим визуализатор.").assertExists()
    }

    @Test fun registeredFamilyRendersUnavailableMessageAndLocaleUpdate() = runComposeUiTest {
        assertTrue(materialRenderers.first().renderer is ComposeUnknownRenderer)
        assertTrue(cupertinoRenderers.first().renderer is ComposeUnknownRenderer)
        val probe = object : ComposeJsonFormsRenderer, ComposeUnknownRenderer {
            @Composable
            override fun Render(props: ComposeJsonFormsRendererProps) = Unit

            @Composable
            override fun RenderUnknown(message: String) {
                BasicText(message, Modifier.testTag("family-unavailable"))
            }
        }
        val registry = materialRenderers.take(1).map { it.copy(renderer = probe) }
        val unknown = Json.parseToJsonElement("""{"type":"VendorWidget"}""").jsonObject.toUiSchemaElement()
        var locale by mutableStateOf("en")
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("{}"),
                schema = JsonSchema(Json.parseToJsonElement("""{"type":"object"}""").jsonObject),
                uischema = unknown,
                renderers = registry,
                i18n = JsonFormsI18nState(locale = locale),
            )
        }
        onNodeWithTag("family-unavailable").assertTextEquals("No applicable renderer found.")
        runOnIdle { locale = "bg" }
        onNodeWithTag("family-unavailable").assertTextEquals("Няма приложим визуализатор.")
    }
}
