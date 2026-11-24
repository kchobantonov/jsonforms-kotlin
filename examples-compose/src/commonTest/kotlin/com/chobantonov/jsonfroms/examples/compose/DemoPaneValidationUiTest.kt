package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.SemanticsActions
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.examples.ExampleAction
import com.chobantonov.jsonfroms.examples.StateProps
import com.chobantonov.jsonfroms.material.renderers.util.MaterialThemePreset
import com.chobantonov.jsonfroms.cupertino.renderers.util.CupertinoThemePreset
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.math.abs
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DemoPaneValidationUiTest {
    @Test
    fun cupertinoTabsScrollAndSelectInNarrowBulgarianView() = runComposeUiTest {
        var selected by mutableStateOf(DemoPane.Demo)
        setContent {
            DemoAppTheme(DemoUiStyle.Cupertino, DemoLocale.Bulgarian, DemoDirection.Ltr,
                false, MaterialThemePreset.Purple, CupertinoThemePreset.Blue) {
                Box(Modifier.width(320.dp)) {
                    DemoPaneSelector(selected, 0, false) { selected = it }
                }
            }
        }
        onNodeWithTag("cupertino-demo-tabs").assertExists()
        onNodeWithTag("cupertino-tab-Demo").assertIsSelected()
        onNodeWithTag("cupertino-tab-Config").performScrollTo().performClick()
        onNodeWithTag("cupertino-tab-Config").assertIsSelected()
        runOnIdle { assertEquals(DemoPane.Config, selected) }
        onNodeWithTag("cupertino-tab-Demo").performScrollTo().performClick()
        onNodeWithTag("cupertino-tab-Demo").assertIsSelected()
    }

    @Test fun materialDemoAdditionalPropertyOverrideRemovesAddAction() = demoAdditionalPropertyOverride(false)
    @Test fun cupertinoDemoAdditionalPropertyOverrideRemovesAddAction() = demoAdditionalPropertyOverride(true)

    private fun demoAdditionalPropertyOverride(cupertino: Boolean) = runComposeUiTest {
        var settings by mutableStateOf(DemoSettings(
            uiStyle = if (cupertino) DemoUiStyle.Cupertino else DemoUiStyle.Material,
        ))
        val state = StateProps(
            data = Json.parseToJsonElement("{}"),
            schema = JsonSchema(Json.parseToJsonElement("""{"type":"object"}""").jsonObject),
            uischema = ControlElement("#"),
            config = mapOf("allowAdditionalPropertiesIfMissing" to true),
        )
        var changes = 0
        setContent { FormCard(state, compact = false, settings = settings,
            onStateChange = { changes++ }, onSettingsChange = { settings = it }) }
        onNodeWithContentDescription("Add button").assertExists()
        runOnIdle { settings = settings.copy(allowAdditionalPropertiesByDefault = false) }
        onNodeWithContentDescription("Add button").assertDoesNotExist()
        assertEquals(0, changes)
    }

    @Test fun materialDemoFilterOverrideKeepsValidation() = demoFilterOverride(false)
    @Test fun cupertinoDemoFilterOverrideKeepsValidation() = demoFilterOverride(true)

    private fun demoFilterOverride(cupertino: Boolean) = runComposeUiTest {
        var settings by mutableStateOf(DemoSettings(
            uiStyle = if (cupertino) DemoUiStyle.Cupertino else DemoUiStyle.Material,
        ))
        val state = StateProps(
            data = Json.parseToJsonElement("{}"),
            schema = JsonSchema(Json.parseToJsonElement(
                """{"type":"object","required":["name"],"properties":{"name":{"type":"string"}}}"""
            ).jsonObject),
            uischema = ControlElement("#/properties/name"),
            config = mapOf("enableFilterErrorsBeforeTouch" to true),
            i18n = JsonFormsI18nState(translateError = { _, _, _ -> "schema required" }),
        )
        var changes = 0
        setContent { FormCard(state, compact = false, settings = settings,
            onStateChange = { changes++ }, onSettingsChange = { settings = it }) }
        onNodeWithText("schema required").assertDoesNotExist()
        runOnIdle { settings = settings.copy(enableFilterErrorsBeforeTouch = false) }
        onNodeWithText("schema required").assertExists()
        assertEquals(0, changes)
    }

    @Test
    fun cupertinoExampleActionSharesTheFormHeadingRow() = runComposeUiTest {
        var state by mutableStateOf(StateProps(
            data = Json.parseToJsonElement("""{"name":"x"}"""),
            schema = JsonSchema(Json.parseToJsonElement(
                """{"type":"object","properties":{"name":{"type":"string"}}}"""
            ).jsonObject),
            uischema = ControlElement("#/properties/name"),
            actions = listOf(ExampleAction("Repair") { current ->
                current.copy(data = buildJsonObject { put("name", "ok") })
            }),
        ))
        setContent {
            FormCard(state, compact = false, settings = DemoSettings(uiStyle = DemoUiStyle.Cupertino),
                onStateChange = { state = it }, onSettingsChange = {})
        }
        val heading = onNodeWithText("JSON Forms").fetchSemanticsNode().boundsInRoot.center.y
        val action = onNodeWithText("Repair").fetchSemanticsNode().boundsInRoot.center.y
        assertTrue(abs(heading - action) < 30f)
        onNodeWithText("Repair").performClick()
        assertEquals("ok", state.data.jsonObject["name"]?.jsonPrimitive?.content)
    }

    @Test
    fun demoTabShowsOnlyPositiveCountsAndNamesTheCombinedLayout() = runComposeUiTest {
        var count by mutableStateOf(0)
        var combined by mutableStateOf(false)
        setContent { DemoPaneSelector(DemoPane.Demo, count, combined) {} }
        onNodeWithText("Demo").assertExists()
        runOnIdle { count = 2 }
        onNodeWithText("Demo (2)").assertExists()
        runOnIdle { combined = true }
        onNodeWithText("Demo and Data (2)").assertExists()
        runOnIdle { count = 0 }
        onNodeWithText("Demo and Data").assertExists()
    }

    @Test
    fun demoValidationIconOpensSchemaAndHostDetails() = runComposeUiTest {
        var locale by mutableStateOf(DemoLocale.English)
        val issues = listOf(
            DemoValidationIssue(DemoErrorSource.Schema, "/name", "too short"),
            DemoValidationIssue(DemoErrorSource.Host, "/email", "rejected"),
        )
        setContent {
            CompositionLocalProvider(LocalDemoLocale provides locale) {
                DemoPaneSelector(DemoPane.Demo, issues.size, false, issues) {}
            }
        }
        onNodeWithContentDescription("2 validation errors. Show details").performClick()
        onNodeWithText("Schema /name: too short").assertExists()
        onNodeWithText("Host /email: rejected").assertExists()
        runOnIdle { locale = DemoLocale.Bulgarian }
        onNodeWithContentDescription("2 грешки. Покажи подробности").assertExists()
        onNodeWithText("Схема /name: too short").assertExists()
        onNodeWithText("Хост /email: rejected").assertExists()
    }

    @Test
    fun demoValidationIconShowsErrorsOnKeyboardFocus() = runComposeUiTest {
        val issues = listOf(DemoValidationIssue(DemoErrorSource.Schema, "/name", "too short"))
        setContent { DemoPaneSelector(DemoPane.Demo, issues.size, false, issues) {} }
        onNodeWithContentDescription("1 validation error. Show details")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithText("Schema /name: too short").assertExists()
    }

    @Test
    fun pendingAnalysisIsShownSeparatelyFromTheConfirmedErrorCount() = runComposeUiTest {
        var pending by mutableStateOf(true)
        var locale by mutableStateOf(DemoLocale.English)
        val issues = listOf(DemoValidationIssue(DemoErrorSource.Host, "/name", "rejected"))
        setContent {
            CompositionLocalProvider(LocalDemoLocale provides locale) {
                DemoPaneSelector(DemoPane.Demo, issues.size, false, issues, pending) {}
            }
        }
        onNodeWithText("Demo (1)").assertExists()
        onNodeWithText(" · Analysis pending").assertExists()
        runOnIdle { locale = DemoLocale.Bulgarian }
        onNodeWithText("Демо (1)").assertExists()
        onNodeWithText(" · Анализът продължава").assertExists()
        runOnIdle { pending = false }
        onNodeWithText(" · Анализът продължава").assertDoesNotExist()
        onNodeWithText("Демо (1)").assertExists()
    }

    @Test
    fun demoTabsSwitchToBulgarianWithoutResettingTheSelectedPane() = runComposeUiTest {
        var locale by mutableStateOf(DemoLocale.English)
        var selected by mutableStateOf(DemoPane.Schema)
        setContent {
            CompositionLocalProvider(LocalDemoLocale provides locale) {
                DemoPaneSelector(selected, 2, true) { selected = it }
            }
        }
        onNodeWithText("Schema").assertExists()
        runOnIdle { locale = DemoLocale.Bulgarian }
        onNodeWithText("Схема").assertExists()
        onNodeWithText("Демо и данни (2)").assertExists()
        onNodeWithText("Данни").performClick()
        runOnIdle { assertEquals(DemoPane.Data, selected) }
    }
}
