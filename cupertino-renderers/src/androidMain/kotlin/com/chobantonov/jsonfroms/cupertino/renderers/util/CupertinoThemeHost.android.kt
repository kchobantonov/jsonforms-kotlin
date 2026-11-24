package com.chobantonov.jsonfroms.cupertino.renderers.util

import androidx.compose.runtime.Composable

@Composable
internal actual fun ProvideCupertinoUIViewControllerIfMissing(content: @Composable () -> Unit) = content()
