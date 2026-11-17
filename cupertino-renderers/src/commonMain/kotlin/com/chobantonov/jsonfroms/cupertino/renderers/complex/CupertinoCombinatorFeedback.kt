package com.chobantonov.jsonfroms.cupertino.renderers.complex

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import com.chobantonov.jsonfroms.cupertino.renderers.util.LocalCupertinoColors
import com.slapps.cupertino.CupertinoText

@Composable
internal fun CupertinoCombinatorOwnErrors(message: String) {
    if (message.isNotBlank()) CupertinoText(
        message,
        modifier = Modifier.semantics { error(message) },
        color = LocalCupertinoColors.current.destructive,
    )
}
