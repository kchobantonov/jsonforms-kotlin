package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class CupertinoEnumArrayNativeUiTest {
    @Test fun oneOfCheckboxesFollowHostReplacementAndLocale() = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"teams":{"type":"array","uniqueItems":true,
            "items":{"oneOf":[{"const":"ops","title":"Operations"},
                               {"const":"fin","title":"Finance"}]}}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"teams":["ops"]}"""))
        var locale by mutableStateOf("en")
        var changes = 0
        val checkbox = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/teams"),
                renderers = cupertinoRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { data = it.data; changes++ })
        }
        assertEquals(2, onAllNodes(checkbox).fetchSemanticsNodes().size)
        onNodeWithText("Operations").assertExists()
        onNodeWithText("Finance").assertExists()
        runOnIdle {
            assertEquals(0, changes)
            locale = "bg-BG"
            data = Json.parseToJsonElement("""{"teams":["fin"]}""")
        }
        assertEquals(0, changes)
        onAllNodes(checkbox)[0].performClick()
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{"teams":["fin","ops"]}"""), data)
            assertEquals(1, changes)
        }
    }
}
