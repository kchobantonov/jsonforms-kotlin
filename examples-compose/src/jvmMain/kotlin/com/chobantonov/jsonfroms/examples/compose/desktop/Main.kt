package com.chobantonov.jsonfroms.examples.compose.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.chobantonov.jsonfroms.examples.compose.ExamplesComposeApp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "JSON Forms Kotlin Examples",
    ) {
        ExamplesComposeApp()
    }
}
