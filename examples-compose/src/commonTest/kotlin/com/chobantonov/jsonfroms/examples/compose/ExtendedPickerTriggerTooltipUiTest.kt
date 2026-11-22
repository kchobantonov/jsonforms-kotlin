package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.extended.renderers.cupertinoExtendedRenderers
import com.chobantonov.jsonfroms.extended.renderers.materialExtendedRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ExtendedPickerTriggerTooltipUiTest {
    @Test fun materialDurationTooltips() = tooltips(false, "duration", "PT1H",
        "Избери продължителност")
    @Test fun cupertinoDurationTooltips() = tooltips(true, "duration", "PT1H",
        "Избери продължителност")
    @Test fun materialCronTooltips() = tooltips(false, "cron", "0 0 9 * * *",
        "Избери график")
    @Test fun cupertinoCronTooltips() = tooltips(true, "cron", "0 0 9 * * *",
        "Избери график")
    @Test fun materialColorTooltips() = tooltips(false, "color", "#2255aa",
        "Избери цвят")
    @Test fun cupertinoColorTooltips() = tooltips(true, "color", "#2255aa",
        "Избери цвят")

    private fun tooltips(cupertino: Boolean, format: String, value: String,
        pickerLabel: String) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"string","format":"$format"}}
        }""").jsonObject)
        var writes = 0
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"value":"$value"}"""),
                schema = schema, uischema = ControlElement("#/properties/value"),
                i18n = JsonFormsI18nState(locale = "bg"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { writes++ })
        }
        onNodeWithContentDescription("Изчисти стойността").assertDoesNotExist()
        onNodeWithContentDescription(pickerLabel).performMouseInput { moveTo(center) }
        waitForIdle()
        onNodeWithText(pickerLabel).assertExists()
        onNodeWithContentDescription("Изчисти стойността").performMouseInput { moveTo(center) }
        waitForIdle()
        onNodeWithText("Изчисти стойността").assertExists()
        assertEquals(0, writes)
    }
}
