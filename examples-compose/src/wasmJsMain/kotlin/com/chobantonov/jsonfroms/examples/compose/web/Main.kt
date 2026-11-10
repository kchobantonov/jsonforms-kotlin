package com.chobantonov.jsonfroms.examples.compose.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeViewport
import com.chobantonov.jsonfroms.examples.compose.ExamplesComposeApp
import com.chobantonov.jsonfroms.examples.compose.exampleHash
import com.chobantonov.jsonfroms.examples.compose.exampleNameFromHash
import kotlinx.browser.window

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        var selectedExampleName by remember { mutableStateOf(exampleNameFromHash(window.location.hash)) }
        DisposableEffect(Unit) {
            window.onhashchange = {
                selectedExampleName = exampleNameFromHash(window.location.hash)
            }
            onDispose { window.onhashchange = null }
        }
        ExamplesComposeApp(
            selectedExampleName = selectedExampleName,
            showLandingWhenUnselected = true,
            onExampleSelected = { name ->
                selectedExampleName = name
                window.location.hash = exampleHash(name)
            },
            onHomeSelected = {
                selectedExampleName = null
                window.location.hash = ""
            },
        )
    }
}
