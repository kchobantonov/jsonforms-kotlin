package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.LabelElement
import com.chobantonov.jsonfroms.core.store.JsonFormsUISchemaRegistryEntry
import com.chobantonov.jsonfroms.examples.ExampleDescription
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class DemoEditorLifecycleUiTest {
    private val example = ExampleDescription(
        name = "editor-lifecycle", label = "Editor lifecycle",
        data = Json.parseToJsonElement("""{"name":"Ada","note":"memo"}"""),
        schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"name":{"type":"string"},"note":{"type":"string"}}}"""
        ).jsonObject),
        uischema = ControlElement("#/properties/name"),
    )

    @Test fun materialInvalidApplyAndReloadKeepActiveFormUntilValidApply() = lifecycle(false)
    @Test fun cupertinoInvalidApplyAndReloadKeepActiveFormUntilValidApply() = lifecycle(true)

    @Test fun materialEveryEditorAnnouncesInvalidJsonAndReloadClearsIt() =
        everyEditorAnnouncesInvalidJson(false)

    @Test fun cupertinoEveryEditorAnnouncesInvalidJsonAndReloadClearsIt() =
        everyEditorAnnouncesInvalidJson(true)

    @Test fun materialEditorActionsAndInputAreKeyboardReachable() = editorKeyboard(false)
    @Test fun cupertinoEditorActionsAndInputAreKeyboardReachable() = editorKeyboard(true)
    @Test fun materialBulgarianEditorExplainsInvalidApply() = bulgarianInvalidApply(false)
    @Test fun cupertinoBulgarianEditorExplainsInvalidApply() = bulgarianInvalidApply(true)

    private fun bulgarianInvalidApply(cupertino: Boolean) = runComposeUiTest {
        val store = MapSettings()
        DemoPreferences(store).save(DemoSettings(
            uiStyle = if (cupertino) DemoUiStyle.Cupertino else DemoUiStyle.Material,
            locale = DemoLocale.Bulgarian))
        setContent { ExamplesComposeApp(examples = listOf(example), settingsStore = store,
            browserViewState = DemoUrlViewState(drawer = false, activePane = DemoPane.Data)) }

        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("{ invalid")
        onNodeWithContentDescription(editorActionLabel(DemoLocale.Bulgarian, DemoPane.Data,
            reload = false)).performClick()
        val message = onAllNodes(hasSetTextAction()).onFirst()
            .fetchSemanticsNode().config[SemanticsProperties.Error]
        assertTrue(message.startsWith("JSON не може да се приложи."))
        onNodeWithText("JSON не може да се приложи.", substring = true).assertExists()
    }

    private fun editorKeyboard(cupertino: Boolean) = runComposeUiTest {
        val store = MapSettings()
        DemoPreferences(store).save(DemoSettings(
            uiStyle = if (cupertino) DemoUiStyle.Cupertino else DemoUiStyle.Material))
        setContent { ExamplesComposeApp(examples = listOf(example), settingsStore = store,
            browserViewState = DemoUrlViewState(drawer = false, activePane = DemoPane.Data)) }

        val reload = onNodeWithContentDescription("Reload Data")
        val apply = onNodeWithContentDescription("Apply Data")
        reload.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        reload.assertIsFocused()
        reload.performKeyInput { keyDown(Key.Tab); keyUp(Key.Tab) }
        apply.assertIsFocused()
        apply.performKeyInput { keyDown(Key.Tab); keyUp(Key.Tab) }
        onAllNodes(hasSetTextAction()).onFirst().assertIsFocused()
    }

    private fun everyEditorAnnouncesInvalidJson(cupertino: Boolean) = runComposeUiTest {
        var view by mutableStateOf(DemoUrlViewState(drawer = false, activePane = DemoPane.Data))
        val store = MapSettings()
        DemoPreferences(store).save(DemoSettings(
            uiStyle = if (cupertino) DemoUiStyle.Cupertino else DemoUiStyle.Material))
        setContent { ExamplesComposeApp(examples = listOf(example), settingsStore = store,
            browserViewState = view, onBrowserViewStateChange = { view = it }) }

        for (pane in DemoPane.entries.filter { it != DemoPane.Demo }) {
            runOnIdle { view = view.copy(activePane = pane) }
            onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("{ invalid")
            onNodeWithContentDescription(editorActionLabel(DemoLocale.English, pane, reload = false))
                .performClick()
            val message = onAllNodes(hasSetTextAction()).onFirst()
                .fetchSemanticsNode().config[SemanticsProperties.Error]
            assertTrue(message.isNotBlank(), "Expected an accessible error for $pane")
            onNodeWithContentDescription(editorActionLabel(DemoLocale.English, pane, reload = true))
                .performClick()
            onAllNodes(hasSetTextAction()).onFirst().assert(
                !SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        }
        runOnIdle { view = view.copy(activePane = DemoPane.Demo) }
        onNodeWithText("Ada").assertExists()
    }

    private fun lifecycle(cupertino: Boolean) = runComposeUiTest {
        var view by mutableStateOf(DemoUrlViewState(drawer = false, activePane = DemoPane.Data))
        val store = MapSettings()
        DemoPreferences(store).save(DemoSettings(
            uiStyle = if (cupertino) DemoUiStyle.Cupertino else DemoUiStyle.Material))
        setContent { ExamplesComposeApp(examples = listOf(example), settingsStore = store,
            browserViewState = view, onBrowserViewStateChange = { view = it }) }

        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("{ invalid")
        onNodeWithContentDescription("Apply Data").performClick()
        onAllNodes(hasSetTextAction()).onFirst().assert(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        runOnIdle { view = view.copy(activePane = DemoPane.Demo) }
        onNodeWithText("Ada").assertExists()

        runOnIdle { view = view.copy(activePane = DemoPane.Data) }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("""{"name":"Grace"}""")
        onAllNodes(hasSetTextAction()).onFirst().assert(
            !SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        onNodeWithContentDescription("Reload Data").performClick()
        onAllNodes(hasSetTextAction()).onFirst().assert(
            !SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        onNodeWithText("Ada", substring = true).assertExists()
        runOnIdle { view = view.copy(activePane = DemoPane.Demo) }
        onNodeWithText("Ada").assertExists()

        runOnIdle { view = view.copy(activePane = DemoPane.Data) }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("""{"name":"Grace"}""")
        onNodeWithContentDescription("Apply Data").performClick()
        runOnIdle { view = view.copy(activePane = DemoPane.Demo) }
        onNodeWithText("Grace").assertExists()
        runOnIdle { view = view.copy(activePane = DemoPane.Data) }
        onNodeWithContentDescription("Reload Data").performClick()
        runOnIdle { view = view.copy(activePane = DemoPane.Demo) }
        onNodeWithText("Grace").assertExists()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("Live")
        runOnIdle { view = view.copy(activePane = DemoPane.Data) }
        onNodeWithText("Ada", substring = true).assertExists()
    }

    @Test fun materialUnappliedDataDraftSurvivesFormEdit() = draftSurvivesFormEdit(false)
    @Test fun cupertinoUnappliedDataDraftSurvivesFormEdit() = draftSurvivesFormEdit(true)

    private fun draftSurvivesFormEdit(cupertino: Boolean) = runComposeUiTest {
        var view by mutableStateOf(DemoUrlViewState(drawer = false, activePane = DemoPane.Data))
        val store = MapSettings()
        DemoPreferences(store).save(DemoSettings(
            uiStyle = if (cupertino) DemoUiStyle.Cupertino else DemoUiStyle.Material))
        setContent { ExamplesComposeApp(examples = listOf(example), settingsStore = store,
            browserViewState = view, onBrowserViewStateChange = { view = it }) }

        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("""{"name":"Draft","note":"memo"}""")
        runOnIdle { view = view.copy(activePane = DemoPane.Demo) }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("Live")
        onNodeWithText("Live").assertExists()
        runOnIdle { view = view.copy(activePane = DemoPane.Data) }
        onNodeWithText("Draft", substring = true).assertExists()
        onNodeWithContentDescription("Apply Data").performClick()
        runOnIdle { view = view.copy(activePane = DemoPane.Demo) }
        onNodeWithText("Draft").assertExists()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("Again")
        runOnIdle { view = view.copy(activePane = DemoPane.Data) }
        onNodeWithText("Again", substring = true).assertExists()
        onNodeWithText("Draft", substring = true).assertDoesNotExist()
    }

    @Test fun materialSideBySideDataDraftTracksOnlyCleanFormEdits() = sideBySideDraft(false)
    @Test fun cupertinoSideBySideDataDraftTracksOnlyCleanFormEdits() = sideBySideDraft(true)

    private fun sideBySideDraft(cupertino: Boolean) = runComposeUiTest {
        val store = MapSettings()
        DemoPreferences(store).save(DemoSettings(
            uiStyle = if (cupertino) DemoUiStyle.Cupertino else DemoUiStyle.Material,
            demoLayout = DemoLayout.DemoAndData))
        setContent { ExamplesComposeApp(examples = listOf(example), settingsStore = store,
            browserViewState = DemoUrlViewState(drawer = false, activePane = DemoPane.Demo)) }

        assertEquals(2, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onAllNodes(hasSetTextAction()).onLast().performTextReplacement(
            """{"name":"Draft","note":"memo"}""")
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("Live")
        onNodeWithText("Draft", substring = true).assertExists()
        onNodeWithContentDescription("Apply Data").performClick()
        assertEquals(2, onAllNodes(hasSetTextAction() and hasText("Draft", substring = true))
            .fetchSemanticsNodes().size)
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("Again")
        assertEquals(2, onAllNodes(hasSetTextAction() and hasText("Again", substring = true))
            .fetchSemanticsNodes().size)
    }

    @Test fun materialSchemaApplyChangesValidationButReloadDoesNot() = schemaLifecycle(false)
    @Test fun cupertinoSchemaApplyChangesValidationButReloadDoesNot() = schemaLifecycle(true)

    private fun schemaLifecycle(cupertino: Boolean) = runComposeUiTest {
        var view by mutableStateOf(DemoUrlViewState(drawer = false, activePane = DemoPane.Schema))
        val store = MapSettings()
        DemoPreferences(store).save(DemoSettings(
            uiStyle = if (cupertino) DemoUiStyle.Cupertino else DemoUiStyle.Material))
        setContent { ExamplesComposeApp(examples = listOf(example), settingsStore = store,
            browserViewState = view, onBrowserViewStateChange = { view = it }) }

        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("{ invalid")
        onNodeWithContentDescription("Apply Schema").performClick()
        runOnIdle { view = view.copy(activePane = DemoPane.Demo) }
        onNodeWithText("Demo (1)").assertDoesNotExist()
        onNodeWithText("Ada").assertExists()

        runOnIdle { view = view.copy(activePane = DemoPane.Schema) }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement(
            """{"type":"object","properties":{"name":{"type":"string","minLength":5},"note":{"type":"string"}}}""")
        onNodeWithContentDescription("Apply Schema").performClick()
        runOnIdle { view = view.copy(activePane = DemoPane.Demo) }
        onNodeWithText("Demo (1)").assertExists()
        runOnIdle { view = view.copy(activePane = DemoPane.Schema) }
        onNodeWithContentDescription("Reload Schema").performClick()
        runOnIdle { view = view.copy(activePane = DemoPane.Demo) }
        onNodeWithText("Demo (1)").assertExists()
    }

    @Test fun materialUiSchemaApplyChangesControlButReloadDoesNot() = uiSchemaLifecycle(false)
    @Test fun cupertinoUiSchemaApplyChangesControlButReloadDoesNot() = uiSchemaLifecycle(true)

    private fun uiSchemaLifecycle(cupertino: Boolean) = runComposeUiTest {
        var view by mutableStateOf(DemoUrlViewState(drawer = false, activePane = DemoPane.UiSchema))
        val store = MapSettings()
        DemoPreferences(store).save(DemoSettings(
            uiStyle = if (cupertino) DemoUiStyle.Cupertino else DemoUiStyle.Material))
        setContent { ExamplesComposeApp(examples = listOf(example), settingsStore = store,
            browserViewState = view, onBrowserViewStateChange = { view = it }) }

        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("""{"type":"Control"}""")
        onNodeWithContentDescription("Apply UI Schema").performClick()
        runOnIdle { view = view.copy(activePane = DemoPane.Demo) }
        onNodeWithText("Ada").assertExists()

        runOnIdle { view = view.copy(activePane = DemoPane.UiSchema) }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement(
            """{"type":"Control","scope":"#/properties/note"}""")
        onNodeWithContentDescription("Apply UI Schema").performClick()
        runOnIdle { view = view.copy(activePane = DemoPane.Demo) }
        onNodeWithText("memo").assertExists()
        onNodeWithText("Ada").assertDoesNotExist()
        runOnIdle { view = view.copy(activePane = DemoPane.UiSchema) }
        onNodeWithContentDescription("Reload UI Schema").performClick()
        runOnIdle { view = view.copy(activePane = DemoPane.Demo) }
        onNodeWithText("memo").assertExists()
    }

    @Test fun materialTranslationsApplyWithoutReloadingFormState() = translationsLifecycle(false)
    @Test fun cupertinoTranslationsApplyWithoutReloadingFormState() = translationsLifecycle(true)

    private fun translationsLifecycle(cupertino: Boolean) = runComposeUiTest {
        val translatedExample = example.copy(
            uischema = ControlElement("#/properties/name", i18n = "name"),
            translations = mapOf("name.label" to "Name"),
            localizedTranslations = mapOf(
                "en" to mapOf("name.label" to "Name"),
                "bg" to mapOf("name.label" to "Име"),
            ),
        )
        var view by mutableStateOf(DemoUrlViewState(drawer = false,
            activePane = DemoPane.Internationalization))
        val store = MapSettings()
        DemoPreferences(store).save(DemoSettings(
            uiStyle = if (cupertino) DemoUiStyle.Cupertino else DemoUiStyle.Material))
        setContent { ExamplesComposeApp(examples = listOf(translatedExample), settingsStore = store,
            browserViewState = view, onBrowserViewStateChange = { view = it }) }

        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("""{"name.label":"Broken"}""")
        onNodeWithContentDescription("Apply Internationalization").performClick()
        runOnIdle { view = view.copy(activePane = DemoPane.Demo) }
        onNodeWithText("Name").assertExists()

        runOnIdle { view = view.copy(activePane = DemoPane.Internationalization) }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement(
            """{"en":{"name.label":"Full name"},"bg":{"name.label":"Пълно име"}}""")
        onNodeWithContentDescription("Apply Internationalization").performClick()
        runOnIdle { view = view.copy(activePane = DemoPane.Demo) }
        onNodeWithText("Full name").assertExists()
        runOnIdle { view = view.copy(activePane = DemoPane.Internationalization) }
        onNodeWithContentDescription("Reload Internationalization").performClick()
        runOnIdle { view = view.copy(activePane = DemoPane.Demo) }
        onNodeWithText("Full name").assertExists()
    }

    @Test fun materialConfigApplyChangesFormButReloadDoesNot() = configLifecycle(false)
    @Test fun cupertinoConfigApplyChangesFormButReloadDoesNot() = configLifecycle(true)

    private fun configLifecycle(cupertino: Boolean) = runComposeUiTest {
        val arrayExample = example.copy(
            data = Json.parseToJsonElement("""{"rows":["a","b","c","d","e","f"]}"""),
            schema = JsonSchema(Json.parseToJsonElement(
                """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"string"}}}}"""
            ).jsonObject),
            uischema = ControlElement("#/properties/rows", options = mapOf("table" to JsonPrimitive(true))),
        )
        var view by mutableStateOf(DemoUrlViewState(drawer = false, activePane = DemoPane.Config))
        val store = MapSettings()
        DemoPreferences(store).save(DemoSettings(
            uiStyle = if (cupertino) DemoUiStyle.Cupertino else DemoUiStyle.Material))
        setContent { ExamplesComposeApp(examples = listOf(arrayExample), settingsStore = store,
            browserViewState = view, onBrowserViewStateChange = { view = it }) }

        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("{ invalid")
        onNodeWithContentDescription("Apply Config").performClick()
        runOnIdle { view = view.copy(activePane = DemoPane.Demo) }
        onNodeWithContentDescription("Next page").assertExists()

        runOnIdle { view = view.copy(activePane = DemoPane.Config) }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement(
            """{"jsonformsExtended":{"array":{"pagination":false}}}""")
        onNodeWithContentDescription("Apply Config").performClick()
        runOnIdle { view = view.copy(activePane = DemoPane.Demo) }
        onNodeWithContentDescription("Next page").assertDoesNotExist()
        runOnIdle { view = view.copy(activePane = DemoPane.Config) }
        onNodeWithContentDescription("Reload Config").performClick()
        runOnIdle { view = view.copy(activePane = DemoPane.Demo) }
        onNodeWithContentDescription("Next page").assertDoesNotExist()
    }

    @Test fun materialRegisteredUiSchemasApplyAndReload() = uiSchemasLifecycle(false)
    @Test fun cupertinoRegisteredUiSchemasApplyAndReload() = uiSchemasLifecycle(true)

    private fun uiSchemasLifecycle(cupertino: Boolean) = runComposeUiTest {
        val registeredExample = example.copy(
            data = Json.parseToJsonElement("""{"person":{"name":"Ada"}}"""),
            schema = JsonSchema(Json.parseToJsonElement(
                """{"type":"object","properties":{"person":{"type":"object","properties":{"name":{"type":"string"}}}}}"""
            ).jsonObject),
            uischema = ControlElement("#/properties/person",
                options = mapOf("detail" to JsonPrimitive("REGISTERED"))),
            uischemas = listOf(JsonFormsUISchemaRegistryEntry(
                tester = { _, _, _ -> 10 }, uischema = LabelElement("Registered detail"))),
        )
        var view by mutableStateOf(DemoUrlViewState(drawer = false, activePane = DemoPane.UiSchemas))
        val store = MapSettings()
        DemoPreferences(store).save(DemoSettings(
            uiStyle = if (cupertino) DemoUiStyle.Cupertino else DemoUiStyle.Material))
        setContent { ExamplesComposeApp(examples = listOf(registeredExample), settingsStore = store,
            browserViewState = view, onBrowserViewStateChange = { view = it }) }

        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("[]")
        onNodeWithContentDescription("Apply UI Schemas").performClick()
        runOnIdle { view = view.copy(activePane = DemoPane.Demo) }
        onNodeWithText("Registered detail").assertExists()

        runOnIdle { view = view.copy(activePane = DemoPane.UiSchemas) }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement(
            """[{"type":"Label","text":"Updated detail"}]""")
        onNodeWithContentDescription("Apply UI Schemas").performClick()
        runOnIdle { view = view.copy(activePane = DemoPane.Demo) }
        onNodeWithText("Updated detail").assertExists()
        onNodeWithText("Registered detail").assertDoesNotExist()
        runOnIdle { view = view.copy(activePane = DemoPane.UiSchemas) }
        onNodeWithContentDescription("Reload UI Schemas").performClick()
        runOnIdle { view = view.copy(activePane = DemoPane.Demo) }
        onNodeWithText("Updated detail").assertExists()
    }
}
