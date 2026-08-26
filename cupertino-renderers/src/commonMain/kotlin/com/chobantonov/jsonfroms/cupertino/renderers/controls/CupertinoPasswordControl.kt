package com.chobantonov.jsonfroms.cupertino.renderers.controls

import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.LocalPreserveEmptyValue
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.and
import com.chobantonov.jsonfroms.core.testers.formatIs
import com.chobantonov.jsonfroms.core.testers.isStringControl
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.withIncreasedRank
import kotlinx.serialization.json.JsonPrimitive

val CupertinoPasswordControl = ComposeJsonFormsRenderer { props ->
    val bindings = cupertinoControlBindings(props, "CupertinoPasswordControl")
    val preserveEmptyValue = LocalPreserveEmptyValue.current
    CupertinoInputControl(
        props = bindings.controlProps,
        value = bindings.stateProps.data.toCupertinoInputString(),
        keyboardType = KeyboardType.Password,
        visualTransformation = PasswordVisualTransformation(),
        clearLabel = bindings.clearLabel,
    ) { value ->
        bindings.dispatchProps.handleChange(
            bindings.stateProps.path,
            if (value.isNotEmpty() || preserveEmptyValue) JsonPrimitive(value) else null,
        )
    }
}

val cupertinoPasswordControlTester: RankedTester = withIncreasedRank(
    100,
    rankWith(2, and(isStringControl, formatIs("password"))),
)
