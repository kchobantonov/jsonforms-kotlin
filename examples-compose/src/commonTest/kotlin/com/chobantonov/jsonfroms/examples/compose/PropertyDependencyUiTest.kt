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
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.LeafCondition
import com.chobantonov.jsonfroms.core.models.Rule
import com.chobantonov.jsonfroms.core.models.RuleEffect
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import com.chobantonov.jsonfroms.core.store.ErrorTranslator
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class PropertyDependencyUiTest {
    @Test fun materialMissingDependentErrorAndTriggerClear() = dependency(false)
    @Test fun cupertinoMissingDependentErrorAndTriggerClear() = dependency(true)
    @Test fun materialOmittedDependentHasGroupFeedback() = groupFeedback(false, false)
    @Test fun cupertinoOmittedDependentHasGroupFeedback() = groupFeedback(true, false)
    @Test fun materialHiddenDependentHasGroupFeedback() = groupFeedback(false, true)
    @Test fun cupertinoHiddenDependentHasGroupFeedback() = groupFeedback(true, true)
    @Test fun materialOmittedDependentHasDirectFeedbackWithoutMarker() = groupFeedback(false, false, false)
    @Test fun cupertinoOmittedDependentHasDirectFeedbackWithoutMarker() = groupFeedback(true, false, false)
    @Test fun materialCollapsedGroupRetainsDirectDependencyFeedback() = groupFeedback(false, false, false, true)
    @Test fun cupertinoCollapsedGroupRetainsDirectDependencyFeedback() = groupFeedback(true, false, false, true)
    @Test fun materialHeaderlessVerticalShowsDependencyFeedback() = headerlessFeedback(false, "vertical")
    @Test fun cupertinoHeaderlessVerticalShowsDependencyFeedback() = headerlessFeedback(true, "vertical")
    @Test fun materialHeaderlessHorizontalShowsDependencyFeedback() = headerlessFeedback(false, "horizontal")
    @Test fun cupertinoHeaderlessHorizontalShowsDependencyFeedback() = headerlessFeedback(true, "horizontal")
    @Test fun materialNestedGroupAvoidsDuplicateDependencyFeedback() = headerlessFeedback(false, "group")
    @Test fun cupertinoNestedGroupAvoidsDuplicateDependencyFeedback() = headerlessFeedback(true, "group")
    @Test fun materialNestedTargetUsesItsControlFeedback() = nestedTargetFeedback(false)
    @Test fun cupertinoNestedTargetUsesItsControlFeedback() = nestedTargetFeedback(true)
    @Test fun materialMultipleDependentsHaveIndependentInlineErrors() = multipleDependents(false)
    @Test fun cupertinoMultipleDependentsHaveIndependentInlineErrors() = multipleDependents(true)

    private fun multipleDependents(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "${'$'}schema":"https://json-schema.org/draft/2020-12/schema",
          "type":"object",
          "properties":{"purchaseOrder":{"type":"string"},"billingAddress":{"type":"string"},
            "billingCity":{"type":"string"}},
          "dependentRequired":{"purchaseOrder":["billingAddress","billingCity"]}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"purchaseOrder":"PO-123"}"""))
        var validity: CombinedValidityStatus? = null
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = VerticalLayout(elements = listOf(
                    ControlElement("#/properties/purchaseOrder"),
                    ControlElement("#/properties/billingAddress"),
                    ControlElement("#/properties/billingCity"),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onValidityChange = { validity = it.status })
        }
        val fieldsWithError = hasSetTextAction() and
            SemanticsMatcher.keyIsDefined(SemanticsProperties.Error)
        assertEquals(3, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        assertEquals(2, onAllNodes(fieldsWithError).fetchSemanticsNodes().size)
        assertEquals(CombinedValidityStatus.Invalid, validity)
        runOnIdle { data = Json.parseToJsonElement(
            """{"purchaseOrder":"PO-123","billingAddress":"Main"}""") }
        waitForIdle()
        assertEquals(1, onAllNodes(fieldsWithError).fetchSemanticsNodes().size)
        assertEquals(CombinedValidityStatus.Invalid, validity)
        runOnIdle { data = Json.parseToJsonElement(
            """{"purchaseOrder":"PO-123","billingAddress":"Main","billingCity":"Sofia"}""") }
        waitForIdle()
        assertEquals(0, onAllNodes(fieldsWithError).fetchSemanticsNodes().size)
        assertEquals(CombinedValidityStatus.Valid, validity)
    }

    private fun nestedTargetFeedback(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "${'$'}schema":"http://json-schema.org/draft-07/schema#",
          "type":"object",
          "properties":{"purchaseOrder":{"type":"string"},"billingAddress":{"type":"string"}},
          "dependencies":{"purchaseOrder":["billingAddress"]}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"purchaseOrder":"PO-123"}"""))
        var validity: CombinedValidityStatus? = null
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = VerticalLayout(elements = listOf(
                    ControlElement("#/properties/purchaseOrder"),
                    GroupElement(label = "Billing", elements = listOf(
                        ControlElement("#/properties/billingAddress"))),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(translateError = ErrorTranslator { error, _, _ ->
                    "Dependency: ${error.keyword}"
                }),
                onValidityChange = { validity = it.status })
        }
        assertEquals(CombinedValidityStatus.Invalid, validity)
        assertEquals(2, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onAllNodes(hasSetTextAction()).onLast().assert(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        assertEquals(1, onAllNodesWithText("Dependency: dependencies").fetchSemanticsNodes().size)
        runOnIdle { data = Json.parseToJsonElement(
            """{"purchaseOrder":"PO-123","billingAddress":"Main Street"}""") }
        waitForIdle()
        assertEquals(CombinedValidityStatus.Valid, validity)
        onAllNodes(hasSetTextAction()).onLast().assert(
            !SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
    }

    private fun headerlessFeedback(cupertino: Boolean, kind: String) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "${'$'}schema":"http://json-schema.org/draft-07/schema#",
          "type":"object",
          "properties":{"purchaseOrder":{"type":"string"},"billingAddress":{"type":"string"}},
          "dependencies":{"purchaseOrder":["billingAddress"]}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"purchaseOrder":"PO-123"}"""))
        var validity: CombinedValidityStatus? = null
        val trigger = ControlElement("#/properties/purchaseOrder")
        val ui = when (kind) {
            "horizontal" -> HorizontalLayout(elements = listOf(trigger))
            "group" -> VerticalLayout(elements = listOf(GroupElement(
                label = "Order", elements = listOf(trigger))))
            else -> VerticalLayout(elements = listOf(trigger))
        }
        setContent {
            JsonForms(data = data, schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(translateError = ErrorTranslator { error, _, _ ->
                    "Dependency: ${error.keyword}"
                }),
                onValidityChange = { validity = it.status })
        }
        onNodeWithText("Dependency: dependencies").assertExists()
        assertEquals(CombinedValidityStatus.Invalid, validity)
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        runOnIdle { data = Json.parseToJsonElement(
            """{"purchaseOrder":"PO-123","billingAddress":"Main Street"}""") }
        waitForIdle()
        onNodeWithText("Dependency: dependencies").assertDoesNotExist()
        assertEquals(CombinedValidityStatus.Valid, validity)
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
    }

    private fun groupFeedback(cupertino: Boolean, hidden: Boolean, indicator: Boolean = true,
        collapsed: Boolean = false) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "${'$'}schema":"http://json-schema.org/draft-07/schema#",
          "type":"object",
          "properties":{"show":{"type":"boolean"},"purchaseOrder":{"type":"string"},
            "billingAddress":{"type":"string"}},
          "dependencies":{"purchaseOrder":["billingAddress"]}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement(
            """{"show":false,"purchaseOrder":"PO-123"}"""))
        var validity: CombinedValidityStatus? = null
        val controls = listOf(ControlElement("#/properties/purchaseOrder")) +
            if (hidden) listOf(ControlElement("#/properties/billingAddress", rule = Rule(
                RuleEffect.SHOW, LeafCondition("#/show", JsonPrimitive(true))))) else emptyList()
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = GroupElement(label = "Order", options = mapOf(
                    "showValidationIndicator" to JsonPrimitive(indicator),
                    "showValidationIndicatorCount" to JsonPrimitive(true),
                    "collapsible" to JsonPrimitive(collapsed),
                    "collapsed" to JsonPrimitive(collapsed),
                ), elements = controls),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(translateError = ErrorTranslator { error, _, _ ->
                    "Dependency: ${error.keyword}"
                }),
                onValidityChange = { validity = it.status },
            )
        }
        assertEquals(if (collapsed) 0 else 1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        if (indicator) onNodeWithContentDescription("1 error").assertExists()
        else onNodeWithContentDescription("1 error").assertDoesNotExist()
        if (!hidden) onNodeWithText("Dependency: dependencies").assertExists()
        assertEquals(CombinedValidityStatus.Invalid, validity)
        runOnIdle { data = Json.parseToJsonElement(
            """{"show":false,"purchaseOrder":"PO-123","billingAddress":"Main Street"}""") }
        waitForIdle()
        onNodeWithContentDescription("1 error").assertDoesNotExist()
        onNodeWithText("Dependency: dependencies").assertDoesNotExist()
        assertEquals(CombinedValidityStatus.Valid, validity)
        if (collapsed) onNodeWithText("Order").performClick()
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
    }

    private fun dependency(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "${'$'}schema":"http://json-schema.org/draft-07/schema#",
          "type":"object",
          "properties":{"purchaseOrder":{"type":"string"},"billingAddress":{"type":"string"}},
          "dependencies":{"purchaseOrder":["billingAddress"]}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"purchaseOrder":"PO-123"}"""))
        var validity: CombinedValidityStatus? = null
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = VerticalLayout(elements = listOf(
                    ControlElement("#/properties/purchaseOrder"),
                    ControlElement("#/properties/billingAddress"),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data },
                onValidityChange = { validity = it.status },
            )
        }
        assertEquals(CombinedValidityStatus.Invalid, validity)
        onAllNodes(hasSetTextAction()).onLast().assert(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        runOnIdle { data = Json.parseToJsonElement(
            """{"purchaseOrder":"PO-123","billingAddress":"Main Street"}""") }
        waitForIdle()
        assertEquals(CombinedValidityStatus.Valid, validity)
        onAllNodes(hasSetTextAction()).onLast().assert(
            !SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("")
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"billingAddress":"Main Street"}"""), data)
        assertEquals(CombinedValidityStatus.Valid, validity)
        assertEquals(2, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
    }
}
