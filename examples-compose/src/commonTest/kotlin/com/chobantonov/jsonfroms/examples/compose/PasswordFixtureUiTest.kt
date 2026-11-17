package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.Layout
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class PasswordFixtureUiTest {
    @Test fun materialAuthoredPasswordPresentations() = fixture(false)
    @Test fun cupertinoAuthoredPasswordPresentations() = fixture(true)

    private fun controls(root: UiSchemaElement?): List<ControlElement> = when (root) {
        is ControlElement -> listOf(root)
        is Layout -> root.elements.flatMap(::controls)
        is CategorizationElement -> root.elements.flatMap(::controls)
        else -> emptyList()
    }

    private fun fixture(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-password-control" }
        val authored = controls(example.uischema)
        fun control(name: String) = authored.first { it.scope == "#/properties/$name" }
        var selected by mutableStateOf(control("password"))
        var data by mutableStateOf(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema, uischema = selected,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        fun show(name: String) { runOnIdle { selected = control(name) } }
        onNodeWithContentDescription("Show password").assertExists()
        assertEquals(JsonPrimitive("short"), data.jsonObject["password"])
        assertEquals(0, changes)
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("correct-horse")
        waitForIdle()
        assertEquals(JsonPrimitive("correct-horse"), data.jsonObject["password"])

        show("recoveryPhrase")
        onNodeWithContentDescription("Show password").assertExists()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("new recovery phrase")
        waitForIdle()
        assertEquals(JsonPrimitive("new recovery phrase"), data.jsonObject["recoveryPhrase"])

        show("verificationCode")
        onNodeWithTag("otp-password-field").assertExists()
        assertEquals(JsonPrimitive("4829"), data.jsonObject["verificationCode"])
        onNodeWithTag("otp-password-field").performTextReplacement("482910")
        waitForIdle()
        assertEquals(JsonPrimitive("482910"), data.jsonObject["verificationCode"])

        show("backupCode")
        onNodeWithTag("otp-password-field").assertDoesNotExist()
        onNodeWithContentDescription("Show password").assertExists()
        show("hint")
        onAllNodes(hasContentDescription("Show password")).assertCountEquals(0)

        show("serviceAccounts")
        onAllNodes(hasContentDescription("Show password")).assertCountEquals(2)
        val accounts = data.jsonObject["serviceAccounts"] as JsonArray
        assertEquals(JsonPrimitive("sk-live-0987654321"), accounts[1].jsonObject["token"])
        onAllNodes(hasSetTextAction()).onLast().performTextReplacement("sk-live-updated")
        waitForIdle()
        val updated = data.jsonObject["serviceAccounts"] as JsonArray
        assertEquals(JsonPrimitive("sk-live-updated"), updated[1].jsonObject["token"])
        assertEquals(4, changes)
    }
}
