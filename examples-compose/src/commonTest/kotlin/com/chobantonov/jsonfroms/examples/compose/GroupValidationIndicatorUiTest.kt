package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.semantics.SemanticsProperties
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.ListWithDetailElement
import com.chobantonov.jsonfroms.core.models.Rule
import com.chobantonov.jsonfroms.core.models.RuleEffect
import com.chobantonov.jsonfroms.core.models.SchemaBasedCondition
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.store.ValidationMode
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import com.chobantonov.jsonfroms.core.store.ErrorTranslator
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

@OptIn(ExperimentalTestApi::class)
class GroupValidationIndicatorUiTest {
    @Test fun materialCollapsedGroupDisclosesItsErrorState() = collapsedErrorSemantics(false)
    @Test fun cupertinoCollapsedGroupDisclosesItsErrorState() = collapsedErrorSemantics(true)

    private fun collapsedErrorSemantics(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("{}"))
        var locale by mutableStateOf("en")
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = GroupElement(label = "Details", options = mapOf(
                    "collapsible" to JsonPrimitive(true),
                    "collapsed" to JsonPrimitive(true),
                    "showValidationIndicator" to JsonPrimitive(true),
                    "showValidationIndicatorCount" to JsonPrimitive(true),
                ), elements = listOf(ControlElement("#/properties/a"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { writes++ })
        }
        fun header() = onNodeWithText("Details").fetchSemanticsNode().config
        kotlin.test.assertEquals("Collapsed", header()[SemanticsProperties.StateDescription])
        kotlin.test.assertEquals("1 error", header()[SemanticsProperties.Error])
        runOnIdle { locale = "bg-BG" }
        kotlin.test.assertEquals("Свита", header()[SemanticsProperties.StateDescription])
        kotlin.test.assertEquals("1 грешка", header()[SemanticsProperties.Error])
        runOnIdle { data = Json.parseToJsonElement("""{"a":"ok"}""") }
        kotlin.test.assertFalse(header().contains(SemanticsProperties.Error))
        kotlin.test.assertEquals("Свита", header()[SemanticsProperties.StateDescription])
        kotlin.test.assertEquals(0, writes)
    }

    @Test fun materialBuiltInValidationLabelFollowsLocale() = builtInValidationLabel(false)
    @Test fun cupertinoBuiltInValidationLabelFollowsLocale() = builtInValidationLabel(true)

    private fun builtInValidationLabel(cupertino: Boolean) = runComposeUiTest {
        var locale by mutableStateOf("en")
        var showCount by mutableStateOf(true)
        var writes = 0
        setContent {
            JsonForms(data = Json.parseToJsonElement("{}"), schema = schema,
                uischema = GroupElement(label = "Details", options = mapOf(
                    "showValidationIndicator" to JsonPrimitive(true),
                    "showValidationIndicatorCount" to JsonPrimitive(showCount),
                ), elements = listOf(ControlElement(scope = "#/properties/a"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { writes++ })
        }
        onNodeWithContentDescription("1 error").assertExists()
        runOnIdle { locale = "bg-BG" }
        onNodeWithContentDescription("1 грешка").assertExists()
        runOnIdle { showCount = false }
        onNodeWithContentDescription("Съдържа грешки").assertExists()
        kotlin.test.assertEquals(0, writes)
    }

    private val requiredFilter = mapOf<String, Any?>(
        "enableFilterErrorsBeforeTouch" to true,
        "filterErrorKeywordsBeforeTouch" to listOf("required"),
    )
    @Test fun materialDisplayPolicyCountAndLocale() = scenario(false)
    @Test fun cupertinoDisplayPolicyCountAndLocale() = scenario(true)

    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","required":["a"],"properties":{"a":{"type":"string"},"b":{"type":"string"}}}"""
    ).jsonObject)

    private fun group(enabled: Boolean) = GroupElement(
        label = "Details",
        options = mapOf(
            "collapsible" to JsonPrimitive(true),
            "collapsed" to JsonPrimitive(true),
            "showValidationIndicator" to JsonPrimitive(enabled),
            "showValidationIndicatorCount" to JsonPrimitive(true),
        ),
        elements = listOf(
            ControlElement(scope = "#/properties/a", rule = Rule(
                effect = RuleEffect.SHOW,
                condition = SchemaBasedCondition(
                    scope = "#/properties/show",
                    schema = buildJsonSchema { put("const", true) },
                    failWhenUndefined = true,
                ),
            )),
            ControlElement(scope = "#/properties/b"),
        ),
    )

    private fun scenario(cupertino: Boolean) = runComposeUiTest {
        var ui by mutableStateOf(group(true))
        var mode by mutableStateOf(ValidationMode.ValidateAndShow)
        var locale by mutableStateOf("en")
        var host by mutableStateOf(listOf(ErrorObject(instancePath = "/b", message = "server")))
        var validity by mutableStateOf<CombinedValidityStatus?>(null)
        var changes = 0
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("{}"), schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                validationMode = mode, additionalErrors = host,
                i18n = JsonFormsI18nState(locale = locale, translate = { key, fallback, _ ->
                    if (key == "group.validationIndicator" && locale == "bg") "Грешки: $fallback" else fallback
                }),
                onChange = { changes++ },
                onValidityChange = { validity = it.status },
            )
        }
        onNodeWithContentDescription("2 errors").assertExists()
        onNodeWithText("Details").performClick()
        kotlin.test.assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onNodeWithText("server").assertExists()
        onNodeWithContentDescription("2 errors").assertExists()
        onNodeWithText("Details").performClick()
        runOnIdle { ui = group(true).copy(options = mapOf("showValidationIndicator" to JsonPrimitive(true))) }
        onNodeWithContentDescription("Contains errors").assertExists()
        onNodeWithContentDescription("2 errors").assertDoesNotExist()
        runOnIdle { ui = group(true); mode = ValidationMode.ValidateAndHide }
        onNodeWithContentDescription("1 error").assertExists()
        onNodeWithContentDescription("2 errors").assertDoesNotExist()
        runOnIdle { locale = "bg" }
        onNodeWithContentDescription("Грешки: 1 грешка").assertExists()
        runOnIdle { ui = group(false) }
        onNodeWithContentDescription("Грешки: 1 грешка").assertDoesNotExist()
        onNodeWithText("Details").performClick()
        onNodeWithText("server").assertExists()
        kotlin.test.assertEquals(CombinedValidityStatus.Invalid, validity)
        onNodeWithText("Details").performClick()
        runOnIdle { ui = group(true); mode = ValidationMode.NoValidation }
        onNodeWithContentDescription("Грешки: 1 грешка").assertExists()
        runOnIdle { ui = group(true).copy(label = null, options = mapOf(
            "showValidationIndicator" to JsonPrimitive(true),
            "showValidationIndicatorCount" to JsonPrimitive(true),
        )) }
        onNodeWithContentDescription("Грешки: 1 грешка").assertDoesNotExist()
        runOnIdle { ui = group(true); host = emptyList() }
        onNodeWithContentDescription("Грешки: 1 грешка").assertDoesNotExist()
        kotlin.test.assertEquals(0, changes)
    }

    @Test fun materialDisabledMarkerRetainsSchemaInlineError() = inlineSchemaError(false)
    @Test fun cupertinoDisabledMarkerRetainsSchemaInlineError() = inlineSchemaError(true)

    private fun inlineSchemaError(cupertino: Boolean) = runComposeUiTest {
        val inlineSchema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"a":{"type":"string","minLength":3},"b":{"type":"string"}}}"""
        ).jsonObject)
        val ui = GroupElement(
            label = "Details",
            options = mapOf("showValidationIndicator" to JsonPrimitive(false)),
            elements = listOf(
                ControlElement(scope = "#/properties/a"),
                ControlElement(scope = "#/properties/b"),
            ),
        )
        var validity by mutableStateOf<CombinedValidityStatus?>(null)
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"a":"x","b":""}"""),
                schema = inlineSchema,
                uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                validationMode = ValidationMode.ValidateAndShow,
                i18n = JsonFormsI18nState(translateError = ErrorTranslator { error, _, _ ->
                    "ERR:${error.keyword}:${error.instancePath}"
                }),
                onValidityChange = { validity = it.status },
            )
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("y")
        onAllNodes(hasSetTextAction()).onLast().performClick()
        onNodeWithText("ERR:minLength:/a").assertExists()
        onNodeWithContentDescription("Contains errors").assertDoesNotExist()
        onNodeWithContentDescription("1 error").assertDoesNotExist()
        kotlin.test.assertEquals(CombinedValidityStatus.Invalid, validity)
    }

    @Test fun materialUntouchedRequiredFieldCountsBeforeInlineMessage() = untouchedRequired(false)
    @Test fun cupertinoUntouchedRequiredFieldCountsBeforeInlineMessage() = untouchedRequired(true)

    private fun untouchedRequired(cupertino: Boolean) = runComposeUiTest {
        val requiredSchema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","required":["a"],"properties":{"a":{"type":"string"},"b":{"type":"string"}}}"""
        ).jsonObject)
        val ui = GroupElement(
            label = "Details",
            options = mapOf(
                "showValidationIndicator" to JsonPrimitive(true),
                "showValidationIndicatorCount" to JsonPrimitive(true),
            ),
            elements = listOf(
                ControlElement(scope = "#/properties/a"),
                ControlElement(scope = "#/properties/b"),
            ),
        )
        var changes = 0
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"b":""}"""),
                schema = requiredSchema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = requiredFilter,
                i18n = JsonFormsI18nState(translateError = { error, _, _ ->
                    "ERR:${error.keyword}:${error.instancePath}"
                }),
                onChange = { changes++ },
            )
        }
        onNodeWithContentDescription("1 error").assertExists()
        onNodeWithText("ERR:required:").assertDoesNotExist()
        onAllNodes(hasSetTextAction()).onFirst().performClick()
        onAllNodes(hasSetTextAction()).onLast().performClick()
        onNodeWithText("ERR:required:").assertExists()
        onNodeWithContentDescription("1 error").assertExists()
        kotlin.test.assertEquals(0, changes)
    }

    @Test fun materialHostErrorAppearsBeforeRequiredFieldBlur() = hostBeforeBlur(false)
    @Test fun cupertinoHostErrorAppearsBeforeRequiredFieldBlur() = hostBeforeBlur(true)

    private fun hostBeforeBlur(cupertino: Boolean) = runComposeUiTest {
        val ui = GroupElement(
            label = "Details",
            options = mapOf(
                "showValidationIndicator" to JsonPrimitive(true),
                "showValidationIndicatorCount" to JsonPrimitive(true),
            ),
            elements = listOf(
                ControlElement(scope = "#/properties/a"),
                ControlElement(scope = "#/properties/b"),
            ),
        )
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("{}"), schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                additionalErrors = listOf(ErrorObject(instancePath = "/a", message = "server")),
                config = requiredFilter,
                i18n = JsonFormsI18nState(translateError = { error, _, _ ->
                    if (error.keyword == "required") "required-inline" else error.message
                }),
            )
        }
        onNodeWithContentDescription("2 errors").assertExists()
        onNodeWithText("server").assertExists()
        onNodeWithText("required-inline").assertDoesNotExist()
    }

    @Test fun materialUntouchedEnumKeepsRequiredInlineQuiet() = untouchedChoice(false, false)
    @Test fun cupertinoUntouchedEnumKeepsRequiredInlineQuiet() = untouchedChoice(true, false)
    @Test fun materialUntouchedOneOfKeepsRequiredInlineQuiet() = untouchedChoice(false, true)
    @Test fun cupertinoUntouchedOneOfKeepsRequiredInlineQuiet() = untouchedChoice(true, true)
    @Test fun materialUntouchedAnyOfKeepsRequiredInlineQuiet() = untouchedChoice(false, false, true)
    @Test fun cupertinoUntouchedAnyOfKeepsRequiredInlineQuiet() = untouchedChoice(true, false, true)

    private fun untouchedChoice(cupertino: Boolean, oneOf: Boolean, anyOf: Boolean = false) = runComposeUiTest {
        val propertySchema = if (anyOf) {
            """{"anyOf":[{"type":"string","enum":["red","blue"]},{"type":"string"}]}"""
        } else if (oneOf) {
            """{"oneOf":[{"const":"yes","title":"Yes"},{"const":"no","title":"No"}]}"""
        } else {
            """{"type":"string","enum":["red","blue"]}"""
        }
        val choiceSchema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","required":["a"],"properties":{"a":$propertySchema,"b":{"type":"string"}}}"""
        ).jsonObject)
        val ui = GroupElement(
            label = "Choices",
            options = mapOf(
                "showValidationIndicator" to JsonPrimitive(true),
                "showValidationIndicatorCount" to JsonPrimitive(true),
            ),
            elements = listOf(
                ControlElement(scope = "#/properties/a"),
                ControlElement(scope = "#/properties/b"),
            ),
        )
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"b":""}"""), schema = choiceSchema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = requiredFilter,
                i18n = JsonFormsI18nState(translateError = { error, _, _ ->
                    if (error.keyword == "required") "required-inline" else error.message
                }),
            )
        }
        onNodeWithContentDescription("1 error").assertExists()
        onNodeWithText("required-inline").assertDoesNotExist()
    }

    @Test fun materialClearedEnumShowsRequiredInline() = clearChoice(false, false)
    @Test fun cupertinoClearedEnumShowsRequiredInline() = clearChoice(true, false)
    @Test fun materialClearedOneOfShowsRequiredInline() = clearChoice(false, true)
    @Test fun cupertinoClearedOneOfShowsRequiredInline() = clearChoice(true, true)

    private fun clearChoice(cupertino: Boolean, oneOf: Boolean) = runComposeUiTest {
        val propertySchema = if (oneOf) {
            """{"oneOf":[{"const":"yes","title":"Yes"},{"const":"no","title":"No"}]}"""
        } else {
            """{"type":"string","enum":["red","blue"]}"""
        }
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","required":["a"],"properties":{"a":$propertySchema}}"""
        ).jsonObject)
        val ui = GroupElement(
            label = "Choices",
            options = mapOf(
                "showValidationIndicator" to JsonPrimitive(true),
                "showValidationIndicatorCount" to JsonPrimitive(true),
            ),
            elements = listOf(ControlElement(scope = "#/properties/a")),
        )
        setContent {
            TestTouchInputMode()
            JsonForms(
                data = Json.parseToJsonElement(if (oneOf) """{"a":"yes"}""" else """{"a":"red"}"""),
                schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(translateError = { error, _, _ ->
                    if (error.keyword == "required") "required-inline" else error.message
                }),
            )
        }
        onNodeWithContentDescription("1 error").assertDoesNotExist()
        onNodeWithContentDescription("Clear value").performClick()
        onNodeWithContentDescription("1 error").assertExists()
        onNodeWithText("required-inline").assertExists()
    }

    @Test fun materialUntouchedCheckboxKeepsRequiredInlineQuiet() = untouchedDiscrete(false, "checkbox")
    @Test fun cupertinoUntouchedCheckboxKeepsRequiredInlineQuiet() = untouchedDiscrete(true, "checkbox")
    @Test fun materialUntouchedToggleKeepsRequiredInlineQuiet() = untouchedDiscrete(false, "toggle")
    @Test fun cupertinoUntouchedToggleKeepsRequiredInlineQuiet() = untouchedDiscrete(true, "toggle")
    @Test fun materialUntouchedRadioKeepsRequiredInlineQuiet() = untouchedDiscrete(false, "radio")
    @Test fun cupertinoUntouchedRadioKeepsRequiredInlineQuiet() = untouchedDiscrete(true, "radio")
    @Test fun materialUntouchedOneOfRadioKeepsRequiredInlineQuiet() = untouchedDiscrete(false, "oneOfRadio")
    @Test fun cupertinoUntouchedOneOfRadioKeepsRequiredInlineQuiet() = untouchedDiscrete(true, "oneOfRadio")
    @Test fun materialUntouchedSliderKeepsRequiredInlineQuiet() = untouchedDiscrete(false, "slider")
    @Test fun cupertinoUntouchedSliderKeepsRequiredInlineQuiet() = untouchedDiscrete(true, "slider")
    @Test fun materialUntouchedCheckboxStillShowsHostError() = untouchedDiscrete(false, "checkbox", true)
    @Test fun cupertinoUntouchedCheckboxStillShowsHostError() = untouchedDiscrete(true, "checkbox", true)

    private fun untouchedDiscrete(cupertino: Boolean, kind: String, host: Boolean = false) = runComposeUiTest {
        val propertySchema = when (kind) {
            "checkbox", "toggle" -> """{"type":"boolean"}"""
            "radio" -> """{"type":"string","enum":["red","blue"]}"""
            "oneOfRadio" -> """{"oneOf":[{"const":"yes","title":"Yes"},{"const":"no","title":"No"}]}"""
            else -> """{"type":"number","minimum":0,"maximum":10,"default":5}"""
        }
        val options = when (kind) {
            "toggle" -> mapOf("toggle" to JsonPrimitive(true))
            "radio", "oneOfRadio" -> mapOf("format" to JsonPrimitive("radio"))
            "slider" -> mapOf("slider" to JsonPrimitive(true))
            else -> emptyMap()
        }
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","required":["a"],"properties":{"a":$propertySchema}}"""
        ).jsonObject)
        val ui = GroupElement(
            label = "Discrete",
            options = mapOf(
                "showValidationIndicator" to JsonPrimitive(true),
                "showValidationIndicatorCount" to JsonPrimitive(true),
            ),
            elements = listOf(ControlElement(scope = "#/properties/a", options = options)),
        )
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("{}"), schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                additionalErrors = if (host) listOf(ErrorObject(instancePath = "/a", message = "server")) else emptyList(),
                config = requiredFilter,
                i18n = JsonFormsI18nState(translateError = { error, _, _ ->
                    if (error.keyword == "required") "required-inline" else error.message
                }),
            )
        }
        onNodeWithContentDescription(if (host) "2 errors" else "1 error").assertExists()
        onNodeWithText("required-inline").assertDoesNotExist()
        if (host) onNodeWithText("server").assertExists()
    }

    @Test fun materialCheckboxInteractionShowsNewSchemaError() = checkboxInteraction(false)
    @Test fun cupertinoCheckboxInteractionShowsNewSchemaError() = checkboxInteraction(true)

    private fun checkboxInteraction(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","required":["a"],"properties":{"a":{"type":"boolean","const":false}}}"""
        ).jsonObject)
        val ui = GroupElement(
            label = "Boolean",
            options = mapOf(
                "showValidationIndicator" to JsonPrimitive(true),
                "showValidationIndicatorCount" to JsonPrimitive(true),
            ),
            elements = listOf(ControlElement(scope = "#/properties/a")),
        )
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("{}"), schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = requiredFilter,
                i18n = JsonFormsI18nState(translateError = { error, _, _ ->
                    "ERR:${error.keyword}:${error.instancePath}"
                }),
            )
        }
        onNodeWithContentDescription("1 error").assertExists()
        onNodeWithText("ERR:required:").assertDoesNotExist()
        onAllNodes(hasClickAction()).onFirst().performClick()
        onNodeWithContentDescription("1 error").assertExists()
        onNodeWithText("ERR:const:/a").assertExists()
    }

    @Test fun materialUntouchedEnumArrayKeepsRequiredInlineQuiet() = enumArrayErrors(false, false)
    @Test fun cupertinoUntouchedEnumArrayKeepsRequiredInlineQuiet() = enumArrayErrors(true, false)
    @Test fun materialEnumArrayShowsOwnMinItems() = enumArrayErrors(false, true)
    @Test fun cupertinoEnumArrayShowsOwnMinItems() = enumArrayErrors(true, true)

    private fun enumArrayErrors(cupertino: Boolean, ownMinItems: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","required":["a"],"properties":{"a":{"type":"array","uniqueItems":true,"minItems":1,"items":{"type":"string","enum":["red","blue"]}}}}"""
        ).jsonObject)
        val ui = GroupElement(
            label = "Choices",
            options = mapOf(
                "showValidationIndicator" to JsonPrimitive(true),
                "showValidationIndicatorCount" to JsonPrimitive(true),
            ),
            elements = listOf(ControlElement(scope = "#/properties/a")),
        )
        setContent {
            JsonForms(
                data = Json.parseToJsonElement(if (ownMinItems) """{"a":[]}""" else "{}"),
                schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = requiredFilter,
                i18n = JsonFormsI18nState(translateError = { error, _, _ ->
                    "ERR:${error.keyword}:${error.instancePath}"
                }),
            )
        }
        onNodeWithContentDescription("1 error").assertExists()
        if (ownMinItems) {
            onNodeWithText("ERR:minItems:/a").assertExists()
        } else {
            onNodeWithText("ERR:required:").assertDoesNotExist()
            onAllNodes(hasClickAction()).onFirst().performClick()
            onNodeWithContentDescription("1 error").assertDoesNotExist()
            onNodeWithText("ERR:required:").assertDoesNotExist()
        }
    }

    @Test fun materialScopesSeparateArrayRows() = rowScopes(false)
    @Test fun cupertinoScopesSeparateArrayRows() = rowScopes(true)

    private fun rowScopes(cupertino: Boolean) = runComposeUiTest {
        val rowGroup: (Int) -> GroupElement = { index -> GroupElement(
            label = "Row $index",
            options = mapOf(
                "collapsible" to JsonPrimitive(true),
                "collapsed" to JsonPrimitive(true),
                "showValidationIndicator" to JsonPrimitive(true),
                "showValidationIndicatorCount" to JsonPrimitive(true),
            ),
            elements = listOf(ControlElement(scope = "#/properties/rows/items/$index/properties/name")),
        ) }
        val listGroup = GroupElement(
            label = "All rows",
            options = mapOf(
                "collapsible" to JsonPrimitive(true),
                "collapsed" to JsonPrimitive(true),
                "showValidationIndicator" to JsonPrimitive(true),
                "showValidationIndicatorCount" to JsonPrimitive(true),
            ),
            elements = listOf(ListWithDetailElement(scope = "#/properties/rows")),
        )
        val ui = VerticalLayout(elements = listOf(rowGroup(1), rowGroup(10), listGroup))
        val data = Json.parseToJsonElement("""{"rows":[${List(11) { "{}" }.joinToString(",") }]}""")
        var host by mutableStateOf(listOf(
            ErrorObject(instancePath = "/rows/1/name", message = "first"),
            ErrorObject(instancePath = "/rows/10/name", message = "second"),
            ErrorObject(instancePath = "/rows/10/name", message = "third"),
        ))
        setContent {
            JsonForms(data = data, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                additionalErrors = host)
        }
        onNodeWithContentDescription("1 error").assertExists()
        onNodeWithContentDescription("2 errors").assertExists()
        onNodeWithContentDescription("3 errors").assertExists()
        runOnIdle { host = host.drop(1) }
        onNodeWithContentDescription("1 error").assertDoesNotExist()
        onNodeWithContentDescription("3 errors").assertDoesNotExist()
        kotlin.test.assertEquals(2,
            onAllNodes(hasContentDescription("2 errors")).fetchSemanticsNodes().size)
    }
}
