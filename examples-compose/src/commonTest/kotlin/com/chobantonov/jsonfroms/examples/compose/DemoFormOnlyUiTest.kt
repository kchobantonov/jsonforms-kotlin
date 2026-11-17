package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.examples.ExampleDescription
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class DemoFormOnlyUiTest {
    @Test fun materialFormOnlyRetainsEditedDataAndLocale() = formOnly(false)
    @Test fun cupertinoFormOnlyRetainsEditedDataAndLocale() = formOnly(true)

    private fun formOnly(cupertino: Boolean) = runComposeUiTest {
        var view by mutableStateOf(DemoUrlViewState(drawer = false))
        val store = MapSettings()
        DemoPreferences(store).save(DemoSettings(
            uiStyle = if (cupertino) DemoUiStyle.Cupertino else DemoUiStyle.Material,
            locale = DemoLocale.Bulgarian))
        val example = ExampleDescription(
            name = "form-only-state", label = "Form-only state",
            data = Json.parseToJsonElement("""{"name":"Ada"}"""),
            schema = JsonSchema(Json.parseToJsonElement(
                """{"type":"object","properties":{"name":{"type":"string"}}}"""
            ).jsonObject),
            uischema = ControlElement("#/properties/name"),
        )
        setContent { ExamplesComposeApp(examples = listOf(example), settingsStore = store,
            browserViewState = view, onBrowserViewStateChange = { view = it }) }

        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("Грейс")
        onNodeWithContentDescription("Покажи само формуляра").performClick()
        onNodeWithText("Грейс").assertExists()
        onNodeWithContentDescription("Отвори настройките").assertDoesNotExist()
        onNodeWithContentDescription("Покажи целия интерфейс").performClick()
        onNodeWithText("Грейс").assertExists()
        onNodeWithContentDescription("Отвори настройките").performClick()
        onNodeWithText("Език").assertExists()
        onNodeWithText("Bulgarian", substring = true).assertExists()
    }
}
