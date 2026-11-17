package com.chobantonov.jsonfroms.examples.compose

import kotlin.test.Test

class DemoSettingsFocusCupertinoJvmTest {
    @Test
    fun cupertinoSettingsKeepsKeyboardFocusInside() = settingsFocusFlow(DemoUiStyle.Cupertino)
}
