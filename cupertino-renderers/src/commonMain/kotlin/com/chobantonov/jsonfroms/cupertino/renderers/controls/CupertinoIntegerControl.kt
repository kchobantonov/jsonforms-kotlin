package com.chobantonov.jsonfroms.cupertino.renderers.controls

import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.isIntegerControl
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.withIncreasedRank

val CupertinoIntegerControl = ComposeJsonFormsRenderer { CupertinoNumericControl(it, integer = true) }
val cupertinoIntegerControlTester: RankedTester = withIncreasedRank(100, rankWith(2, isIntegerControl))
