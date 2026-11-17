package com.chobantonov.jsonfroms.examples.compose.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeViewport
import com.chobantonov.jsonfroms.examples.compose.ExamplesComposeApp
import com.chobantonov.jsonfroms.examples.compose.DemoUrlState
import com.chobantonov.jsonfroms.examples.compose.DemoUrlViewState
import com.chobantonov.jsonfroms.examples.compose.parseDemoUrl
import com.chobantonov.jsonfroms.examples.compose.writeDemoUrl
import kotlinx.browser.window

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        var route by remember { mutableStateOf(parseDemoUrl(window.location.search, window.location.hash)) }
        fun navigate(next: DemoUrlState) {
            if (next == route) return
            val (search, hash) = writeDemoUrl(window.location.search, window.location.hash, next)
            window.history.pushState(null, "", "${window.location.pathname}$search$hash")
            route = next
        }
        DisposableEffect(Unit) {
            window.onhashchange = {
                route = parseDemoUrl(window.location.search, window.location.hash)
            }
            window.onpopstate = {
                route = parseDemoUrl(window.location.search, window.location.hash)
            }
            onDispose {
                window.onhashchange = null
                window.onpopstate = null
            }
        }
        ExamplesComposeApp(
            selectedExampleName = route.exampleName,
            showLandingWhenUnselected = true,
            browserViewState = route.view,
            onBrowserViewStateChange = { navigate(route.copy(view = it)) },
            onExampleSelected = { name -> navigate(route.copy(exampleName = name)) },
            onHomeSelected = { navigate(DemoUrlState(null, DemoUrlViewState())) },
        )
    }
}
