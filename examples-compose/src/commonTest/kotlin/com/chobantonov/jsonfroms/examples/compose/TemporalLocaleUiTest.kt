package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.store.ValidationMode
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class TemporalLocaleUiTest {
    @Test fun materialDateUsesLocaleDefaults() = temporal(false, "date")
    @Test fun cupertinoDateUsesLocaleDefaults() = temporal(true, "date")
    @Test fun materialTimeUsesLocaleDefaults() = temporal(false, "time")
    @Test fun cupertinoTimeUsesLocaleDefaults() = temporal(true, "time")
    @Test fun materialDateTimeUsesLocaleDefaults() = temporal(false, "date-time")
    @Test fun cupertinoDateTimeUsesLocaleDefaults() = temporal(true, "date-time")

    private fun temporal(cupertino: Boolean, format: String) = runComposeUiTest {
        val displayFormat = when (format) {
            "date" -> "DD.MM.YYYY"
            "time" -> "HH:mm"
            else -> "DD.MM.YYYY HH:mm"
        }
        val formatOption = when (format) {
            "date" -> "dateFormat"
            "time" -> "timeFormat"
            else -> "dateTimeFormat"
        }
        val englishAction = when (format) {
            "date" -> "Choose date"
            "time" -> "Choose time"
            else -> "Choose date and time"
        }
        val bulgarianAction = when (format) {
            "date" -> "Избери дата"
            "time" -> "Избери час"
            else -> "Избери дата и час"
        }
        val englishError = when (format) {
            "date" -> "Invalid date. Expected $displayFormat."
            "time" -> "Invalid time. Expected $displayFormat."
            else -> "Invalid date and time. Expected $displayFormat."
        }
        val bulgarianError = when (format) {
            "date" -> "Невалидна дата. Очакван формат: $displayFormat."
            "time" -> "Невалиден час. Очакван формат: $displayFormat."
            else -> "Невалидна дата и час. Очакван формат: $displayFormat."
        }
        var locale by mutableStateOf("en-US")
        setContent {
            JsonForms(data = JsonPrimitive("bad"),
                schema = buildJsonSchema { put("type", "string") },
                uischema = ControlElement(scope = "#", options = mapOf(
                    "format" to JsonPrimitive(format),
                    formatOption to JsonPrimitive(displayFormat),
                    "mask" to JsonPrimitive(false),
                    "showActions" to JsonPrimitive(true),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                validationMode = ValidationMode.NoValidation,
                i18n = JsonFormsI18nState(locale = locale))
        }
        onNodeWithContentDescription(englishAction).assertExists()
        onNodeWithText(englishError).assertExists()
        runOnIdle { locale = "bg-BG" }
        onNodeWithContentDescription(bulgarianAction).assertExists()
        onNodeWithText(bulgarianError).assertExists()
        onNodeWithContentDescription(bulgarianAction).performClick()
        onNodeWithText("Отказ").performClick()
        onNodeWithText(bulgarianError).assertExists()
    }
}
