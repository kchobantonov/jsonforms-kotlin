package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class MaterialDatePickerMonthJvmTest {
    @Test fun opensOnStoredDateMonth() = runComposeUiTest {
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"value":"2025-01-10"}"""),
                schema = JsonSchema(Json.parseToJsonElement(
                    """{"type":"object","properties":{"value":{"type":"string","format":"date"}}}"""
                ).jsonObject),
                uischema = ControlElement("#/properties/value"),
                renderers = materialRenderers,
            )
        }
        onNodeWithContentDescription("Choose date").performClick()
        onNodeWithText("January 2025").assertExists()
    }
}
