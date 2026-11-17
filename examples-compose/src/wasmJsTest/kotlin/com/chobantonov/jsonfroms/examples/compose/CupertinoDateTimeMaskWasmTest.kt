package com.chobantonov.jsonfroms.examples.compose

import kotlin.test.Test
import kotlinx.serialization.json.JsonPrimitive

class CupertinoDateTimeMaskWasmTest {
    private val cases = TemporalMaskOptionUiTest()

    @Test fun defaultMaskRejectsLetters() = cases.cupertinoDateTimeMask(null, false)
    @Test fun booleanFalseAllowsDraftLetters() = cases.cupertinoDateTimeMask(JsonPrimitive(false), true)
    @Test fun stringFalseDoesNotDisableMask() = cases.cupertinoDateTimeMask(JsonPrimitive("false"), false)
}
