package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@OptIn(ExperimentalTestApi::class)
class SpecCategorizationFixtureUiTest {
    @Test fun materialImportedThreePresentationsShowDataStatus() = scenario(false)
    @Test fun cupertinoImportedThreePresentationsShowDataStatus() = scenario(true)

    private fun scenario(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-categorization" }
        val outer = (example.uischema as VerticalLayout).elements
            .filterIsInstance<CategorizationElement>().single()
        val inner = outer.elements.map { category ->
            (category as CategoryElement).elements.filterIsInstance<CategorizationElement>().single()
        }
        assertEquals(3, inner.size)
        assertEquals(null, inner[0].options["variant"])
        assertEquals(JsonPrimitive("stepper"), inner[1].options["variant"])
        assertEquals(JsonPrimitive("accordion"), inner[2].options["variant"])
        var presentation by mutableStateOf(0)
        var locale by mutableStateOf("en")
        var data by mutableStateOf(example.data)
        var changes = 0
        setContent {
            val translations = example.localizedTranslations[locale].orEmpty()
            JsonForms(data = data, schema = example.schema!!, uischema = inner[presentation],
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = example.config,
                i18n = JsonFormsI18nState(
                    locale = locale,
                    translate = { key, fallback, _ -> translations[key] ?: fallback },
                ),
                onChange = { changes++ })
        }
        for (index in inner.indices) {
            runOnIdle { presentation = index }
            onNodeWithContentDescription("Notes, Section contains data").assertExists()
            onNodeWithText("Business").assertDoesNotExist()
            if (index == 2) {
                onNodeWithText("Manchester").assertExists()
                onNodeWithText("Planning").performTouchInput { click() }
            } else {
                onNodeWithText("Manchester").assertDoesNotExist()
                onNodeWithText("Planning").performTouchInput { click() }
                onNodeWithText("Manchester").assertExists()
                onNodeWithText("Contact").performTouchInput { click() }
            }
            onNodeWithText("Manchester").assertDoesNotExist()
        }
        runOnIdle { locale = "bg" }
        onNodeWithContentDescription("Бележки, Секцията съдържа данни").assertExists()
        runOnIdle {
            data = JsonObject((data as JsonObject).toMutableMap().apply {
                put("isBusiness", Json.parseToJsonElement("true"))
            })
        }
        onNodeWithText("Фирма").assertExists()
        assertEquals(0, changes)
    }
}
