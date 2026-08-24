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

val i18nTranslations = mapOf(
    "basicInfoGroup.label" to "Basic Information",
    "additionalInformationLabel" to "Additional Information",
)

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
)
