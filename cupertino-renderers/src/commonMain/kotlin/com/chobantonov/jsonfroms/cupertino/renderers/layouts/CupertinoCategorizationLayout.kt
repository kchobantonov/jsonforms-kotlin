package com.chobantonov.jsonfroms.cupertino.renderers.layouts

import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.material.renderers.layouts.MaterialCategorizationLayout
import com.chobantonov.jsonfroms.material.renderers.layouts.materialCategorizationTester

val CupertinoCategorizationLayout = ComposeJsonFormsRenderer { MaterialCategorizationLayout.Render(it) }
val cupertinoCategorizationTester = materialCategorizationTester
