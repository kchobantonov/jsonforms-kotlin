package com.chobantonov.jsonfroms.cupertino.renderers.layouts

import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.material.renderers.layouts.MaterialArrayLayoutRenderer
import com.chobantonov.jsonfroms.material.renderers.layouts.materialArrayLayoutTester
import com.slapps.cupertino.CupertinoAlertDialog
import com.slapps.cupertino.CupertinoText
import com.slapps.cupertino.ExperimentalCupertinoApi
import com.slapps.cupertino.cancel
import com.slapps.cupertino.destructive

@OptIn(ExperimentalCupertinoApi::class)
val CupertinoArrayLayoutRenderer = ComposeJsonFormsRenderer { props ->
    MaterialArrayLayoutRenderer(props) { title, message, accept, decline, onConfirm, onDismiss ->
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
}
val cupertinoArrayLayoutTester = materialArrayLayoutTester
