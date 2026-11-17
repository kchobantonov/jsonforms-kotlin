package com.chobantonov.jsonfroms.cupertino.renderers.complex

import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.material.renderers.complex.MaterialArrayControlRenderer
import com.chobantonov.jsonfroms.cupertino.renderers.ActionTooltip
import com.chobantonov.jsonfroms.material.renderers.complex.materialArrayControlTester
import com.chobantonov.jsonfroms.material.renderers.additional.DetailDialogFrame
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.slapps.cupertino.CupertinoAlertDialog
import com.slapps.cupertino.CupertinoButton
import com.slapps.cupertino.CupertinoButtonDefaults
import com.slapps.cupertino.CupertinoSurface
import com.slapps.cupertino.CupertinoText
import com.slapps.cupertino.ExperimentalCupertinoApi
import com.slapps.cupertino.cancel
import com.slapps.cupertino.destructive

@OptIn(ExperimentalCupertinoApi::class)
val CupertinoArrayControlRenderer = ComposeJsonFormsRenderer { props ->
    MaterialArrayControlRenderer(props, rowDetailDialog = { title, content, apply, cancelLabel,
        applyTooltip, cancelTooltip, canApply, options, maximize, restore, close, resize, onApply, onCancel ->
        DetailDialogFrame(
            options = options,
            resizeLabel = resize,
            onDismiss = onCancel,
            title = { CupertinoText(title) },
            actions = { maximized, toggle ->
                if (options.maximizable) CupertinoButton(
                    onClick = toggle, colors = CupertinoButtonDefaults.plainButtonColors(),
                    modifier = Modifier.testTag("row-detail-dialog-maximize"),
                ) { CupertinoText(if (maximized) restore else maximize) }
                CupertinoButton(onClick = onCancel, colors = CupertinoButtonDefaults.plainButtonColors(),
                    modifier = Modifier.semantics { contentDescription = close }) { CupertinoText("×") }
            },
            footer = {
                if (cancelTooltip == null) CupertinoButton(onClick = onCancel,
                    colors = CupertinoButtonDefaults.plainButtonColors()) { CupertinoText(cancelLabel) }
                else ActionTooltip(cancelTooltip) {
                    CupertinoButton(onClick = onCancel,
                        colors = CupertinoButtonDefaults.plainButtonColors()) { CupertinoText(cancelLabel) }
                }
                if (canApply) {
                    if (applyTooltip == null) CupertinoButton(onClick = onApply,
                        colors = CupertinoButtonDefaults.filledButtonColors()) { CupertinoText(apply) }
                    else ActionTooltip(applyTooltip) {
                        CupertinoButton(onClick = onApply,
                            colors = CupertinoButtonDefaults.filledButtonColors()) { CupertinoText(apply) }
                    }
                }
            },
            surface = { modifier, body -> CupertinoSurface(modifier = modifier) { body() } },
            content = content,
        )
    }, confirmationDialog = { title, message, accept, decline, onConfirm, onDismiss ->
        CupertinoAlertDialog(
            onDismissRequest = onDismiss,
            title = { CupertinoText(title) },
            message = { CupertinoText(message) },
            buttons = {
                cancel(onClick = onDismiss) { CupertinoText(decline) }
                destructive(onClick = onConfirm) { CupertinoText(accept) }
            },
        )
    })
}
val cupertinoArrayControlTester = materialArrayControlTester
