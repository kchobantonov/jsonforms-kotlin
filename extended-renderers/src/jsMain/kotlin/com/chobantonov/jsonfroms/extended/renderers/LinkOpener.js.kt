package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.ui.platform.UriHandler

internal actual fun openPortableLink(url: String, target: String, rel: String?, uriHandler: UriHandler) {
    if (target == "_self") {
        uriHandler.openUri(url)
        return
    }
    val anchor: dynamic = js("document.createElement('a')")
    anchor.href = url
    anchor.target = target
    if (rel != null) anchor.rel = rel
    val body: dynamic = js("document.body")
    body.appendChild(anchor)
    anchor.click()
    anchor.remove()
}
