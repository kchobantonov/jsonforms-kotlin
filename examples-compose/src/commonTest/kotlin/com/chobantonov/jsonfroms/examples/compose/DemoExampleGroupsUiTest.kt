package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.examples.ExampleDescription
import com.chobantonov.jsonfroms.examples.ExampleFeatureGroup
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonObject

@OptIn(ExperimentalTestApi::class)
class DemoExampleGroupsUiTest {
    @Test fun materialGroupsExamplesWithoutChangingSelectionIds() = groupedNavigation(false)
    @Test fun cupertinoGroupsExamplesInBulgarianWithoutChangingSelectionIds() = groupedNavigation(true)

    private fun groupedNavigation(cupertino: Boolean) = runComposeUiTest {
        val store = MapSettings()
        DemoPreferences(store).save(DemoSettings(
            uiStyle = if (cupertino) DemoUiStyle.Cupertino else DemoUiStyle.Material,
            locale = if (cupertino) DemoLocale.Bulgarian else DemoLocale.English,
        ))
        val examples = listOf(
            ExampleDescription("plain-control", "Plain control fixture", JsonObject(emptyMap()),
                featureGroup = ExampleFeatureGroup.Controls),
            ExampleDescription("array-detail", "Array detail fixture", JsonObject(emptyMap()),
                featureGroup = ExampleFeatureGroup.Collections),
            ExampleDescription("table-pages", "Table pages fixture", JsonObject(emptyMap()),
                featureGroup = ExampleFeatureGroup.Collections),
        )
        var selected by mutableStateOf<String?>(null)
        setContent { ExamplesComposeApp(examples = examples, settingsStore = store,
            onExampleSelected = { selected = it }) }

        onNodeWithText(if (cupertino) "Полета" else "Controls").assertExists()
        onNodeWithText(if (cupertino) "Колекции и таблици" else "Collections and tables").assertExists()
        if (cupertino) {
            onNodeWithText("Примери от JSON Forms").performClick()
            onNodeWithText("Примери от спецификацията").assertExists()
            onNodeWithText("Всички примери").performClick()
        }
        onAllNodes(hasSetTextAction()).onFirst()
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onAllNodes(hasSetTextAction()).onFirst().performKeyInput {
            keyDown(Key.Tab); keyUp(Key.Tab)
        }
        onAllNodesWithText("Plain control fixture").onFirst().assertIsFocused()
        onNodeWithText("Array detail fixture")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithText("Array detail fixture").assertIsFocused()
        onNodeWithText("Array detail fixture").performKeyInput {
            keyDown(Key.Enter); keyUp(Key.Enter)
        }
        runOnIdle { assertEquals("array-detail", selected) }
        onAllNodesWithText("Array detail fixture").onFirst().assertIsSelected()
        onNodeWithText("Table pages fixture").performClick()
        runOnIdle { assertEquals("table-pages", selected) }
        onAllNodesWithText("Table pages fixture").onFirst().assertIsSelected()
        assertEquals("#table-pages", exampleHash(selected!!))
    }
}
