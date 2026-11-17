package com.chobantonov.jsonfroms.cupertino.renderers.complex

import androidx.compose.runtime.Composable
import com.slapps.cupertino.CupertinoAlertDialog
import com.slapps.cupertino.CupertinoText
import com.slapps.cupertino.ExperimentalCupertinoApi
import com.slapps.cupertino.cancel
import com.slapps.cupertino.destructive

@OptIn(ExperimentalCupertinoApi::class)
@Composable
internal fun CupertinoBranchConfirmationDialog(
    title: String, message: String, accept: String, decline: String,
    onConfirm: () -> Unit, onDismiss: () -> Unit,
) {
    CupertinoAlertDialog(
        onDismissRequest = onDismiss,
        title = { CupertinoText(title) },
        message = { CupertinoText(message) },
        buttons = {
            cancel(onClick = onDismiss) { CupertinoText(decline) }
            destructive(onClick = onConfirm) { CupertinoText(accept) }
        },
    )
}
