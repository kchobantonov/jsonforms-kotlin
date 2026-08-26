package com.chobantonov.jsonfroms.examples

import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.reducers.Middleware
import com.chobantonov.jsonfroms.core.store.JsonFormsCellRendererRegistryEntry
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry
import com.chobantonov.jsonfroms.core.store.JsonFormsUISchemaRegistryEntry
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import kotlinx.serialization.json.JsonElement

data class ExampleAction(
    val label: String,
    val apply: (StateProps) -> StateProps,
)

data class ExampleDescription(
    val name: String,
    val label: String,
    val data: JsonElement,
    val schema: JsonSchema? = null,
    val uischema: UiSchemaElement? = null,
    val uischemas: List<JsonFormsUISchemaRegistryEntry> = emptyList(),
    val config: Map<String, Any?> = emptyMap(),
    val actions: List<ExampleAction> = emptyList(),
    val additionalErrors: List<ErrorObject> = emptyList(),
    val middleware: Middleware? = null,
    val i18n: JsonFormsI18nState? = null,
    val readonly: Boolean = false,
    val translations: Map<String, String> = emptyMap(),
    val localizedTranslations: Map<String, Map<String, String>> = emptyMap(),
    val dataProvided: Boolean = true,
)

data class StateProps(
    val data: JsonElement,
    val schema: JsonSchema? = null,
    val uischema: UiSchemaElement? = null,
    val renderers: List<JsonFormsRendererRegistryEntry<*>> = emptyList(),
    val cells: List<JsonFormsCellRendererRegistryEntry<*>> = emptyList(),
    val config: Map<String, Any?> = emptyMap(),
    val uischemas: List<JsonFormsUISchemaRegistryEntry> = emptyList(),
    val additionalErrors: List<ErrorObject> = emptyList(),
    val actions: List<ExampleAction> = emptyList(),
    val middleware: Middleware? = null,
    val i18n: JsonFormsI18nState? = null,
    val readonly: Boolean = false,
    val translations: Map<String, String> = emptyMap(),
    val localizedTranslations: Map<String, Map<String, String>> = emptyMap(),
    val dataProvided: Boolean = true,
)
