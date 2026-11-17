package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ArrayChoicesFixtureUiTest {
    @Test fun materialImportedChoicesAndTokens() = fixture(false)
    @Test fun cupertinoImportedChoicesAndTokens() = fixture(true)

    private fun fixture(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-array-choices" }
        var data by mutableStateOf(example.data)
        setContent {
            JsonForms(data = data, schema = example.schema, uischema = example.uischema,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        onNodeWithText("Choice presentations").performClick()
        onNodeWithText("Operations").assertExists()
        onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .onLast().performClick()
        assertEquals(JsonArray(listOf(JsonPrimitive("Email"), JsonPrimitive("Post"))),
            data.jsonObject["channels"])
        onNodeWithContentDescription("Add choice: Alert channels").performClick()
        mainClock.advanceTimeBy(200)
        onNodeWithContentDescription("SMS").performClick()
        assertEquals(JsonArray(listOf(JsonPrimitive("Email"), JsonPrimitive("Post"), JsonPrimitive("SMS"))),
            data.jsonObject["alerts"])
        onNodeWithContentDescription("Add choice: Notified teams").performClick()
        onNodeWithContentDescription("Finance").performClick()
        assertEquals(JsonArray(listOf(JsonPrimitive("ops"), JsonPrimitive("fin"))), data.jsonObject["teams"])
        onNodeWithText("Free tokens").performClick()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("newtag")
        onNodeWithContentDescription("Add choice: Catalog tags").performClick()
        assertEquals(JsonArray(listOf(JsonPrimitive("oversize"), JsonPrimitive("refrigerated"), JsonPrimitive("newtag"))),
            data.jsonObject["tags"])
        onAllNodesWithText("Remove choice: A-117").onLast().performClick()
        assertEquals(JsonArray(listOf(JsonPrimitive("A-117"), JsonPrimitive("B-204"))),
            data.jsonObject["scans"])
        onNodeWithContentDescription("Add choice: Printed labels")
            .performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithContentDescription("Heavy").performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(JsonArray(listOf(JsonPrimitive("Fragile"), JsonPrimitive("Heavy"))),
            data.jsonObject["labels"])
        onNodeWithText("Bounds and invalid values").performClick()
        onNodeWithContentDescription("Add choice: Office locations").performClick()
        onNodeWithContentDescription("Felixstowe").performClick()
        assertEquals(JsonArray(listOf(JsonPrimitive("Dover"), JsonPrimitive("Felixstowe"))),
            data.jsonObject["bonded"])
        onNodeWithContentDescription("Add choice: Office locations").performClick()
        onNodeWithContentDescription("Hull").performClick()
        onNodeWithContentDescription("Add choice: Office locations").assertIsNotEnabled()
        onNodeWithText("Remove choice: Dover").performClick()
        onNodeWithText("Remove choice: Felixstowe").assertIsNotEnabled()
        assertEquals(JsonArray(listOf(JsonPrimitive("Felixstowe"), JsonPrimitive("Hull"))),
            data.jsonObject["bonded"])
        onNodeWithText("Fax").assertExists()
        onNodeWithText("Remove choice: Fax").performClick()
        assertEquals(JsonArray(listOf(JsonPrimitive("Email"))), data.jsonObject["imported"])
    }
}
