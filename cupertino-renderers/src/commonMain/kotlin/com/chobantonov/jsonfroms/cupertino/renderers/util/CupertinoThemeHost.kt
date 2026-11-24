package com.chobantonov.jsonfroms.cupertino.renderers.util

import androidx.compose.runtime.Composable

@Composable
internal expect fun ProvideCupertinoUIViewControllerIfMissing(content: @Composable () -> Unit)
