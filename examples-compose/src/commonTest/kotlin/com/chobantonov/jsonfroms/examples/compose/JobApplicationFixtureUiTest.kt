package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import com.chobantonov.jsonfroms.extended.renderers.cupertinoExtendedCells
import com.chobantonov.jsonfroms.extended.renderers.cupertinoExtendedRenderers
import com.chobantonov.jsonfroms.extended.renderers.materialExtendedCells
import com.chobantonov.jsonfroms.extended.renderers.materialExtendedRenderers
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class JobApplicationFixtureUiTest {
    @Test fun materialNamedTemplatesAndLocale() = namedTemplatesAndLocale(false)
    @Test fun cupertinoNamedTemplatesAndLocale() = namedTemplatesAndLocale(true)

    private fun namedTemplatesAndLocale(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-job-application" }
        assertEquals(listOf("applicant", "applicantAddress", "jobDetails"),
            example.uischemas.map { it.uischema.name })
        var data by mutableStateOf(example.data)
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            val translations = example.localizedTranslations.getValue(locale)
            JsonForms(data = data, schema = example.schema, uischema = example.uischema,
                uischemas = example.uischemas, config = example.config,
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                cells = if (cupertino) cupertinoExtendedCells else materialExtendedCells,
                i18n = JsonFormsI18nState(locale = locale,
                    translate = { key, fallback, _ -> translations[key] ?: fallback }),
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Personal Information").assertExists()
        onAllNodesWithText("Address Information").onFirst().assertExists()
        onAllNodes(hasSetTextAction() and hasText("John")).onFirst().performTextReplacement("Grace")
        runOnIdle {
            assertEquals("Grace", data.jsonObject.getValue("applicant").jsonObject
                .getValue("firstName").jsonPrimitive.content)
            assertEquals(1, changes)
            locale = "bg"
        }
        onNodeWithText("Лична информация").assertExists()
        onAllNodesWithText("Информация за позицията").onFirst().performClick()
        onAllNodes(hasSetTextAction() and hasText("Software Developer")).onFirst().assertExists()
        runOnIdle { assertEquals(1, changes) }
    }
}
