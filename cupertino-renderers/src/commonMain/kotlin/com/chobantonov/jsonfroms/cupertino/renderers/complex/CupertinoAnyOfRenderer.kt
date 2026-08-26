package com.chobantonov.jsonfroms.cupertino.renderers.complex

import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.material.renderers.complex.MaterialAnyOfRenderer
import com.chobantonov.jsonfroms.material.renderers.complex.materialAnyOfControlTester

val CupertinoAnyOfRenderer = ComposeJsonFormsRenderer { MaterialAnyOfRenderer.Render(it) }
val cupertinoAnyOfControlTester = materialAnyOfControlTester
