package com.chobantonov.jsonfroms.cupertino.renderers.controls

import androidx.compose.runtime.Composable
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.LocalPreserveEmptyValue
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.and
import com.chobantonov.jsonfroms.core.testers.isMultiLineControl
import com.chobantonov.jsonfroms.core.testers.isStringControl
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.withIncreasedRank
import kotlinx.serialization.json.JsonPrimitive

val CupertinoTextControl = ComposeJsonFormsRenderer { CupertinoTextControl(it, singleLine = true) }
val CupertinoMultiTextControl = ComposeJsonFormsRenderer { CupertinoTextControl(it, singleLine = false) }

@Composable
private fun CupertinoTextControl(props: ComposeJsonFormsRendererProps, singleLine: Boolean) {
    val bindings = cupertinoControlBindings(props, "CupertinoTextControl")
    val preserveEmptyValue = LocalPreserveEmptyValue.current
    CupertinoInputControl(
        props = bindings.controlProps,
        value = bindings.stateProps.data.toCupertinoInputString(),
        singleLine = singleLine,
        clearLabel = bindings.clearLabel,
    ) { value ->
        bindings.dispatchProps.handleChange(
            bindings.stateProps.path,
            if (value.isNotEmpty() || preserveEmptyValue) JsonPrimitive(value) else null,
        )
    }
}

val cupertinoTextControlTester: RankedTester = withIncreasedRank(100, rankWith(1, isStringControl))
val cupertinoMultiTextControlTester: RankedTester = withIncreasedRank(
    100,
    rankWith(2, and(isStringControl, isMultiLineControl)),
)
