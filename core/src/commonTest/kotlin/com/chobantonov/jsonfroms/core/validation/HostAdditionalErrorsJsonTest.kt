package com.chobantonov.jsonfroms.core.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HostAdditionalErrorsJsonTest {
    @Test fun decodesPathsAndHostOwnershipWithoutDiscardingOptionalFields() {
        val text = """[{"instancePath":"/contact/email","dataPath":".contact.email","schemaPath":"#/email","keyword":"server","message":"Rejected","params":{"owner":"demo-server","i18nKey":"server.email"},"absoluteKeywordLocation":"https://example.test/schema#/email"}]"""

        assertEquals(listOf(ErrorObject(
            instancePath = "/contact/email",
            dataPath = ".contact.email",
            schemaPath = "#/email",
            keyword = "server",
            message = "Rejected",
            params = mapOf("owner" to "demo-server", "i18nKey" to "server.email"),
            absoluteKeywordLocation = "https://example.test/schema#/email",
        )), decodeHostAdditionalErrors(text))
    }

    @Test fun absentAndEmptyAttributesSupplyNoHostErrors() {
        assertEquals(emptyList(), decodeHostAdditionalErrors(null))
        assertEquals(emptyList(), decodeHostAdditionalErrors("[]"))
        assertEquals(listOf(ErrorObject(message = "Global")),
            decodeHostAdditionalErrors("""[{"message":"Global"}]"""))
    }

    @Test fun rejectsMalformedHostErrorPayloadsAtomically() {
        for (text in listOf(
            "not json", "{}", "[null]", "[{}]", """[{"message":1}]""",
            """[{"message":"Valid"},{"message":"Invalid","params":{"owner":4}}]""",
            """[{"message":"Invalid","instancePath":null}]""",
        )) {
            assertNull(decodeHostAdditionalErrors(text), text)
        }
    }
}
