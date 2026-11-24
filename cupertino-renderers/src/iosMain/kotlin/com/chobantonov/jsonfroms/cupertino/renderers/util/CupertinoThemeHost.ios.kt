package com.chobantonov.jsonfroms.cupertino.renderers.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.uikit.LocalUIViewController
import platform.UIKit.UIViewController

@Composable
internal actual fun ProvideCupertinoUIViewControllerIfMissing(content: @Composable () -> Unit) {
    val fallbackController = remember { UIViewController() }
    CompositionLocalProvider(LocalUIViewController providesDefault fallbackController, content = content)
}
