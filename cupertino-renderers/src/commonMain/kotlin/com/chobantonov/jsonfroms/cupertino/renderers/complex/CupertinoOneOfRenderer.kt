package com.chobantonov.jsonfroms.cupertino.renderers.complex

import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.core.testers.withIncreasedRank
import com.chobantonov.jsonfroms.material.renderers.complex.MaterialOneOfRenderer
import com.chobantonov.jsonfroms.material.renderers.complex.materialOneOfControlTester

val CupertinoOneOfRenderer = ComposeJsonFormsRenderer { props ->
    MaterialOneOfRenderer(props, confirmationDialog = { title, message, accept, decline, confirm, dismiss ->
        CupertinoBranchConfirmationDialog(title, message, accept, decline, confirm, dismiss)
    })
}
val cupertinoOneOfControlTester = withIncreasedRank(100, materialOneOfControlTester)
