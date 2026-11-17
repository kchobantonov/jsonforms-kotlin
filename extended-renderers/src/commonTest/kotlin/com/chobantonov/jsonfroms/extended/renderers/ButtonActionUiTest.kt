package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.input.key.Key
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.models.UnknownUiSchemaElement
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.models.toUiSchemaElement
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.store.ValidationMode
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@OptIn(ExperimentalTestApi::class)
class ButtonActionUiTest {
    private val schema = JsonSchema(buildJsonObject { put("type", "object") })

    @Test fun materialImportedLocaleButtonsEmitAuthoredCommands() = importedLocaleButtons(false)
    @Test fun cupertinoImportedLocaleButtonsEmitAuthoredCommands() = importedLocaleButtons(true)

    private fun importedLocaleButtons(cupertino: Boolean) = runComposeUiTest {
        val fixture = getBuiltInExamples().first { it.name == "spec-button-actions" }
        val categories = assertIs<VerticalLayout>(fixture.uischema).elements
            .filterIsInstance<CategorizationElement>().single().elements
            .filterIsInstance<CategoryElement>()
        val action = categories.first { it.name == "action" }
        val localeRow = action.elements.filterIsInstance<HorizontalLayout>().first()
        val buttons = localeRow.elements.map { assertIs<UnknownUiSchemaElement>(it) }
        assertEquals(listOf("Button", "Button"), buttons.map { it.type })
        assertEquals(listOf("setLocale", "setLocale"), buttons.map {
            (it.properties["action"] as JsonPrimitive).content
        })
        val events = mutableListOf<ExtendedActionEvent>()
        var changes = 0
        setContent {
            JsonForms(data = fixture.data, schema = fixture.schema!!,
                uischema = localeRow,
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf(ExtendedActionEventHandlerConfigKey to
                    ExtendedActionEventHandler(events::add)),
                onChange = { changes++ })
        }
        onNodeWithText("English").performClick()
        onNodeWithText("Български").performClick()
        waitForIdle()
        assertEquals(listOf("setLocale", "setLocale"), events.map { it.action })
        assertEquals(listOf(JsonPrimitive("en"), JsonPrimitive("bg")),
            events.map { it.params["locale"] })
        assertEquals(0, changes)
    }

    @Test fun materialSelectsPortableCommandAndReportsScripts() = commands(false)
    @Test fun cupertinoSelectsPortableCommandAndReportsScripts() = commands(true)
    @Test fun materialParsedScriptStillNeedsRuntimePermission() = parsedScriptPermission(false)
    @Test fun cupertinoParsedScriptStillNeedsRuntimePermission() = parsedScriptPermission(true)
    @Test fun materialNormalizesOmittedActionParams() = omittedActionParams(false)
    @Test fun cupertinoNormalizesOmittedActionParams() = omittedActionParams(true)
    @Test fun materialReportsUnhandledActionAfterPress() = unhandledAction(false)
    @Test fun cupertinoReportsUnhandledActionAfterPress() = unhandledAction(true)

    private fun unhandledAction(cupertino: Boolean) = runComposeUiTest {
        val authored = Json.parseToJsonElement(
            """{"type":"Button","label":"Submit","action":"submit"}"""
        ).jsonObject.toUiSchemaElement()
        var handlerPresent by mutableStateOf(false)
        var reporterPresent by mutableStateOf(false)
        var locale by mutableStateOf("en")
        var calls = 0
        var changes = 0
        val diagnostics = mutableListOf<UiDiagnostic>()
        setContent {
            JsonForms(data = buildJsonObject {}, schema = schema, uischema = authored,
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = (if (reporterPresent) mapOf(
                    UiDiagnosticReporterConfigKey to UiDiagnosticReporter(diagnostics::add),
                ) else emptyMap()) +
                    if (handlerPresent) mapOf(ExtendedActionHandlerConfigKey to ExtendedActionHandler { _, _ -> calls++ })
                    else emptyMap(),
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { changes++ })
        }
        onNodeWithText("Submit").assertIsEnabled().performClick()
        waitForIdle()
        assertEquals(0, diagnostics.size)
        runOnIdle { reporterPresent = true }
        onNodeWithText("Submit").assertIsEnabled().performClick()
        waitForIdle()
        onNodeWithText("Submit").assertIsEnabled().performClick()
        waitForIdle()
        assertEquals(listOf("action.unhandled"), diagnostics.map { it.code })
        assertEquals("submit", diagnostics.single().details["action"])
        assertEquals(0, calls)
        assertEquals(0, changes)

        runOnIdle { handlerPresent = true }
        onNodeWithText("Submit").performClick()
        waitForIdle()
        assertEquals(1, calls)
        runOnIdle { locale = "bg-BG"; handlerPresent = false }
        onNodeWithText("Submit").assertIsEnabled().performClick()
        waitForIdle()
        assertEquals(listOf("action.unhandled", "action.unhandled"), diagnostics.map { it.code })
        assertEquals("Няма регистриран обработчик за това действие.", diagnostics.last().message)
        assertEquals(0, changes)
    }

    private fun omittedActionParams(cupertino: Boolean) = runComposeUiTest {
        val authored = Json.parseToJsonElement(
            """{"type":"Button","label":"Submit","action":"submit"}"""
        ).jsonObject.toUiSchemaElement()
        val events = mutableListOf<ExtendedActionEvent>()
        val legacyCalls = mutableListOf<Pair<String, JsonObject>>()
        var useEventHandler by mutableStateOf(true)
        var changes = 0
        setContent {
            JsonForms(data = buildJsonObject {}, schema = schema, uischema = authored,
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = if (useEventHandler) mapOf(
                    ExtendedActionEventHandlerConfigKey to ExtendedActionEventHandler { events += it },
                ) else mapOf(
                    ExtendedActionHandlerConfigKey to ExtendedActionHandler { action, params ->
                        legacyCalls += action to params
                    },
                ),
                onChange = { changes++ })
        }
        onNodeWithText("Submit").performClick()
        waitForIdle()
        assertEquals(1, events.size)
        assertEquals("submit", events.single().action)
        assertEquals(JsonObject(emptyMap()), events.single().params)
        assertEquals(0, changes)

        runOnIdle { useEventHandler = false }
        onNodeWithText("Submit").performClick()
        waitForIdle()
        assertEquals(listOf("submit" to JsonObject(emptyMap())), legacyCalls)
        assertEquals(1, events.size)
        assertEquals(0, changes)
    }

    private fun parsedScriptPermission(cupertino: Boolean) = runComposeUiTest {
        val authored = Json.parseToJsonElement(
            """{"type":"Button","label":"Execute","script":"console.log(1)","options":{"reactAntd":{"danger":true},"svelteFlowbite":{"color":"red"}}}"""
        ).jsonObject.toUiSchemaElement()
        var permission by mutableStateOf(false)
        var calls = 0
        var changes = 0
        val diagnostics = mutableListOf<UiDiagnostic>()
        setContent {
            JsonForms(data = buildJsonObject {}, schema = schema, uischema = authored,
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf(
                    "security" to mapOf("allowScriptEvaluation" to true),
                    "jsonFormsExtended" to mapOf("security" to mapOf("allowScriptEvaluation" to true)),
                    "jsonformsExtended" to mapOf("security" to mapOf("allowScriptEvaluation" to permission)),
                    ExtendedScriptHandlerConfigKey to ExtendedScriptHandler { _, _ -> calls++ },
                    UiDiagnosticReporterConfigKey to UiDiagnosticReporter(diagnostics::add),
                ),
                onChange = { changes++ })
        }
        onNodeWithText("Execute").assertIsNotEnabled()
        waitForIdle()
        assertEquals("button.evaluationDisabled", diagnostics.last().code)
        assertEquals(0, calls)
        assertEquals(0, changes)
        runOnIdle { permission = true }
        onNodeWithText("Execute").assertIsEnabled().performClick()
        waitForIdle()
        assertEquals(1, calls)
        assertEquals(0, changes)
    }

    private fun commands(cupertino: Boolean) = runComposeUiTest {
        var element by mutableStateOf(button())
        val calls = mutableListOf<Pair<String, JsonObject>>()
        val diagnostics = mutableListOf<UiDiagnostic>()
        setContent {
            JsonForms(
                data = buildJsonObject {}, schema = schema, uischema = element,
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf(
                    ExtendedActionHandlerConfigKey to ExtendedActionHandler { action, params ->
                        calls += action to params
                    },
                    UiDiagnosticReporterConfigKey to UiDiagnosticReporter(diagnostics::add),
                ),
                i18n = JsonFormsI18nState(locale = "bg-BG"),
            )
        }

        onNodeWithText("Execute").performClick()
        waitForIdle()
        assertEquals(listOf(""), calls.map { it.first })

        runOnIdle { element = button(name = "stable") }
        onNodeWithText("Execute").performClick()
        waitForIdle()
        assertEquals(listOf("", "stable"), calls.map { it.first })

        runOnIdle { element = button(name = "stable", action = "save", script = "throw 1",
            options = mapOf("action" to JsonPrimitive("wrong"), "label" to JsonPrimitive("Wrong"))) }
        onNodeWithText("Execute").performClick()
        waitForIdle()
        assertEquals(listOf("", "stable", "save"), calls.map { it.first })
        assertEquals("42", calls.last().second["value"]?.toString()?.trim('"'))
        assertEquals("button.actionScriptConflict", diagnostics.last().code)
        assertEquals("Действието на бутона има предимство пред скрипта", diagnostics.last().message)

        runOnIdle { element = button(name = "stable", script = "throw 1") }
        onNodeWithText("Execute").assertIsNotEnabled()
        assertEquals("button.scriptUnsupported", diagnostics.last().code)
        assertEquals("Изпълнението на скрипт за бутона не е достъпно в тази среда", diagnostics.last().message)
        assertEquals(3, calls.size)
    }

    @Test fun materialPreventsDuplicatePendingAction() = pending(false)
    @Test fun cupertinoPreventsDuplicatePendingAction() = pending(true)

    @Test fun materialHonorsDisabledAndReadonly() = disabled(false)
    @Test fun cupertinoHonorsDisabledAndReadonly() = disabled(true)
    @Test fun materialActionPermissionsIgnoreValidationMode() = actionPermissionAcrossMode(false)
    @Test fun cupertinoActionPermissionsIgnoreValidationMode() = actionPermissionAcrossMode(true)

    private fun actionPermissionAcrossMode(cupertino: Boolean) = runComposeUiTest {
        var mode by mutableStateOf(ValidationMode.NoValidation)
        var readonly by mutableStateOf(false)
        var disabled by mutableStateOf(false)
        val calls = mutableListOf<String>()
        var changes = 0
        setContent {
            JsonForms(
                data = buildJsonObject {}, schema = schema,
                uischema = button(action = "save", options = mapOf("disabled" to JsonPrimitive(disabled))),
                readonly = readonly,
                validationMode = mode,
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf(ExtendedActionHandlerConfigKey to ExtendedActionHandler { action, _ ->
                    calls += action
                }),
                onChange = { changes++ },
            )
        }
        onNodeWithText("Execute").assertIsEnabled().performClick()
        waitForIdle()
        assertEquals(listOf("save"), calls)
        runOnIdle { readonly = true }
        onNodeWithText("Execute").assertIsNotEnabled()
        runOnIdle { readonly = false; disabled = true }
        onNodeWithText("Execute").assertIsNotEnabled()
        runOnIdle { disabled = false; mode = ValidationMode.ValidateAndShow }
        onNodeWithText("Execute").assertIsEnabled().performClick()
        waitForIdle()
        assertEquals(listOf("save", "save"), calls)
        assertEquals(0, changes)
    }

    @Test fun materialKeyboardActivationRespectsDisabled() = keyboard(false)
    @Test fun cupertinoKeyboardActivationRespectsDisabled() = keyboard(true)

    @Test fun materialActionEventCarriesLiveContext() = actionEvent(false)
    @Test fun cupertinoActionEventCarriesLiveContext() = actionEvent(true)

    @Test fun materialPendingEditBlocksActionSnapshot() = pendingEdit(false)
    @Test fun cupertinoPendingEditBlocksActionSnapshot() = pendingEdit(true)

    private fun pendingEdit(cupertino: Boolean) = runComposeUiTest {
        val formSchema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"date":{"type":"string","format":"date"}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"date":"2025-01-10"}"""))
        val events = mutableListOf<ExtendedActionEvent>()
        setContent {
            JsonForms(data = data, schema = formSchema,
                uischema = VerticalLayout(elements = listOf(
                    ControlElement("#/properties/date", options = mapOf("mask" to JsonPrimitive(false))),
                    button(action = "save"),
                )),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf(ExtendedActionEventHandlerConfigKey to ExtendedActionEventHandler {
                    events += it
                }),
                i18n = JsonFormsI18nState(locale = "bg-BG"),
                onChange = { data = it.data })
        }
        onNodeWithText("Execute").assertIsEnabled()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("not-a-date")
        onNodeWithText("Execute").assertIsNotEnabled().assert(SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription,
            "Завърши текущата редакция, преди да изпълниш действието.",
        ))
        assertEquals(0, events.size)
        assertEquals(Json.parseToJsonElement("""{"date":"2025-01-10"}"""), data)
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("01/11/2025")
        onNodeWithText("Execute").assertIsEnabled().performClick()
        waitForIdle()
        assertEquals(1, events.size)
        assertEquals(Json.parseToJsonElement("""{"date":"2025-01-11"}"""),
            events.single().context.data)
    }

    @Test fun materialScriptRequiresTrustAndWaits() = script(false)
    @Test fun cupertinoScriptRequiresTrustAndWaits() = script(true)

    private fun script(cupertino: Boolean) = runComposeUiTest {
        val source = "await Promise.resolve()"
        val gate = CompletableDeferred<Unit>()
        var permission by mutableStateOf<Any?>(false)
        var element by mutableStateOf(button(name = "ignored", script = source))
        val scripts = mutableListOf<Pair<String, ExtendedActionEvent>>()
        val actions = mutableListOf<String>()
        val diagnostics = mutableListOf<UiDiagnostic>()
        setContent {
            JsonForms(
                data = buildJsonObject {}, schema = schema, uischema = element,
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf(
                    "jsonformsExtended" to mapOf("security" to mapOf("allowScriptEvaluation" to permission)),
                    ExtendedScriptHandlerConfigKey to ExtendedScriptHandler { script, event ->
                        scripts += script to event
                        gate.await()
                    },
                    ExtendedActionEventHandlerConfigKey to ExtendedActionEventHandler { event ->
                        actions += event.action
                    },
                    UiDiagnosticReporterConfigKey to UiDiagnosticReporter(diagnostics::add),
                ),
                i18n = JsonFormsI18nState(locale = "bg-BG"),
            )
        }
        onNodeWithText("Execute").assertIsNotEnabled()
        waitForIdle()
        assertEquals("button.evaluationDisabled", diagnostics.last().code)
        assertEquals("Изпълнението на скрипт за бутона е изключено", diagnostics.last().message)
        runOnIdle { permission = "true" }
        onNodeWithText("Execute").assertIsNotEnabled()
        runOnIdle { permission = 1 }
        onNodeWithText("Execute").assertIsNotEnabled()
        runOnIdle { permission = true }
        onNodeWithText("Execute").assertIsEnabled()
        mainClock.autoAdvance = false
        onNodeWithText("Execute").performClick()
        mainClock.advanceTimeBy(100)
        onNodeWithText("Execute").assertIsNotEnabled()
        assertEquals(1, scripts.size)
        assertEquals(source, scripts.single().first)
        assertEquals("", scripts.single().second.action)
        assertEquals("42", scripts.single().second.params["value"]?.toString())
        assertEquals(emptyList(), actions)
        runOnIdle { gate.complete(Unit) }
        mainClock.advanceTimeBy(100)
        mainClock.autoAdvance = true
        onNodeWithText("Execute").assertIsEnabled()

        runOnIdle { element = button(action = "save", script = source) }
        onNodeWithText("Execute").performClick()
        waitForIdle()
        assertEquals(listOf("save"), actions)
        assertEquals(1, scripts.size)
        assertEquals("button.actionScriptConflict", diagnostics.last().code)
    }

    private fun actionEvent(cupertino: Boolean) = runComposeUiTest {
        val element = button(name = "stable", action = "save")
        var data by mutableStateOf(buildJsonObject { put("revision", 1) })
        var locale by mutableStateOf("en")
        var hostContext by mutableStateOf(Json.parseToJsonElement("""{"external":{"tenant":"A"},"custom":{"flag":true}}"""))
        var additionalErrors by mutableStateOf(listOf(ErrorObject(instancePath = "/revision", keyword = "host", message = "First host error")))
        val events = mutableListOf<ExtendedActionEvent>()
        var legacyCalls = 0
        setContent {
            JsonForms(
                data = data, schema = schema, uischema = element,
                context = hostContext,
                additionalErrors = additionalErrors,
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf(
                    ExtendedActionHandlerConfigKey to ExtendedActionHandler { _, _ -> legacyCalls++ },
                    ExtendedActionEventHandlerConfigKey to ExtendedActionEventHandler { event -> events += event },
                ),
                i18n = JsonFormsI18nState(locale = locale),
            )
        }
        onNodeWithText("Execute").performClick()
        waitForIdle()
        assertEquals(1, events.size)
        assertEquals("save", events[0].action)
        assertEquals("Execute", events[0].label)
        assertEquals("42", events[0].params["value"]?.toString())
        assertEquals(element, events[0].element)
        assertEquals("", events[0].path)
        assertEquals(data, events[0].context.data)
        assertEquals(schema, events[0].context.schema)
        assertEquals(element, events[0].context.uischema)
        assertEquals("en", events[0].context.locale)
        assertEquals(hostContext, events[0].context.hostContext)
        assertEquals("A", (events[0].context.external as JsonObject)["tenant"]?.let { (it as JsonPrimitive).content })
        assertEquals(false, events[0].context.readonly)
        assertEquals(additionalErrors, events[0].context.additionalErrors)
        assertEquals(true, ExtendedActionEventHandlerConfigKey in events[0].context.config)
        assertEquals(0, legacyCalls)

        runOnIdle {
            data = buildJsonObject { put("revision", 2) }
            locale = "bg-BG"
            hostContext = Json.parseToJsonElement("""{"external":{"tenant":"B"},"custom":{"flag":false}}""")
            additionalErrors = listOf(ErrorObject(instancePath = "/revision", keyword = "host", message = "Second host error"))
        }
        onNodeWithText("Execute").performClick()
        waitForIdle()
        assertEquals(2, events.size)
        assertEquals("bg-BG", events[1].context.locale)
        assertEquals("2", (events[1].context.data as JsonObject)["revision"]?.toString())
        assertEquals("1", (events[0].context.data as JsonObject)["revision"]?.toString())
        assertEquals(hostContext, events[1].context.hostContext)
        assertEquals("B", (events[1].context.external as JsonObject)["tenant"]?.let { (it as JsonPrimitive).content })
        assertEquals("A", (events[0].context.external as JsonObject)["tenant"]?.let { (it as JsonPrimitive).content })
        assertEquals("First host error", events[0].context.additionalErrors.single().message)
        assertEquals("Second host error", events[1].context.additionalErrors.single().message)
        assertEquals(0, legacyCalls)
    }

    private fun keyboard(cupertino: Boolean) = runComposeUiTest {
        var disabled by mutableStateOf(false)
        val calls = mutableListOf<String>()
        setContent {
            JsonForms(
                data = buildJsonObject {}, schema = schema,
                uischema = button(action = "save", options = mapOf("disabled" to JsonPrimitive(disabled))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf(ExtendedActionHandlerConfigKey to ExtendedActionHandler { action, _ ->
                    calls += action
                }),
            )
        }
        onNodeWithText("Execute").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.Role, Role.Button,
        ))
        onNodeWithText("Execute").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithText("Execute").assertIsFocused()
        onNodeWithText("Execute").performKeyInput { keyDown(Key.Enter); keyUp(Key.Enter) }
        waitForIdle()
        onNodeWithText("Execute").performKeyInput { keyDown(Key.Spacebar); keyUp(Key.Spacebar) }
        waitForIdle()
        assertEquals(listOf("save", "save"), calls)

        runOnIdle { disabled = true }
        onNodeWithText("Execute").assertIsNotEnabled()
        onNodeWithText("Execute").performKeyInput { keyDown(Key.Enter); keyUp(Key.Enter) }
        onNodeWithText("Execute").performKeyInput { keyDown(Key.Spacebar); keyUp(Key.Spacebar) }
        waitForIdle()
        assertEquals(listOf("save", "save"), calls)
    }

    private fun disabled(cupertino: Boolean) = runComposeUiTest {
        var option by mutableStateOf<JsonPrimitive?>(null)
        var readonly by mutableStateOf(false)
        val calls = mutableListOf<String>()
        setContent {
            JsonForms(
                data = buildJsonObject {}, schema = schema,
                uischema = UnknownUiSchemaElement(
                    type = "Button",
                    options = option?.let { mapOf("disabled" to it) } ?: emptyMap(),
                    properties = buildJsonObject { put("label", "Execute"); put("action", "save") },
                ),
                readonly = readonly,
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf(ExtendedActionHandlerConfigKey to ExtendedActionHandler { action, _ ->
                    calls += action
                }),
            )
        }
        onNodeWithText("Execute").assertIsEnabled()
        runOnIdle { option = JsonPrimitive(true) }
        onNodeWithText("Execute").assertIsNotEnabled()
        runOnIdle { option = JsonPrimitive(false) }
        onNodeWithText("Execute").assertIsEnabled()
        onNodeWithText("Execute").performClick()
        waitForIdle()
        assertEquals(listOf("save"), calls)
        runOnIdle { readonly = true }
        onNodeWithText("Execute").assertIsNotEnabled()
        runOnIdle { readonly = false; option = JsonPrimitive("true") }
        onNodeWithText("Execute").assertIsEnabled()
    }

    private fun pending(cupertino: Boolean) = runComposeUiTest {
        val gate = CompletableDeferred<Unit>()
        val calls = mutableListOf<String>()
        var locale by mutableStateOf("en")
        setContent {
            JsonForms(
                data = buildJsonObject {}, schema = schema, uischema = button(action = "save"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf(ExtendedActionHandlerConfigKey to ExtendedActionHandler { action, _ ->
                    calls += action
                    gate.await()
                }),
                i18n = JsonFormsI18nState(locale = locale),
            )
        }
        onNodeWithText("Execute").assertIsEnabled()
        mainClock.autoAdvance = false
        onNodeWithText("Execute").performClick()
        mainClock.advanceTimeBy(100)
        onNodeWithText("Execute").assertIsNotEnabled()
        onNodeWithText("Execute").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription, "Running",
        ))
        assertEquals(listOf("save"), calls)
        runOnIdle { locale = "bg-BG" }
        mainClock.advanceTimeBy(100)
        onNodeWithText("Execute").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription, "Изпълнява се",
        ))
        runOnIdle { gate.complete(Unit) }
        mainClock.advanceTimeBy(100)
        mainClock.autoAdvance = true
        onNodeWithText("Execute").assertIsEnabled()
        onNodeWithText("Execute").performClick()
        waitForIdle()
        assertEquals(listOf("save", "save"), calls)
    }

    private fun button(name: String? = null, action: String? = null, script: String? = null,
        options: Map<String, JsonPrimitive> = emptyMap()) =
        UnknownUiSchemaElement("Button", options = options, properties = buildJsonObject {
            put("label", "Execute")
            if (name != null) put("name", name)
            if (action != null) put("action", action)
            if (script != null) put("script", script)
            put("params", buildJsonObject { put("value", 42) })
        })
}
