package com.chobantonov.jsonfroms.compose

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.chobantonov.jsonfroms.core.util.StringMask

/** Shows separators without adding them to the text field's editable buffer. */
class StringMaskVisualTransformation(private val mask: StringMask) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val display = mask.visualize(text.text)
        val offsets = display.rawOffsets
        return TransformedText(AnnotatedString(display.text), object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                offsets[offset.coerceIn(0, offsets.lastIndex)]

            override fun transformedToOriginal(offset: Int): Int =
                offsets.indexOfLast { it <= offset }.coerceAtLeast(0)
        })
    }
}
