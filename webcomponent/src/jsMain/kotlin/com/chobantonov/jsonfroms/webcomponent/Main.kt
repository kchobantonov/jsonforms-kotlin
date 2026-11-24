package com.chobantonov.jsonfroms.webcomponent

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.Categorization
import com.chobantonov.jsonfroms.core.models.Layout
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.models.UnknownUiSchemaElement
import com.chobantonov.jsonfroms.core.models.toUiSchemaElement
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import com.chobantonov.jsonfroms.core.validation.decodeHostAdditionalErrors
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.store.JsonFormsUISchemaRegistryEntry
import com.chobantonov.jsonfroms.core.store.Translator
import com.chobantonov.jsonfroms.core.store.ValidationMode
import com.chobantonov.jsonfroms.core.testers.NOT_APPLICABLE
import com.chobantonov.jsonfroms.extended.renderers.materialExtendedRenderers
import com.chobantonov.jsonfroms.extended.renderers.cupertinoExtendedRenderers
import com.chobantonov.jsonfroms.extended.renderers.ExtendedActionEventHandler
import com.chobantonov.jsonfroms.extended.renderers.ExtendedActionEventHandlerConfigKey
import com.chobantonov.jsonfroms.extended.renderers.ExtendedScriptHandler
import com.chobantonov.jsonfroms.extended.renderers.ExtendedScriptHandlerConfigKey
import com.chobantonov.jsonfroms.extended.renderers.ExtendedFilePickerConfigKey
import com.chobantonov.jsonfroms.extended.renderers.browserFilePicker
import com.chobantonov.jsonfroms.material.renderers.materialCells
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoCells
import com.chobantonov.jsonfroms.cupertino.renderers.util.CupertinoAppTheme
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlin.js.JsName
import kotlin.js.JSON
import kotlin.js.Promise
import kotlin.js.jsTypeOf
import kotlinx.coroutines.await
import org.w3c.dom.HTMLElement

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
    val resolvedTheme: dynamic = js("window.jsonFormsKotlinResolvedTheme")
    val shadowRoot: dynamic = element.shadowRoot ?: element.attachShadow(js("({mode: 'open'})"))
    val host: dynamic = js("document.createElement('div')")
    val viewportStyle: dynamic = js("document.createElement('style')")
    viewportStyle.textContent = ":host { display: block; position: relative; min-height: 240px; }"
    host.style.position = "absolute"
    host.style.inset = "0"
    host.style.width = "100%"
    host.style.height = "100%"
    shadowRoot.appendChild(host)
    shadowRoot.appendChild(viewportStyle)
    val resizeObserver: dynamic = js("typeof ResizeObserver === 'undefined' ? null : new ResizeObserver(() => window.dispatchEvent(new Event('resize')))")
    resizeObserver?.observe(host)

    var lastDataAttribute = element.getAttribute("data") as String?
    var lastDataProvidedAttribute = element.getAttribute("data-provided") as String?
    var dataText by mutableStateOf(lastDataAttribute ?: "{}")
    var dataProvided by mutableStateOf(lastDataProvidedAttribute != "false")
    var schemaText by mutableStateOf(element.getAttribute("schema") as String?)
    var uiSchemaText by mutableStateOf(element.getAttribute("uischema") as String?)
    var uiSchemasText by mutableStateOf(element.getAttribute("uischemas") as String?)
    var propertyUiSchemaRegistry by mutableStateOf(element.uiSchemaRegistry as Any?)
    var registryEpoch by mutableIntStateOf(0)
    var configText by mutableStateOf(element.getAttribute("config") as String?)
    var contextText by mutableStateOf(element.getAttribute("context") as String?)
    var translationsText by mutableStateOf(element.getAttribute("translations") as String?)
    var additionalErrorsText by mutableStateOf(element.getAttribute("additional-errors") as String?)
    var localeText by mutableStateOf(supportedLocale(element.getAttribute("lang") as String?))
    var themeText by mutableStateOf(resolvedTheme(element.getAttribute("theme") as String?).toString())
    var rendererFamily by mutableStateOf(if (element.getAttribute("renderer-family") == "cupertino")
        "cupertino" else "material")
    var connected by mutableStateOf(true)
    var readonly by mutableStateOf(element.hasAttribute("readonly") as Boolean)
    var validationModeText by mutableStateOf(element.getAttribute("validation-mode") as String?)
    var direction by mutableStateOf((element.getAttribute("dir") as String?)?.lowercase())
    element._jsonformsRefresh = { changedAttribute: String? ->
        val currentDataAttribute = element.getAttribute("data") as String?
        if (changedAttribute == "data" || element._jsonformsDataChangedWhileDetached == true ||
            currentDataAttribute != lastDataAttribute) {
            lastDataAttribute = currentDataAttribute
            dataText = currentDataAttribute ?: "{}"
            element._jsonformsDataChangedWhileDetached = false
        }
        val currentDataProvidedAttribute = element.getAttribute("data-provided") as String?
        if (changedAttribute == "data-provided" ||
            element._jsonformsDataProvidedChangedWhileDetached == true ||
            currentDataProvidedAttribute != lastDataProvidedAttribute) {
            lastDataProvidedAttribute = currentDataProvidedAttribute
            dataProvided = currentDataProvidedAttribute != "false"
            element._jsonformsDataProvidedChangedWhileDetached = false
        }
        schemaText = element.getAttribute("schema") as String?
        uiSchemaText = element.getAttribute("uischema") as String?
        uiSchemasText = element.getAttribute("uischemas") as String?
        propertyUiSchemaRegistry = element.uiSchemaRegistry as Any?
        configText = element.getAttribute("config") as String?
        contextText = element.getAttribute("context") as String?
        translationsText = element.getAttribute("translations") as String?
        additionalErrorsText = element.getAttribute("additional-errors") as String?
        localeText = supportedLocale(element.getAttribute("lang") as String?)
        themeText = resolvedTheme(element.getAttribute("theme") as String?).toString()
        rendererFamily = if (element.getAttribute("renderer-family") == "cupertino")
            "cupertino" else "material"
        readonly = element.hasAttribute("readonly") as Boolean
        validationModeText = element.getAttribute("validation-mode") as String?
        direction = (element.getAttribute("dir") as String?)?.lowercase()
    }
    element._jsonformsRegistryRefresh = {
        propertyUiSchemaRegistry = element.uiSchemaRegistry as Any?
        registryEpoch++
    }
    element._jsonformsConnectionChange = { isConnected: Boolean ->
        connected = isConnected
        if (isConnected) resizeObserver?.observe(host) else resizeObserver?.disconnect()
    }
    val actionHandler = ExtendedActionEventHandler { event ->
        val dispatchAction: dynamic = js("window.jsonFormsKotlinDispatchAction")
        val metadata = browserActionMetadata(event, uiSchemaText, configText)
        dispatchAction(element, event.action, JSON.parse<dynamic>(event.params.toString()), metadata)
            .unsafeCast<Promise<dynamic>>().await()
    }
    val scriptHandler = ExtendedScriptHandler { script, event ->
        val executeScript: dynamic = js("window.jsonFormsKotlinExecuteScript")
        executeScript(element, script, browserActionMetadata(event, uiSchemaText, configText))
            .unsafeCast<Promise<dynamic>>().await()
    }
    val diagnosticReporter = UiDiagnosticReporter { diagnostic ->
        val detail: dynamic = js("({})")
        detail.code = diagnostic.code
        detail.severity = diagnostic.severity.name
        detail.message = diagnostic.message
        detail.path = diagnostic.details["path"]?.toString()
        val options: dynamic = js("({})")
        options.detail = detail
        options.bubbles = true
        element.dispatchEvent(BrowserCustomEvent("jsonforms-diagnostic", options))
    }
    val filePicker = browserFilePicker()

    ComposeViewport(viewportContainer = host.unsafeCast<HTMLElement>()) {
        if (!connected) return@ComposeViewport
        val data = parseJson(dataText)
        val schema = schemaText?.let(::parseJson) as? JsonObject
        val uiSchema = (uiSchemaText?.let(::parseJson) as? JsonObject)
            ?.let { runCatching { it.toUiSchemaElement() }.getOrNull() }
        val namedUiSchemas = parseNamedUiSchemas(uiSchemasText)
        val functionUiSchemas = remember(propertyUiSchemaRegistry, registryEpoch) {
            parseFunctionUiSchemas(propertyUiSchemaRegistry, element)
        }
        val authoredConfig = configText?.let(::parseJson) as? JsonObject
        val authoredContext = contextText?.let(::parseJson)
        val catalogs = parseTranslationCatalogs(translationsText)
        val additionalErrors = decodeHostAdditionalErrors(additionalErrorsText)
        val language = localeText.substringBefore('-').lowercase()
        val translations = catalogs?.get(localeText) ?: catalogs?.get(language).orEmpty()
        val i18n = JsonFormsI18nState(locale = localeText, translate = Translator { key, fallback, values ->
            (translations[key] ?: fallback)?.let { message ->
                CatalogParameter.replace(message) { match ->
                    when (val value = values[match.groupValues[1]]) {
                        is String, is Number, is Boolean -> value.toString()
                        else -> match.value
                    }
                }
            }
        })
        CompositionLocalProvider(LocalLayoutDirection provides if (direction == "rtl")
            LayoutDirection.Rtl else LayoutDirection.Ltr) {
            val formContent: @Composable () -> Unit = {
                if (data == null || (schemaText != null && schema == null) ||
                    (uiSchemaText != null && uiSchema == null) ||
                    (uiSchemasText != null && namedUiSchemas == null) ||
                    functionUiSchemas == null ||
                    (configText != null && authoredConfig == null) ||
                    (contextText != null && authoredContext !is JsonObject) ||
                    (translationsText != null && catalogs == null) || additionalErrors == null
                ) {
                    Text("Invalid JSON Forms input")
                } else {
                    JsonForms(
                        data = data,
                        dataProvided = dataProvided,
                        context = authoredContext,
                        schema = schema?.let(::JsonSchema),
                        uischema = uiSchema,
                        uischemas = namedUiSchemas.orEmpty() + functionUiSchemas,
                        readonly = readonly,
                        validationMode = when (validationModeText) {
                            "validate-and-hide" -> ValidationMode.ValidateAndHide
                            "no-validation" -> ValidationMode.NoValidation
                            else -> ValidationMode.ValidateAndShow
                        },
                        i18n = i18n,
                        additionalErrors = additionalErrors,
                        config = authoredConfig?.toConfigMap().orEmpty() + mapOf(
                            ExtendedActionEventHandlerConfigKey to actionHandler,
                            ExtendedScriptHandlerConfigKey to scriptHandler,
                            ExtendedFilePickerConfigKey to filePicker,
                            UiDiagnosticReporterConfigKey to diagnosticReporter,
                        ),
                        renderers = if (rendererFamily == "cupertino") cupertinoExtendedRenderers
                            else materialExtendedRenderers,
                        cells = if (rendererFamily == "cupertino") cupertinoCells else materialCells,
                        onChange = { change ->
                            val value = change.data.toString()
                            if (value != dataText || change.dataProvided != dataProvided) {
                                dataText = value
                                dataProvided = change.dataProvided
                                val detail: dynamic = js("({})")
                                detail.data = JSON.parse<dynamic>(value)
                                detail.dataProvided = change.dataProvided
                                val options: dynamic = js("({})")
                                options.detail = detail
                                options.bubbles = true
                                element.dispatchEvent(BrowserCustomEvent("jsonforms-change", options))
                            }
                        },
                    )
                }
            }
            if (rendererFamily == "cupertino") {
                CupertinoAppTheme(darkTheme = themeText == "dark", content = formContent)
            } else {
                MaterialTheme(colorScheme = if (themeText == "dark") darkColorScheme() else lightColorScheme(),
                    content = formContent)
            }
        }
    }
}

private fun parseJson(value: String): JsonElement? =
    try { Json.parseToJsonElement(value) } catch (_: Exception) { null }

private fun supportedLocale(value: String?): String =
    if (value?.trim()?.replace('_', '-')?.substringBefore('-')?.lowercase() == "bg") "bg" else "en"

private val CatalogParameter = Regex("\\{([A-Za-z][A-Za-z0-9_]*)\\}")

private fun parseNamedUiSchemas(text: String?): List<JsonFormsUISchemaRegistryEntry>? {
    if (text == null) return emptyList()
    val entries = parseJson(text) as? JsonArray ?: return null
    return runCatching { entries.map { entry ->
        val ui = ((entry as? JsonObject)?.get("uischema") as? JsonObject)
            ?.toUiSchemaElement() ?: error("uischemas entries require a UI schema")
        JsonFormsUISchemaRegistryEntry({ _, _, _ -> NOT_APPLICABLE }, ui)
    } }.getOrNull()
}

private fun parseFunctionUiSchemas(value: Any?, host: dynamic): List<JsonFormsUISchemaRegistryEntry>? {
    if (value == null) return emptyList()
    val entries: dynamic = value.asDynamic()
    val isArray: dynamic = js("Array.isArray")
    if (!(isArray(entries) as Boolean)) return null
    return runCatching { (0 until (entries.length as Int)).map { index ->
        val entry: dynamic = entries[index]
        val tester: dynamic = entry.tester
        require(jsTypeOf(tester) == "function") { "uiSchemaRegistry entries require tester functions" }
        val uiText = JSON.stringify(entry.uischema)
        val ui = (parseJson(uiText) as? JsonObject)?.toUiSchemaElement()
            ?: error("uiSchemaRegistry entries require a UI schema")
        JsonFormsUISchemaRegistryEntry({ schema, schemaPath, path ->
            runCatching {
                val rawSchema: dynamic = schema?.value?.let { JSON.parse<dynamic>(it.toString()) }
                val rank: dynamic = tester(rawSchema, schemaPath, path)
                if (jsTypeOf(rank) == "number" && (rank as Double).isFinite())
                    (rank as Double).toInt() else NOT_APPLICABLE
            }.getOrElse { failure ->
                val detail: dynamic = js("({})")
                detail.code = "uischema.testerFailed"
                detail.severity = "ERROR"
                detail.message = failure.toString()
                detail.path = path
                val options: dynamic = js("({})")
                options.detail = detail
                options.bubbles = true
                host.dispatchEvent(BrowserCustomEvent("jsonforms-diagnostic", options))
                NOT_APPLICABLE
            }
        }, ui)
    } }.getOrNull()
}

private fun parseTranslationCatalogs(text: String?): Map<String, Map<String, String>>? {
    if (text == null) return emptyMap()
    val catalogs = parseJson(text) as? JsonObject ?: return null
    return runCatching { catalogs.mapValues { (_, entries) ->
        (entries as? JsonObject)?.mapValues { (_, value) ->
            (value as? JsonPrimitive)?.takeIf { it.isString }?.content
                ?: error("translations must contain string values")
        } ?: error("translation catalogs must be objects")
    } }.getOrNull()
}

private fun JsonObject.toConfigMap(): Map<String, Any?> = mapValues { (_, value) -> value.toConfigValue() }

private fun JsonElement.toConfigValue(): Any? = when (this) {
    is JsonObject -> toConfigMap()
    is JsonArray -> map { it.toConfigValue() }
    JsonNull -> null
    is JsonPrimitive -> if (isString) content else booleanOrNull ?: longOrNull ?: doubleOrNull ?: content
}

private fun browserActionMetadata(event: com.chobantonov.jsonfroms.extended.renderers.ExtendedActionEvent,
    uiSchemaText: String?, configText: String?): dynamic {
    val metadata: dynamic = js("({})")
    metadata.label = event.label
    metadata.path = event.path
    metadata.params = JSON.parse<dynamic>(event.params.toString())
    val fallbackElement = JsonObject(event.element.properties + mapOf(
        "type" to JsonPrimitive(event.element.type),
        "options" to JsonObject(event.element.options),
    ))
    val rawRoot = uiSchemaText?.let(::parseJson) as? JsonObject
    val parsedRoot = event.context.uischema
    val authoredElement = if (rawRoot != null && parsedRoot != null)
        findAuthoredElement(parsedRoot, rawRoot, event.element) else null
    metadata.element = JSON.parse<dynamic>((authoredElement ?: fallbackElement).toString())
    val context: dynamic = (event.context.hostContext as? JsonObject)
        ?.let { JSON.parse<dynamic>(it.toString()) } ?: js("({})")
    context.data = JSON.parse<dynamic>(event.context.data.toString())
    context.schema = JSON.parse<dynamic>(event.context.schema.value.toString())
    context.uischema = uiSchemaText?.let { JSON.parse<dynamic>(it) }
    context.readonly = event.context.readonly
    context.locale = event.context.locale
    context.errors = JSON.parse<dynamic>(actionErrorsJson(event.context.errors).toString())
    context.additionalErrors = JSON.parse<dynamic>(actionErrorsJson(event.context.additionalErrors).toString())
    configText?.let { context.config = JSON.parse<dynamic>(it) }
    metadata.context = context
    return metadata
}

private fun actionErrorsJson(errors: List<ErrorObject>): JsonArray = JsonArray(errors.map { error ->
    buildJsonObject {
        put("instancePath", error.instancePath)
        error.dataPath?.let { put("dataPath", it) }
        error.schemaPath?.let { put("schemaPath", it) }
        error.keyword?.let { put("keyword", it) }
        put("message", error.message)
        put("params", JsonObject(error.params.mapValues { JsonPrimitive(it.value) }))
        error.absoluteKeywordLocation?.let { put("absoluteKeywordLocation", it) }
    }
})

private fun findAuthoredElement(
    parsed: UiSchemaElement,
    authored: JsonObject,
    target: UnknownUiSchemaElement,
): JsonObject? {
    if (parsed === target) return authored
    val children = when (parsed) {
        is Layout -> parsed.elements
        is Categorization -> parsed.elements
        is UnknownUiSchemaElement -> parsed.elements
        else -> emptyList()
    }
    val rawChildren = authored["elements"] as? JsonArray ?: return null
    for (index in children.indices) {
        val rawChild = rawChildren.getOrNull(index) as? JsonObject ?: continue
        findAuthoredElement(children[index], rawChild, target)?.let { return it }
    }
    return null
}
