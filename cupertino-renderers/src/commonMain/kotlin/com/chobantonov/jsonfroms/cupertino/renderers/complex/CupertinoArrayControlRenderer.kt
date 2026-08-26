package com.chobantonov.jsonfroms.cupertino.renderers.complex

import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.material.renderers.complex.MaterialArrayControlRenderer
import com.chobantonov.jsonfroms.material.renderers.complex.materialArrayControlTester

val CupertinoArrayControlRenderer = ComposeJsonFormsRenderer { MaterialArrayControlRenderer.Render(it) }
val cupertinoArrayControlTester = materialArrayControlTester
