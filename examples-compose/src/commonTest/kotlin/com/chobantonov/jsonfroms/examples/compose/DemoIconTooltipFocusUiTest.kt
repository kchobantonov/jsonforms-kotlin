package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.examples.ExampleDescription
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlinx.serialization.json.JsonObject

@OptIn(ExperimentalTestApi::class)
class DemoIconTooltipFocusUiTest {
    @Test fun materialEnglishToolbarTooltipsOpenOnFocus() = iconTooltips(false)
    @Test fun cupertinoBulgarianToolbarTooltipsOpenOnFocus() = iconTooltips(true)

    private fun iconTooltips(cupertino: Boolean) = runComposeUiTest {
        val store = MapSettings()
        DemoPreferences(store).save(DemoSettings(
            uiStyle = if (cupertino) DemoUiStyle.Cupertino else DemoUiStyle.Material,
            locale = if (cupertino) DemoLocale.Bulgarian else DemoLocale.English,
        ))
        setContent { ExamplesComposeApp(
            examples = listOf(ExampleDescription("focus", "Focus example", JsonObject(emptyMap()))),
            settingsStore = store,
        ) }
        val settingsLabel = if (cupertino) "Отвори настройките" else "Open settings"
        val formLabel = if (cupertino) "Покажи само формуляра" else "Show form only"
        onNodeWithContentDescription(settingsLabel)
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithContentDescription(settingsLabel).assertIsFocused()
        onNodeWithText(settingsLabel).assertExists()
        onNodeWithContentDescription(formLabel)
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithContentDescription(formLabel).assertIsFocused()
        onNodeWithText(formLabel).assertExists()
        onNodeWithText(settingsLabel).assertDoesNotExist()
    }
}
