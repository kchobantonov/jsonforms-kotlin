package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SingleCombinatorBranchUiTest {
    @Test fun materialOneOfShowsOnlyBranchWithoutSelector() = scenario(false, "oneOf")
    @Test fun cupertinoOneOfShowsOnlyBranchWithoutSelector() = scenario(true, "oneOf")
    @Test fun materialAnyOfShowsOnlyBranchWithoutTab() = scenario(false, "anyOf")
    @Test fun cupertinoAnyOfShowsOnlyBranchWithoutTab() = scenario(true, "anyOf")
    @Test fun materialOneOfShowsOnlyBranchWhenValueIsMissing() = scenario(false, "oneOf", true)
    @Test fun cupertinoOneOfShowsOnlyBranchWhenValueIsMissing() = scenario(true, "oneOf", true)

    private fun scenario(cupertino: Boolean, keyword: String, missing: Boolean = false) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"choice":{"type":"object",
                "description":"Help for choice","$keyword":[
                {"title":"Only branch","type":"object","properties":{
                    "name":{"type":"string","title":"Name"}},"required":["name"]}
            ]}}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement(
            if (missing) "{}" else """{"choice":{}}"""))
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/choice"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; writes++ })
        }
        onNodeWithText("Only branch").assertExists()
        onNodeWithText("Help for choice").assertExists()
        assertEquals(0, onAllNodes(hasClickAction() and hasText("Only branch"))
            .fetchSemanticsNodes().size)
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        runOnIdle { assertEquals(0, writes) }
        onAllNodes(hasSetTextAction())[0].performTextReplacement("Ada")
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{"choice":{"name":"Ada"}}"""), data)
            assertEquals(1, writes)
        }
    }
}
