package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.store.JsonFormsUISchemaRegistryEntry
import com.chobantonov.jsonfroms.core.testers.TesterContext
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.cupertino.renderers.complex.cupertinoObjectControlTester
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import com.chobantonov.jsonfroms.material.renderers.complex.materialObjectControlTester
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class ConditionalFieldsFixtureUiTest {
    @Test fun optedInAllOfObjectWinsWithoutStealingAlternatives() {
        val control = ControlElement("#", options = mapOf("conditionalFields" to JsonPrimitive(true)))
        val allOf = JsonSchema(Json.parseToJsonElement("""{"type":"object","allOf":[{}]}""").jsonObject)
        val alternative = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","allOf":[{}],"anyOf":[{},{}]}""").jsonObject)
        assertEquals(4, materialObjectControlTester(control, allOf, TesterContext(rootSchema = allOf)))
        assertEquals(104, cupertinoObjectControlTester(control, allOf, TesterContext(rootSchema = allOf)))
        assertEquals(2, materialObjectControlTester(control, alternative, TesterContext(rootSchema = alternative)))
        assertEquals(102, cupertinoObjectControlTester(control, alternative, TesterContext(rootSchema = alternative)))
    }
    @Test fun materialGeneratedConditionalFieldsFollowCurrentData() = fields(false, false)
    @Test fun cupertinoGeneratedConditionalFieldsFollowCurrentData() = fields(true, false)
    @Test fun materialAuthoredConditionalFieldsKeepTheirGroups() = fields(false, true)
    @Test fun cupertinoAuthoredConditionalFieldsKeepTheirGroups() = fields(true, true)
    @Test fun materialMissingObjectProjectsElseWithoutWriting() = missingObject(false)
    @Test fun cupertinoMissingObjectProjectsElseWithoutWriting() = missingObject(true)
    @Test fun materialInvalidConditionShowsBulgarianDiagnostic() = invalidCondition(false)
    @Test fun cupertinoInvalidConditionShowsBulgarianDiagnostic() = invalidCondition(true)
    @Test fun materialRetainedFieldIsNotAnAdditionalProperty() = reservedAdditional(false)
    @Test fun cupertinoRetainedFieldIsNotAnAdditionalProperty() = reservedAdditional(true)
    @Test fun materialOverlappingFieldKeepsStringEditor() = overlappingField(false)
    @Test fun cupertinoOverlappingFieldKeepsStringEditor() = overlappingField(true)
    @Test fun materialRegisteredDetailKeepsPlacementAcrossBranchChanges() = registeredDetail(false)
    @Test fun cupertinoRegisteredDetailKeepsPlacementAcrossBranchChanges() = registeredDetail(true)

    private fun registeredDetail(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-conditional-fields" }
        val detail = GroupElement(label = "Registered fields", elements = listOf(
            ControlElement("#/properties/customerType"),
            GroupElement(label = "Business group", elements = listOf(
                ControlElement("#/properties/vatNumber"),
                ControlElement("#/properties/billingAddress"),
            )),
            GroupElement(label = "Personal group", elements = listOf(
                ControlElement("#/properties/nickname"),
            )),
        ))
        val registry = listOf(JsonFormsUISchemaRegistryEntry(
            tester = { _, scope, _ -> if (scope == "#") 10 else -1 },
            uischema = detail,
        ))
        var data by mutableStateOf<JsonElement>(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema,
                uischema = ControlElement("#", options = mapOf(
                    "detail" to JsonPrimitive("REGISTERED"),
                    "conditionalFields" to JsonPrimitive(true))),
                uischemas = registry,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodes(hasText("Registered fields")).onFirst().assertExists()
        onAllNodes(hasText("Business group")).onFirst().assertExists()
        onAllNodes(hasText("Personal group")).onFirst().assertExists()
        onAllNodes(hasText("Vat Number", substring = true, ignoreCase = true)).onFirst().assertExists()
        onAllNodes(hasText("Billing Address", substring = true, ignoreCase = true)).onFirst().assertExists()
        onAllNodes(hasText("Nickname", substring = true, ignoreCase = true)).assertCountEquals(0)

        runOnIdle { data = JsonObject(data.jsonObject +
            ("customerType" to JsonPrimitive("personal")) +
            ("nickname" to JsonPrimitive("Ace")) - "purchaseOrder") }
        onAllNodes(hasText("Registered fields")).onFirst().assertExists()
        onAllNodes(hasText("Business group")).onFirst().assertExists()
        onAllNodes(hasText("Personal group")).onFirst().assertExists()
        onAllNodes(hasText("Nickname", substring = true, ignoreCase = true)).onFirst().assertExists()
        onAllNodes(hasText("Vat Number", substring = true, ignoreCase = true)).assertCountEquals(0)
        onAllNodes(hasText("Billing Address", substring = true, ignoreCase = true)).assertCountEquals(0)
        assertEquals(JsonPrimitive("VAT123"), data.jsonObject["vatNumber"])
        assertEquals(JsonPrimitive("London"), data.jsonObject["billingAddress"])
        assertEquals(0, changes)
    }

    private fun overlappingField(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{"type":"object",
          "properties":{"kind":{"type":"string"},"shared":{"type":"string"}},
          "allOf":[{"properties":{"shared":{"maxLength":3}}}],
          "if":{"properties":{"kind":{"const":"business"}},"required":["kind"]},
          "then":{"properties":{"shared":{"minLength":3}}}}""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"kind":"business","shared":"abc"}"""))
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#", options = mapOf("conditionalFields" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        onAllNodes(hasSetTextAction() and hasText("abc")).onFirst().performTextReplacement("abcd")
        assertEquals(JsonPrimitive("abc"), data.jsonObject["shared"])
    }

    private fun reservedAdditional(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-conditional-fields" }
        val category = (example.uischema as CategorizationElement).elements
            .filterIsInstance<CategoryElement>().single { it.label == "Generated fields" }
        val control = category.elements.filterIsInstance<ControlElement>().single()
        val schema = JsonSchema(JsonObject(example.schema!!.toJsonObject() +
            ("additionalProperties" to JsonObject(mapOf("type" to JsonPrimitive("string"))))))
        var data by mutableStateOf<JsonElement>(JsonObject(example.data.jsonObject +
            ("note" to JsonPrimitive("free"))))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = control, config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodes(hasText("note")).onFirst().assertExists()
        onAllNodes(hasContentDescription("Delete button")).assertCountEquals(1)

        runOnIdle { data = JsonObject(data.jsonObject + ("customerType" to JsonPrimitive("personal"))) }
        onAllNodes(hasText("Vat Number", substring = true, ignoreCase = true)).assertCountEquals(0)
        onAllNodes(hasText("vatNumber")).assertCountEquals(0)
        onAllNodes(hasText("note")).onFirst().assertExists()
        onAllNodes(hasContentDescription("Delete button")).assertCountEquals(1)
        assertEquals(JsonPrimitive("VAT123"), data.jsonObject["vatNumber"])
        assertEquals(0, changes)
    }

    private fun invalidCondition(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{"type":"object",
          "if":{"${'$'}ref":"#/${'$'}defs/missing"},
          "then":{"properties":{"yes":{"type":"string"}}},
          "else":{"properties":{"no":{"type":"string"}}}}""").jsonObject)
        var data by mutableStateOf<JsonElement>(JsonObject(emptyMap()))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#", options = mapOf("conditionalFields" to JsonPrimitive(true))),
                i18n = JsonFormsI18nState(locale = "bg-BG"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodes(hasText("Условието за полетата не може да бъде оценено.")).onFirst().assertExists()
        onAllNodes(hasText("Yes")).assertCountEquals(0)
        onAllNodes(hasText("No")).assertCountEquals(0)
        assertEquals(0, changes)
    }

    private fun missingObject(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-conditional-fields" }
        val category = (example.uischema as CategorizationElement).elements
            .filterIsInstance<CategoryElement>().single { it.label == "Generated fields" }
        val control = category.elements.filterIsInstance<ControlElement>().single()
        var data by mutableStateOf<JsonElement>(JsonNull)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema, uischema = control, config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodes(hasText("Nickname", substring = true, ignoreCase = true)).onFirst().assertExists()
        onAllNodes(hasText("Vat Number", substring = true, ignoreCase = true)).assertCountEquals(0)
        assertEquals(JsonNull, data)
        assertEquals(0, changes)

        runOnIdle { data = example.data }
        onAllNodes(hasText("Vat Number", substring = true, ignoreCase = true)).onFirst().assertExists()
        onAllNodes(hasText("Nickname", substring = true, ignoreCase = true)).assertCountEquals(0)
        assertEquals(0, changes)
    }

    private fun fields(cupertino: Boolean, authored: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-conditional-fields" }
        val category = (example.uischema as CategorizationElement).elements
            .filterIsInstance<CategoryElement>().single { it.label == if (authored) "Authored placement" else "Generated fields" }
        val control = category.elements.filterIsInstance<ControlElement>().single()
        var data by mutableStateOf<JsonElement>(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema, uischema = control, config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        fun field(label: String) = onAllNodes(hasText(label, substring = true, ignoreCase = true)).onFirst()
        field("Vat Number").assertExists()
        field("Billing Address").assertExists()
        onAllNodes(hasText("Nickname", substring = true, ignoreCase = true)).assertCountEquals(0)
        if (authored) {
            field("Identity").assertExists()
            field("Billing").assertExists()
        }
        assertEquals(0, changes)

        runOnIdle {
            data = JsonObject(data.jsonObject + ("customerType" to JsonPrimitive("personal")) +
                ("nickname" to JsonPrimitive("Ace")))
        }
        field("Nickname").assertExists()
        onAllNodes(hasText("Vat Number", substring = true, ignoreCase = true)).assertCountEquals(0)
        field("Billing Address").assertExists()
        assertEquals(JsonPrimitive("VAT123"), data.jsonObject["vatNumber"])
        assertEquals(0, changes)

        runOnIdle { data = JsonObject(data.jsonObject - "purchaseOrder") }
        onAllNodes(hasText("Billing Address", substring = true, ignoreCase = true)).assertCountEquals(0)
        assertEquals(JsonPrimitive("London"), data.jsonObject["billingAddress"])
        assertEquals(0, changes)
    }
}
