package com.chobantonov.jsonfroms.compose

import androidx.compose.ui.text.AnnotatedString
import com.chobantonov.jsonfroms.core.util.StringMask
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class StringMaskVisualTransformationTest {
    private val mask = StringMask.fromOptions(
        Json.parseToJsonElement("""{"mask":"###-###"}""").jsonObject)!!

    @Test fun separatorOffsetsKeepCaretWithRawDigits() {
        val transformed = StringMaskVisualTransformation(mask).filter(AnnotatedString("482913"))
        assertEquals("482-913", transformed.text.text)
        assertEquals(4, transformed.offsetMapping.originalToTransformed(3))
        assertEquals(7, transformed.offsetMapping.originalToTransformed(6))
        assertEquals(3, transformed.offsetMapping.transformedToOriginal(4))
        assertEquals(6, transformed.offsetMapping.transformedToOriginal(7))
    }

    @Test fun literalPrefixAndInvalidImportMapSafely() {
        val phone = StringMask.fromOptions(Json.parseToJsonElement(
            """{"mask":"+1 (###) ###-####","returnMaskedValue":true}"""
        ).jsonObject)!!
        val formatted = StringMaskVisualTransformation(phone).filter(AnnotatedString("5035550142"))
        assertEquals("+1 (503) 555-0142", formatted.text.text)
        assertEquals(4, formatted.offsetMapping.originalToTransformed(0))
        assertEquals(9, formatted.offsetMapping.originalToTransformed(3))
        assertEquals(10, formatted.offsetMapping.transformedToOriginal(formatted.text.length))

        val imported = StringMaskVisualTransformation(mask).filter(AnnotatedString("PENDING-REVIEW"))
        assertEquals("PENDING-REVIEW", imported.text.text)
        assertEquals(7, imported.offsetMapping.originalToTransformed(7))
    }
}
