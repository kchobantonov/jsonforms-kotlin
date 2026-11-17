package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.validation.OptimumCodeJsonSchemaValidator
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class BranchFeedbackFixtureUiTest {
    @Test fun materialImportedBranchFeedbackComparesTwoEditors() = scenario(false)
    @Test fun cupertinoImportedBranchFeedbackComparesTwoEditors() = scenario(true)

    private fun scenario(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-mixed-control" }
        val schema = requireNotNull(requireNotNull(example.schema).properties).getValue("branchFeedback")
        val initial = example.data.jsonObject.getValue("branchFeedback")
        assertTrue(OptimumCodeJsonSchemaValidator().validate(schema, initial).isEmpty())
        var data by mutableStateOf<JsonElement>(initial)
        var writes = 0
        var documentErrors = -1
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = VerticalLayout(elements = listOf(
                    ControlElement("#"),
                    ControlElement("#", options = mapOf(
                        "validateActiveBranch" to JsonPrimitive(false))),
                )),
                config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; documentErrors = it.errors.size; writes++ })
        }
        val errors = hasSetTextAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.Error)
        assertEquals(0, onAllNodes(errors).fetchSemanticsNodes().size)
        onAllNodesWithText("Short text")[0].performClick()
        assertEquals(1, onAllNodes(errors).fetchSemanticsNodes().size)
        onAllNodesWithText("Short text")[1].performClick()
        assertEquals(1, onAllNodes(errors).fetchSemanticsNodes().size)
        runOnIdle { assertEquals(0, writes) }
        onAllNodesWithText("Long text")[0].performClick()
        assertEquals(0, onAllNodes(errors).fetchSemanticsNodes().size)
        runOnIdle { assertEquals(0, writes); assertEquals(initial, data) }
        assertTrue(OptimumCodeJsonSchemaValidator().validate(schema, data).isEmpty())
        assertEquals(-1, documentErrors)
    }
}
