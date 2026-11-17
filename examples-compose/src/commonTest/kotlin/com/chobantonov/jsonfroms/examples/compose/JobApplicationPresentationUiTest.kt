package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.cupertino.renderers.util.CupertinoAppTheme
import com.chobantonov.jsonfroms.examples.StateProps
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class JobApplicationPresentationUiTest {
    @Test fun materialFormActionsLeaveApplicationAndDataAlone() = jobFormActions(false)

    @Test fun jobFormLocaleDoesNotTranslateDemoChrome() = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-job-application" }
        val storage = MapSettings()
        DemoPreferences(storage).save(DemoSettings(uiStyle = DemoUiStyle.Material,
            themeMode = DemoThemeMode.Light))
        setContent { ExamplesComposeApp(examples = listOf(example), settingsStore = storage) }
        onAllNodesWithText(example.label).onFirst().assertExists()
        onNodeWithText("Български").performClick()
        onNodeWithText("Лична информация").assertExists()
        onAllNodesWithText(example.label).onFirst().assertExists()
        onNodeWithContentDescription("Open settings").performClick()
        onNodeWithText("Locale").assertExists()
        onNodeWithText("English (en)").assertExists()
    }
}

@OptIn(ExperimentalTestApi::class)
internal fun jobFormActions(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-job-application" }
        val originalData = example.data
        var state by mutableStateOf(StateProps(
            data = originalData, schema = example.schema, uischema = example.uischema,
            uischemas = example.uischemas, config = example.config,
            localizedTranslations = example.localizedTranslations,
        ))
        var settings by mutableStateOf(DemoSettings(
            uiStyle = if (cupertino) DemoUiStyle.Cupertino else DemoUiStyle.Material,
            themeMode = DemoThemeMode.Light,
        ))
        var dataChanges = 0
        setContent {
            val form: @androidx.compose.runtime.Composable () -> Unit = {
                FormCard(state, false, settings,
                    onStateChange = { state = it; dataChanges++ },
                    onSettingsChange = { settings = it })
            }
            if (cupertino) CupertinoAppTheme(false) { form() }
            else MaterialTheme { form() }
        }
        onNodeWithText("Personal Information").assertExists()
        onNodeWithText("Български").performClick()
        onNodeWithText("Лична информация").assertExists()
        onNodeWithText("Смени тема").performClick()
        runOnIdle {
            assertEquals(DemoLocale.Bulgarian, settings.formLocaleOverride)
            assertEquals(true, settings.formDarkThemeOverride)
            assertEquals(DemoLocale.English, settings.locale)
            assertEquals(DemoThemeMode.Light, settings.themeMode)
            assertEquals(originalData, state.data)
            assertEquals(0, dataChanges)
        }
        onNodeWithText("English").performClick()
        onNodeWithText("Personal Information").assertExists()
        runOnIdle {
            assertEquals(DemoLocale.English, settings.formLocaleOverride)
            assertEquals(true, settings.formDarkThemeOverride)
            assertEquals(originalData, state.data)
            assertEquals(0, dataChanges)
        }
}
