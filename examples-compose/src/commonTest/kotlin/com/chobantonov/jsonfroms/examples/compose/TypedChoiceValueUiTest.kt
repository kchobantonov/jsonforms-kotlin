package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.LeafCondition
import com.chobantonov.jsonfroms.core.models.Rule
import com.chobantonov.jsonfroms.core.models.RuleEffect
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class TypedChoiceValueUiTest {
    @Test fun materialEnumRadioKeepsNumberAndStringDistinct() = choose(false, false)
    @Test fun cupertinoEnumRadioKeepsNumberAndStringDistinct() = choose(true, false)
    @Test fun materialConstOneOfRadioKeepsNumberAndStringDistinct() = choose(false, true)
    @Test fun cupertinoConstOneOfRadioKeepsNumberAndStringDistinct() = choose(true, true)
    @Test fun materialConstOneOfDropdownKeepsNumberAndStringDistinct() = selectDropdown(false)
    @Test fun cupertinoConstOneOfDropdownKeepsNumberAndStringDistinct() = selectDropdown(true)
    @Test fun materialDropdownShowsUnknownValueUntilSelection() = unknownDropdown(false)
    @Test fun cupertinoDropdownShowsUnknownValueUntilSelection() = unknownDropdown(true)
    @Test fun materialEnumDropdownShowsUnknownNull() = unknownNull(false, false)
    @Test fun cupertinoEnumDropdownShowsUnknownNull() = unknownNull(true, false)
    @Test fun materialOneOfDropdownShowsUnknownNull() = unknownNull(false, true)
    @Test fun cupertinoOneOfDropdownShowsUnknownNull() = unknownNull(true, true)
    @Test fun materialEnumSearchShowsUnknownNull() = unknownNull(false, false, true)
    @Test fun cupertinoEnumSearchShowsUnknownNull() = unknownNull(true, false, true)
    @Test fun materialOneOfSearchShowsUnknownNull() = unknownNull(false, true, true)
    @Test fun cupertinoOneOfSearchShowsUnknownNull() = unknownNull(true, true, true)

    private fun unknownNull(cupertino: Boolean, oneOf: Boolean, autocomplete: Boolean = false) = runComposeUiTest {
        val choices = if (oneOf) """"oneOf":[
            {"const":"red","title":"Red"},{"const":"blue","title":"Blue"}
        ]""" else """"enum":["red","blue"]"""
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"choice":{$choices}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"choice":null}"""))
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/choice", options =
                    mapOf("autocomplete" to JsonPrimitive(autocomplete))),
                i18n = JsonFormsI18nState(locale = locale),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Current value outside choices: null").assertExists()
        onNodeWithText("null").assertExists()
        runOnIdle { data = Json.parseToJsonElement("{}") }
        onNodeWithText("Current value outside choices: null").assertDoesNotExist()
        runOnIdle {
            assertEquals(0, changes)
            locale = "bg-BG"
            data = Json.parseToJsonElement("""{"choice":null}""")
        }
        onNodeWithText("Текуща стойност извън опциите: null").assertExists()
        if (autocomplete) onAllNodes(hasSetTextAction()).onFirst()
            .performTextReplacement(if (oneOf) "Red" else "red")
        else onNodeWithText("null").performClick()
        onAllNodesWithText(if (oneOf) "Red" else "red").onLast().performClick()
        runOnIdle {
            assertEquals(JsonPrimitive("red"), data.jsonObject["choice"])
            assertEquals(1, changes)
        }
        onNodeWithText("Текуща стойност извън опциите: null").assertDoesNotExist()
    }
    @Test fun materialRadioPreservesNullEmptyAndCompositeChoices() = selectShapes(false, true)
    @Test fun cupertinoRadioPreservesNullEmptyAndCompositeChoices() = selectShapes(true, true)
    @Test fun materialEnumRadioPreservesNullEmptyAndCompositeChoices() = selectShapes(false, true, false)
    @Test fun cupertinoEnumRadioPreservesNullEmptyAndCompositeChoices() = selectShapes(true, true, false)
    @Test fun materialDropdownPreservesNullEmptyAndCompositeChoices() = selectShapes(false, false)
    @Test fun cupertinoDropdownPreservesNullEmptyAndCompositeChoices() = selectShapes(true, false)
    @Test fun materialClearDistinguishesNullFromAbsent() = clearNull(false)
    @Test fun cupertinoClearDistinguishesNullFromAbsent() = clearNull(true)
    @Test fun materialPlainEnumDropdownPreservesShapesAndLocalizesEmpty() = plainEnumDropdown(false)
    @Test fun cupertinoPlainEnumDropdownPreservesShapesAndLocalizesEmpty() = plainEnumDropdown(true)
    @Test fun materialUntitledOneOfEmptyChoiceHasLocalizedLabel() = untitledOneOfEmpty(false)
    @Test fun cupertinoUntitledOneOfEmptyChoiceHasLocalizedLabel() = untitledOneOfEmpty(true)
    @Test fun materialPlainEnumDropdownKeepsDuplicateLabelsDistinct() = duplicateLabelDropdown(false, false)
    @Test fun cupertinoPlainEnumDropdownKeepsDuplicateLabelsDistinct() = duplicateLabelDropdown(true, false)
    @Test fun materialOneOfDropdownKeepsDuplicateLabelsDistinct() = duplicateLabelDropdown(false, true)
    @Test fun cupertinoOneOfDropdownKeepsDuplicateLabelsDistinct() = duplicateLabelDropdown(true, true)
    @Test fun materialEnumRadioDistinguishesEqualLabelsAccessibly() = duplicateLabelRadio(false, false)
    @Test fun cupertinoEnumRadioDistinguishesEqualLabelsAccessibly() = duplicateLabelRadio(true, false)
    @Test fun materialOneOfRadioDistinguishesEqualLabelsAccessibly() = duplicateLabelRadio(false, true)
    @Test fun cupertinoOneOfRadioDistinguishesEqualLabelsAccessibly() = duplicateLabelRadio(true, true)
    @Test fun materialDropdownKeepsTranslatedEqualLabelsDistinct() = translatedCollision(false, false)
    @Test fun cupertinoDropdownKeepsTranslatedEqualLabelsDistinct() = translatedCollision(true, false)
    @Test fun materialRadioKeepsTranslatedEqualLabelsDistinct() = translatedCollision(false, true)
    @Test fun cupertinoRadioKeepsTranslatedEqualLabelsDistinct() = translatedCollision(true, true)
    @Test fun materialEnumRadioDefaultsHorizontal() = radioOrientation(false, false, null, false, false)
    @Test fun cupertinoEnumRadioDefaultsHorizontal() = radioOrientation(true, false, null, false, false)
    @Test fun materialOneOfRadioStacksWhenVertical() = radioOrientation(false, true, true, false, false)
    @Test fun cupertinoOneOfRadioStacksWhenVertical() = radioOrientation(true, true, true, false, false)
    @Test fun materialEnumRadioWrapsNarrowWidth() = radioOrientation(false, false, null, false, true)
    @Test fun cupertinoEnumRadioWrapsNarrowWidth() = radioOrientation(true, false, null, false, true)
    @Test fun materialOneOfRadioLocalFalseOverridesGlobalVertical() = radioOrientation(false, true, false, true, false)
    @Test fun cupertinoOneOfRadioLocalFalseOverridesGlobalVertical() = radioOrientation(true, true, false, true, false)
    @Test fun materialRadioUsesNestedDefaultVertical() = radioOrientation(false, false, null, true, false)
    @Test fun cupertinoRadioUsesNestedDefaultVertical() = radioOrientation(true, false, null, true, false)
    @Test fun materialEnumRadioShowsUnknownAndDistinguishesMissingNull() = unknownRadio(false, false)
    @Test fun cupertinoEnumRadioShowsUnknownAndDistinguishesMissingNull() = unknownRadio(true, false)
    @Test fun materialOneOfRadioShowsUnknownAndDistinguishesMissingNull() = unknownRadio(false, true)
    @Test fun cupertinoOneOfRadioShowsUnknownAndDistinguishesMissingNull() = unknownRadio(true, true)
    @Test fun materialEnumRadioClearRemovesSelectedNull() = radioClearNull(false, false)
    @Test fun cupertinoEnumRadioClearRemovesSelectedNull() = radioClearNull(true, false)
    @Test fun materialOneOfRadioClearRemovesSelectedNull() = radioClearNull(false, true)
    @Test fun cupertinoOneOfRadioClearRemovesSelectedNull() = radioClearNull(true, true)
    @Test fun materialRadioClearRespectsLiveGuards() = radioClearGuards(false)
    @Test fun cupertinoRadioClearRespectsLiveGuards() = radioClearGuards(true)
    @Test fun materialRadioClearUsesCurrentPropertyPath() = radioClearDynamicPath(false)
    @Test fun cupertinoRadioClearUsesCurrentPropertyPath() = radioClearDynamicPath(true)
    @Test fun materialRadioClearRespectsDisableRule() = radioClearDisabled(false)
    @Test fun cupertinoRadioClearRespectsDisableRule() = radioClearDisabled(true)

    private fun radioClearDisabled(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"choice":{"enum":["x","y"]},"locked":{"type":"boolean"}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"choice":"x","locked":false}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/choice", options = mapOf(
                    "format" to JsonPrimitive("radio")),
                    rule = Rule(RuleEffect.DISABLE, LeafCondition("#/locked", JsonPrimitive(true)))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("x").performMouseInput {
            moveTo(Offset(-100f, -100f)); moveTo(center)
        }
        onNodeWithContentDescription("Clear value").assertExists()
        runOnIdle { data = Json.parseToJsonElement("""{"choice":"x","locked":true}""") }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        runOnIdle { data = Json.parseToJsonElement("""{"choice":"x","locked":false}""") }
        onNodeWithContentDescription("x").performMouseInput {
            moveTo(Offset(-100f, -100f)); moveTo(center)
        }
        onNodeWithContentDescription("Clear value").performClick()
        waitForIdle()
        assertFalse("choice" in data.jsonObject)
        assertEquals(1, changes)
    }

    private fun radioClearGuards(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"choice":{"enum":["x","y"]}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"choice":"x"}"""))
        var clearable by mutableStateOf(false)
        var readonly by mutableStateOf(false)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/choice", options = mapOf(
                    "format" to JsonPrimitive("radio"), "clearable" to JsonPrimitive(clearable))),
                readonly = readonly,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        runOnIdle { clearable = true }
        onNodeWithContentDescription("x").performMouseInput {
            moveTo(Offset(-100f, -100f)); moveTo(center)
        }
        onNodeWithContentDescription("Clear value").assertExists()
        runOnIdle { readonly = true }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        runOnIdle { readonly = false }
        onNodeWithContentDescription("Clear value").performClick()
        waitForIdle()
        assertFalse("choice" in data.jsonObject)
        assertEquals(1, changes)
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        runOnIdle { data = Json.parseToJsonElement("""{"choice":"y"}""") }
        onNodeWithContentDescription("y").performMouseInput {
            moveTo(Offset(-100f, -100f)); moveTo(center)
        }
        onNodeWithContentDescription("Clear value").assertExists()
        assertEquals(1, changes)
    }

    private fun radioClearDynamicPath(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"enum":["x","y"]},"second":{"enum":["x","y"]}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"first":"x","second":"y"}"""))
        var field by mutableStateOf("first")
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/$field", options = mapOf(
                    "format" to JsonPrimitive("radio"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("x").performMouseInput {
            moveTo(Offset(-100f, -100f)); moveTo(center)
        }
        onNodeWithContentDescription("Clear value").performClick()
        runOnIdle {
            assertFalse("first" in data.jsonObject)
            field = "second"
        }
        onNodeWithContentDescription("y").performMouseInput {
            moveTo(Offset(-100f, -100f)); moveTo(center)
        }
        onNodeWithContentDescription("Clear value").performClick()
        waitForIdle()
        assertFalse("second" in data.jsonObject)
        assertEquals(2, changes)
    }

    private fun radioClearNull(cupertino: Boolean, oneOf: Boolean) = runComposeUiTest {
        val choices = if (oneOf) """"oneOf":[
          {"const":null,"title":"Null choice"},{"const":"x","title":"X choice"}
        ]""" else """"enum":[null,"x"]"""
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","required":["choice"],"properties":{"choice":{$choices}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"choice":null}"""))
        var locale by mutableStateOf("en")
        var validity: CombinedValidityStatus? = null
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/choice", options = mapOf(
                    "format" to JsonPrimitive("radio"),
                )),
                i18n = JsonFormsI18nState(locale = locale),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ },
                onValidityChange = { validity = it.status })
        }
        val radios = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
        onAllNodes(radios)[0].assertIsSelected()
        onAllNodes(radios)[0].performMouseInput {
            moveTo(Offset(-100f, -100f)); moveTo(center)
        }
        onNodeWithContentDescription("Clear value").assertExists()
        assertEquals(0, changes)
        runOnIdle { locale = "bg" }
        onNodeWithContentDescription("Изчисти стойността").performClick()
        waitForIdle()
        assertFalse("choice" in data.jsonObject)
        assertEquals(CombinedValidityStatus.Invalid, validity)
        assertEquals(1, changes)
        onAllNodes(radios)[0].assertIsNotSelected()
        onNodeWithContentDescription("Изчисти стойността").assertDoesNotExist()
        onAllNodes(radios)[0].performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("null"), data.jsonObject["choice"])
        assertEquals(CombinedValidityStatus.Valid, validity)
        assertEquals(2, changes)
    }

    private fun radioOrientation(
        cupertino: Boolean, oneOf: Boolean, vertical: Boolean?,
        globalVertical: Boolean, narrow: Boolean,
    ) = runComposeUiTest {
        val choices = if (oneOf) """"oneOf":[
          {"const":"red","title":"Red"},{"const":"blue","title":"Blue"}
        ]""" else """"enum":["red","blue"]"""
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"choice":{$choices}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"choice":"red"}"""))
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            Box(Modifier.width(if (narrow) 100.dp else 420.dp)) {
                JsonForms(data = data, schema = schema,
                    uischema = ControlElement("#/properties/choice", options =
                        mapOf("format" to JsonPrimitive("radio")) +
                            (vertical?.let { mapOf("vertical" to JsonPrimitive(it)) } ?: emptyMap())),
                    config = mapOf("vertical" to true, "jsonformsExtended" to mapOf(
                        "radio" to mapOf("vertical" to globalVertical))),
                    i18n = JsonFormsI18nState(locale = locale),
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                    onChange = { data = it.data; changes++ })
            }
        }
        val radios = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
        val bounds = onAllNodes(radios).fetchSemanticsNodes().map { it.boundsInRoot }
        assertEquals(2, bounds.size)
        val verticalLayout = vertical ?: globalVertical
        if (verticalLayout || narrow) {
            assertTrue(bounds[1].top > bounds[0].top)
        } else {
            assertTrue(kotlin.math.abs(bounds[1].top - bounds[0].top) < 2f)
            assertTrue(bounds[1].left > bounds[0].left)
        }
        onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,
            if (verticalLayout) "Vertical choices" else "Horizontal choices"))
            .fetchSemanticsNodes().also { assertEquals(1, it.size) }
        runOnIdle { locale = "bg" }
        onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,
            if (verticalLayout) "Вертикални избори" else "Хоризонтални избори"))
            .fetchSemanticsNodes().also { assertEquals(1, it.size) }
        assertEquals(0, changes)
        onAllNodes(radios)[1].performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("blue"), data.jsonObject["choice"])
    }

    private fun translatedCollision(cupertino: Boolean, radio: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"choice":{"type":"string","enum":["eng","fin"]}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"choice":"eng"}"""))
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/choice", options = mapOf(
                    if (radio) "format" to JsonPrimitive("radio")
                    else "autocomplete" to JsonPrimitive(false),
                )),
                i18n = JsonFormsI18nState(locale = locale, translate = { key, fallback, _ ->
                    if (locale == "bg" && (key.endsWith(".eng") || key.endsWith(".fin")))
                        "Екип" else fallback
                }),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        runOnIdle { locale = "bg" }
        assertEquals(JsonPrimitive("eng"), data.jsonObject["choice"])
        assertEquals(0, changes)
        if (!radio) onNodeWithText("Екип").performClick()
        onNodeWithContentDescription("Екип (\"fin\")").performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("fin"), data.jsonObject["choice"])
        assertEquals(1, changes)
    }

    private fun duplicateLabelRadio(cupertino: Boolean, oneOf: Boolean) = runComposeUiTest {
        val choices = if (oneOf) """"oneOf":[
          {"const":1,"title":"Same"},
          {"const":"1","title":"Same"},
          {"const":true,"title":"Other"}
        ]""" else """"enum":[1,"1",true]"""
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"choice":{$choices}}}"""
        ).jsonObject)
        val label = if (oneOf) "Same" else "1"
        var data by mutableStateOf(Json.parseToJsonElement("""{"choice":true}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/choice", options = mapOf(
                    "format" to JsonPrimitive("radio"),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("$label (1)").performClick()
        waitForIdle()
        assertEquals(JsonPrimitive(1), data.jsonObject["choice"])
        onNodeWithContentDescription("$label (\"1\")").performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("1"), data.jsonObject["choice"])
        assertEquals(2, changes)
    }

    private fun unknownRadio(cupertino: Boolean, oneOf: Boolean) = runComposeUiTest {
        val choices = if (oneOf) """"oneOf":[
          {"const":null,"title":"Null choice"},
          {"const":"x","title":"X choice"}
        ]""" else """"enum":[null,"x"]"""
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"choice":{$choices}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{}"""))
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/choice", options = mapOf(
                    "format" to JsonPrimitive("radio"),
                )),
                i18n = JsonFormsI18nState(locale = locale),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        val radios = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
        assertEquals(2, onAllNodes(radios).fetchSemanticsNodes().size)
        onAllNodes(radios)[0].assertIsNotSelected()
        onAllNodes(radios)[1].assertIsNotSelected()
        assertEquals(0, changes)

        runOnIdle { data = Json.parseToJsonElement("""{"choice":42}""") }
        onNodeWithText("Current value outside choices: 42").assertExists()
        onAllNodes(radios)[0].assertIsNotSelected()
        assertEquals(JsonPrimitive(42), data.jsonObject["choice"])
        assertEquals(0, changes)

        runOnIdle { locale = "bg" }
        onNodeWithText("Текуща стойност извън опциите: 42").assertExists()
        onAllNodes(radios)[0].performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("null"), data.jsonObject["choice"])
        onAllNodes(radios)[0].assertIsSelected()
        assertEquals(1, changes)
    }

    private fun duplicateLabelDropdown(cupertino: Boolean, oneOf: Boolean) = runComposeUiTest {
        val choices = if (oneOf) """"oneOf":[
          {"const":1,"title":"Same"},
          {"const":"1","title":"Same"},
          {"const":true,"title":"Other"}
        ]""" else """"enum":[1,"1",true]"""
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"choice":{$choices}}}"""
        ).jsonObject)
        val duplicateLabel = if (oneOf) "Same" else "1"
        var data by mutableStateOf(Json.parseToJsonElement("""{"choice":true}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/choice", options = mapOf(
                    "autocomplete" to JsonPrimitive(false),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText(if (oneOf) "Other" else "true").performClick()
        assertEquals(2, onAllNodesWithText(duplicateLabel).fetchSemanticsNodes().size)
        onNodeWithContentDescription("$duplicateLabel (1)").performClick()
        waitForIdle()
        assertEquals(JsonPrimitive(1), data.jsonObject["choice"])
        onNodeWithText(duplicateLabel).performClick()
        assertEquals(3, onAllNodesWithText(duplicateLabel).fetchSemanticsNodes().size)
        onNodeWithContentDescription("$duplicateLabel (\"1\")").performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("1"), data.jsonObject["choice"])
        assertEquals(2, changes)
    }

    private fun untitledOneOfEmpty(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"choice":{"oneOf":[{"const":""},{"const":"x"}]}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"choice":""}"""))
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/choice", options = mapOf(
                    "autocomplete" to JsonPrimitive(false),
                )),
                i18n = JsonFormsI18nState(locale = locale),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Empty value").assertExists()
        runOnIdle { locale = "bg" }
        onNodeWithText("Празна стойност").performClick()
        onNodeWithText("x").performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("x"), data.jsonObject["choice"])
        assertEquals(1, changes)
    }

    private fun plainEnumDropdown(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"choice":{"enum":[null,"",{"x":1},[1,2]]}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"choice":null}"""))
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/choice", options = mapOf(
                    "autocomplete" to JsonPrimitive(false),
                )),
                i18n = JsonFormsI18nState(locale = locale),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("null").performClick()
        onNodeWithText("Empty value").performClick()
        waitForIdle()
        assertEquals(JsonPrimitive(""), data.jsonObject["choice"])
        runOnIdle { locale = "bg" }
        onNodeWithText("Празна стойност").performClick()
        onNodeWithText("{\"x\":1}").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"x":1}"""), data.jsonObject["choice"])
        onNodeWithText("{\"x\":1}").performClick()
        onNodeWithText("[1,2]").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("[1,2]"), data.jsonObject["choice"])
        onNodeWithText("[1,2]").performClick()
        onNodeWithText("null").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("null"), data.jsonObject["choice"])
        assertEquals(4, changes)
    }

    private fun clearNull(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"choice":{"oneOf":[
            {"const":null,"title":"Null choice"},
            {"const":"","title":"Empty choice"}
          ]}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"choice":null}"""))
        var changes = 0
        setContent {
            TestTouchInputMode()
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/choice", options = mapOf(
                    "autocomplete" to JsonPrimitive(false),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        assertEquals(Json.parseToJsonElement("null"), data.jsonObject["choice"])
        onNodeWithText("Null choice").assertExists()
        onNodeWithText("Null choice").performMouseInput { moveTo(center) }
        onNodeWithContentDescription("Clear value").performClick()
        waitForIdle()
        assertFalse("choice" in data.jsonObject)
        assertEquals(1, changes)
    }

    private fun selectShapes(cupertino: Boolean, radio: Boolean, oneOf: Boolean = true) = runComposeUiTest {
        val choices = if (oneOf) """"oneOf":[
            {"const":null,"title":"Null choice"},
            {"const":"","title":"Empty choice"},
            {"const":{"x":1},"title":"Object choice"},
            {"const":[1,2],"title":"Array choice"}
          ]""" else """"enum":[null,"",{"x":1},[1,2]]"""
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"choice":{$choices}}
        }""").jsonObject)
        val labels = listOf("Null choice", "Empty choice", "Object choice", "Array choice")
        val values = listOf("null", "\"\"", "{\"x\":1}", "[1,2]")
        var data by mutableStateOf(Json.parseToJsonElement("""{"choice":null}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/choice", options = mapOf(
                    if (radio) "format" to JsonPrimitive("radio")
                    else "autocomplete" to JsonPrimitive(false),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        assertEquals(Json.parseToJsonElement(values[0]), data.jsonObject["choice"])
        val radios = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
        if (radio) {
            assertEquals(4, onAllNodes(radios).fetchSemanticsNodes().size)
            onAllNodes(radios)[0].assertIsSelected()
        } else {
            onNodeWithText(labels[0]).assertExists()
        }
        for (next in listOf(1, 2, 3, 0)) {
            if (radio) onAllNodes(radios)[next].performClick()
            else {
                val current = (next + 3) % 4
                onNodeWithText(labels[current]).performClick()
                onNodeWithText(labels[next]).performClick()
            }
            waitForIdle()
            assertEquals(Json.parseToJsonElement(values[next]), data.jsonObject["choice"])
            if (radio) onAllNodes(radios)[next].assertIsSelected()
        }
        assertEquals(4, changes)
    }

    private fun unknownDropdown(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"choice":{"oneOf":[
            {"const":1,"title":"Number one"},
            {"const":"1","title":"String one"}
          ]}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"choice":42}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/choice", options = mapOf(
                    "autocomplete" to JsonPrimitive(false),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("42").assertExists()
        assertEquals(JsonPrimitive(42), data.jsonObject["choice"])
        assertEquals(0, changes)
        onNodeWithText("42").performClick()
        onNodeWithText("String one").performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("1"), data.jsonObject["choice"])
        assertEquals(1, changes)
    }

    private fun selectDropdown(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"choice":{"oneOf":[
            {"const":1,"title":"Number one"},
            {"const":"1","title":"String one"},
            {"const":true,"title":"Yes"}
          ]}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"choice":1}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/choice", options = mapOf(
                    "autocomplete" to JsonPrimitive(false),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Number one").performClick()
        onNodeWithText("String one").performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("1"), data.jsonObject["choice"])
        onNodeWithText("String one").performClick()
        onNodeWithText("Number one").performClick()
        waitForIdle()
        assertEquals(JsonPrimitive(1), data.jsonObject["choice"])
        onNodeWithText("Number one").performClick()
        onNodeWithText("Yes").performClick()
        waitForIdle()
        assertEquals(JsonPrimitive(true), data.jsonObject["choice"])
        assertEquals(3, changes)
    }

    private fun choose(cupertino: Boolean, oneOf: Boolean) = runComposeUiTest {
        val choices = if (oneOf) """"oneOf":[
          {"const":1,"title":"Number one"},
          {"const":"1","title":"String one"},
          {"const":true,"title":"Yes"}
        ]""" else """"enum":[1,"1",true]"""
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"choice":{$choices}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"choice":1}"""))
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/choice", options = mapOf(
                    "format" to JsonPrimitive("radio"),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        val radios = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
        assertEquals(3, onAllNodes(radios).fetchSemanticsNodes().size)
        onAllNodes(radios)[0].assertIsSelected()
        onAllNodes(radios)[1].assertIsNotSelected()
        onAllNodes(radios)[1].performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("1"), data.jsonObject["choice"])
        onAllNodes(radios)[0].assertIsNotSelected()
        onAllNodes(radios)[1].assertIsSelected()
        onAllNodes(radios)[0].performClick()
        waitForIdle()
        assertEquals(JsonPrimitive(1), data.jsonObject["choice"])
        onAllNodes(radios)[2].performClick()
        waitForIdle()
        assertEquals(JsonPrimitive(true), data.jsonObject["choice"])
    }
}
