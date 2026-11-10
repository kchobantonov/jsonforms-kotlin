package com.chobantonov.jsonfroms.webcomponent

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.toUiSchemaElement
import com.chobantonov.jsonfroms.extended.renderers.materialExtendedRenderers
import com.chobantonov.jsonfroms.extended.renderers.ExtendedActionHandler
import com.chobantonov.jsonfroms.extended.renderers.ExtendedActionHandlerConfigKey
import com.chobantonov.jsonfroms.extended.renderers.ExtendedFilePickerConfigKey
import com.chobantonov.jsonfroms.extended.renderers.browserFilePicker
import com.chobantonov.jsonfroms.material.renderers.materialCells
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlin.js.JsName
import kotlin.js.JSON

private var nextViewportId = 0

@JsName("CustomEvent")
private external class BrowserCustomEvent(type: String, options: dynamic)

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val factory: dynamic = js("window.jsonFormsKotlinElementFactory")
    val registry: dynamic = js("customElements")
    if (registry.get("jsonforms-kotlin-compose") == null) {
        registry.define("jsonforms-kotlin-compose", factory { element: dynamic -> mountForm(element) })
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun mountForm(element: dynamic) {
    val id = "jsonforms-kotlin-viewport-${nextViewportId++}"
    val host: dynamic = js("document.createElement('div')")
    host.id = id
    host.style.width = "100%"
    host.style.height = "100%"
    host.style.minHeight = "240px"
    element.appendChild(host)

    var dataText by mutableStateOf(element.getAttribute("data") as String? ?: "{}")
    var schemaText by mutableStateOf(element.getAttribute("schema") as String?)
    var uiSchemaText by mutableStateOf(element.getAttribute("uischema") as String?)
    var readonly by mutableStateOf(element.hasAttribute("readonly") as Boolean)
    element._jsonformsRefresh = {
        dataText = element.getAttribute("data") as String? ?: "{}"
        schemaText = element.getAttribute("schema") as String?
        uiSchemaText = element.getAttribute("uischema") as String?
        readonly = element.hasAttribute("readonly") as Boolean
    }
    val actionHandler = ExtendedActionHandler { action, params ->
        val detail: dynamic = js("({})")
        detail.action = action
        detail.params = JSON.parse<dynamic>(params.toString())
        val options: dynamic = js("({})")
        options.detail = detail
        options.bubbles = true
        element.dispatchEvent(BrowserCustomEvent("jsonforms-action", options))
    }
    val filePicker = browserFilePicker()

    ComposeViewport(viewportContainerId = id) {
        val data = parseJson(dataText)
        val schema = schemaText?.let(::parseJson) as? JsonObject
        val uiSchema = (uiSchemaText?.let(::parseJson) as? JsonObject)
            ?.let { runCatching { it.toUiSchemaElement() }.getOrNull() }
        MaterialTheme {
            if (data == null || (schemaText != null && schema == null) ||
                (uiSchemaText != null && uiSchema == null)
            ) {
                Text("Invalid JSON Forms input")
            } else {
                JsonForms(
                    data = data,
                    schema = schema?.let(::JsonSchema),
                    uischema = uiSchema,
                    readonly = readonly,
                    config = mapOf(ExtendedActionHandlerConfigKey to actionHandler, ExtendedFilePickerConfigKey to filePicker),
                    renderers = materialExtendedRenderers,
                    cells = materialCells,
                    onChange = { change ->
                        val value = change.data.toString()
                        if (value != dataText) {
                            dataText = value
                            val detail: dynamic = js("({})")
                            detail.data = JSON.parse<dynamic>(value)
                            val options: dynamic = js("({})")
                            options.detail = detail
                            options.bubbles = true
                            element.dispatchEvent(BrowserCustomEvent("jsonforms-change", options))
                        }
                    },
                )
            }
        }
    }
}

private fun parseJson(value: String): JsonElement? =
    try { Json.parseToJsonElement(value) } catch (_: Exception) { null }
