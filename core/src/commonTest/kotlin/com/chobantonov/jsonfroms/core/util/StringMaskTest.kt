package com.chobantonov.jsonfroms.core.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StringMaskTest {
    private fun mask(options: String) = StringMask.fromOptions(Json.parseToJsonElement(options).jsonObject)!!

    @Test fun rawAndFormattedStorageAreSeparate() {
        val reference = mask("""{"mask":"###-###"}""")
        assertEquals("482-913", reference.display("482913"))
        assertEquals("482913", reference.raw("482-913"))
        assertEquals(listOf(0, 1, 2, 4, 5, 6, 7),
            reference.visualize("482913").rawOffsets)
        assertEquals("482913", reference.edit("482-913"))
        assertEquals("48291", reference.edit("482-91"))
        assertEquals("PENDING-REVIEW", reference.display("PENDING-REVIEW"))
        assertEquals("PENDING-REVIE", reference.edit("PENDING-REVIE", "PENDING-REVIEW"))
        assertEquals("PENDING-REVI", reference.edit("PENDING-REVI", "PENDING-REVIE"))
        assertEquals("482913", reference.edit("482-913", "PENDING-REVI"))
        assertEquals("", reference.edit(""))

        val phone = mask("""{"mask":"+1 (###) ###-####","returnMaskedValue":true}""")
        assertEquals("+1 (503) 555-0142", phone.display("+1 (503) 555-0142"))
        assertEquals("5035550142", phone.raw("+1 (503) 555-0142"))
        assertEquals("+1 (503) 555-0142", phone.edit("5035550142"))
        assertEquals("+1 (503) 555-0142", phone.edit("+1 (503) 555-0142"))
        assertEquals("+1 (123) 555-0142", phone.edit("1235550142"))
    }

    @Test fun alternativesTokensAndEagerSeparators() {
        val account = mask("""{"mask":["#### ####","#### #### ####"]}""")
        assertEquals("1234 5678 9012", account.display("123456789012"))
        assertEquals("12345678", account.edit("1234 5678"))
        assertEquals("123456789012", account.edit("1234 5678 9012"))
        val deck = mask("""{"mask":"D-##","tokens":{"D":"[A-C]"}}""")
        assertEquals("B-07", deck.display("B07"))
        assertEquals("B07", deck.edit("B-07"))
        val letters = mask("""{"mask":"AAAA","tokens":{"A":"\\p{L}"}}""")
        assertEquals("Süd", letters.edit("Sü7d"))
        val eager = mask("""{"mask":"##-##","eager":true}""")
        assertEquals("81-", eager.display("81"))
        assertEquals("81-4", eager.display("814"))
    }

    @Test fun nonMaskOptionsDoNotActivateMasking() {
        assertNull(StringMask.fromOptions(Json.parseToJsonElement("""{"mask":false}""").jsonObject))
        assertNull(StringMask.fromOptions(emptyMap()))
    }
}
