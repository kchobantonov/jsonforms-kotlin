package com.chobantonov.jsonfroms.examples.compose

internal expect val isApplePlatform: Boolean

internal fun resolveUiStyle(style: DemoUiStyle, applePlatform: Boolean): DemoUiStyle = when (style) {
    DemoUiStyle.System -> if (applePlatform) DemoUiStyle.Cupertino else DemoUiStyle.Material
    else -> style
}
