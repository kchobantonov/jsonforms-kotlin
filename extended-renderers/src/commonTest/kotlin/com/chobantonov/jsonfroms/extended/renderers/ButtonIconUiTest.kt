package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.UnknownUiSchemaElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@OptIn(ExperimentalTestApi::class)
class ButtonIconUiTest {
    @Test fun materialUsesOnlyTopLevelDecorativeIcon() = icon(false)
    @Test fun cupertinoUsesOnlyTopLevelDecorativeIcon() = icon(true)

    private fun icon(cupertino: Boolean) = runComposeUiTest {
        var icon by mutableStateOf<String?>(null)
        val calls = mutableListOf<String>()
        setContent {
            JsonForms(
                data = buildJsonObject {},
                schema = JsonSchema(buildJsonObject { put("type", "object") }),
                uischema = UnknownUiSchemaElement(
                    type = "Button",
                    options = mapOf("icon" to JsonPrimitive("wrong")),
                    properties = buildJsonObject {
                        put("label", "Run")
                        put("action", "run")
                        icon?.let { put("icon", it) }
                    },
                ),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf(ExtendedActionHandlerConfigKey to ExtendedActionHandler { action, _ ->
                    calls += action
                }),
            )
        }
        fun buttonWidth() = onNode(hasClickAction()).fetchSemanticsNode().boundsInRoot.width
        val withoutIcon = buttonWidth()
        onNodeWithText("wrong").assertDoesNotExist()

        runOnIdle { icon = "+" }
        val withIcon = buttonWidth()
        assertTrue(withIcon > withoutIcon + 8f, "The icon should add visible width")
        onNodeWithText("+").assertDoesNotExist()
        onNodeWithText("Run").performClick()
        waitForIdle()
        assertEquals(listOf("run"), calls)

        runOnIdle { icon = null }
        assertEquals(withoutIcon, buttonWidth(), 1f)
    }
}
