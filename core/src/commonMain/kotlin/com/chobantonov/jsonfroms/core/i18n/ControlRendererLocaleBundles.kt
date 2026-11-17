package com.chobantonov.jsonfroms.core.i18n

import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.util.SliderPositionError
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Default strings shared by Material and Cupertino control renderers. */
val controlRendererLocaleBundles: Map<String, JsonObject> = mapOf(
    "bg" to JsonObject(mapOf(
        "control.clear" to JsonPrimitive("Изчисти стойността"),
        "control.required" to JsonPrimitive("Задължително поле"),
        "control.invalidValue" to JsonPrimitive("Невалидна стойност"),
        "input.invalidInteger" to JsonPrimitive("Невалидно цяло число."),
        "input.invalidNumber" to JsonPrimitive("Невалидно число."),
        "input.outOfRange" to JsonPrimitive("Извън позволения диапазон."),
        "input.invalidStep" to JsonPrimitive("Невалидна числова стъпка."),
        "input.increment" to JsonPrimitive("Увеличи стойността"),
        "input.decrement" to JsonPrimitive("Намали стойността"),
        "slider.currentValue" to JsonPrimitive("Текуща стойност"),
        "slider.invalidValue" to JsonPrimitive("Невалидна текуща стойност"),
        "slider.noPositions" to JsonPrimitive("Няма избираема стойност в диапазона на плъзгача."),
        "slider.tooManyPositions" to JsonPrimitive("Твърде много позиции за плъзгача."),
        "slider.invalidRange" to JsonPrimitive("Невалиден диапазон на плъзгача."),
        "slider.invalidIncrement" to JsonPrimitive("Невалидна стъпка на плъзгача."),
        "slider.unsupportedPrecision" to JsonPrimitive("Точността на плъзгача не се поддържа."),
        "slider.selectValue" to JsonPrimitive("Избери стойност"),
        "enum.invalidOption" to JsonPrimitive("Изберете валидна опция."),
        "enum.emptyOption" to JsonPrimitive("Празна стойност"),
        "radio.unknownValue" to JsonPrimitive("Текуща стойност извън опциите"),
        "choiceGroup.horizontal" to JsonPrimitive("Хоризонтални избори"),
        "choiceGroup.vertical" to JsonPrimitive("Вертикални избори"),
        "boolean.notSet" to JsonPrimitive("Не е зададено"),
        "boolean.invalidValue" to JsonPrimitive("Невалидна стойност"),
        "password.showPasswordTooltip" to JsonPrimitive("Покажи паролата"),
        "password.showPasswordAriaLabel" to JsonPrimitive("Покажи паролата"),
        "password.hidePasswordTooltip" to JsonPrimitive("Скрий паролата"),
        "password.hidePasswordAriaLabel" to JsonPrimitive("Скрий паролата"),
    )),
)

/** Prefers the scoped form key and retains the older `clear` key as an alias. */
fun translateControlClear(i18n: JsonFormsI18nState, values: Map<String, Any?> = emptyMap()): String {
    val fallback = translateRendererText(null, i18n.locale, "control.clear", "Clear value",
        controlRendererLocaleBundles)
    val scoped = i18n.translate?.translate("control.clear", fallback, values)
    if (scoped != null && scoped != fallback) return scoped
    return i18n.translate?.translate("clear", fallback, values) ?: fallback
}

fun translateControlLocalError(i18n: JsonFormsI18nState, key: String, documentedDefault: String): String =
    translateRendererText(i18n.translate, i18n.locale, key, documentedDefault,
        controlRendererLocaleBundles)

fun translateControlEmptyOption(i18n: JsonFormsI18nState): String =
    translateRendererText(i18n.translate, i18n.locale, "enum.emptyOption", "Empty value",
        controlRendererLocaleBundles)

fun translateRadioUnknownValue(i18n: JsonFormsI18nState): String =
    translateRendererText(i18n.translate, i18n.locale, "radio.unknownValue",
        "Current value outside choices", controlRendererLocaleBundles)

fun translateChoiceGroupOrientation(i18n: JsonFormsI18nState, vertical: Boolean): String =
    translateRendererText(i18n.translate, i18n.locale,
        if (vertical) "choiceGroup.vertical" else "choiceGroup.horizontal",
        if (vertical) "Vertical choices" else "Horizontal choices", controlRendererLocaleBundles)

fun translateControlRequired(i18n: JsonFormsI18nState): String =
    translateRendererText(i18n.translate, i18n.locale, "control.required", "Required field",
        controlRendererLocaleBundles)

fun translateControlInvalidValue(i18n: JsonFormsI18nState): String =
    translateRendererText(i18n.translate, i18n.locale, "control.invalidValue", "Invalid value",
        controlRendererLocaleBundles)

fun translateBooleanNotSet(i18n: JsonFormsI18nState): String =
    translateRendererText(i18n.translate, i18n.locale, "boolean.notSet", "Not set",
        controlRendererLocaleBundles)

fun translateBooleanInvalidValue(i18n: JsonFormsI18nState): String =
    translateRendererText(i18n.translate, i18n.locale, "boolean.invalidValue", "Invalid value",
        controlRendererLocaleBundles)

fun translateSliderStatus(i18n: JsonFormsI18nState, value: String?, invalid: Boolean): String {
    if (value == null) return translateBooleanNotSet(i18n)
    val key = if (invalid) "slider.invalidValue" else "slider.currentValue"
    val fallback = if (invalid) "Invalid current value" else "Current value"
    return "${translateControlLocalError(i18n, key, fallback)}: $value"
}

fun translateSliderDiagnostic(i18n: JsonFormsI18nState, error: SliderPositionError): String {
    val (key, fallback) = when (error) {
        SliderPositionError.InvalidRange -> "slider.invalidRange" to "Invalid slider range."
        SliderPositionError.InvalidIncrement -> "slider.invalidIncrement" to "Invalid slider increment."
        SliderPositionError.NoPosition -> "slider.noPositions" to "No selectable value in slider range."
        SliderPositionError.TooManyPositions -> "slider.tooManyPositions" to "Too many slider positions."
        SliderPositionError.UnsupportedPrecision -> "slider.unsupportedPrecision" to "Slider precision is unsupported."
    }
    return translateControlLocalError(i18n, key, fallback)
}

data class PasswordActionText(val tooltip: String, val ariaLabel: String)

fun translatePasswordActionText(
    i18n: JsonFormsI18nState,
    i18nKeyPrefix: String?,
    visible: Boolean,
    values: Map<String, Any?> = emptyMap(),
): PasswordActionText {
    val verb = if (visible) "hide" else "show"
    val documentedDefault = if (visible) "Hide password" else "Show password"
    fun text(suffix: String): String {
        val key = "${verb}Password"
        return translateRendererText(i18n.translate, i18n.locale, key + suffix,
            documentedDefault, controlRendererLocaleBundles, values,
            i18nKeyPrefix = i18nKeyPrefix, bundleKey = "password.${key + suffix}")
    }
    return PasswordActionText(text("Tooltip"), text("AriaLabel"))
}

/** The format is inserted before the locale default reaches the host translator. */
fun temporalControlLocaleBundles(format: String): Map<String, JsonObject> = mapOf(
    "bg" to JsonObject(mapOf(
        "temporal.chooseDateTooltip" to JsonPrimitive("Избери дата"),
        "temporal.chooseDateAriaLabel" to JsonPrimitive("Избери дата"),
        "temporal.invalidDate" to JsonPrimitive("Невалидна дата. Очакван формат: $format."),
        "temporal.selectDate" to JsonPrimitive("Избери дата"),
        "temporal.year" to JsonPrimitive("Година"),
        "temporal.month" to JsonPrimitive("Месец"),
        "temporal.previousYear" to JsonPrimitive("Предишна"),
        "temporal.nextYear" to JsonPrimitive("Следваща"),
        "temporal.previousYears" to JsonPrimitive("Предишните 12 години"),
        "temporal.nextYears" to JsonPrimitive("Следващите 12 години"),
        "temporal.chooseTimeTooltip" to JsonPrimitive("Избери час"),
        "temporal.chooseTimeAriaLabel" to JsonPrimitive("Избери час"),
        "temporal.invalidTime" to JsonPrimitive("Невалиден час. Очакван формат: $format."),
        "temporal.selectTime" to JsonPrimitive("Избери час"),
        "temporal.hours" to JsonPrimitive("Часове"),
        "temporal.minutes" to JsonPrimitive("Минути"),
        "temporal.seconds" to JsonPrimitive("Секунди"),
        "temporal.chooseDateTimeTooltip" to JsonPrimitive("Избери дата и час"),
        "temporal.chooseDateTimeAriaLabel" to JsonPrimitive("Избери дата и час"),
        "temporal.invalidDateTime" to JsonPrimitive("Невалидна дата и час. Очакван формат: $format."),
        "temporal.selectDateTime" to JsonPrimitive("Избери дата и час"),
        "temporal.cancel" to JsonPrimitive("Отказ"),
        "temporal.ok" to JsonPrimitive("Готово"),
        "temporal.next" to JsonPrimitive("Напред"),
        "temporal.rangeMinimum" to JsonPrimitive("Стойността трябва да е на или след"),
        "temporal.rangeExclusiveMinimum" to JsonPrimitive("Стойността трябва да е след"),
        "temporal.rangeMaximum" to JsonPrimitive("Стойността трябва да е на или преди"),
        "temporal.rangeExclusiveMaximum" to JsonPrimitive("Стойността трябва да е преди"),
        "temporal.invalidBoundType" to JsonPrimitive("Свързаната граница трябва да е низ."),
        "temporal.invalidBoundReference" to JsonPrimitive("Невалидна препратка към границата за дата или час."),
        "temporal.date.outOfRange" to JsonPrimitive("Изберете дата в позволения диапазон."),
        "temporal.time.outOfRange" to JsonPrimitive("Изберете час в позволения диапазон."),
        "temporal.dateTime.outOfRange" to JsonPrimitive("Изберете дата и час в позволения диапазон."),
    )),
)

fun translateTemporalControlText(
    i18n: JsonFormsI18nState,
    i18nKeyPrefix: String?,
    key: String,
    documentedDefault: String,
    format: String,
    values: Map<String, Any?> = emptyMap(),
): String = translateRendererText(i18n.translate, i18n.locale, key, documentedDefault,
    temporalControlLocaleBundles(format), values, i18nKeyPrefix,
    bundleKey = "temporal.$key")

fun translateTemporalRangeViolation(
    i18n: JsonFormsI18nState,
    i18nKeyPrefix: String?,
    format: String,
    keyword: String,
    bound: String,
): String {
    val (key, fallback) = when (keyword) {
        "formatMinimum" -> "rangeMinimum" to "Value must be on or after"
        "formatExclusiveMinimum" -> "rangeExclusiveMinimum" to "Value must be after"
        "formatMaximum" -> "rangeMaximum" to "Value must be on or before"
        else -> "rangeExclusiveMaximum" to "Value must be before"
    }
    return translateTemporalControlText(i18n, i18nKeyPrefix, key, fallback, format,
        mapOf("bound" to bound)) + " $bound."
}
