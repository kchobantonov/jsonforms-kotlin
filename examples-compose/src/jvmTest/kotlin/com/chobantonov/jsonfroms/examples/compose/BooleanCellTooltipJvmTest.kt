package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class BooleanCellTooltipJvmTest {
    private val schema = JsonSchema(Json.parseToJsonElement("""{
      "type":"array","items":{"type":"object","properties":{
        "active":{"type":"boolean","title":"Active"}
      }}
    }""").jsonObject)

    @Test fun materialCellTooltipShowsTranslatedError() = scenario(false)
    @Test fun cupertinoCellTooltipShowsTranslatedError() = scenario(true)

    private fun scenario(cupertino: Boolean) = runComposeUiTest {
        setContent {
            JsonForms(data = Json.parseToJsonElement("""[{"active":"false"}]"""), schema = schema,
                uischema = ControlElement("#", options = mapOf(
                    "table" to JsonPrimitive(true),
                    "columnDefs" to Json.parseToJsonElement("""[{"field":"active"}]"""),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = "bg-BG", translateError = { _, _, _ ->
                    "Невалидна булева стойност"
                }))
        }
        val translatedMessage = "Невалидна булева стойност"
        assertEquals(1, onAllNodes(hasText(translatedMessage)).fetchSemanticsNodes().size)
        onNodeWithContentDescription("Active съдържа грешки: Невалидна булева стойност")
            .performMouseInput { moveTo(center) }
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        assertEquals(2, onAllNodes(hasText(translatedMessage)).fetchSemanticsNodes().size)
    }
}
