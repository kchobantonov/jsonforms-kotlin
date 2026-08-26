package com.chobantonov.jsonfroms.cupertino.renderers.complex

import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.material.renderers.complex.MaterialOneOfRenderer
import com.chobantonov.jsonfroms.material.renderers.complex.materialOneOfControlTester

val CupertinoOneOfRenderer = ComposeJsonFormsRenderer { MaterialOneOfRenderer.Render(it) }
val cupertinoOneOfControlTester = materialOneOfControlTester
