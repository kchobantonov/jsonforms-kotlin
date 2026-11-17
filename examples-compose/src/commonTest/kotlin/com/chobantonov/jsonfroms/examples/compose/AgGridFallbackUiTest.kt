package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class AgGridFallbackUiTest {
    @Test fun materialReportsAndShowsPortableTable() = scenario(false)
    @Test fun cupertinoReportsAndShowsPortableTable() = scenario(true)
    @Test fun materialNoCatalogFallbackFollowsLocale() = localizedFallback(false)
    @Test fun cupertinoNoCatalogFallbackFollowsLocale() = localizedFallback(true)

    @Test fun nativePaginationOverridesPortableValuesInFallback() = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":{"type":"string"}}"""
        ).jsonObject)
        var native by mutableStateOf<JsonElement>(Json.parseToJsonElement(
            """{"pagination":false}"""
        ))
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""["a","b","c","d","e","f","g"]"""),
                schema = schema,
                uischema = ControlElement("#", options = mapOf(
                    "variant" to JsonPrimitive("ag-grid"),
                    "pagination" to Json.parseToJsonElement("""{"pageSize":3}"""),
                    "agGridOptions" to native,
                )),
                renderers = materialRenderers,
            )
        }
        assertEquals(7, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onNodeWithContentDescription("Next page").assertDoesNotExist()
        runOnIdle { native = Json.parseToJsonElement("""{
            "pagination":true,"paginationPageSize":2,"paginationPageSizeSelector":false
        }""") }
        assertEquals(2, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onNodeWithContentDescription("Next page").assertExists()
        onNodeWithContentDescription("Page size").assertDoesNotExist()
    }

    private fun scenario(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"}}}}}}"""
        ).jsonObject)
        val data = Json.parseToJsonElement("""{"rows":[{"name":"Ada"}]}""")
        val diagnostics = mutableListOf<UiDiagnostic>()
        var unrelated by mutableStateOf(false)
        var changes = 0
        setContent {
            JsonForms(
                data = data,
                schema = schema,
                uischema = ControlElement(
                    scope = "#/properties/rows",
                    options = mapOf("variant" to JsonPrimitive("ag-grid")),
                ),
                config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter { diagnostics += it },
                    "unrelated" to unrelated),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++ },
            )
        }
        onNodeWithText("AG Grid is unavailable here; showing an array table.").assertExists()
        onNodeWithText("Ada").assertExists()
        waitForIdle()
        assertEquals(listOf("variant.unsupported"), diagnostics.map { it.code })
        assertEquals("rows", diagnostics.single().details["path"])
        runOnIdle { unrelated = true }
        waitForIdle()
        assertEquals(1, diagnostics.size)
        assertEquals(0, changes)
    }

    private fun localizedFallback(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":{"type":"string"}}"""
        ).jsonObject)
        var locale by mutableStateOf("en-US")
        val diagnostics = mutableListOf<UiDiagnostic>()
        var changes = 0
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""["Ada"]"""),
                schema = schema,
                uischema = ControlElement("#", options = mapOf("variant" to JsonPrimitive("ag-grid"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter { diagnostics += it }),
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { changes++ },
            )
        }
        onNodeWithText("AG Grid is unavailable here; showing an array table.").assertExists()
        onNodeWithText("Ada").assertExists()
        runOnIdle { locale = "bg-BG" }
        onNodeWithText("AG Grid не е наличен тук; показва се таблица за масив.").assertExists()
        onNodeWithText("Ada").assertExists()
        waitForIdle()
        assertEquals(listOf("variant.unsupported"), diagnostics.map { it.code })
        assertEquals(0, changes)
    }
}
