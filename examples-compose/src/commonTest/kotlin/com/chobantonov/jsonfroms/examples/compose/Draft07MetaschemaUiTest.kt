package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class Draft07MetaschemaUiTest {
    @Test fun materialReferencedAllOfUsesTheOuterScalarControl() = referencedAllOf(false)
    @Test fun cupertinoReferencedAllOfUsesTheOuterScalarControl() = referencedAllOf(true)
    @Test fun materialUnrestrictedAllOfKeepsSchemaValidation() = referencedAllOf(false, restrict = false)
    @Test fun cupertinoUnrestrictedAllOfKeepsSchemaValidation() = referencedAllOf(true, restrict = false)

    private fun referencedAllOf(cupertino: Boolean, restrict: Boolean = true) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-draft-07-metaschema" }
        var data by mutableStateOf<JsonElement>(JsonObject(example.data.jsonObject + ("minLength" to JsonPrimitive(1))))
        var changes = 0
        setContent {
            JsonForms(
                data = data,
                schema = example.schema,
                uischema = ControlElement(scope = "#/properties/minLength"),
                config = mapOf("restrict" to restrict),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ },
            )
        }
        onNodeWithText("No applicable renderer found.").assertDoesNotExist()
        onNodeWithText("Min Length").assertExists()
        onAllNodes(hasSetTextAction()).assertCountEquals(1)
        assertEquals(0, changes)
        onAllNodes(hasSetTextAction() and hasText("1")).onFirst().performTextReplacement("2")
        assertEquals(JsonPrimitive(2), data.jsonObject["minLength"])
        assertEquals(1, changes)
        onAllNodes(hasSetTextAction() and hasText("2")).onFirst().performTextReplacement("-1")
        assertEquals(JsonPrimitive(if (restrict) 2 else -1), data.jsonObject["minLength"])
        assertEquals(if (restrict) 1 else 2, changes)
        onAllNodes(hasSetTextAction() and hasText("-1")).onFirst().assert(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        onAllNodes(hasSetTextAction() and hasText("-1")).onFirst().performTextReplacement("3")
        assertEquals(JsonPrimitive(3), data.jsonObject["minLength"])
        assertEquals(if (restrict) 2 else 3, changes)
        onAllNodes(hasSetTextAction() and hasText("3")).onFirst().assert(
            !SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
    }
}
