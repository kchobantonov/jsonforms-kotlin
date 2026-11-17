package com.chobantonov.jsonfroms.examples.compose

import com.chobantonov.jsonfroms.core.models.LabelElement
import com.chobantonov.jsonfroms.core.store.JsonFormsUISchemaRegistryEntry
import com.chobantonov.jsonfroms.core.store.ValidationMode
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import com.chobantonov.jsonfroms.examples.StateProps
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class WebComponentInputBridgeTest {
    @Test fun currentFormAndSettingsCrossTheHostBridgeWithRegistryLimitVisible() {
        val currentData = Json.parseToJsonElement("""{"name":"Changed in Compose"}""")
        val state = StateProps(
            data = currentData,
            uischemas = listOf(
                JsonFormsUISchemaRegistryEntry({ _, _, _ -> 10 },
                    LabelElement("Named detail", name = "named-detail")),
                JsonFormsUISchemaRegistryEntry({ _, _, _ -> 10 },
                    LabelElement("Unnamed detail")),
            ),
            localizedTranslations = mapOf(
                "en" to mapOf("name.label" to "Name"),
                "bg" to mapOf("name.label" to "Име"),
            ),
        )
        val settings = DemoSettings(locale = DemoLocale.Bulgarian,
            formLocaleOverride = DemoLocale.English, direction = DemoDirection.Rtl,
            formDarkThemeOverride = true)

        val input = webComponentInput(state, settings,
            readonly = true, formOnly = false, useDarkTheme = false)

        assertEquals(currentData, Json.parseToJsonElement(input.data))
        assertTrue(input.dataProvided)
        assertEquals(DemoLocale.Bulgarian, input.locale)
        assertEquals(DemoLocale.English, input.formLocale)
        assertEquals(DemoDirection.Rtl, input.direction)
        assertTrue(input.readonly)
        assertEquals(ValidationMode.ValidateAndShow, input.validationMode)
        assertTrue(input.darkTheme)
        assertEquals(setOf("en", "bg"), Json.parseToJsonElement(input.translations!!).jsonObject.keys)
        assertEquals(2, input.unavailableRegistryTesters)
        assertEquals("Функциите за избор на регистрирани UI схеми не са налични тук",
            webComponentRegistryTesterNotice(input.locale, input.unavailableRegistryTesters))
        val transferred = Json.parseToJsonElement(input.uischemas!!).jsonArray
        assertEquals(1, transferred.size)
        assertEquals("named-detail", transferred.single().jsonObject.getValue("uischema")
            .jsonObject.getValue("name").jsonPrimitive.content)
    }

    @Test fun noRegistryEntriesProduceNoTesterWarning() {
        val input = webComponentInput(StateProps(data = JsonObject(emptyMap())),
            DemoSettings(), readonly = false, formOnly = true, useDarkTheme = false)
        assertEquals(0, input.unavailableRegistryTesters)
        assertEquals(null, webComponentRegistryTesterNotice(input.locale, input.unavailableRegistryTesters))
        assertEquals(0, Json.parseToJsonElement(input.uischemas!!).jsonArray.size)
        assertTrue(input.formOnly)
    }

    @Test fun englishRegistryNoticeIsAvailableWithoutGermanLocale() {
        assertEquals("Registered UI Schema tester functions are unavailable here",
            webComponentRegistryTesterNotice(DemoLocale.English, 1))
    }

    @Test fun legacyEnglishTranslationsCrossTheHostBridge() {
        val state = StateProps(
            data = JsonObject(emptyMap()),
            translations = mapOf("name.label" to "Name from legacy catalog"),
        )

        val input = webComponentInput(state, DemoSettings(),
            readonly = false, formOnly = false, useDarkTheme = false)

        assertEquals("Name from legacy catalog",
            Json.parseToJsonElement(input.translations!!).jsonObject.getValue("en")
                .jsonObject.getValue("name.label").jsonPrimitive.content)
        assertEquals(setOf("en"), Json.parseToJsonElement(input.translations).jsonObject.keys)
    }

    @Test fun validationAndSettingsOverridesCrossWithMissingData() {
        val state = StateProps(
            data = Json.parseToJsonElement("null"),
            dataProvided = false,
            config = mapOf("customFlag" to "kept", "hideAvatar" to true),
        )
        val settings = DemoSettings(
            validationMode = ValidationMode.NoValidation,
            hideAvatar = false,
            hideRequiredAsterisk = true,
            enableFilterErrorsBeforeTouch = true,
        )

        val input = webComponentInput(state, settings,
            readonly = false, formOnly = false, useDarkTheme = false)
        val config = Json.parseToJsonElement(input.config!!).jsonObject

        assertEquals("null", input.data)
        assertEquals(false, input.dataProvided)
        assertEquals(ValidationMode.NoValidation, input.validationMode)
        assertEquals("kept", config.getValue("customFlag").jsonPrimitive.content)
        assertEquals("false", config.getValue("hideAvatar").jsonPrimitive.content)
        assertEquals("true", config.getValue("hideRequiredAsterisk").jsonPrimitive.content)
        assertEquals("true", config.getValue("enableFilterErrorsBeforeTouch").jsonPrimitive.content)
    }

    @Test fun hostErrorsKeepTheirPathsOwnersAndParameters() {
        val error = ErrorObject(
            instancePath = "/contact/email",
            schemaPath = "#/properties/contact/properties/email",
            keyword = "server",
            message = "Rejected",
            params = mapOf("owner" to DemoServerErrorOwner, "i18nKey" to "server.email"),
        )
        val input = webComponentInput(
            StateProps(data = JsonObject(emptyMap()), additionalErrors = listOf(error)),
            DemoSettings(), readonly = false, formOnly = false, useDarkTheme = false)
        val transferred = Json.parseToJsonElement(input.additionalErrors).jsonArray.single().jsonObject

        assertEquals("/contact/email", transferred.getValue("instancePath").jsonPrimitive.content)
        assertEquals("#/properties/contact/properties/email",
            transferred.getValue("schemaPath").jsonPrimitive.content)
        assertEquals("server", transferred.getValue("keyword").jsonPrimitive.content)
        assertEquals("Rejected", transferred.getValue("message").jsonPrimitive.content)
        assertEquals("server.email", transferred.getValue("params").jsonObject
            .getValue("i18nKey").jsonPrimitive.content)
        assertEquals(DemoServerErrorOwner, transferred.getValue("params").jsonObject
            .getValue("owner").jsonPrimitive.content)
    }
}
