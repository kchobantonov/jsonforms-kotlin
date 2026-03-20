package com.chobantonov.jsonfroms.examples.compose.ios

import androidx.compose.ui.window.ComposeUIViewController
import com.chobantonov.jsonfroms.examples.compose.ExamplesComposeApp
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController(
    configure = {
        // Some IDE-driven iOS runs regenerate the host app Info.plist without
        // preserving CADisableMinimumFrameDurationOnPhone. Keep the demo app
        // runnable even when that host-side plist wiring is stale.
        enforceStrictPlistSanityCheck = false
    },
) {
    ExamplesComposeApp()
}
