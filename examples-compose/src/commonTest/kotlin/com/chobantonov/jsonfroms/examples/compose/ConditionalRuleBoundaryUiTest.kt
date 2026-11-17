package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.Rule
import com.chobantonov.jsonfroms.core.models.RuleEffect
import com.chobantonov.jsonfroms.core.models.SchemaBasedCondition
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import com.chobantonov.jsonfroms.core.store.ValidationMode
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ConditionalRuleBoundaryUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement("""{
      "type":"object","properties":{"show":{"type":"boolean"},"name":{"type":"string"}},
      "if":{"properties":{"show":{"const":true}},"required":["show"]},
      "then":{"required":["name"]}
    }""").jsonObject)

    @Test fun materialRuleReadsDataUnderNoValidation() = dataRule(false)
    @Test fun cupertinoRuleReadsDataUnderNoValidation() = dataRule(true)
    @Test fun materialSchemaConditionDoesNotCreateVisibilityRule() = schemaCondition(false)
    @Test fun cupertinoSchemaConditionDoesNotCreateVisibilityRule() = schemaCondition(true)
    @Test fun materialElseValidationDoesNotHideControl() = conditionalKeyword(false, true)
    @Test fun cupertinoElseValidationDoesNotHideControl() = conditionalKeyword(true, true)
    @Test fun materialDependencyValidationDoesNotHideControl() = conditionalKeyword(false, false)
    @Test fun cupertinoDependencyValidationDoesNotHideControl() = conditionalKeyword(true, false)

    private fun conditionalKeyword(cupertino: Boolean, elseBranch: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(if (elseBranch) """{
          "type":"object","properties":{"mode":{"type":"string"},"name":{"type":"string"}},
          "if":{"properties":{"mode":{"const":"strict"}},"required":["mode"]},
          "else":{"required":["name"]}
        }""" else """{
          "type":"object","properties":{"flag":{"type":"boolean"},"name":{"type":"string"}},
          "dependentRequired":{"flag":["name"]}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement(
            if (elseBranch) """{"mode":"relaxed"}""" else """{"flag":true}"""))
        var validity: CombinedValidityStatus? = null
        var schemaErrorCount = 0
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ControlElement("#/properties/name"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onValidityChange = {
                    validity = it.status
                    schemaErrorCount = it.schemaErrorCount
                },
            )
        }
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        assertEquals(CombinedValidityStatus.Invalid, validity)
        assertEquals(1, schemaErrorCount)
        if (!elseBranch) onAllNodes(hasSetTextAction()).onFirst().assert(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        runOnIdle { data = Json.parseToJsonElement(
            if (elseBranch) """{"mode":"strict"}""" else """{}""") }
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        assertEquals(CombinedValidityStatus.Valid, validity)
        assertEquals(0, schemaErrorCount)
        if (!elseBranch) onAllNodes(hasSetTextAction()).onFirst().assert(
            !SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
    }

    private fun dataRule(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"show":false,"name":"Ada"}"""))
        var hostErrors by mutableStateOf(listOf(ErrorObject(instancePath = "/name", message = "server")))
        var validity by mutableStateOf<CombinedValidityStatus?>(null)
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ControlElement("#/properties/name", rule = Rule(
                    RuleEffect.SHOW,
                    SchemaBasedCondition(
                        scope = "#/properties/show",
                        schema = JsonSchema(Json.parseToJsonElement("""{"const":true}""").jsonObject),
                        failWhenUndefined = true,
                    ),
                )),
                validationMode = ValidationMode.NoValidation,
                additionalErrors = hostErrors,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++ },
                onValidityChange = { validity = it.status },
            )
        }
        assertEquals(0, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        assertEquals(CombinedValidityStatus.Invalid, validity)
        runOnIdle { hostErrors = emptyList() }
        assertEquals(0, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        assertEquals(CombinedValidityStatus.Unvalidated, validity)
        runOnIdle { data = Json.parseToJsonElement("""{"show":true,"name":"Ada"}""") }
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        assertEquals(0, changes)
    }

    private fun schemaCondition(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"show":false}"""))
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ControlElement("#/properties/name"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
            )
        }
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        runOnIdle { data = Json.parseToJsonElement("""{"show":true}""") }
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
    }
}
