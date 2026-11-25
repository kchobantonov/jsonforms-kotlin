package com.chobantonov.jsonfroms.core.i18n

import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.mappers.enumToEnumOptionMapper
import com.chobantonov.jsonfroms.core.mappers.oneOfToEnumOptionMapper
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.util.SliderPositionError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class ControlRendererLocaleBundlesTest {
    @Test fun datePickerModeTooltipsUseFormLocaleAndHostOverride() {
        val english = JsonFormsI18nState(locale = "en")
        val bulgarian = JsonFormsI18nState(locale = "bg-BG")
        val inputKey = "switchToInputModeTooltip"
        val calendarKey = "switchToCalendarModeTooltip"
        assertEquals("Switch to text input mode", translateTemporalControlText(
            english, null, inputKey, "Switch to text input mode", "MM/DD/YYYY"))
        assertEquals("Превключи към въвеждане на дата", translateTemporalControlText(
            bulgarian, null, inputKey, "Switch to text input mode", "MM/DD/YYYY"))
        assertEquals("Превключи към календар", translateTemporalControlText(
            bulgarian, null, calendarKey, "Switch to calendar mode", "MM/DD/YYYY"))
        val host = JsonFormsI18nState(locale = "bg", translate = { _, _, _ -> "Host mode tooltip" })
        assertEquals("Host mode tooltip", translateTemporalControlText(
            host, null, inputKey, "Switch to text input mode", "MM/DD/YYYY"))
    }

    @Test fun emptyEnumLabelIsLocalizedWithoutChangingItsValue() {
        assertEquals("Empty value", translateControlEmptyOption(JsonFormsI18nState(locale = "en")))
        assertEquals("Празна стойност", translateControlEmptyOption(JsonFormsI18nState(locale = "bg-BG")))
        assertEquals("Empty value", translateControlEmptyOption(JsonFormsI18nState(locale = "de-DE")))
        val i18n = JsonFormsI18nState(locale = "bg", translate = { key, fallback, _ ->
            if (key == "choice.enum.emptyOption") "Host empty" else fallback
        })
        val option = enumToEnumOptionMapper(JsonPrimitive(""), i18n.translate, "choice",
            translateControlEmptyOption(i18n))
        assertEquals("Host empty", option.label)
        assertEquals(JsonPrimitive(""), option.value)
        val oneOfOption = oneOfToEnumOptionMapper(
            JsonSchema(Json.parseToJsonElement("""{"const":""}""").jsonObject),
            i18n.translate, "choice", translateControlEmptyOption(i18n))
        assertEquals("Host empty", oneOfOption.label)
        assertEquals(JsonPrimitive(""), oneOfOption.value)
    }

    @Test fun requiredStateUsesEnglishBulgarianAndHostOverride() {
        assertEquals("Required field", translateControlRequired(JsonFormsI18nState(locale = "en-US")))
        assertEquals("Задължително поле", translateControlRequired(JsonFormsI18nState(locale = "bg-BG")))
        assertEquals("Required field", translateControlRequired(JsonFormsI18nState(locale = "de-DE")))
        assertEquals("Host required", translateControlRequired(JsonFormsI18nState(
            locale = "bg", translate = { key, fallback, _ ->
                assertEquals("control.required", key)
                assertEquals("Задължително поле", fallback)
                "Host required"
            })))
    }

    @Test fun sliderStatusAndDiagnosticsUseEnglishBulgarianFallback() {
        val bulgarian = JsonFormsI18nState(locale = "bg-BG")
        val unsupported = JsonFormsI18nState(locale = "de-DE")
        assertEquals("Не е зададено", translateSliderStatus(bulgarian, null, invalid = false))
        assertEquals("Текуща стойност: 2", translateSliderStatus(bulgarian, "2", invalid = false))
        assertEquals("Невалидна текуща стойност: \"3\"",
            translateSliderStatus(bulgarian, "\"3\"", invalid = true))
        assertEquals("Няма избираема стойност в диапазона на плъзгача.",
            translateSliderDiagnostic(bulgarian, SliderPositionError.NoPosition))
        assertEquals("No selectable value in slider range.",
            translateSliderDiagnostic(unsupported, SliderPositionError.NoPosition))
    }

    @Test fun numericActionsUseEnglishAndBulgarianOnly() {
        val english = JsonFormsI18nState(locale = "en-US")
        val bulgarian = JsonFormsI18nState(locale = "bg-BG")
        val unsupported = JsonFormsI18nState(locale = "de-DE")
        assertEquals("Increase value", translateControlLocalError(english,
            "input.increment", "Increase value"))
        assertEquals("Увеличи стойността", translateControlLocalError(bulgarian,
            "input.increment", "Increase value"))
        assertEquals("Намали стойността", translateControlLocalError(bulgarian,
            "input.decrement", "Decrease value"))
        assertEquals("Невалидна числова стъпка.", translateControlLocalError(bulgarian,
            "input.invalidStep", "Invalid numeric step."))
        assertEquals("Increase value", translateControlLocalError(unsupported,
            "input.increment", "Increase value"))
    }

    @Test fun booleanNotSetUsesOnlyEnglishAndBulgarianDefaults() {
        assertEquals("Not set", translateBooleanNotSet(JsonFormsI18nState(locale = "en-US")))
        assertEquals("Не е зададено", translateBooleanNotSet(JsonFormsI18nState(locale = "bg-BG")))
        assertEquals("Not set", translateBooleanNotSet(JsonFormsI18nState(locale = "de-DE")))
        assertEquals("Host pending", translateBooleanNotSet(JsonFormsI18nState(
            locale = "bg", translate = { key, _, _ ->
                assertEquals("boolean.notSet", key)
                "Host pending"
            })))
    }

    @Test fun regionalFallbackAndDocumentedDefault() {
        assertEquals("Clear value", translateControlClear(JsonFormsI18nState(locale = "en-US")))
        assertEquals("Изчисти стойността", translateControlClear(JsonFormsI18nState(locale = "bg-BG")))
        assertEquals("Clear value", translateControlClear(JsonFormsI18nState(locale = "fr-FR")))
        assertEquals("Невалидно цяло число.", translateControlLocalError(
            JsonFormsI18nState(locale = "bg-BG"), "input.invalidInteger", "Invalid integer."))
        assertEquals("Невалидно число.", translateControlLocalError(
            JsonFormsI18nState(locale = "bg-BG"), "input.invalidNumber", "Invalid number."))
        assertEquals("Изберете валидна опция.", translateControlLocalError(
            JsonFormsI18nState(locale = "bg"), "enum.invalidOption", "Select a valid option."))
    }

    @Test fun formTranslatorKeepsExistingClearKeyAndWinsOverBundle() {
        var receivedDefault: String? = null
        var receivedControl: Any? = null
        val keys = mutableListOf<String>()
        val i18n = JsonFormsI18nState(locale = "bg-BG", translate = { key, fallback, values ->
            keys += key
            receivedDefault = fallback
            receivedControl = values["control"]
            if (key == "clear") "Custom clear" else fallback
        })
        assertEquals("Custom clear", translateControlClear(i18n, mapOf("control" to "Age")))
        assertEquals(listOf("control.clear", "clear"), keys)
        assertEquals("Изчисти стойността", receivedDefault)
        assertEquals("Age", receivedControl)
    }

    @Test fun scopedFormKeyTakesPrecedenceOverLegacyAlias() {
        val keys = mutableListOf<String>()
        val i18n = JsonFormsI18nState(locale = "bg-BG", translate = { key, fallback, _ ->
            keys += key
            if (key == "control.clear") "Scoped clear" else fallback
        })
        assertEquals("Scoped clear", translateControlClear(i18n))
        assertEquals(listOf("control.clear"), keys)
    }

    @Test fun formTranslatorOverridesLocalErrorAfterBundleSelection() {
        var receivedDefault: String? = null
        val i18n = JsonFormsI18nState(locale = "bg-BG", translate = { key, fallback, _ ->
            assertEquals("input.invalidNumber", key)
            receivedDefault = fallback
            "Host error"
        })
        assertEquals("Host error", translateControlLocalError(i18n,
            "input.invalidNumber", "Invalid number."))
        assertEquals("Невалидно число.", receivedDefault)
    }

    @Test fun passwordActionUsesScopedKeysAndRegionalBundle() {
        val seen = mutableMapOf<String, String?>()
        val i18n = JsonFormsI18nState(locale = "bg-BG", translate = { key, fallback, _ ->
            seen[key] = fallback
            if (key == "secret.showPasswordAriaLabel") "Reveal secret" else fallback
        })
        assertEquals(PasswordActionText("Покажи паролата", "Reveal secret"),
            translatePasswordActionText(i18n, "secret", visible = false))
        assertEquals("Покажи паролата", seen["secret.showPasswordTooltip"])
        assertEquals("Покажи паролата", seen["secret.showPasswordAriaLabel"])
        assertEquals(PasswordActionText("Скрий паролата", "Скрий паролата"),
            translatePasswordActionText(JsonFormsI18nState(locale = "bg-BG"), null, visible = true))
        assertEquals(PasswordActionText("Show password", "Show password"),
            translatePasswordActionText(JsonFormsI18nState(locale = "fr"), null, visible = false))
    }

    @Test fun temporalDefaultsIncludeAuthoredFormatAndPreserveScopedOverrides() {
        val keys = listOf("chooseDateTooltip", "chooseDateAriaLabel", "invalidDate", "selectDate",
            "chooseTimeTooltip", "chooseTimeAriaLabel", "invalidTime", "selectTime",
            "chooseDateTimeTooltip", "chooseDateTimeAriaLabel", "invalidDateTime", "selectDateTime",
            "cancel", "ok", "next")
        for (locale in listOf("bg", "bg-BG")) for (key in keys) {
            val value = translateTemporalControlText(JsonFormsI18nState(locale = locale), null,
                key, "English default", "DD.MM.YYYY")
            assertEquals(false, value == "English default", "$locale: $key")
            if (key.startsWith("invalid")) assertEquals(true, "DD.MM.YYYY" in value)
        }
        var receivedFallback: String? = null
        val i18n = JsonFormsI18nState(locale = "bg-BG", translate = { key, fallback, _ ->
            assertEquals("appointment.invalidDate", key)
            receivedFallback = fallback
            "Host date error"
        })
        assertEquals("Host date error", translateTemporalControlText(i18n, "appointment",
            "invalidDate", "Invalid date. Expected DD.MM.YYYY.", "DD.MM.YYYY"))
        assertEquals("Невалидна дата. Очакван формат: DD.MM.YYYY.", receivedFallback)
        assertEquals("Cancel", translateTemporalControlText(JsonFormsI18nState(locale = "fr"),
            null, "cancel", "Cancel", "DD.MM.YYYY"))
    }
}
