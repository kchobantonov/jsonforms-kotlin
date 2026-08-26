package com.chobantonov.jsonfroms.examples.examples

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.models.LabelElement
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.store.Translator
import com.chobantonov.jsonfroms.examples.ExampleDescription
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

val i18nUiSchema = VerticalLayout(
    elements = listOf(
        GroupElement(
            i18n = "basicInfoGroup",
            elements = listOf(
                ControlElement(scope = "#/properties/name"),
                ControlElement(scope = "#/properties/birthDate"),
            ),
        ),
        LabelElement(text = "additionalInformationLabel"),
        HorizontalLayout(
            elements = listOf(
                ControlElement(scope = "#/properties/nationality"),
                ControlElement(scope = "#/properties/vegetarian"),
            ),
        ),
    ),
)

val i18nData = buildJsonObject {
    put("vegetarian", false)
    put("birthDate", "1985-06-02")
    putJsonObject("personalData") {
        put("age", 34)
    }
    put("postalCode", "12345")
}

val i18nEnglishTranslations = mapOf(
    "basicInfoGroup.label" to "Basic Information",
    "additionalInformationLabel" to "Additional Information",
    "name.label" to "Name",
    "birthDate.label" to "Birth date",
    "nationality.label" to "Nationality",
    "vegetarian.label" to "Vegetarian",
    "nationality.DE" to "Germany",
    "nationality.IT" to "Italy",
    "nationality.JP" to "Japan",
    "nationality.US" to "United States",
    "nationality.RU" to "Russia",
    "nationality.Other" to "Other",
)

val i18nGermanTranslations = mapOf(
    "basicInfoGroup.label" to "Grundinformationen",
    "additionalInformationLabel" to "Zusätzliche Informationen",
    "name.label" to "Name",
    "birthDate.label" to "Geburtsdatum",
    "nationality.label" to "Nationalität",
    "vegetarian.label" to "Vegetarisch",
    "nationality.DE" to "Deutschland",
    "nationality.IT" to "Italien",
    "nationality.JP" to "Japan",
    "nationality.US" to "Vereinigte Staaten",
    "nationality.RU" to "Russland",
    "nationality.Other" to "Andere",
)

val i18nBulgarianTranslations = mapOf(
    "basicInfoGroup.label" to "Основна информация",
    "additionalInformationLabel" to "Допълнителна информация",
    "name.label" to "Име",
    "birthDate.label" to "Дата на раждане",
    "nationality.label" to "Националност",
    "vegetarian.label" to "Вегетарианец",
    "nationality.DE" to "Германия",
    "nationality.IT" to "Италия",
    "nationality.JP" to "Япония",
    "nationality.US" to "Съединени щати",
    "nationality.RU" to "Русия",
    "nationality.Other" to "Друга",
)

val i18nTranslations = i18nEnglishTranslations

private val exampleTranslator = Translator { key, defaultMessage, _ ->
    i18nTranslations[key] ?: defaultMessage
}

val i18nExample = ExampleDescription(
    name = "i18n",
    label = "Person (i18n)",
    data = i18nData,
    schema = personCoreSchema,
    uischema = i18nUiSchema,
    i18n = JsonFormsI18nState(
        translate = exampleTranslator,
        locale = "en",
    ),
    translations = i18nTranslations,
    localizedTranslations = mapOf(
        "en" to i18nEnglishTranslations,
        "de" to i18nGermanTranslations,
        "bg" to i18nBulgarianTranslations,
    ),
)
