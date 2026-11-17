package com.chobantonov.jsonfroms.cupertino.renderers.layouts

import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.material.renderers.layouts.materialCategorizationStepperTester

val CupertinoCategorizationStepperLayout = ComposeJsonFormsRenderer {
    CupertinoCategorizationContent(it, stepper = true)
}
val cupertinoCategorizationStepperTester = materialCategorizationStepperTester
