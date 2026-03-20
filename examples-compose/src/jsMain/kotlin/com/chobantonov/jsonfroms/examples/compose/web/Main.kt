package com.chobantonov.jsonfroms.examples.compose.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.chobantonov.jsonfroms.examples.compose.ExamplesComposeApp

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        ExamplesComposeApp()
    }
}
