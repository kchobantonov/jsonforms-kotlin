package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.LabelElement
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.store.JsonFormsUISchemaRegistryEntry
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class ArrayRowDetailDialogUiTest {
    @Test fun materialDialogRestoresFocusToEditTrigger() = dialogFocus(false)
    @Test fun cupertinoDialogRestoresFocusToEditTrigger() = dialogFocus(true)

    private fun dialogFocus(cupertino: Boolean) = runComposeUiTest {
        setContent {
            JsonForms(data = initial, schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onNodeWithText("Edit details")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithText("Edit details").assertIsFocused()
        onNodeWithText("Edit details").performClick()
        onNodeWithTag("row-detail-dialog-frame").assertIsFocused()
        onNodeWithText("Cancel").performClick()
        onNodeWithText("Edit details").assertIsFocused()
    }

    @Test fun materialDialogActionsFollowBulgarianLocale() = dialogLocale(false)
    @Test fun cupertinoDialogActionsFollowBulgarianLocale() = dialogLocale(true)

    private fun dialogLocale(cupertino: Boolean) = runComposeUiTest {
        var locale by mutableStateOf("en")
        var writes = 0
        setContent {
            JsonForms(data = initial, schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale), onChange = { writes++ })
        }
        onNodeWithText("Edit details").assertExists()
        runOnIdle { locale = "bg-BG" }
        onNodeWithText("Редактиране на подробностите").performClick()
        onNodeWithText("Подробности за елемент 1").assertExists()
        onNodeWithText("Приложи").assertExists()
        onNodeWithText("Отказ").performClick()
        assertEquals(0, writes)
    }

    @Test fun materialRowMarkerUsesBuiltInBulgarianLabel() = builtInRowLabel(false)
    @Test fun cupertinoRowMarkerUsesBuiltInBulgarianLabel() = builtInRowLabel(true)

    private fun builtInRowLabel(cupertino: Boolean) = runComposeUiTest {
        val constrained = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"name":{"type":"string","minLength":2},"note":{"type":"string"}}}}}}"""
        ).jsonObject)
        var locale by mutableStateOf("en")
        var writes = 0
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"rows":[{"name":"A","note":"private"}]}"""),
                schema = constrained,
                uischema = ui.copy(options = ui.options + mapOf(
                    "showValidationIndicator" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { writes++ })
        }
        onNodeWithContentDescription("Item 1: 1 error").assertExists()
        runOnIdle { locale = "bg-BG" }
        onNodeWithContentDescription("Елемент 1: 1 грешка").assertExists()
        assertEquals(0, writes)
    }

    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"},"note":{"type":"string"}}}},"sibling":{"type":"string"}}}"""
    ).jsonObject)
    private val initial = Json.parseToJsonElement(
        """{"rows":[{"name":"Ada","note":"private"}],"sibling":"one"}"""
    )
    private val ui = ControlElement("#/properties/rows", options = mapOf(
        "table" to JsonPrimitive(true),
        "columnDefs" to Json.parseToJsonElement("""[{"field":"name"}]"""),
        "rowDetail" to Json.parseToJsonElement("""{
          "presentation":"dialog",
          "detail":{"type":"VerticalLayout","elements":[
            {"type":"Control","scope":"#/properties/name"},
            {"type":"Control","scope":"#/properties/note"}
          ]}
        }"""),
    ))

    @Test fun materialCancelAndApplyWholeRowDraft() = edit(false)
    @Test fun cupertinoCancelAndApplyWholeRowDraft() = edit(true)
    @Test fun materialNestedListScrollKeepsDetailDraft() = nestedList(false)
    @Test fun cupertinoNestedListScrollKeepsDetailDraft() = nestedList(true)

    private fun nestedList(cupertino: Boolean) = runComposeUiTest {
        val nestedSchema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"},"tasks":{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"}}}}}}}}}"""
        ).jsonObject)
        val tasks = (1..30).joinToString(prefix = "[", postfix = "]") { """{"name":"Task $it"}""" }
        val initial = Json.parseToJsonElement("""{"rows":[{"name":"Parent","tasks":$tasks}]}""")
        val nestedUi = ControlElement("#/properties/rows", options = mapOf(
            "table" to JsonPrimitive(true),
            "columnDefs" to Json.parseToJsonElement("""[{"field":"name"}]"""),
            "rowDetail" to Json.parseToJsonElement("""{
              "presentation":"dialog",
              "detail":{"type":"ListWithDetail","scope":"#/properties/tasks",
                "options":{"elementLabelProp":"name"}}
            }"""),
        ))
        var changes = 0
        var pixelsPerDp = 1f
        setContent {
            val density = LocalDensity.current.density
            SideEffect { pixelsPerDp = density }
            JsonForms(
                data = initial, schema = nestedSchema, uischema = nestedUi,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++ },
            )
        }
        onNodeWithText("Edit details").performClick()
        val scroll = onNodeWithTag("list-detail-navigation-scroll")
        assertTrue(scroll.fetchSemanticsNode().boundsInRoot.height <= 384f * pixelsPerDp + 2f)
        onNodeWithText("Task 1").performClick()
        val editor = onAllNodes(hasSetTextAction() and hasText("Task 1")).onFirst()
        val editorBounds = editor.fetchSemanticsNode().boundsInRoot
        fun offset() = scroll.fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange].value()
        val before = offset()
        scroll.performTouchInput { swipeUp() }
        waitForIdle()
        assertTrue(offset() > before)
        assertEquals(editorBounds, editor.fetchSemanticsNode().boundsInRoot)
        assertEquals(0, changes)
        onNodeWithText("Cancel").performClick()
        assertEquals(0, changes)
    }

    private fun edit(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(initial)
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ },
            )
        }
        onNodeWithText("Note").assertDoesNotExist()
        onNodeWithText("Edit details").performClick()
        onNodeWithText("Item 1 details").assertExists()
        onNodeWithText("Note").assertExists()
        onNodeWithText("Edit details").assertIsNotEnabled()
        assertEquals(1, onAllNodes(hasSetTextAction() and hasText("Ada")).fetchSemanticsNodes().size)
        onAllNodes(hasSetTextAction() and hasText("Ada")).onFirst().assertIsEnabled()
        onAllNodes(hasSetTextAction() and hasText("private")).onFirst().performTextReplacement("discard")
        onNodeWithText("Cancel").performClick()
        waitForIdle()
        assertEquals(initial, data)
        assertEquals(0, changes)
        onNodeWithText("Edit details").assertIsEnabled()
        onAllNodes(hasSetTextAction() and hasText("Ada")).onFirst().assertIsEnabled()
        onNodeWithText("Edit details").performClick()
        onAllNodes(hasSetTextAction() and hasText("private")).onFirst().performTextReplacement("applied")
        onNodeWithText("Apply").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement(
            """{"rows":[{"name":"Ada","note":"applied"}],"sibling":"one"}"""
        ), data)
        assertEquals(1, changes)
    }

    @Test fun materialEqualExternalRowReplacementClosesDraft() = externalReplacement(false)
    @Test fun cupertinoEqualExternalRowReplacementClosesDraft() = externalReplacement(true)

    @Test fun materialRegistryLooksUpOriginalRowPath() = registryDetail(false)
    @Test fun cupertinoRegistryLooksUpOriginalRowPath() = registryDetail(true)

    private fun registryDetail(cupertino: Boolean) = runComposeUiTest {
        val registry = listOf(
            JsonFormsUISchemaRegistryEntry(
                tester = { _, _, path -> if (path == "rows.0") 10 else -1 },
                uischema = LabelElement("First row detail"),
            ),
            JsonFormsUISchemaRegistryEntry(
                tester = { _, _, path -> if (path == "rows.1") 10 else -1 },
                uischema = VerticalLayout(elements = listOf(
                    LabelElement("Second row detail"), ControlElement("#/properties/note"),
                )),
            ),
        )
        setContent {
            JsonForms(
                data = Json.parseToJsonElement(
                    """{"rows":[{"name":"Ada","note":"one"},{"name":"Bob","note":"two"}],"sibling":"s"}"""
                ),
                schema = schema,
                uischema = ControlElement("#/properties/rows", options = mapOf(
                    "table" to JsonPrimitive(true),
                    "columnDefs" to Json.parseToJsonElement("""[{"field":"name"}]"""),
                    "rowDetail" to Json.parseToJsonElement("""{"presentation":"dialog"}"""),
                )),
                uischemas = registry,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
            )
        }
        onAllNodes(hasText("Edit details")).onFirst().performClick()
        onNodeWithText("First row detail").assertExists()
        onNodeWithText("Cancel").performClick()
        onAllNodes(hasText("Edit details")).onLast().performClick()
        onNodeWithText("Second row detail").assertExists()
        onNodeWithText("First row detail").assertDoesNotExist()
        onNodeWithText("two").assertExists()
    }

    private fun externalReplacement(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(initial)
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++ },
            )
        }
        onNodeWithText("Edit details").performClick()
        onAllNodes(hasSetTextAction() and hasText("private")).onFirst().performTextReplacement("draft")
        runOnIdle {
            data = Json.parseToJsonElement(
                """{"rows":[{"name":"Ada","note":"private"}],"sibling":"two"}"""
            )
        }
        waitForIdle()
        onNodeWithText("Item 1 details").assertDoesNotExist()
        assertEquals(0, changes)
    }

    @Test fun readonlyRowCanInspectDetails() = runComposeUiTest {
        setContent {
            JsonForms(
                data = initial, schema = schema, uischema = ui,
                readonly = true, renderers = materialRenderers,
            )
        }
        onNodeWithText("View details").performClick()
        onNodeWithText("Note").assertExists()
        onNodeWithText("Apply").assertDoesNotExist()
        onNodeWithText("Close").performClick()
        onNodeWithText("Item 1 details").assertDoesNotExist()
    }

    @Test fun pendingDateInputCannotBeSilentlyApplied() = runComposeUiTest {
        val dateSchema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":{"type":"string","format":"date"}}"""
        ).jsonObject)
        val original = Json.parseToJsonElement("""["2024-03-15"]""")
        var data by mutableStateOf(original)
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = dateSchema,
                uischema = ControlElement("#", options = mapOf(
                    "table" to JsonPrimitive(true),
                    "rowDetail" to Json.parseToJsonElement(
                        """{"presentation":"dialog","detail":{"type":"Control","scope":"#"}}"""
                    ),
                )),
                renderers = materialRenderers,
                onChange = { data = it.data; changes++ },
            )
        }
        onNodeWithText("Edit details").performClick()
        onAllNodes(hasSetTextAction()).onLast().performTextReplacement("03/15/202")
        waitForIdle()
        onNodeWithText("Apply").assertDoesNotExist()
        assertEquals(0, changes)
        onAllNodes(hasSetTextAction()).onLast().performTextReplacement("03/15/2024")
        waitForIdle()
        onNodeWithText("Apply").assertExists()
        onNodeWithText("Cancel").performClick()
        assertEquals(original, data)
        assertEquals(0, changes)
    }
}
