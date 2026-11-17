package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.Rule
import com.chobantonov.jsonfroms.core.models.RuleEffect
import com.chobantonov.jsonfroms.core.models.SchemaBasedCondition
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

@OptIn(ExperimentalTestApi::class)
class VisibleEmptyContainerUiTest {
    @Test fun materialGroupKeepsHeadingWhenOnlyChildHides() = group(false)
    @Test fun cupertinoGroupKeepsHeadingWhenOnlyChildHides() = group(true)
    @Test fun materialTabsKeepEmptyCategorySelectable() = category(false, false)
    @Test fun cupertinoTabsKeepEmptyCategorySelectable() = category(true, false)
    @Test fun materialStepsKeepEmptyCategorySelectable() = category(false, true)
    @Test fun cupertinoStepsKeepEmptyCategorySelectable() = category(true, true)

    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"first":{"type":"string"},"second":{"type":"string"},"show":{"type":"boolean"}}}"""
    ).jsonObject)
    private val childRule = Rule(
        effect = RuleEffect.SHOW,
        condition = SchemaBasedCondition(
            scope = "#/properties/show",
            schema = buildJsonSchema { put("const", true) },
            failWhenUndefined = true,
        ),
    )
    private val hiddenChild = ControlElement(scope = "#/properties/first", rule = childRule)

    private fun group(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"show":true}"""))
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = GroupElement(label = "Visible group", elements = listOf(hiddenChild)),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
            )
        }
        onNodeWithText("First").assertExists()
        runOnIdle { data = Json.parseToJsonElement("""{"show":false}""") }
        onNodeWithText("Visible group").assertExists()
        onNodeWithText("First").assertDoesNotExist()
        runOnIdle { data = Json.parseToJsonElement("""{"show":true}""") }
        onNodeWithText("First").assertExists()
    }

    private fun category(cupertino: Boolean, stepper: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"show":true}"""))
        val options = if (stepper) mapOf("variant" to JsonPrimitive("stepper")) else emptyMap()
        val ui = CategorizationElement(
            options = options,
            elements = listOf(
                CategoryElement(label = "Empty candidate", name = "empty",
                    elements = listOf(hiddenChild)),
                CategoryElement(label = "Other category", name = "other",
                    elements = listOf(ControlElement(scope = "#/properties/second"))),
            ),
        )
        setContent {
            JsonForms(data = data, schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onNodeWithText("First").assertExists()
        runOnIdle { data = Json.parseToJsonElement("""{"show":false}""") }
        onNodeWithText("Empty candidate").assertExists()
        onNodeWithText("First").assertDoesNotExist()
        onNodeWithText("Second").assertDoesNotExist()
        onNodeWithText("Other category").performClick()
        onNodeWithText("Second").assertExists()
        onNodeWithText("Empty candidate").performClick()
        onNodeWithText("Empty candidate").assertExists()
        onNodeWithText("Second").assertDoesNotExist()
        runOnIdle { data = Json.parseToJsonElement("""{"show":true}""") }
        onNodeWithText("First").assertExists()
    }
}
