package com.chobantonov.jsonfroms.core.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RestrictedStringEditTest {
    @Test fun preventsExcessWithoutChangingAlreadyInvalidData() {
        assertFalse(acceptsRestrictedStringEdit("ab", "abcd", 3, true))
        assertTrue(acceptsRestrictedStringEdit("abcdef", "abcde", 3, true))
        assertFalse(acceptsRestrictedStringEdit("abcdef", "abcdefg", 3, true))
        assertTrue(acceptsRestrictedStringEdit("abcdef", "abc", 3, true))
        assertTrue(acceptsRestrictedStringEdit("ab", "abcd", 3, false))
    }

    @Test fun countsSupplementaryCharactersAsOne() {
        assertTrue(acceptsRestrictedStringEdit("", "😀😀", 2, true))
        assertFalse(acceptsRestrictedStringEdit("😀😀", "😀😀a", 2, true))
        assertTrue(acceptsRestrictedStringEdit("😀😀😀", "😀😀", 2, true))
    }
}
