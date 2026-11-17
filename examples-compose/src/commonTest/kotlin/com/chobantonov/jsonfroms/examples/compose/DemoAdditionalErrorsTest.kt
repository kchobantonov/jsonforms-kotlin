package com.chobantonov.jsonfroms.examples.compose

import com.chobantonov.jsonfroms.core.validation.ErrorObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class DemoAdditionalErrorsTest {
    @Test
    fun reportActionKeepsPathsAndTranslationKeys() {
        val params = Json.parseToJsonElement("""{"errors":[{"instancePath":"/contact/email","keyword":"server","message":"Rejected","params":{"i18nKey":"server.email"}},{"instancePath":"/contact/phone","message":"Unreachable"}]}""") as JsonObject
        val errors = serverErrorsFromAction(params)
        assertEquals(listOf("/contact/email", "/contact/phone"), errors.map { it.instancePath })
        assertEquals("server.email", errors.first().params["i18nKey"])
        assertEquals("Rejected", errors.first().message)
        assertEquals(DemoServerErrorOwner, errors.first().params["owner"])
    }

    @Test
    fun serverActionReplacesOnlyItsOwnedErrors() {
        val params = Json.parseToJsonElement("""{"errors":[{"instancePath":"/email","message":"New"}]}""") as JsonObject
        val unrelated = ErrorObject(instancePath = "/email", message = "Another host", params = mapOf("owner" to "other"))
        val oldServer = ErrorObject(instancePath = "/email", message = "Old", params = mapOf("owner" to DemoServerErrorOwner))
        val published = replaceServerErrors(listOf(unrelated, oldServer), params)
        assertEquals(listOf(unrelated, ErrorObject(instancePath = "/email", message = "New", params = mapOf("owner" to DemoServerErrorOwner))), published)
        assertEquals(listOf(unrelated), clearServerErrors(published))
    }
}
