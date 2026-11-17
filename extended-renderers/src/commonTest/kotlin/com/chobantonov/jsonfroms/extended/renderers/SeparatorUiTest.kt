package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.LeafCondition
import com.chobantonov.jsonfroms.core.models.LabelElement
import com.chobantonov.jsonfroms.core.models.SeparatorElement
import com.chobantonov.jsonfroms.core.models.Rule
import com.chobantonov.jsonfroms.core.models.RuleEffect
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class SeparatorUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement("""{"type":"object"}""").jsonObject)
    private val data = Json.parseToJsonElement("{}")

    @Test fun materialHorizontalDividerIsStaticAndLocalized() = horizontal(false)
    @Test fun cupertinoHorizontalDividerIsStaticAndLocalized() = horizontal(true)
    @Test fun materialVerticalDividerUsesNarrowRowWidth() = vertical(false)
    @Test fun cupertinoVerticalDividerUsesNarrowRowWidth() = vertical(true)
    @Test fun materialSeparatorReportsInvalidOrientation() = invalidOrientation(false)
    @Test fun cupertinoSeparatorReportsInvalidOrientation() = invalidOrientation(true)
    @Test fun materialSeparatorFollowsVisibilityRule() = ruleVisibility(false)
    @Test fun cupertinoSeparatorFollowsVisibilityRule() = ruleVisibility(true)

    private fun invalidOrientation(cupertino: Boolean) = runComposeUiTest {
        val diagnostics = mutableListOf<UiDiagnostic>()
        setContent {
            JsonForms(
                data = data,
                schema = schema,
                uischema = SeparatorElement(options = mapOf("vertical" to JsonPrimitive("yes"))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter { diagnostics += it }),
            )
        }
        waitForIdle()
        onNodeWithContentDescription("Horizontal separator").assertExists()
        assertEquals(listOf("separator.invalidVertical"), diagnostics.map { it.code })
    }

    private fun ruleVisibility(cupertino: Boolean) = runComposeUiTest {
        var formData by mutableStateOf(Json.parseToJsonElement("""{"show":false}"""))
        setContent {
            JsonForms(
                data = formData,
                schema = schema,
                uischema = SeparatorElement(rule = Rule(RuleEffect.SHOW,
                    LeafCondition(scope = "#/properties/show", expectedValue = JsonPrimitive(true)))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
            )
        }
        onNodeWithContentDescription("Horizontal separator").assertDoesNotExist()
        runOnIdle { formData = Json.parseToJsonElement("""{"show":true}""") }
        onNodeWithContentDescription("Horizontal separator").assertExists()
    }

    private fun horizontal(cupertino: Boolean) = runComposeUiTest {
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            Box(Modifier.width(240.dp)) {
                JsonForms(
                    data = data,
                    schema = schema,
                    uischema = SeparatorElement(),
                    renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                    i18n = JsonFormsI18nState(locale = locale, translate = { key, fallback, _ ->
                        if (key == "separator.horizontal" && locale == "bg") "HOST DIVIDER" else fallback
                    }),
                    onChange = { changes++ },
                )
            }
        }
        val bounds = onNodeWithContentDescription("Horizontal separator").fetchSemanticsNode().boundsInRoot
        assertTrue(bounds.width > 200f)
        assertTrue(bounds.height > 0f)
        assertEquals(0, onAllNodes(hasClickAction()).fetchSemanticsNodes().size)
        assertEquals(0, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        assertEquals(0, onAllNodes(isFocusable()).fetchSemanticsNodes().size)
        runOnIdle { locale = "en-US" }
        onNodeWithContentDescription("Horizontal separator").assertExists()
        runOnIdle { locale = "bg" }
        onNodeWithContentDescription("HOST DIVIDER").assertExists()
        assertEquals(0, changes)
    }

    private fun vertical(cupertino: Boolean) = runComposeUiTest {
        setContent {
            Box(Modifier.width(360.dp)) {
                JsonForms(
                    data = data,
                    schema = schema,
                    uischema = HorizontalLayout(
                        options = mapOf("gap" to JsonPrimitive(8)),
                        elements = listOf(
                            LabelElement("Left"),
                            SeparatorElement(options = mapOf("vertical" to JsonPrimitive(true))),
                            LabelElement("Right"),
                        ),
                    ),
                    renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                )
            }
        }
        val bounds = onNodeWithContentDescription("Vertical separator").fetchSemanticsNode().boundsInRoot
        assertTrue(bounds.width in 0.5f..3f, "vertical divider width=${bounds.width}")
        assertTrue(bounds.height > 10f, "vertical divider height=${bounds.height}")
    }
}
