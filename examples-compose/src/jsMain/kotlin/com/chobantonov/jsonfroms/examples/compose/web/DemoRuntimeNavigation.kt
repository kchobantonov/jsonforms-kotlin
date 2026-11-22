package com.chobantonov.jsonfroms.examples.compose.web

import com.chobantonov.jsonfroms.examples.compose.DemoBrowserRuntime
import com.chobantonov.jsonfroms.examples.compose.DemoRuntimeHandoff
import com.chobantonov.jsonfroms.examples.compose.decodeDemoRuntimeHandoff
import com.chobantonov.jsonfroms.examples.compose.demoRuntimeDestination
import com.chobantonov.jsonfroms.examples.compose.demoRuntimeHandoffToken
import com.chobantonov.jsonfroms.examples.compose.encode
import com.chobantonov.jsonfroms.examples.compose.parseDemoUrl
import com.chobantonov.jsonfroms.examples.compose.withoutDemoRuntimeHandoffToken
import kotlinx.browser.window
import kotlin.random.Random

private const val RuntimeTransferKey = "jsonforms.demo.runtime."

internal fun consumeRuntimeHandoff(runtime: DemoBrowserRuntime): DemoRuntimeHandoff? {
    val token = demoRuntimeHandoffToken(window.location.search) ?: return null
    val cleanSearch = withoutDemoRuntimeHandoffToken(window.location.search)
    val stored = runCatching { window.sessionStorage.getItem(RuntimeTransferKey + token) }.getOrNull()
    runCatching { window.sessionStorage.removeItem(RuntimeTransferKey + token) }
    window.history.replaceState(null, "", "${window.location.pathname}$cleanSearch${window.location.hash}")
    val route = parseDemoUrl(cleanSearch, window.location.hash)
    return decodeDemoRuntimeHandoff(stored)?.takeIf {
        it.target == runtime && it.exampleName == route.exampleName
    }
}

internal fun changeRuntime(
    current: DemoBrowserRuntime, target: DemoBrowserRuntime, handoff: DemoRuntimeHandoff?,
) {
    val token = handoff?.let {
        Random.nextLong().toULong().toString()
    }
    if (handoff != null && token != null) {
        window.sessionStorage.setItem(RuntimeTransferKey + token, handoff.encode())
    }
    window.location.href = demoRuntimeDestination(
        window.location.pathname, window.location.search, window.location.hash,
        current, target, handoff, token,
    )
}
