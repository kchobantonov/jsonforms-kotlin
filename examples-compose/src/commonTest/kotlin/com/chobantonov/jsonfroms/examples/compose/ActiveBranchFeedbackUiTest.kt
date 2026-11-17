package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.ValidationMode
import com.chobantonov.jsonfroms.core.validation.OptimumCodeJsonSchemaValidator
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ActiveBranchFeedbackUiTest {
    private fun schema(keyword: String) = JsonSchema(Json.parseToJsonElement("""{
        "type":"object","properties":{"choice":{"type":"object","$keyword":[
            {"title":"Strict","type":"object","properties":{
                "name":{"type":"string","minLength":5}},"required":["name"]},
            {"title":"Flexible","type":"object","properties":{
                "name":{"type":"string","minLength":2}},"required":["name"]}
        ]}}
    }""").jsonObject)
    private val initial = Json.parseToJsonElement("""{"choice":{"name":"Ada"}}""")

    @Test fun materialDefaultShowsLocalErrorWithoutChangingDocumentValidity() = scenario(false, "anyOf", null, null, true)
    @Test fun cupertinoDefaultShowsLocalErrorWithoutChangingDocumentValidity() = scenario(true, "anyOf", null, null, true)
    @Test fun materialControlFalseOverridesGlobalTrue() = scenario(false, "anyOf", true, false, false)
    @Test fun cupertinoControlFalseOverridesGlobalTrue() = scenario(true, "anyOf", true, false, false)
    @Test fun materialControlTrueOverridesGlobalFalse() = scenario(false, "anyOf", false, true, true)
    @Test fun cupertinoControlTrueOverridesGlobalFalse() = scenario(true, "anyOf", false, true, true)
    @Test fun materialOneOfKeepsFullyValidBranchSelected() = scenario(false, "oneOf", null, null, false)
    @Test fun cupertinoOneOfKeepsFullyValidBranchSelected() = scenario(true, "oneOf", null, null, false)
    @Test fun materialOneOfFalseHidesLocalError() = scenario(false, "oneOf", true, false, false)
    @Test fun cupertinoOneOfFalseHidesLocalError() = scenario(true, "oneOf", true, false, false)
    @Test fun materialHiddenValidationHidesLocalError() = scenario(false, "anyOf", null, null, false, ValidationMode.ValidateAndHide)
    @Test fun cupertinoHiddenValidationHidesLocalError() = scenario(true, "anyOf", null, null, false, ValidationMode.ValidateAndHide)
    @Test fun materialNoValidationHidesLocalError() = scenario(false, "anyOf", null, null, false, ValidationMode.NoValidation)
    @Test fun cupertinoNoValidationHidesLocalError() = scenario(true, "anyOf", null, null, false, ValidationMode.NoValidation)
    @Test fun materialUnresolvedBranchRefDoesNotShowLocalCompilationFailure() = unresolvedRef(false)
    @Test fun cupertinoUnresolvedBranchRefDoesNotShowLocalCompilationFailure() = unresolvedRef(true)
    @Test fun materialAnyOfShowsBranchObjectError() = rootError(false, "anyOf", true)
    @Test fun cupertinoAnyOfShowsBranchObjectError() = rootError(true, "anyOf", true)
    @Test fun materialOneOfKeepsFullyValidObjectBranchSelected() = rootError(false, "oneOf", true)
    @Test fun cupertinoOneOfKeepsFullyValidObjectBranchSelected() = rootError(true, "oneOf", true)
    @Test fun materialOneOfCanHideBranchObjectError() = rootError(false, "oneOf", false)
    @Test fun cupertinoOneOfCanHideBranchObjectError() = rootError(true, "oneOf", false)

    private fun unresolvedRef(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"choice":{"oneOf":[
                {"title":"Only branch","type":"object","properties":{
                    "name":{ "${'$'}ref":"#/definitions/absent" }} }
            ]}}
        }""").jsonObject)
        var writes = 0
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"choice":{"name":"Ada"}}"""),
                schema = schema, uischema = ControlElement("#/properties/choice"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { writes++ })
        }
        onNodeWithText("Only branch").assertExists()
        assertEquals(0, onAllNodesWithText("Unable to compile JSON Schema", substring = true)
            .fetchSemanticsNodes().size)
        runOnIdle { assertEquals(0, writes) }
    }

    private fun rootError(cupertino: Boolean, keyword: String, feedback: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"choice":{"type":"object","$keyword":[
                {"title":"Strict","type":"object","minProperties":2,
                    "properties":{"name":{"type":"string"},"age":{"type":"integer"}}},
                {"title":"Flexible","type":"object","minProperties":1,
                    "properties":{"name":{"type":"string"}}}
            ]}}
        }""").jsonObject)
        assertTrue(OptimumCodeJsonSchemaValidator().validate(schema, initial).isEmpty())
        var data by mutableStateOf<JsonElement>(initial)
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/choice", options =
                    mapOf("validateActiveBranch" to JsonPrimitive(feedback))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; writes++ })
        }
        if (keyword == "anyOf") onNodeWithText("Strict").performClick()
        val errorNodes = onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        assertEquals(if (feedback && keyword == "anyOf") 1 else 0,
            errorNodes.fetchSemanticsNodes().size)
        runOnIdle { assertEquals(0, writes) }
        if (feedback && keyword == "anyOf") {
            runOnIdle { data = Json.parseToJsonElement("""{"choice":{"name":"Ada","age":5}}""") }
            waitForIdle()
            assertEquals(0, errorNodes.fetchSemanticsNodes().size)
            runOnIdle { assertEquals(0, writes) }
        }
    }

    private fun scenario(cupertino: Boolean, keyword: String, global: Boolean?, control: Boolean?,
        feedback: Boolean, validationMode: ValidationMode? = null) = runComposeUiTest {
        val schema = schema(keyword)
        assertTrue(OptimumCodeJsonSchemaValidator().validate(schema, initial).isEmpty())
        var data by mutableStateOf<JsonElement>(initial)
        var writes = 0
        var documentErrors = -1
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/choice", options = if (control == null)
                    emptyMap() else mapOf("validateActiveBranch" to JsonPrimitive(control))),
                config = if (global == null) emptyMap() else mapOf("validateActiveBranch" to global),
                validationMode = validationMode,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; documentErrors = it.errors.size; writes++ })
        }
        if (keyword == "anyOf") onNodeWithText("Strict").performClick()
        val editorWithError = hasSetTextAction() and
            SemanticsMatcher.keyIsDefined(SemanticsProperties.Error)
        assertEquals(if (feedback && keyword == "anyOf") 1 else 0,
            onAllNodes(editorWithError).fetchSemanticsNodes().size)
        runOnIdle { assertEquals(0, writes) }
        onAllNodes(hasSetTextAction())[0].performTextReplacement("Al")
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{"choice":{"name":"Al"}}"""), data)
            assertEquals(1, writes)
            assertEquals(0, documentErrors)
        }
    }
}
