package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.Rule
import com.chobantonov.jsonfroms.core.models.RuleEffect
import com.chobantonov.jsonfroms.core.models.SchemaBasedCondition
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

@OptIn(ExperimentalTestApi::class)
class CategoryDataPresenceUiTest {
    @Test fun materialTabsIncludeHiddenBoundData() = scenario(false, "tabs")
    @Test fun cupertinoTabsIncludeHiddenBoundData() = scenario(true, "tabs")
    @Test fun materialStepsIncludeHiddenBoundData() = scenario(false, "stepper")
    @Test fun cupertinoStepsIncludeHiddenBoundData() = scenario(true, "stepper")
    @Test fun materialAccordionIncludesClosedBoundData() = scenario(false, "accordion")
    @Test fun cupertinoAccordionIncludesClosedBoundData() = scenario(true, "accordion")

    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"a":{"type":"string"},"b":{"type":"number"},"other":{"type":"string"},"show":{"type":"boolean"}}}"""
    ).jsonObject)
    private val hiddenRule = Rule(
        effect = RuleEffect.SHOW,
        condition = SchemaBasedCondition(
            scope = "#/properties/show",
            schema = buildJsonSchema { put("const", true) },
            failWhenUndefined = true,
        ),
    )
    private fun ui(variant: String, indicator: Boolean? = null) = CategorizationElement(
        options = (if (variant == "tabs") emptyMap() else mapOf("variant" to JsonPrimitive(variant))) +
            mapOf("initial" to JsonPrimitive("first")),
        elements = listOf(
            CategoryElement(label = "First", name = "first",
                elements = listOf(ControlElement(scope = "#/properties/a"))),
            CategoryElement(
                label = "Second", name = "second",
                options = if (indicator == null) emptyMap() else
                    mapOf("showDataIndicator" to JsonPrimitive(indicator)),
                elements = listOf(ControlElement(scope = "#/properties/b", rule = hiddenRule)),
            ),
        ),
    )

    private fun scenario(cupertino: Boolean, variant: String) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement(
            """{"a":" ","b":0,"other":"unrelated","show":false}"""
        ))
        var layout by mutableStateOf(ui(variant))
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = layout,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf("jsonformsExtended" to mapOf("showDataIndicator" to true)),
                i18n = JsonFormsI18nState(locale = locale, translate = { key, fallback, _ ->
                    if (key == "group.dataIndicator" && locale == "bg") "Съдържа данни" else fallback
                }),
                onChange = { changes++ })
        }
        onNodeWithContentDescription("Second, Contains data").assertExists()
        onNodeWithContentDescription("First, Contains data").assertDoesNotExist()
        onNodeWithText("B").assertDoesNotExist()
        runOnIdle { locale = "bg" }
        onNodeWithContentDescription("Second, Съдържа данни").assertExists()
        runOnIdle { data = Json.parseToJsonElement(
            """{"a":" ","b":null,"other":"unrelated","show":false}"""
        ) }
        onNodeWithContentDescription("Second, Съдържа данни").assertDoesNotExist()
        runOnIdle { data = Json.parseToJsonElement(
            """{"a":" ","b":0,"other":"unrelated","show":false}"""
        ) }
        onNodeWithContentDescription("Second, Съдържа данни").assertExists()
        runOnIdle { layout = ui(variant, indicator = false) }
        onNodeWithContentDescription("Second, Съдържа данни").assertDoesNotExist()
        assertEquals(0, changes)
    }
}
