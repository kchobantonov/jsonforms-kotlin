package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsCellProps
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.DispatchCell
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class UnknownCellLocaleUiTest {
    @Test fun materialExplainsUnavailableCell() = unavailableCell(false)

    @Test fun cupertinoExplainsUnavailableCell() = unavailableCell(true)

    private fun unavailableCell(cupertino: Boolean) = runComposeUiTest {
        var locale by mutableStateOf("en")
        var changes = 0
        val diagnostics = mutableListOf<UiDiagnostic>()
        val cellProbe = JsonFormsRendererRegistryEntry(
            tester = { ui, _, _ -> if (ui is ControlElement) 1000 else -1 },
            renderer = ComposeJsonFormsRenderer { props ->
                DispatchCell(ComposeJsonFormsCellProps(
                    state = props.state,
                    dispatch = props.dispatch,
                    uischema = props.uischema as ControlElement,
                    schema = props.schema,
                    rootSchema = props.rootSchema,
                    path = props.path,
                    enabled = props.enabled,
                    renderers = props.renderers,
                    cells = emptyList(),
                    config = props.config,
                ))
            },
        )
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"name":"Ada"}"""),
                schema = JsonSchema(Json.parseToJsonElement(
                    """{"type":"object","properties":{"name":{"type":"string"}}}"""
                ).jsonObject),
                uischema = ControlElement(scope = "#/properties/name"),
                renderers = (if (cupertino) cupertinoRenderers else materialRenderers) + cellProbe,
                config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter { diagnostics += it }),
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { changes++ },
            )
        }
        onNodeWithText("No applicable cell found.").assertExists()
        waitForIdle()
        assertEquals(listOf("cell.unavailable"), diagnostics.map { it.code })
        assertEquals("Control", diagnostics.single().details["elementType"])
        assertEquals(0, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        runOnIdle { locale = "bg-BG" }
        onNodeWithText("Няма приложим редактор за клетка.").assertExists()
        assertEquals(1, diagnostics.size)
        assertEquals(0, changes)
    }
}
