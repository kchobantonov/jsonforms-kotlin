package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.LabelElement
import com.chobantonov.jsonfroms.core.models.ListWithDetailElement
import com.chobantonov.jsonfroms.core.store.JsonFormsUISchemaRegistryEntry
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class ListDetailIdentityUiTest {
    @Test fun movingASelectedRowRetainsItsDetailEditor() = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"}}}}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement(
            """{"rows":[{"name":"Ada"},{"name":"Bob"}]}"""
        ))
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ListWithDetailElement("#/properties/rows", options = mapOf(
                    "elementLabelProp" to JsonPrimitive("name"),
                    "showSortButtons" to JsonPrimitive(true),
                    "detail" to Json.parseToJsonElement(
                        """{"type":"Control","scope":"#/properties/name"}"""
                    ),
                )),
                renderers = materialRenderers, onChange = { data = it.data },
            )
        }
        onNodeWithText("Bob").performClick()
        val editor = onAllNodes(hasSetTextAction()).onFirst().fetchSemanticsNode().id
        onAllNodes(hasContentDescription("Move up"))[1].performClick()
        waitForIdle()
        assertEquals(editor, onAllNodes(hasSetTextAction()).onFirst().fetchSemanticsNode().id)
        assertEquals(Json.parseToJsonElement("""{"rows":[{"name":"Bob"},{"name":"Ada"}]}"""), data)
    }

    @Test fun movingStructurallyEqualRowsStillPublishesAndMovesSelection() = equalRows(false)
    @Test fun cupertinoMovingEqualRowsKeepsPathSpecificDetail() = equalRows(true)

    private fun equalRows(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"}}}}}}"""
        ).jsonObject)
        val initial = Json.parseToJsonElement("""{"rows":[{"name":"same"},{"name":"same"}]}""")
        var data by mutableStateOf(initial)
        var published: kotlinx.serialization.json.JsonElement? = null
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ListWithDetailElement("#/properties/rows", options = mapOf(
                    "elementLabelProp" to JsonPrimitive("name"),
                    "showSortButtons" to JsonPrimitive(true),
                )),
                uischemas = listOf(JsonFormsUISchemaRegistryEntry(
                    tester = { _, _, path -> if (path == "rows.0") 10 else -1 },
                    uischema = LabelElement("First path detail"),
                ), JsonFormsUISchemaRegistryEntry(
                    tester = { _, _, path -> if (path == "rows.1") 10 else -1 },
                    uischema = LabelElement("Second path detail"),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { published = it.data; data = it.data; changes++ },
            )
        }
        onAllNodes(hasClickAction() and hasText("same"))[1].performClick()
        onNodeWithText("Second path detail").assertExists()
        val beforeRows = initial.jsonObject["rows"] as JsonArray
        onAllNodes(hasContentDescription("Move up"))[1].performClick()
        waitForIdle()
        assertEquals(1, changes)
        onNodeWithText("No selection").assertDoesNotExist()
        onNodeWithText("Second path detail").assertDoesNotExist()
        onNodeWithText("First path detail").assertExists()
        val publishedRows = published!!.jsonObject["rows"] as JsonArray
        assertEquals(true, publishedRows[0] === beforeRows[1])
    }

    @Test fun externalUpdateFollowsUniqueSelectedObjectReference() = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"}}}}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement(
            """{"rows":[{"name":"Ada"},{"name":"Bob"}]}"""
        ))
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ListWithDetailElement("#/properties/rows", options = mapOf(
                    "elementLabelProp" to JsonPrimitive("name"),
                )),
                uischemas = listOf(JsonFormsUISchemaRegistryEntry(
                    tester = { _, _, path -> if (path == "rows.0") 10 else -1 },
                    uischema = LabelElement("First path detail"),
                ), JsonFormsUISchemaRegistryEntry(
                    tester = { _, _, path -> if (path == "rows.1") 10 else -1 },
                    uischema = LabelElement("Second path detail"),
                )),
                renderers = materialRenderers, onChange = { changes++ },
            )
        }
        onNodeWithText("Bob").performClick()
        onNodeWithText("Second path detail").assertExists()
        val oldRows = data.jsonObject["rows"] as JsonArray
        data = JsonObject(mapOf("rows" to JsonArray(listOf(oldRows[1]))))
        waitForIdle()
        onNodeWithText("First path detail").assertExists()
        onNodeWithText("No selection").assertDoesNotExist()
        assertEquals(0, changes)
    }

    @Test fun externalEquivalentRowReplacementClearsSelection() = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"}}}},"sibling":{"type":"string"}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement(
            """{"rows":[{"name":"Ada"},{"name":"Bob"}],"sibling":"before"}"""
        ))
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ListWithDetailElement("#/properties/rows", options = mapOf(
                    "elementLabelProp" to JsonPrimitive("name"),
                )),
                uischemas = listOf(JsonFormsUISchemaRegistryEntry(
                    tester = { _, _, path -> if (path == "rows.1") 10 else -1 },
                    uischema = LabelElement("Selected detail"),
                )),
                renderers = materialRenderers, onChange = { changes++ },
            )
        }
        onNodeWithText("Bob").performClick()
        onNodeWithText("Selected detail").assertExists()
        data = Json.parseToJsonElement(
            """{"rows":[{"name":"Ada"},{"name":"Bob"}],"sibling":"after"}"""
        )
        waitForIdle()
        onNodeWithText("No selection").assertExists()
        onNodeWithText("Selected detail").assertDoesNotExist()
        assertEquals(0, changes)
    }

    @Test fun preferredRestrictionHonorsMinAndMaxItems() = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","minItems":2,"maxItems":2,"items":{"type":"object","properties":{"name":{"type":"string"}}}}"""
        ).jsonObject)
        val data = Json.parseToJsonElement("""[{"name":"Ada"},{"name":"Bob"}]""")
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ListWithDetailElement("#", options = mapOf(
                    "elementLabelProp" to JsonPrimitive("name"),
                    "showSortButtons" to JsonPrimitive(true),
                )),
                renderers = materialRenderers, onChange = { changes++ },
            )
        }
        onAllNodes(hasContentDescription("Add item"))[0].assertIsNotEnabled()
        onAllNodes(hasContentDescription("Delete item"))[0].assertIsNotEnabled()
        onAllNodes(hasContentDescription("Delete item"))[1].assertIsNotEnabled()
        assertEquals(0, changes)
    }

    @Test fun movesAndDeletesPreserveTheSelectedOccurrence() = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"}}}}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"rows":[{"name":"Ada"},{"name":"Bob"},{"name":"Cyd"}]}"""))
        var changes = 0
        val registry = (0..2).map { index ->
            JsonFormsUISchemaRegistryEntry(
                tester = { _, _, path -> if (path == "rows.$index") 10 else -1 },
                uischema = LabelElement("Detail $index"),
            )
        }
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ListWithDetailElement("#/properties/rows", options = mapOf(
                    "elementLabelProp" to JsonPrimitive("name"),
                    "showSortButtons" to JsonPrimitive(true),
                )),
                uischemas = registry, renderers = materialRenderers,
                onChange = { data = it.data; changes++ },
            )
        }
        onNodeWithText("No selection").assertExists()
        waitForIdle()
        onNodeWithText("Bob").performClick()
        onNodeWithText("Detail 1").assertExists()
        assertEquals(0, changes)

        onAllNodes(hasContentDescription("Move up"))[1].performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"rows":[{"name":"Bob"},{"name":"Ada"},{"name":"Cyd"}]}"""), data)
        onNodeWithText("Detail 0").assertExists()

        onAllNodes(hasContentDescription("Delete item"))[1].performClick()
        onNodeWithText("Yes").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"rows":[{"name":"Bob"},{"name":"Cyd"}]}"""), data)
        onNodeWithText("Detail 0").assertExists()

        onAllNodes(hasContentDescription("Delete item"))[0].performClick()
        onNodeWithText("Yes").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"rows":[{"name":"Cyd"}]}"""), data)
        onNodeWithText("No selection").assertExists()
        assertEquals(3, changes)
    }

    @Test fun materialConfirmationCanCancelAndThenDeleteSelectedRow() = confirmationCanCancel(false)
    @Test fun cupertinoConfirmationCanCancelAndThenDeleteSelectedRow() = confirmationCanCancel(true)

    private fun confirmationCanCancel(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"}}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""[{"name":"Ada"},{"name":"Bob"}]"""))
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ListWithDetailElement("#", options = mapOf(
                    "elementLabelProp" to JsonPrimitive("name"),
                    "confirmation" to Json.parseToJsonElement("""{"delete":"always"}"""),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ },
            )
        }
        onNodeWithText("Ada").performClick()
        onAllNodes(hasContentDescription("Delete item"))[0].performClick()
        onNodeWithText("Delete this value?").assertExists()
        onNodeWithText("No").performClick()
        assertEquals(0, changes)
        onAllNodes(hasContentDescription("Delete item"))[0].performClick()
        onNodeWithText("Yes").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""[{"name":"Bob"}]"""), data)
        onNodeWithText("No selection").assertExists()
        assertEquals(1, changes)
    }
}
