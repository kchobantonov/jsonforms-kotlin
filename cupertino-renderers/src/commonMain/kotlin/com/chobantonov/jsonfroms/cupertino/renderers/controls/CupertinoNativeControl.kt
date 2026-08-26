package com.chobantonov.jsonfroms.cupertino.renderers.controls

import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.material.renderers.controls.MaterialNativeControl
import com.chobantonov.jsonfroms.material.renderers.controls.materialNativeControlTester

val CupertinoNativeControl = ComposeJsonFormsRenderer { MaterialNativeControl.Render(it) }
val cupertinoNativeControlTester = materialNativeControlTester
