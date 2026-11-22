package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.Rule
import com.chobantonov.jsonfroms.core.models.RuleEffect
import com.chobantonov.jsonfroms.core.models.SchemaBasedCondition
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.store.ValidationMode
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

@OptIn(ExperimentalTestApi::class)
class CategoryValidationIndicatorUiTest {
    @Test fun materialTabsCountRuleHiddenDescendant() = hiddenDescendant(false, null)
    @Test fun cupertinoTabsCountRuleHiddenDescendant() = hiddenDescendant(true, null)
    @Test fun materialStepsCountRuleHiddenDescendant() = hiddenDescendant(false, "stepper")
    @Test fun cupertinoStepsCountRuleHiddenDescendant() = hiddenDescendant(true, "stepper")
    @Test fun materialAccordionCountsRuleHiddenDescendant() = hiddenDescendant(false, "accordion")
    @Test fun cupertinoAccordionCountsRuleHiddenDescendant() = hiddenDescendant(true, "accordion")

    private fun hiddenDescendant(cupertino: Boolean, variant: String?) = runComposeUiTest {
        val hiddenSchema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","required":["name"],"properties":{"name":{"type":"string"},"show":{"type":"boolean"}}}"""
        ).jsonObject)
        val conditionalName = ControlElement(scope = "#/properties/name", rule = Rule(
            effect = RuleEffect.SHOW,
            condition = SchemaBasedCondition(scope = "#/properties/show",
                schema = buildJsonSchema { put("const", true) }, failWhenUndefined = true),
        ))
        val ui = CategorizationElement(
            options = variant?.let { mapOf("variant" to JsonPrimitive(it)) } ?: emptyMap(),
            elements = listOf(
                CategoryElement(label = "Basic", elements = listOf(ControlElement("#/properties/show"))),
                CategoryElement(label = "Details", options = mapOf(
                    "showValidationIndicator" to JsonPrimitive(true),
                    "showValidationIndicatorCount" to JsonPrimitive(true),
                ), elements = listOf(conditionalName)),
            ),
        )
        var data by mutableStateOf(Json.parseToJsonElement("{}"))
        var writes = 0
        setContent {
            JsonForms(data = data, schema = hiddenSchema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { writes++ })
        }
        onNodeWithContentDescription("Details, 1 error").assertExists()
        onNodeWithText("Name").assertDoesNotExist()
        runOnIdle { data = Json.parseToJsonElement("""{"show":true}""") }
        onNodeWithContentDescription("Details, 1 error").assertExists()
        runOnIdle { data = Json.parseToJsonElement("""{"show":true,"name":"Ada"}""") }
        onNodeWithContentDescription("Details, 1 error").assertDoesNotExist()
        kotlin.test.assertEquals(0, writes)
    }
    @Test fun materialTabsUseBuiltInBulgarianStatus() = builtInBulgarianStatus(false, null)
    @Test fun cupertinoTabsUseBuiltInBulgarianStatus() = builtInBulgarianStatus(true, null)
    @Test fun materialStepsUseBuiltInBulgarianStatus() = builtInBulgarianStatus(false, "stepper")
    @Test fun cupertinoStepsUseBuiltInBulgarianStatus() = builtInBulgarianStatus(true, "stepper")
    @Test fun materialAccordionUsesBuiltInBulgarianStatus() = builtInBulgarianStatus(false, "accordion")
    @Test fun cupertinoAccordionUsesBuiltInBulgarianStatus() = builtInBulgarianStatus(true, "accordion")

    private fun builtInBulgarianStatus(cupertino: Boolean, variant: String?) = runComposeUiTest {
        var locale by mutableStateOf("en")
        var showCount by mutableStateOf(true)
        var writes = 0
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"name":"Ada"}"""), schema = schema,
                uischema = CategorizationElement(
                    options = variant?.let { mapOf("variant" to JsonPrimitive(it)) } ?: emptyMap(),
                    elements = listOf(CategoryElement(label = "Details", options = mapOf(
                        "showValidationIndicator" to JsonPrimitive(true),
                        "showValidationIndicatorCount" to JsonPrimitive(showCount),
                        "showDataIndicator" to JsonPrimitive(true),
                    ), elements = listOf(ControlElement(scope = "#/properties/name")))),
                ),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                additionalErrors = listOf(ErrorObject(instancePath = "/name", message = "server")),
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { writes++ })
        }
        onNodeWithContentDescription("Details, 1 error, Contains data").assertExists()
        runOnIdle { locale = "bg-BG" }
        onNodeWithContentDescription("Details, 1 грешка, Съдържа данни").assertExists()
        runOnIdle { showCount = false }
        onNodeWithContentDescription("Details, Съдържа грешки, Съдържа данни").assertExists()
        kotlin.test.assertEquals(0, writes)
    }

    @Test fun materialTabsSummarizeInactiveCategory() = scenario(false, false)
    @Test fun cupertinoTabsSummarizeInactiveCategory() = scenario(true, false)
    @Test fun materialStepsSummarizeInactiveCategory() = scenario(false, true)
    @Test fun cupertinoStepsSummarizeInactiveCategory() = scenario(true, true)
    @Test fun materialCategoryScopeReplacementRefreshesBothIndicators() = scopeReplacement(false)
    @Test fun cupertinoCategoryScopeReplacementRefreshesBothIndicators() = scopeReplacement(true)

    private fun scopeReplacement(cupertino: Boolean) = runComposeUiTest {
        fun layout(field: String) = CategorizationElement(elements = listOf(CategoryElement(
            label = "Details",
            options = mapOf(
                "showValidationIndicator" to JsonPrimitive(true),
                "showValidationIndicatorCount" to JsonPrimitive(true),
                "showDataIndicator" to JsonPrimitive(true),
            ),
            elements = listOf(ControlElement(scope = "#/properties/$field")),
        )))
        var ui by mutableStateOf(layout("name"))
        var data by mutableStateOf(Json.parseToJsonElement("""{"name":"Ada"}"""))
        var host by mutableStateOf(listOf(ErrorObject(instancePath = "/name", message = "server")))
        setContent {
            JsonForms(data = data, schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                additionalErrors = host)
        }
        onNodeWithContentDescription("Details, 1 error, Contains data").assertExists()
        runOnIdle { ui = layout("other") }
        onNodeWithContentDescription("Details, 1 error, Contains data").assertDoesNotExist()
        onNodeWithText("Details").assertExists()
        runOnIdle {
            data = Json.parseToJsonElement("""{"name":"Ada","other":"Ready"}""")
            host = listOf(ErrorObject(instancePath = "/other", message = "other server"))
        }
        onNodeWithContentDescription("Details, 1 error, Contains data").assertExists()
    }

    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","required":["name"],"properties":{"name":{"type":"string"},"other":{"type":"string"}}}"""
    ).jsonObject)

    private fun ui(stepper: Boolean, indicator: Boolean, count: Boolean? = true) = CategorizationElement(
        options = if (stepper) mapOf("variant" to JsonPrimitive("stepper")) else emptyMap(),
        elements = listOf(
            CategoryElement(label = "Basic", elements = listOf(ControlElement(scope = "#/properties/other"))),
            CategoryElement(
                label = "Details",
                options = mapOf("showValidationIndicator" to JsonPrimitive(indicator)) +
                    (if (count == null) emptyMap() else mapOf(
                        "showValidationIndicatorCount" to JsonPrimitive(count),
                    )),
                elements = listOf(ControlElement(scope = "#/properties/name")),
            ),
        ),
    )

    private fun scenario(cupertino: Boolean, stepper: Boolean) = runComposeUiTest {
        var layout by mutableStateOf(ui(stepper, true))
        var mode by mutableStateOf(ValidationMode.ValidateAndShow)
        var locale by mutableStateOf("en")
        var host by mutableStateOf(listOf(ErrorObject(instancePath = "/name", message = "server")))
        var changes = 0
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("{}"), schema = schema, uischema = layout,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                validationMode = mode, additionalErrors = host,
                config = mapOf("jsonformsExtended" to mapOf("showValidationIndicator" to true)),
                i18n = JsonFormsI18nState(locale = locale, translate = { key, fallback, _ ->
                    if (key == "category.validationIndicator" && locale == "bg") "Грешки: $fallback" else fallback
                }),
                onChange = { changes++ },
            )
        }
        onNodeWithContentDescription("Details, 2 errors").assertExists()
        runOnIdle { layout = ui(stepper, true, count = null) }
        onNodeWithContentDescription("Details, Contains errors").assertExists()
        onNodeWithContentDescription("Details, 2 errors").assertDoesNotExist()
        runOnIdle { layout = ui(stepper, true); mode = ValidationMode.ValidateAndHide }
        onNodeWithContentDescription("Details, 1 error").assertExists()
        runOnIdle { locale = "bg" }
        onNodeWithContentDescription("Details, Грешки: 1 грешка").assertExists()
        onNodeWithContentDescription("Details, Грешки: 1 грешка").performTouchInput { click() }
        waitForIdle()
        onNodeWithText("server").assertExists()
        runOnIdle { layout = ui(stepper, false) }
        onNodeWithContentDescription("Details, Грешки: 1 грешка").assertDoesNotExist()
        runOnIdle { layout = ui(stepper, true); mode = ValidationMode.NoValidation }
        onNodeWithContentDescription("Details, Грешки: 1 грешка").assertExists()
        runOnIdle { host = emptyList() }
        onNodeWithContentDescription("Details, Грешки: 1 грешка").assertDoesNotExist()
        kotlin.test.assertEquals(0, changes)
    }
}
