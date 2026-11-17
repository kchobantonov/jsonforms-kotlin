package com.chobantonov.jsonfroms.examples.compose

import com.chobantonov.jsonfroms.core.validation.ErrorObject
import com.chobantonov.jsonfroms.examples.StateProps
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DemoFormActionsTest {
    private val empty = JsonObject(emptyMap())

    @Test fun formLocaleAndThemeActionsChangeOnlySettings() {
        val state = StateProps(data = empty)
        val settings = DemoSettings(locale = DemoLocale.English)
        val lang = demoFormActionEffect(state, settings, "changeLang",
            Json.parseToJsonElement("""{"lang":"bg-BG"}""") as JsonObject, false)
        assertNull(lang.state)
        assertEquals(DemoLocale.Bulgarian, lang.settings?.formLocaleOverride)

        val theme = demoFormActionEffect(state, lang.settings!!, "toggleMode", empty, false)
        assertNull(theme.state)
        assertEquals(true, theme.settings?.formDarkThemeOverride)
        assertEquals(DemoLocale.English, theme.settings?.locale)
    }

    @Test fun serverActionsPreserveOtherOwnersAndFormData() {
        val other = ErrorObject(instancePath = "/email", message = "Other",
            params = mapOf("owner" to "host"))
        val old = ErrorObject(instancePath = "/email", message = "Old",
            params = mapOf("owner" to DemoServerErrorOwner))
        val state = StateProps(data = Json.parseToJsonElement("""{"email":"a"}"""),
            additionalErrors = listOf(other, old))
        val params = Json.parseToJsonElement(
            """{"errors":[{"instancePath":"/email","message":"New"}]}""") as JsonObject

        val reported = demoFormActionEffect(state, DemoSettings(), "reportServer", params, false)
        assertNull(reported.settings)
        assertEquals(state.data, reported.state?.data)
        assertEquals(listOf(other, ErrorObject(instancePath = "/email", message = "New",
            params = mapOf("owner" to DemoServerErrorOwner))), reported.state?.additionalErrors)
        val cleared = demoFormActionEffect(reported.state!!, DemoSettings(), "clearServer", empty, false)
        assertEquals(listOf(other), cleared.state?.additionalErrors)
        assertEquals(state.data, cleared.state?.data)
    }

    @Test fun statusActionUsesFormLocaleAndSuppliesPreviouslyMissingData() {
        val state = StateProps(data = Json.parseToJsonElement("null"), dataProvided = false)
        val settings = DemoSettings(locale = DemoLocale.English,
            formLocaleOverride = DemoLocale.Bulgarian)
        val params = Json.parseToJsonElement(
            """{"statusByLocale":{"en":"Done","bg":"Готово"}}""") as JsonObject

        val effect = demoFormActionEffect(state, settings, "applyStatus", params, false)
        assertNull(effect.settings)
        assertEquals(true, effect.state?.dataProvided)
        assertEquals("Готово", effect.state?.data?.jsonObject?.getValue("status")?.jsonPrimitive?.content)
        assertEquals("applyStatus", effect.state?.data?.jsonObject
            ?.getValue("lastTriggeredBy")?.jsonPrimitive?.content)
    }
}
