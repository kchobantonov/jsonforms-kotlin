package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoCells
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialCells
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class EnumArrayTableSelectionUiTest {
    @Test fun materialExplicitTableUsesChoiceCells() = explicitTable(false)
    @Test fun cupertinoExplicitTableUsesChoiceCells() = explicitTable(true)

    private fun explicitTable(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"array","uniqueItems":true,
            "items":{"type":"string","enum":["Email","SMS"]}
        }""").jsonObject)
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""["Email"]"""),
                schema = schema,
                uischema = ControlElement("#", options = mapOf("table" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                cells = if (cupertino) cupertinoCells else materialCells,
            )
        }
        onNodeWithTag(if (cupertino) "cupertino-enum-cell" else "material-enum-cell",
            useUnmergedTree = true).assertExists()
        onNodeWithText("Email").assertExists()
    }
}
