package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.uikit.LocalUIViewController
import com.chobantonov.jsonfroms.cupertino.renderers.util.CupertinoAppTheme
import platform.UIKit.UIViewController
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame

@OptIn(ExperimentalTestApi::class)
class CupertinoThemeHostIosTest {
    @Test
    fun embeddedHostControllerKeepsPrecedence() = runComposeUiTest {
        val hostController = UIViewController()
        var observedController: UIViewController? = null
        setContent {
            CompositionLocalProvider(LocalUIViewController provides hostController) {
                CupertinoAppTheme(darkTheme = false) {
                    observedController = LocalUIViewController.current
                }
            }
        }
        runOnIdle { assertSame(hostController, observedController) }
    }

    @Test
    fun themeCanRenderWithoutHostController() = runComposeUiTest {
        var observedController: UIViewController? = null
        setContent {
            CupertinoAppTheme(darkTheme = false) {
                observedController = LocalUIViewController.current
            }
        }
        runOnIdle { assertNotNull(observedController) }
    }
}
