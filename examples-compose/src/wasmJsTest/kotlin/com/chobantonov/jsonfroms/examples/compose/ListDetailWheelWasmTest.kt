package com.chobantonov.jsonfroms.examples.compose

import kotlin.test.Test

class ListDetailWheelWasmTest {
    @Test fun materialWheelScrollKeepsSelectedDetail() = ListDetailSplitterUiTest().wheelKeepsDetail(false)
    @Test fun cupertinoWheelScrollKeepsSelectedDetail() = ListDetailSplitterUiTest().wheelKeepsDetail(true)
}
