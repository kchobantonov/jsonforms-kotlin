package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.semantics.SemanticsProperties
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.validation.OptimumCodeJsonSchemaValidator
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.examples.ExampleDescription
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalTestApi::class)
class CombinatorsFixtureUiTest {
    private fun fixture() = getBuiltInExamples().first { it.name == "spec-combinators" }

    private fun category(example: ExampleDescription, label: String): CategoryElement {
        val categorization = (example.uischema as VerticalLayout).elements
            .filterIsInstance<CategorizationElement>().single()
        return categorization.elements.filterIsInstance<CategoryElement>().single { it.label == label }
    }

    private fun control(example: ExampleDescription, category: String, scope: String): ControlElement =
        category(example, category).elements.filterIsInstance<ControlElement>().single { it.scope == scope }

    @Test fun materialImportedOneOfConfirmsBranchChange() = oneOf(false)
    @Test fun cupertinoImportedOneOfConfirmsBranchChange() = oneOf(true)
    @Test fun materialImportedAnyOfNavigatesWithoutWriting() = anyOf(false)
    @Test fun cupertinoImportedAnyOfNavigatesWithoutWriting() = anyOf(true)
    @Test fun materialImportedAllOfRendersEveryBranch() = allOf(false)
    @Test fun cupertinoImportedAllOfRendersEveryBranch() = allOf(true)
    @Test fun materialImportedSchemaConditionAndUiRuleStayIndependent() = conditional(false)
    @Test fun cupertinoImportedSchemaConditionAndUiRuleStayIndependent() = conditional(true)
    @Test fun materialImportedAppointmentFieldsFollowMethod() = appointment(false)
    @Test fun cupertinoImportedAppointmentFieldsFollowMethod() = appointment(true)
    @Test fun materialImportedScalarAlternativesUseSingleInputs() = scalar(false)
    @Test fun cupertinoImportedScalarAlternativesUseSingleInputs() = scalar(true)
    @Test fun importedScalarAlternativesValidateBothValues() {
        val example = fixture()
        val schema = requireNotNull(example.schema)
        val validator = OptimumCodeJsonSchemaValidator()
        assertTrue(validator.validate(schema, example.data).isEmpty())
        val invalidAttendees = JsonObject(example.data.jsonObject + ("attendees" to JsonPrimitive(30)))
        assertTrue(validator.validate(schema, invalidAttendees).any { it.instancePath == "/attendees" })
        val invalidSeats = JsonObject(example.data.jsonObject + ("seatsPerTable" to JsonPrimitive(15)))
        assertTrue(validator.validate(schema, invalidSeats).any { it.instancePath == "/seatsPerTable" })
    }

    private fun oneOf(cupertino: Boolean) = runComposeUiTest {
        val example = fixture()
        var data by mutableStateOf<JsonElement>(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema,
                uischema = control(example, "oneOf", "#/properties/contact"), config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Email contact").performClick()
        onNodeWithText("Phone contact").performClick()
        onNodeWithText("Change this branch?").assertExists()
        runOnIdle { assertEquals(example.data, data); assertEquals(0, changes) }
        onNodeWithText("Yes").performClick()
        runOnIdle {
            val contact = data.jsonObject.getValue("contact").jsonObject
            assertEquals("Alex Moreau", contact.getValue("name").jsonPrimitive.content)
            assertEquals("phone", contact.getValue("kind").jsonPrimitive.content)
            assertNull(contact["email"])
            assertEquals(1, changes)
        }
    }

    private fun anyOf(cupertino: Boolean) = runComposeUiTest {
        val example = fixture()
        var changes = 0
        setContent {
            JsonForms(data = example.data, schema = example.schema,
                uischema = control(example, "anyOf", "#/properties/location"), config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++ })
        }
        onNodeWithText("Online").performClick()
        onNodeWithText("Change this branch?").assertDoesNotExist()
        assertEquals(0, changes)
    }

    private fun allOf(cupertino: Boolean) = runComposeUiTest {
        val example = fixture()
        var changes = 0
        setContent {
            JsonForms(data = example.data, schema = example.schema,
                uischema = control(example, "allOf", "#/properties/equipment"), config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++ })
        }
        onAllNodes(hasSetTextAction() and hasText("Projector")).onFirst().assertExists()
        onAllNodes(hasSetTextAction() and hasText("AB1234")).onFirst().assertExists()
        onNodeWithText("In working order").assertExists()
        assertEquals(0, changes)
    }

    private fun conditional(cupertino: Boolean) = runComposeUiTest {
        val example = fixture()
        var data by mutableStateOf<JsonElement>(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema,
                uischema = VerticalLayout(elements = category(example, "Schema condition vs UI rule").elements),
                config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodesWithText("Needed by").assertCountEquals(1)
        assertEquals(0, changes)
        onAllNodes(hasClickAction()).onFirst().performClick()
        assertEquals(JsonPrimitive(true), data.jsonObject["urgent"])
        onAllNodesWithText("Needed by").assertCountEquals(2)
        assertEquals(JsonPrimitive(true), data.jsonObject["urgent"])
        assertEquals(1, changes)
        assertTrue(OptimumCodeJsonSchemaValidator().validate(requireNotNull(example.schema), data)
            .any { it.keyword == "required" })
    }

    private fun appointment(cupertino: Boolean) = runComposeUiTest {
        val example = fixture()
        var data by mutableStateOf<JsonElement>(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema,
                uischema = control(example, "Fields that appear from the schema", "#/properties/appointment"),
                config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Collection point").assertExists()
        onNodeWithText("Postcode").assertDoesNotExist()
        onAllNodesWithText("collect").onFirst().performClick()
        onNodeWithText("post").performClick()
        assertEquals("post", data.jsonObject.getValue("appointment").jsonObject.getValue("method").jsonPrimitive.content)
        onNodeWithText("Postcode").assertExists()
        assertEquals(1, changes)
    }

    private fun scalar(cupertino: Boolean) = runComposeUiTest {
        val example = fixture()
        var data by mutableStateOf<JsonElement>(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema,
                uischema = VerticalLayout(elements = category(example, "One input, several alternatives").elements),
                config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodes(hasSetTextAction()).assertCountEquals(2)
        onAllNodes(hasSetTextAction() and hasText("8")).onFirst().performTextReplacement("30")
        assertEquals(JsonPrimitive(30), data.jsonObject["attendees"])
        assertEquals(1, changes)
        assertTrue(OptimumCodeJsonSchemaValidator().validate(requireNotNull(example.schema), data)
            .any { it.instancePath == "/attendees" })
        onAllNodes(hasSetTextAction() and hasText("30")).onFirst().assert(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        onAllNodes(hasSetTextAction() and hasText("30")).onFirst().performTextReplacement("8")
        assertEquals(JsonPrimitive(8), data.jsonObject["attendees"])
        assertEquals(2, changes)
        onAllNodes(hasSetTextAction() and hasText("8")).onFirst().assert(
            !SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        onAllNodes(hasSetTextAction() and hasText("6")).onFirst().performTextReplacement("15")
        assertEquals(JsonPrimitive(15), data.jsonObject["seatsPerTable"])
        assertEquals(3, changes)
        onAllNodes(hasSetTextAction() and hasText("15")).onFirst().assert(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        onAllNodes(hasSetTextAction() and hasText("15")).onFirst().performTextReplacement("10")
        assertEquals(JsonPrimitive(10), data.jsonObject["seatsPerTable"])
        assertEquals(4, changes)
        onAllNodes(hasSetTextAction() and hasText("10")).onFirst().assert(
            !SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
    }
}
