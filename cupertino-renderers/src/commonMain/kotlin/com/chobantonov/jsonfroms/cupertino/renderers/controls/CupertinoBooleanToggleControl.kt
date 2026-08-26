package com.chobantonov.jsonfroms.cupertino.renderers.controls

import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.and
import com.chobantonov.jsonfroms.core.testers.isBooleanControl
import com.chobantonov.jsonfroms.core.testers.optionIs
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.withIncreasedRank

val CupertinoBooleanToggleControl = ComposeJsonFormsRenderer { CupertinoBooleanControl(it, toggle = true) }
val cupertinoBooleanToggleControlTester: RankedTester = withIncreasedRank(
    100,
    rankWith(3, and(isBooleanControl, optionIs("toggle", true))),
)
