package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class SpecEditorDetailsFixtureUiTest {
    @Test fun materialImportedObjectAndMixedDetails() = scenario(false)
    @Test fun cupertinoImportedObjectAndMixedDetails() = scenario(true)

    private fun scenario(cupertino: Boolean) = runComposeUiTest {
        val fixture = getBuiltInExamples().first { it.name == "spec-editor-details" }
        setContent {
            JsonForms(
                data = fixture.data, schema = fixture.schema, uischema = fixture.uischema,
                uischemas = fixture.uischemas, config = fixture.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
            )
        }
        onNodeWithText("Generated object").assertExists()
        onNodeWithText("Object-specific note").assertExists()
    }
}
