package com.chobantonov.jsonfroms.cupertino.renderers.complex

import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.material.renderers.complex.MaterialAllOfRenderer
import com.chobantonov.jsonfroms.material.renderers.complex.materialAllOfControlTester

val CupertinoAllOfRenderer = ComposeJsonFormsRenderer { MaterialAllOfRenderer.Render(it) }
val cupertinoAllOfControlTester = materialAllOfControlTester
