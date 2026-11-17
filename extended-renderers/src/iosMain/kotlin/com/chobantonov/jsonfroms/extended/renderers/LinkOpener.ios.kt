package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.ui.platform.UriHandler

internal actual fun openPortableLink(url: String, target: String, rel: String?, uriHandler: UriHandler) {
    uriHandler.openUri(url)
}
