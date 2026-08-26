package com.chobantonov.jsonfroms.cupertino.renderers.complex

import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.material.renderers.complex.MaterialEnumArrayRenderer
import com.chobantonov.jsonfroms.material.renderers.complex.materialEnumArrayRendererTester

val CupertinoEnumArrayRenderer = ComposeJsonFormsRenderer { MaterialEnumArrayRenderer.Render(it) }
val cupertinoEnumArrayRendererTester = materialEnumArrayRendererTester
