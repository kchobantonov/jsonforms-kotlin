package com.chobantonov.jsonfroms.cupertino.renderers.controls

import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.isNumberControl
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.withIncreasedRank

val CupertinoNumberControl = ComposeJsonFormsRenderer { CupertinoNumericControl(it, integer = false) }
val cupertinoNumberControlTester: RankedTester = withIncreasedRank(100, rankWith(2, isNumberControl))
