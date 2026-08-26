package com.chobantonov.jsonfroms.cupertino.renderers.layouts

import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.material.renderers.layouts.MaterialArrayLayoutRenderer
import com.chobantonov.jsonfroms.material.renderers.layouts.materialArrayLayoutTester

val CupertinoArrayLayoutRenderer = ComposeJsonFormsRenderer { MaterialArrayLayoutRenderer.Render(it) }
val cupertinoArrayLayoutTester = materialArrayLayoutTester
