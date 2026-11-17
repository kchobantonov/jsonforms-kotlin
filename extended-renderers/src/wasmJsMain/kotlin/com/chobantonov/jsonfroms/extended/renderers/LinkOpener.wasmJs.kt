package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.ui.platform.UriHandler
import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun openPortableLink(url: String, target: String, rel: String?, uriHandler: UriHandler) {
    if (target == "_self") uriHandler.openUri(url)
    else openBrowserAnchor(url, target, rel.orEmpty())
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun openBrowserAnchor(url: String, target: String, rel: String): Unit = js(
    """{
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.target = target;
        anchor.rel = rel;
        document.body.appendChild(anchor);
        anchor.click();
        anchor.remove();
    }"""
)
