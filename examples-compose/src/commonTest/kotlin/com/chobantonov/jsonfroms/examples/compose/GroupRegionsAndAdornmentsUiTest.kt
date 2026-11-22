package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.models.Layout
import com.chobantonov.jsonfroms.core.models.Categorization
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.models.toUiSchemaElement
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.extended.renderers.cupertinoExtendedRenderers
import com.chobantonov.jsonfroms.extended.renderers.materialExtendedRenderers
import com.chobantonov.jsonfroms.extended.renderers.ExtendedActionHandler
import com.chobantonov.jsonfroms.extended.renderers.ExtendedActionHandlerConfigKey
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class GroupRegionsAndAdornmentsUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement("""{"type":"object","properties":{"name":{"type":"string"}}}""").jsonObject)

    @Test fun materialGroupRegionsCollapseTogether() = groupRegions(false)
    @Test fun cupertinoGroupRegionsCollapseTogether() = groupRegions(true)
    @Test fun materialImportedCardRegionFixture() = importedCard(false)
    @Test fun cupertinoImportedCardRegionFixture() = importedCard(true)
    @Test fun materialImportedCardCoverFillsWidth() = importedCardCoverWidth(false)
    @Test fun cupertinoImportedCardCoverFillsWidth() = importedCardCoverWidth(true)
    @Test fun materialImportedCardFooterDispatchesAction() = importedCardAction(false)
    @Test fun cupertinoImportedCardFooterDispatchesAction() = importedCardAction(true)
    @Test fun materialImportedAdornmentFixture() = importedAdornments(false)
    @Test fun cupertinoImportedAdornmentFixture() = importedAdornments(true)
    @Test fun materialEntireImportedAdornmentFixture() = fullImportedAdornments(false)
    @Test fun cupertinoEntireImportedAdornmentFixture() = fullImportedAdornments(true)
    @Test fun materialHeaderInputDoesNotToggleDisclosure() = interactiveHeader(false)
    @Test fun cupertinoHeaderInputDoesNotToggleDisclosure() = interactiveHeader(true)
    @Test fun materialGroupIndicatorsUseBodyOnly() = groupIndicatorsUseBodyOnly(false)
    @Test fun cupertinoGroupIndicatorsUseBodyOnly() = groupIndicatorsUseBodyOnly(true)
    @Test fun materialCardRegionsWrapAtNarrowWidth() = cardRegionsWrap(false)
    @Test fun cupertinoCardRegionsWrapAtNarrowWidth() = cardRegionsWrap(true)
    @Test fun materialRegionRulesAndNestedDataPaths() = regionRulesAndNestedDataPaths(false)
    @Test fun cupertinoRegionRulesAndNestedDataPaths() = regionRulesAndNestedDataPaths(true)
    @Test fun materialGroupReadonlyReachesRegions() = groupReadonlyReachesRegions(false)
    @Test fun cupertinoGroupReadonlyReachesRegions() = groupReadonlyReachesRegions(true)
    @Test fun materialGroupReadonlyReachesBooleanAndChoiceRegions() = groupReadonlyNonText(false)
    @Test fun cupertinoGroupReadonlyReachesBooleanAndChoiceRegions() = groupReadonlyNonText(true)
    @Test fun materialNestedRegionLayoutsPreserveReadonly() = nestedRegionReadonly(false)
    @Test fun cupertinoNestedRegionLayoutsPreserveReadonly() = nestedRegionReadonly(true)
    @Test fun materialGroupRulesApplyToAllRegions() = groupRulesApplyToAllRegions(false)
    @Test fun cupertinoGroupRulesApplyToAllRegions() = groupRulesApplyToAllRegions(true)
    @Test fun materialTextAdornmentsStayOutsideData() = adornments(false)
    @Test fun cupertinoTextAdornmentsStayOutsideData() = adornments(true)
    @Test fun materialNumberAdornments() = formattedAdornment(false, "number", null, "42")
    @Test fun cupertinoNumberAdornments() = formattedAdornment(true, "number", null, "42")
    @Test fun materialDurationAdornments() = formattedAdornment(false, "string", "duration", "PT1H")
    @Test fun cupertinoDurationAdornments() = formattedAdornment(true, "string", "duration", "PT1H")
    @Test fun materialCronAdornments() = formattedAdornment(false, "string", "cron", "0 * * * *")
    @Test fun cupertinoCronAdornments() = formattedAdornment(true, "string", "cron", "0 * * * *")
    @Test fun materialColorAdornments() = formattedAdornment(false, "string", "color", "#ff0000")
    @Test fun cupertinoColorAdornments() = formattedAdornment(true, "string", "color", "#ff0000")
    @Test fun materialDateAdornmentsKeepPicker() = formattedAdornment(false, "string", "date", "2025-01-10", "Choose date")
    @Test fun cupertinoDateAdornmentsKeepPicker() = formattedAdornment(true, "string", "date", "2025-01-10", "Choose date")
    @Test fun materialTimeAdornmentsKeepPicker() = formattedAdornment(false, "string", "time", "09:30:00", "Choose time")
    @Test fun cupertinoTimeAdornmentsKeepPicker() = formattedAdornment(true, "string", "time", "09:30:00", "Choose time")
    @Test fun materialDateTimeAdornmentsKeepPicker() = formattedAdornment(false, "string", "date-time", "2025-01-10T09:30:00Z", "Choose date and time")
    @Test fun cupertinoDateTimeAdornmentsKeepPicker() = formattedAdornment(true, "string", "date-time", "2025-01-10T09:30:00Z", "Choose date and time")
    @Test fun materialPasswordAdornmentsKeepReveal() = formattedAdornment(false, "string", "password", "Secret", "Show password")
    @Test fun cupertinoPasswordAdornmentsKeepReveal() = formattedAdornment(true, "string", "password", "Secret", "Show password")
    @Test fun materialEnumAdornments() = choiceAdornments(false, "enum")
    @Test fun cupertinoEnumAdornments() = choiceAdornments(true, "enum")
    @Test fun materialOneOfAdornments() = choiceAdornments(false, "oneOf")
    @Test fun cupertinoOneOfAdornments() = choiceAdornments(true, "oneOf")
    @Test fun materialAnyOfTextChoiceAdornments() = choiceAdornments(false, "anyOf")
    @Test fun cupertinoAnyOfTextChoiceAdornments() = choiceAdornments(true, "anyOf")
    @Test fun materialAdornmentsFollowLocale() = localizedAdornments(false)
    @Test fun cupertinoAdornmentsFollowLocale() = localizedAdornments(true)
    @Test fun materialAdornmentsUseLogicalLtrEdges() = logicalEdges(false, false)
    @Test fun cupertinoAdornmentsUseLogicalLtrEdges() = logicalEdges(true, false)
    @Test fun materialAdornmentsUseLogicalRtlEdges() = logicalEdges(false, true)
    @Test fun cupertinoAdornmentsUseLogicalRtlEdges() = logicalEdges(true, true)
    @Test fun materialChoiceAdornmentsFitLtr() = nativeEdgeGeometry(false, "choice", false)
    @Test fun cupertinoChoiceAdornmentsFitLtr() = nativeEdgeGeometry(true, "choice", false)
    @Test fun materialChoiceAdornmentsFitRtl() = nativeEdgeGeometry(false, "choice", true)
    @Test fun cupertinoChoiceAdornmentsFitRtl() = nativeEdgeGeometry(true, "choice", true)
    @Test fun materialPasswordAdornmentsFitLtr() = nativeEdgeGeometry(false, "password", false)
    @Test fun cupertinoPasswordAdornmentsFitLtr() = nativeEdgeGeometry(true, "password", false)
    @Test fun materialPasswordAdornmentsFitRtl() = nativeEdgeGeometry(false, "password", true)
    @Test fun cupertinoPasswordAdornmentsFitRtl() = nativeEdgeGeometry(true, "password", true)
    @Test fun materialDateAdornmentsFitLtr() = nativeEdgeGeometry(false, "date", false)
    @Test fun cupertinoDateAdornmentsFitLtr() = nativeEdgeGeometry(true, "date", false)
    @Test fun materialDateAdornmentsFitRtl() = nativeEdgeGeometry(false, "date", true)
    @Test fun cupertinoDateAdornmentsFitRtl() = nativeEdgeGeometry(true, "date", true)
    @Test fun materialTimeAdornmentsFitLtr() = nativeEdgeGeometry(false, "time", false)
    @Test fun cupertinoTimeAdornmentsFitLtr() = nativeEdgeGeometry(true, "time", false)
    @Test fun materialTimeAdornmentsFitRtl() = nativeEdgeGeometry(false, "time", true)
    @Test fun cupertinoTimeAdornmentsFitRtl() = nativeEdgeGeometry(true, "time", true)
    @Test fun materialDateTimeAdornmentsFitLtr() = nativeEdgeGeometry(false, "date-time", false)
    @Test fun cupertinoDateTimeAdornmentsFitLtr() = nativeEdgeGeometry(true, "date-time", false)
    @Test fun materialDateTimeAdornmentsFitRtl() = nativeEdgeGeometry(false, "date-time", true)
    @Test fun cupertinoDateTimeAdornmentsFitRtl() = nativeEdgeGeometry(true, "date-time", true)
    @Test fun materialDurationAdornmentsFitLtr() = extendedEdgeGeometry(false, "duration", false)
    @Test fun cupertinoDurationAdornmentsFitLtr() = extendedEdgeGeometry(true, "duration", false)
    @Test fun materialDurationAdornmentsFitRtl() = extendedEdgeGeometry(false, "duration", true)
    @Test fun cupertinoDurationAdornmentsFitRtl() = extendedEdgeGeometry(true, "duration", true)
    @Test fun materialCronAdornmentsFitLtr() = extendedEdgeGeometry(false, "cron", false)
    @Test fun cupertinoCronAdornmentsFitLtr() = extendedEdgeGeometry(true, "cron", false)
    @Test fun materialCronAdornmentsFitRtl() = extendedEdgeGeometry(false, "cron", true)
    @Test fun cupertinoCronAdornmentsFitRtl() = extendedEdgeGeometry(true, "cron", true)
    @Test fun materialColorAdornmentsFitLtr() = extendedEdgeGeometry(false, "color", false)
    @Test fun cupertinoColorAdornmentsFitLtr() = extendedEdgeGeometry(true, "color", false)
    @Test fun materialColorAdornmentsFitRtl() = extendedEdgeGeometry(false, "color", true)
    @Test fun cupertinoColorAdornmentsFitRtl() = extendedEdgeGeometry(true, "color", true)
    @Test fun materialIntegerStepAdornmentsFitLtr() = numericStepGeometry(false, false, false)
    @Test fun cupertinoIntegerStepAdornmentsFitLtr() = numericStepGeometry(true, false, false)
    @Test fun materialIntegerStepAdornmentsFitRtl() = numericStepGeometry(false, true, false)
    @Test fun cupertinoIntegerStepAdornmentsFitRtl() = numericStepGeometry(true, true, false)
    @Test fun materialDecimalStepAdornmentsFitLtr() = numericStepGeometry(false, false, true)
    @Test fun cupertinoDecimalStepAdornmentsFitLtr() = numericStepGeometry(true, false, true)
    @Test fun materialDecimalStepAdornmentsFitRtl() = numericStepGeometry(false, true, true)
    @Test fun cupertinoDecimalStepAdornmentsFitRtl() = numericStepGeometry(true, true, true)

    private fun groupRegions(cupertino: Boolean) = runComposeUiTest {
        val ui = Json.parseToJsonElement("""{
          "type":"Group","label":"Unused heading","options":{"collapsible":true,"collapsed":true},
          "cover":{"type":"Label","text":"Cover region"},
          "header":[{"type":"Label","text":"Authored heading"},
            {"type":"Label","text":"Supporting text"}],
          "elements":[{"type":"Control","scope":"#/properties/name"}],
          "footer":[{"type":"Label","text":"Footer region"}]
        }""").jsonObject.toUiSchemaElement()
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"name":"Ada"}"""), schema = schema,
                uischema = ui, renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onNodeWithText("Cover region").assertExists()
        onNodeWithText("Authored heading").assertExists()
        onNodeWithText("Supporting text").assertExists()
        onNodeWithText("Unused heading").assertDoesNotExist()
        onNodeWithText("Footer region").assertDoesNotExist()
        assertEquals(0, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onNodeWithText("Authored heading").performClick()
        onNodeWithText("Footer region").assertExists()
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
    }

    private fun adornments(cupertino: Boolean) = runComposeUiTest {
        val ui = Json.parseToJsonElement("""{
          "type":"Control","scope":"#/properties/name","options":{
            "startAdornment":"#","endAdornment":[{"kind":"text","text":"internal",
              "tooltip":"Reference help"},
              {"kind":"icon","icon":"unknown","label":"Info"}]}
        }""").jsonObject.toUiSchemaElement()
        var writes = 0
        setContent {
            Box(Modifier.width(240.dp)) {
                JsonForms(data = Json.parseToJsonElement("""{"name":"Ada"}"""), schema = schema,
                    uischema = ui, renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                    onChange = { writes++ })
            }
        }
        onAllNodesWithText("#").onFirst().assertExists()
        onAllNodesWithText("internal").onFirst().assertExists()
        val startBefore = onNodeWithText("#", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val endBefore = onNodeWithText("internal", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val fieldBefore = onAllNodes(hasSetTextAction()).onFirst().fetchSemanticsNode().boundsInRoot
        assertEquals("Reference help", onNodeWithText("internal").fetchSemanticsNode()
            .config[SemanticsProperties.StateDescription])
        onNodeWithText("Info").assertExists()
        onNodeWithText("internal").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        waitForIdle()
        onNodeWithText("Reference help").assertExists()
        assertEquals(startBefore, onNodeWithText("#", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot)
        assertEquals(endBefore, onNodeWithText("internal", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot)
        assertEquals(fieldBefore, onAllNodes(hasSetTextAction()).onFirst()
            .fetchSemanticsNode().boundsInRoot)
        assertEquals(0, writes)
    }

    private fun importedCard(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-group-layout" }
        val regionGroup = requireNotNull(cardRegion(requireNotNull(example.uischema)))
        setContent {
            JsonForms(data = example.data, schema = requireNotNull(example.schema), uischema = regionGroup,
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = example.config)
        }
        onNodeWithContentDescription("Sunlit courtyard at a boutique hotel").assertExists()
        onNodeWithText("Hospitality partner").assertExists()
        onNodeWithText("Preview language").assertDoesNotExist()
        onNodeWithText("Hospitality partner").performClick()
        onNodeWithText("Preview language").assertExists()
    }

    private fun importedCardCoverWidth(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-group-layout" }
        val regionGroup = requireNotNull(cardRegion(requireNotNull(example.uischema)))
        var width by mutableStateOf(700.dp)
        var writes = 0
        setContent {
            Box(Modifier.width(width).testTag("card-width-host")) {
                JsonForms(data = example.data, schema = requireNotNull(example.schema),
                    uischema = regionGroup,
                    renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                    config = example.config,
                    onChange = { writes++ })
            }
        }
        val cover = onNodeWithContentDescription("Sunlit courtyard at a boutique hotel")
        val host = onNodeWithTag("card-width-host")
        waitUntil(timeoutMillis = 5_000) {
            cover.fetchSemanticsNode().boundsInRoot.width > 0f
        }
        val wideHostWidth = host.fetchSemanticsNode().boundsInRoot.width
        val wideCoverWidth = cover.fetchSemanticsNode().boundsInRoot.width
        assertTrue(wideCoverWidth >= wideHostWidth - 2f,
            "Authored 100% cover should span the wide card: cover=$wideCoverWidth host=$wideHostWidth")
        runOnIdle { width = 240.dp }
        val narrowHostWidth = host.fetchSemanticsNode().boundsInRoot.width
        val narrowCoverWidth = cover.fetchSemanticsNode().boundsInRoot.width
        assertTrue(narrowCoverWidth >= narrowHostWidth - 2f && narrowCoverWidth <= narrowHostWidth + 2f,
            "Cover should follow the narrow card width: cover=$narrowCoverWidth host=$narrowHostWidth")
        onNodeWithText("Hospitality partner").performClick()
        cover.assertExists()
        assertEquals(0, writes)
    }

    private fun cardRegion(element: UiSchemaElement): GroupElement? = when (element) {
        is GroupElement -> if (element.cover != null) element
            else element.elements.firstNotNullOfOrNull(::cardRegion)
        is Layout -> element.elements.firstNotNullOfOrNull(::cardRegion)
        is Categorization -> element.elements.firstNotNullOfOrNull(::cardRegion)
        else -> null
    }

    private fun importedCardAction(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-group-layout" }
        val regionGroup = requireNotNull(cardRegion(requireNotNull(example.uischema)))
        val actions = mutableListOf<Pair<String, String?>>()
        var writes = 0
        setContent {
            JsonForms(data = example.data, schema = requireNotNull(example.schema), uischema = regionGroup,
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = example.config + mapOf(ExtendedActionHandlerConfigKey to ExtendedActionHandler { action, params ->
                    actions += action to params["locale"]?.toString()?.trim('"')
                }),
                onChange = { writes++ })
        }
        onNodeWithContentDescription("Sunlit courtyard at a boutique hotel").assertExists()
        onNodeWithText("Hospitality partner").performClick()
        onNodeWithText("Preview language").assertExists()
        onNodeWithText("Български").performClick()
        waitForIdle()
        assertEquals(listOf<Pair<String, String?>>("setLocale" to "bg"), actions)
        assertEquals(0, writes)
    }

    private fun regionRulesAndNestedDataPaths(cupertino: Boolean) = runComposeUiTest {
        val nestedSchema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{
            "hideRegions":{"type":"boolean"},
            "details":{"type":"object","properties":{
              "cover":{"type":"string"},"header":{"type":"string"},
              "body":{"type":"string"},"footer":{"type":"string"}}}
          }
        }""").jsonObject)
        val hideRule = """"rule":{"effect":"HIDE","condition":{
          "scope":"#/properties/hideRegions","expectedValue":true}}"""
        val ui = Json.parseToJsonElement("""{
          "type":"Group","label":"Details",
          "cover":{"type":"Control","scope":"#/properties/details/properties/cover",$hideRule},
          "header":[{"type":"Control","scope":"#/properties/details/properties/header",$hideRule}],
          "elements":[{"type":"Control","scope":"#/properties/details/properties/body"}],
          "footer":[{"type":"Control","scope":"#/properties/details/properties/footer",$hideRule}]
        }""").jsonObject.toUiSchemaElement()
        var data by mutableStateOf(Json.parseToJsonElement("""{
          "hideRegions":false,
          "details":{"cover":"Cover","header":"Header","body":"Body","footer":"Footer"}
        }"""))
        var readonly by mutableStateOf(false)
        var writes = 0
        setContent {
            JsonForms(data = data, schema = nestedSchema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                readonly = readonly,
                onChange = { data = it.data; writes++ })
        }
        assertEquals(4, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onAllNodes(hasSetTextAction())[0].performTextReplacement("New cover")
        onAllNodes(hasSetTextAction())[1].performTextReplacement("New header")
        onAllNodes(hasSetTextAction())[3].performTextReplacement("New footer")
        waitForIdle()
        assertEquals(3, writes)
        assertEquals(Json.parseToJsonElement("""{
          "hideRegions":false,
          "details":{"cover":"New cover","header":"New header","body":"Body","footer":"New footer"}
        }"""), data)
        runOnIdle {
            data = Json.parseToJsonElement("""{
              "hideRegions":true,
              "details":{"cover":"New cover","header":"New header","body":"Body","footer":"New footer"}
            }""")
        }
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onAllNodes(hasSetTextAction())[0].performTextReplacement("New body")
        waitForIdle()
        assertEquals("New body", data.jsonObject.getValue("details").jsonObject.getValue("body")
            .toString().trim('"'))
        assertEquals(4, writes)
        runOnIdle {
            data = Json.parseToJsonElement("""{
              "hideRegions":false,
              "details":{"cover":"New cover","header":"New header","body":"New body","footer":"New footer"}
            }""")
            readonly = true
        }
        assertEquals(0, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        listOf("New cover", "New header", "New body", "New footer")
            .forEach { onNodeWithText(it).assertExists() }
        assertEquals(4, writes)
        runOnIdle { readonly = false }
        assertEquals(4, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
    }

    private fun groupReadonlyReachesRegions(cupertino: Boolean) = runComposeUiTest {
        val fieldSchema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{
            "cover":{"type":"string"},"header":{"type":"string"},
            "body":{"type":"string"},"footer":{"type":"string"}}
        }""").jsonObject)
        var groupReadonly by mutableStateOf(true)
        val data = Json.parseToJsonElement("""{
          "cover":"Cover","header":"Header","body":"Body","footer":"Footer"
        }""")
        var writes = 0
        setContent {
            TestTouchInputMode()
            val fieldOptions = """"options":{"clearable":true}"""
            val ui = Json.parseToJsonElement("""{
              "type":"Group","options":{"readonly":$groupReadonly},
              "cover":{"type":"Control","scope":"#/properties/cover",$fieldOptions},
              "header":[{"type":"Control","scope":"#/properties/header",$fieldOptions}],
              "elements":[{"type":"Control","scope":"#/properties/body",$fieldOptions}],
              "footer":[{"type":"Control","scope":"#/properties/footer",$fieldOptions}]
            }""").jsonObject.toUiSchemaElement()
            JsonForms(data = data, schema = fieldSchema, uischema = ui,
                config = mapOf("separateReadonlyFromDisabled" to true),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { writes++ })
        }
        listOf("Cover", "Header", "Body", "Footer")
            .forEach { onAllNodesWithText(it).onLast().assertIsEnabled() }
        assertEquals(0, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        assertEquals(0, onAllNodesWithContentDescription("Clear value").fetchSemanticsNodes().size)
        assertEquals(0, writes)
        runOnIdle { groupReadonly = false }
        assertEquals(4, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        assertEquals(0, onAllNodesWithContentDescription("Clear value").fetchSemanticsNodes().size)
        onAllNodes(hasSetTextAction()).onFirst().performClick()
        assertEquals(1, onAllNodesWithContentDescription("Clear value").fetchSemanticsNodes().size)
    }

    private fun groupReadonlyNonText(cupertino: Boolean) = runComposeUiTest {
        val fieldSchema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{
            "cover":{"type":"boolean"},
            "header":{"type":"string","enum":["Red","Blue"]},
            "body":{"type":"string","enum":["Red","Blue"]},
            "footer":{"type":"boolean"}}
        }""").jsonObject)
        val data = Json.parseToJsonElement("""{
          "cover":true,"header":"Red","body":"Blue","footer":false
        }""")
        val ui = Json.parseToJsonElement("""{
          "type":"Group","options":{"readonly":true},
          "cover":{"type":"Control","scope":"#/properties/cover"},
          "header":[{"type":"Control","scope":"#/properties/header","options":{"clearable":true}}],
          "elements":[{"type":"Control","scope":"#/properties/body","options":{"format":"radio"}}],
          "footer":[{"type":"Control","scope":"#/properties/footer"}]
        }""").jsonObject.toUiSchemaElement()
        var writes = 0
        setContent {
            JsonForms(data = data, schema = fieldSchema, uischema = ui,
                config = mapOf("separateReadonlyFromDisabled" to true),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { writes++ })
        }
        val toggles = onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ToggleableState))
        assertEquals(2, toggles.fetchSemanticsNodes().size)
        repeat(2) { toggles[it].assertIsNotEnabled() }
        val radios = onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        assertEquals(2, radios.fetchSemanticsNodes().size)
        repeat(2) { radios[it].assertIsNotEnabled() }
        assertEquals(0, onAllNodesWithContentDescription("Clear value").fetchSemanticsNodes().size)
        assertEquals(0, writes)
    }

    private fun nestedRegionReadonly(cupertino: Boolean) = runComposeUiTest {
        val fieldSchema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{
            "header":{"type":"string"},"body":{"type":"string"},"footer":{"type":"string"}}
        }""").jsonObject)
        val ui = Json.parseToJsonElement("""{
          "type":"Group",
          "header":[{"type":"VerticalLayout","options":{"readonly":true},
            "elements":[{"type":"Control","scope":"#/properties/header",
              "options":{"clearable":true}}]}],
          "elements":[{"type":"Control","scope":"#/properties/body",
            "options":{"clearable":true}}],
          "footer":[{"type":"HorizontalLayout","options":{"readonly":true},
            "elements":[{"type":"Control","scope":"#/properties/footer",
              "options":{"clearable":true}}]}]
        }""").jsonObject.toUiSchemaElement()
        var data by mutableStateOf(Json.parseToJsonElement("""{
          "header":"Header","body":"Body","footer":"Footer"
        }"""))
        var writes = 0
        setContent {
            TestTouchInputMode()
            JsonForms(data = data, schema = fieldSchema, uischema = ui,
                config = mapOf("separateReadonlyFromDisabled" to true),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; writes++ })
        }
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        assertEquals(0, onAllNodesWithContentDescription("Clear value").fetchSemanticsNodes().size)
        onAllNodes(hasSetTextAction()).onFirst().performClick()
        assertEquals(1, onAllNodesWithContentDescription("Clear value").fetchSemanticsNodes().size)
        onAllNodes(hasSetTextAction())[0].performTextReplacement("Changed")
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{
          "header":"Header","body":"Changed","footer":"Footer"
        }"""), data)
        assertEquals(1, writes)
    }

    private fun groupRulesApplyToAllRegions(cupertino: Boolean) = runComposeUiTest {
        val fieldSchema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{
            "locked":{"type":"boolean"},"cover":{"type":"string"},
            "header":{"type":"string"},"body":{"type":"string"},
            "footer":{"type":"string"}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{
          "locked":true,"cover":"Cover","header":"Header",
          "body":"Body","footer":"Footer"
        }"""))
        var effect by mutableStateOf("DISABLE")
        var writes = 0
        setContent {
            val ui = Json.parseToJsonElement("""{
              "type":"Group","label":"Card",
              "rule":{"effect":"$effect","condition":{
                "scope":"#/properties/locked","expectedValue":true}},
              "cover":{"type":"Control","scope":"#/properties/cover"},
              "header":[{"type":"Control","scope":"#/properties/header"}],
              "elements":[{"type":"Control","scope":"#/properties/body"}],
              "footer":[{"type":"Control","scope":"#/properties/footer"}]
            }""").jsonObject.toUiSchemaElement()
            JsonForms(data = data, schema = fieldSchema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; writes++ })
        }
        assertEquals(0, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        listOf("Cover", "Header", "Body", "Footer")
            .forEach { onAllNodesWithText(it).onFirst().assertExists() }
        runOnIdle {
            data = Json.parseToJsonElement("""{
              "locked":false,"cover":"Cover","header":"Header",
              "body":"Body","footer":"Footer"
            }""")
        }
        assertEquals(4, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onAllNodes(hasSetTextAction())[3].performTextReplacement("Changed")
        waitForIdle()
        assertEquals("Changed", data.jsonObject.getValue("footer").toString().trim('"'))
        assertEquals(1, writes)
        runOnIdle {
            effect = "HIDE"
            data = Json.parseToJsonElement("""{
              "locked":true,"cover":"Cover","header":"Header",
              "body":"Body","footer":"Changed"
            }""")
        }
        listOf("Cover", "Header", "Body", "Changed")
            .forEach { assertEquals(0, onAllNodesWithText(it).fetchSemanticsNodes().size) }
        assertEquals(0, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        assertEquals(1, writes)
    }

    private fun importedAdornments(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-input-adornments" }
        val root = requireNotNull(example.uischema) as Layout
        val stringGroup = root.elements.first() as GroupElement
        setContent {
            JsonForms(data = example.data, schema = requireNotNull(example.schema), uischema = stringGroup,
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers)
        }
        onAllNodesWithText("#").onFirst().assertExists()
        onAllNodesWithText("internal").onFirst().assertExists()
        assertEquals(3, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
    }

    private fun fullImportedAdornments(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-input-adornments" }
        var writes = 0
        setContent {
            JsonForms(data = example.data, schema = requireNotNull(example.schema),
                uischema = requireNotNull(example.uischema),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { writes++ })
        }
        listOf("#", "internal", "ID", "€", "kg", "Choice", "Due", "At", "Keep", "Run")
            .forEach { label -> onAllNodesWithText(label).onFirst().assertExists() }
        onAllNodesWithText("🔒", useUnmergedTree = true).onFirst().assertExists()
        onAllNodesWithContentDescription("Secret").onFirst().assertExists()
        assertEquals(0, writes)
    }

    private fun interactiveHeader(cupertino: Boolean) = runComposeUiTest {
        val headerSchema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"header":{"type":"string"},"body":{"type":"string"}}
        }""").jsonObject)
        val ui = Json.parseToJsonElement("""{
          "type":"Group","options":{"collapsible":true,"collapsed":true},
          "header":[{"type":"Label","text":"Heading"},
            {"type":"Control","scope":"#/properties/header"}],
          "elements":[{"type":"Control","scope":"#/properties/body"}]
        }""").jsonObject.toUiSchemaElement()
        var data by mutableStateOf(Json.parseToJsonElement("""{"header":"Old","body":"Hidden"}"""))
        setContent {
            JsonForms(data = data, schema = headerSchema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onAllNodes(hasSetTextAction())[0].performTextReplacement("New")
        assertEquals("New", data.jsonObject["header"]?.toString()?.trim('"'))
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
    }

    private fun groupIndicatorsUseBodyOnly(cupertino: Boolean) = runComposeUiTest {
        val regionSchema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","required":["body"],"properties":{
            "cover":{"type":"string","minLength":2},
            "header":{"type":"string","minLength":2},
            "body":{"type":"string","minLength":2},
            "footer":{"type":"string","minLength":2}}
        }""").jsonObject)
        val ui = Json.parseToJsonElement("""{
          "type":"Group","options":{"collapsible":true,"collapsed":true,
            "showDataIndicator":true,"showValidationIndicator":true,
            "showValidationIndicatorCount":true},
          "cover":{"type":"Control","scope":"#/properties/cover"},
          "header":[{"type":"Label","text":"Heading"},
            {"type":"Control","scope":"#/properties/header"}],
          "elements":[{"type":"Control","scope":"#/properties/body"}],
          "footer":[{"type":"Control","scope":"#/properties/footer"}]
        }""").jsonObject.toUiSchemaElement()
        var data by mutableStateOf(Json.parseToJsonElement("""{
          "cover":"x","header":"x","footer":"x"
        }"""))
        var writes = 0
        setContent {
            JsonForms(data = data, schema = regionSchema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { writes++ })
        }
        fun heading() = onNodeWithText("Heading").fetchSemanticsNode().config
        assertEquals("1 error", heading()[SemanticsProperties.Error])
        onNodeWithContentDescription("Contains data").assertDoesNotExist()
        runOnIdle { data = Json.parseToJsonElement("""{
          "cover":"x","header":"x","body":"ok","footer":"x"
        }""") }
        assertFalse(heading().contains(SemanticsProperties.Error))
        onNodeWithContentDescription("Contains data").assertExists()
        runOnIdle { data = Json.parseToJsonElement("""{
          "cover":"x","header":"x","body":"x","footer":"x"
        }""") }
        assertEquals("1 error", heading()[SemanticsProperties.Error])
        assertEquals(0, writes)
    }

    private fun cardRegionsWrap(cupertino: Boolean) = runComposeUiTest {
        val ui = Json.parseToJsonElement("""{
          "type":"Group","label":"Unused heading",
          "cover":{"type":"Label","text":"Cover region"},
          "header":[{"type":"Label","text":"Hospitality partner"},
            {"type":"Label","text":"Verified service partner · Member since 2018"}],
          "elements":[{"type":"Control","scope":"#/properties/name"}],
          "footer":[{"type":"Label","text":"Preview language"},
            {"type":"Label","text":"English preview"},
            {"type":"Label","text":"Български преглед"}]
        }""").jsonObject.toUiSchemaElement()
        var width by mutableStateOf(700.dp)
        setContent {
            Box(Modifier.width(width)) {
                JsonForms(data = Json.parseToJsonElement("""{"name":"Ada"}"""), schema = schema,
                    uischema = ui, renderers = if (cupertino) cupertinoRenderers else materialRenderers)
            }
        }
        fun top(label: String) = onNodeWithText(label, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot.top
        assertTrue(kotlin.math.abs(top("Hospitality partner") -
            top("Verified service partner · Member since 2018")) <= 2f)
        assertTrue(kotlin.math.abs(top("English preview") - top("Български преглед")) <= 2f)
        onNodeWithText("Unused heading").assertDoesNotExist()
        runOnIdle { width = 240.dp }
        assertTrue(top("Verified service partner · Member since 2018") > top("Hospitality partner"),
            "Supporting heading must wrap at 240 dp")
        assertTrue(top("Български преглед") > top("English preview"),
            "Footer content must wrap at 240 dp")
        val body = onAllNodes(hasSetTextAction()).onFirst().fetchSemanticsNode().boundsInRoot
        assertTrue(top("Preview language") >= body.bottom,
            "Footer must follow body inside card")
    }

    private fun formattedAdornment(cupertino: Boolean, type: String, format: String?, value: String,
        nativeAction: String? = null) = runComposeUiTest {
        val formatMember = format?.let { ",\"format\":\"$it\"" }.orEmpty()
        val fieldSchema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"$type"$formatMember}}
        }""").jsonObject)
        val ui = Json.parseToJsonElement("""{
          "type":"Control","scope":"#/properties/value",
          "options":{"startAdornment":"prefix","endAdornment":"suffix"}
        }""").jsonObject.toUiSchemaElement()
        val data = Json.parseToJsonElement("""{"value":${if (type == "number") value else "\"$value\""}}""")
        var writes = 0
        setContent {
            JsonForms(data = data, schema = fieldSchema, uischema = ui,
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { writes++ })
        }
        onNodeWithText("prefix").assertExists()
        onNodeWithText("suffix").assertExists()
        assertEquals(0, writes)
        if (nativeAction != null) {
            onNodeWithContentDescription(nativeAction).performClick()
            if (format == "password") onNodeWithContentDescription("Hide password").assertExists()
            onNodeWithText("prefix").assertExists()
            onNodeWithText("suffix").assertExists()
            assertEquals(0, writes)
        }
    }

    private fun choiceAdornments(cupertino: Boolean, kind: String) = runComposeUiTest {
        val choice = when (kind) {
            "enum" -> """"type":"string","enum":["Red","Blue"]"""
            "oneOf" -> """"oneOf":[{"const":"Red","title":"Red"},{"const":"Blue","title":"Blue"}]"""
            else -> """"anyOf":[{"type":"string","enum":["Red","Blue"]},{"type":"string"}]"""
        }
        val choiceSchema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"choice":{$choice}}
        }""").jsonObject)
        val ui = Json.parseToJsonElement("""{
          "type":"Control","scope":"#/properties/choice",
          "options":{"autocomplete":true,"startAdornment":"prefix","endAdornment":"suffix"}
        }""").jsonObject.toUiSchemaElement()
        var data by mutableStateOf(Json.parseToJsonElement("""{"choice":"Red"}"""))
        var writes = 0
        setContent {
            JsonForms(data = data, schema = choiceSchema,
                uischema = ui, renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; writes++ })
        }
        onNodeWithText("prefix").assertExists()
        onNodeWithText("suffix").assertExists()
        assertEquals(0, writes)
        if (kind == "anyOf") {
            onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("Custom")
            waitForIdle()
            assertEquals(Json.parseToJsonElement("""{"choice":"Custom"}"""), data)
            assertEquals(1, writes)
            onNodeWithText("prefix").assertExists()
            onNodeWithText("suffix").assertExists()
        }
    }

    private fun localizedAdornments(cupertino: Boolean) = runComposeUiTest {
        val ui = Json.parseToJsonElement("""{
          "type":"Control","scope":"#/properties/name","options":{
            "startAdornment":{"kind":"text","text":"Pounds","tooltip":"Currency help","i18n":"currency.unit"},
            "endAdornment":{"kind":"icon","icon":"unknown","label":"Info","i18n":"currency.info"}}
        }""").jsonObject.toUiSchemaElement()
        var locale by mutableStateOf("en")
        var writes = 0
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"name":"Ada"}"""), schema = schema,
                uischema = ui, renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale, translate = { key, fallback, _ ->
                    if (locale == "bg") when (key) {
                        "currency.unit.text" -> "Лева"
                        "currency.unit.tooltip" -> "Помощ за валутата"
                        "currency.info.label" -> "Информация"
                        else -> fallback
                    } else fallback
                }),
                onChange = { writes++ })
        }
        onNodeWithText("Pounds").assertExists()
        onNodeWithText("Info").assertExists()
        runOnIdle { locale = "bg" }
        onNodeWithText("Pounds").assertDoesNotExist()
        onNodeWithText("Лева").assertExists()
        onNodeWithText("Информация").assertExists()
        assertEquals("Помощ за валутата", onNodeWithText("Лева").fetchSemanticsNode()
            .config[SemanticsProperties.StateDescription])
        assertEquals(0, writes)
    }

    private fun logicalEdges(cupertino: Boolean, rtl: Boolean) = runComposeUiTest {
        val ui = Json.parseToJsonElement("""{
          "type":"Control","scope":"#/properties/name",
          "options":{"startAdornment":"START","endAdornment":"END"}
        }""").jsonObject.toUiSchemaElement()
        setContent {
            TestTouchInputMode()
            CompositionLocalProvider(LocalLayoutDirection provides
                (if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr)) {
                Box(Modifier.width(240.dp)) {
                    JsonForms(data = Json.parseToJsonElement("""{"name":"Ada"}"""), schema = schema,
                        uischema = ui,
                        renderers = if (cupertino) cupertinoRenderers else materialRenderers)
                }
            }
        }
        val start = onNodeWithText("START", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val end = onNodeWithText("END", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val field = onAllNodes(hasSetTextAction()).onFirst().fetchSemanticsNode().boundsInRoot
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        onAllNodes(hasSetTextAction()).onFirst().performMouseInput { moveTo(center) }
        waitForIdle()
        val clear = onNodeWithContentDescription("Clear value", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        if (rtl) assertTrue(start.left >= end.right,
            "RTL start must be to the right of end: $start, $end")
        else assertTrue(start.right <= end.left,
            "LTR start must be to the left of end: $start, $end")
        assertTrue(start.left >= field.left - 2f && start.right <= field.right + 2f,
            "Start must remain inside field: $start, $field")
        assertTrue(end.left >= field.left - 2f && end.right <= field.right + 2f,
            "End must remain inside field: $end, $field")
        assertTrue(clear.left >= field.left - 2f && clear.right <= field.right + 2f,
            "Clear must remain inside the 240 dp field: $clear, $field")
        assertTrue(end.right <= clear.left || clear.right <= end.left,
            "End adornment must not overlap clear: $end, $clear")
    }

    private fun nativeEdgeGeometry(cupertino: Boolean, kind: String, rtl: Boolean) = runComposeUiTest {
        val property = when (kind) {
            "choice" -> """{"type":"string","enum":["Red","Blue"]}"""
            else -> """{"type":"string","format":"$kind"}"""
        }
        val fieldSchema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":$property}
        }""").jsonObject)
        val value = when (kind) {
            "choice" -> "Red"
            "password" -> "Secret"
            "time" -> "09:30:00"
            "date-time" -> "2025-01-10T09:30:00Z"
            else -> "2025-01-10"
        }
        val nativeAction = when (kind) {
            "password" -> "Show password"
            "date" -> "Choose date"
            "time" -> "Choose time"
            "date-time" -> "Choose date and time"
            else -> "Clear value"
        }
        val ui = Json.parseToJsonElement("""{
          "type":"Control","scope":"#/properties/value",
          "options":{"autocomplete":true,"startAdornment":"S","endAdornment":"E"}
        }""").jsonObject.toUiSchemaElement()
        setContent {
            TestTouchInputMode()
            CompositionLocalProvider(LocalLayoutDirection provides
                (if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr)) {
                Box(Modifier.width(240.dp)) {
                    JsonForms(data = Json.parseToJsonElement("""{"value":"$value"}"""),
                        schema = fieldSchema, uischema = ui,
                        renderers = if (cupertino) cupertinoRenderers else materialRenderers)
                }
            }
        }
        val start = onNodeWithText("S", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val end = onNodeWithText("E", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        onAllNodes(hasSetTextAction()).onFirst().performMouseInput { moveTo(center) }
        waitForIdle()
        val action = onNodeWithContentDescription(nativeAction, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val field = onAllNodes(hasSetTextAction()).onFirst().fetchSemanticsNode().boundsInRoot
        if (rtl) assertTrue(start.left >= end.right, "$kind RTL edges overlap or reverse: $start, $end")
        else assertTrue(start.right <= end.left, "$kind LTR edges overlap or reverse: $start, $end")
        listOf(start, end, action).forEach { bounds ->
            assertTrue(bounds.left >= field.left - 2f && bounds.right <= field.right + 2f,
                "$kind part escapes 240 dp field: $bounds, $field")
        }
        assertTrue(end.right <= action.left || action.right <= end.left,
            "$kind end adornment overlaps native action: $end, $action")
    }

    private fun numericStepGeometry(cupertino: Boolean, rtl: Boolean, decimal: Boolean) = runComposeUiTest {
        val fieldSchema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"${if (decimal) "number" else "integer"}"}}
        }""").jsonObject)
        val ui = Json.parseToJsonElement("""{
          "type":"Control","scope":"#/properties/value",
          "options":{"startAdornment":"S","endAdornment":"E"}
        }""").jsonObject.toUiSchemaElement()
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides
                (if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr)) {
                Box(Modifier.width(240.dp)) {
                    JsonForms(data = Json.parseToJsonElement("""{"value":${if (decimal) "7.5" else "7"}}"""),
                        schema = fieldSchema, uischema = ui,
                        renderers = if (cupertino) cupertinoRenderers else materialRenderers)
                }
            }
        }
        val start = onNodeWithText("S", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val end = onNodeWithText("E", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val decrease = onNodeWithContentDescription("Decrease value", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val increase = onNodeWithContentDescription("Increase value", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val field = onAllNodes(hasSetTextAction()).onFirst().fetchSemanticsNode().boundsInRoot
        val kind = if (decimal) "Number" else "Integer"
        if (rtl) assertTrue(start.left >= end.right, "$kind RTL edges overlap or reverse: $start, $end")
        else assertTrue(start.right <= end.left, "$kind LTR edges overlap or reverse: $start, $end")
        listOf(start, end, decrease, increase).forEach { bounds ->
            assertTrue(bounds.left >= field.left - 2f && bounds.right <= field.right + 2f,
                "$kind part escapes 240 dp field: $bounds, $field")
        }
        listOf(decrease, increase).forEach { action ->
            assertTrue(end.right <= action.left || action.right <= end.left,
                "$kind end adornment overlaps step action: $end, $action")
        }
    }

    private fun extendedEdgeGeometry(cupertino: Boolean, kind: String, rtl: Boolean) = runComposeUiTest {
        val fieldSchema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"string","format":"$kind"}}
        }""").jsonObject)
        val value = when (kind) {
            "duration" -> "PT1H"
            "cron" -> "0 * * * *"
            else -> "#ff0000"
        }
        val action = when (kind) {
            "duration" -> "Choose duration"
            "cron" -> "Choose schedule"
            else -> "Choose color"
        }
        val ui = Json.parseToJsonElement("""{
          "type":"Control","scope":"#/properties/value",
          "options":{"startAdornment":"S","endAdornment":"E"}
        }""").jsonObject.toUiSchemaElement()
        setContent {
            TestTouchInputMode()
            CompositionLocalProvider(LocalLayoutDirection provides
                (if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr)) {
                Box(Modifier.width(240.dp)) {
                    JsonForms(data = Json.parseToJsonElement("""{"value":"$value"}"""),
                        schema = fieldSchema, uischema = ui,
                        renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers)
                }
            }
        }
        val start = onNodeWithText("S", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val end = onNodeWithText("E", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        onAllNodes(hasSetTextAction()).onFirst().performMouseInput { moveTo(center) }
        waitForIdle()
        val clear = onNodeWithContentDescription("Clear value", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val field = onAllNodes(hasSetTextAction()).onFirst().fetchSemanticsNode().boundsInRoot
        val picker = onNodeWithContentDescription(action, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        if (rtl) assertTrue(start.left >= end.right, "$kind RTL edges overlap or reverse: $start, $end")
        else assertTrue(start.right <= end.left, "$kind LTR edges overlap or reverse: $start, $end")
        listOf(start, end, clear, picker).forEach { bounds ->
            assertTrue(bounds.left >= field.left - 2f && bounds.right <= field.right + 2f,
                "$kind part escapes 240 dp field: $bounds, $field")
        }
        assertTrue(end.right <= clear.left || clear.right <= end.left,
            "$kind end adornment overlaps clear: $end, $clear")
        assertTrue(end.right <= picker.left || picker.right <= end.left,
            "$kind end adornment overlaps picker: $end, $picker")
        assertTrue(clear.right <= picker.left || picker.right <= clear.left,
            "$kind clear overlaps picker: $clear, $picker")
    }
}
