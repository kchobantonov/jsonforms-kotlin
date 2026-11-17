package com.chobantonov.jsonfroms.cupertino.renderers.additional

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.useJsonForms
import com.chobantonov.jsonfroms.core.i18n.defaultErrorTranslator
import com.chobantonov.jsonfroms.core.i18n.defaultTranslator
import com.chobantonov.jsonfroms.core.i18n.getI18nKey
import com.chobantonov.jsonfroms.core.i18n.translateErrorForPath
import com.chobantonov.jsonfroms.core.i18n.translateRendererText
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.JsonSchemaOrSchemas
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.store.Translator
import com.chobantonov.jsonfroms.core.util.JsonPointer
import com.chobantonov.jsonfroms.core.util.Resolve
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import com.chobantonov.jsonfroms.cupertino.renderers.ActionTooltip
import com.chobantonov.jsonfroms.cupertino.renderers.util.LocalCupertinoColors
import com.slapps.cupertino.CupertinoAlertDialog
import com.slapps.cupertino.CupertinoButton
import com.slapps.cupertino.CupertinoButtonDefaults
import com.slapps.cupertino.CupertinoText
import com.slapps.cupertino.ExperimentalCupertinoApi
import com.slapps.cupertino.cancel
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@OptIn(ExperimentalCupertinoApi::class)
@Composable
internal fun CupertinoListErrorIndicator(errors: List<ErrorObject>, count: Int?, label: String) {
    val form = useJsonForms().state.jsonforms
    val i18n = form.i18n
    val root = form.core.schema
    var open by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val translatedErrors = errors.map { error ->
        translateErrorForPath(error, i18n.translateError ?: defaultErrorTranslator,
            i18n.translate ?: defaultTranslator, summaryErrorSchema(error, root),
            path = summaryErrorSegments(error).joinToString("."))
    }
    val message = translatedErrors.joinToString("\n")
    if (open) {
        val visible = if (expanded) errors.indices else errors.indices.take(3)
        CupertinoAlertDialog(
            onDismissRequest = { open = false },
            title = { CupertinoText(label) },
            message = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    visible.forEach { index ->
                        val error = errors[index]
                        val row = error.instancePath.split('/').filter(String::isNotEmpty)
                            .map(JsonPointer::decode).firstNotNullOfOrNull(String::toIntOrNull)
                        val field = summaryFieldLabel(error, root, i18n.translate ?: defaultTranslator)
                        val rowLabel = row?.let {
                            summaryText(i18n, "validation.row", "Row ${it + 1}", "Ред ${it + 1}",
                                mapOf("index" to it + 1))
                        }
                        val context = listOfNotNull(rowLabel, field).joinToString(" · ")
                            .ifEmpty { error.instancePath.ifEmpty { label } }
                        CupertinoText("$context: ${translatedErrors[index]}",
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                    }
                    if (errors.size > 3) {
                        val fallback = if (expanded) "Show less" else "Show ${errors.size - 3} more"
                        val bg = if (expanded) "Покажи по-малко" else "Покажи още ${errors.size - 3}"
                        CupertinoButton(onClick = { expanded = !expanded },
                            colors = CupertinoButtonDefaults.plainButtonColors()) {
                            CupertinoText(summaryText(i18n,
                                if (expanded) "validation.showLess" else "validation.showMore",
                                fallback, bg, mapOf("count" to errors.size - 3)))
                        }
                    }
                }
            },
            buttons = {
                cancel(onClick = { open = false }) {
                    CupertinoText(summaryText(i18n, "validation.close", "Close", "Затвори"))
                }
            },
        )
    }
    ActionTooltip(message) {
        Box(Modifier.size(22.dp).background(LocalCupertinoColors.current.destructive, CircleShape)
            .clearAndSetSemantics {
                contentDescription = label
                stateDescription = message
            }.clickable { expanded = false; open = true }, contentAlignment = Alignment.Center) {
            CupertinoText(count?.toString() ?: "●", color = Color.White)
        }
    }
}

private fun summaryFieldLabel(error: ErrorObject, root: JsonSchema, translator: Translator): String? {
    val segments = summaryErrorSegments(error)
    val field = segments.lastOrNull()?.takeUnless { it.toIntOrNull() != null } ?: return null
    val schema = summaryErrorSchema(error, root)
    val fallback = schema?.title?.takeIf(String::isNotBlank) ?: field
    val key = getI18nKey(schema, null, segments.joinToString("."), "label")
    return translator.translate(key, fallback, emptyMap())?.takeIf(String::isNotBlank) ?: fallback
}

private fun summaryErrorSegments(error: ErrorObject): List<String> =
    error.instancePath.split('/').filter(String::isNotEmpty).map(JsonPointer::decode) +
        listOfNotNull(error.params["missingProperty"]?.takeIf(String::isNotBlank))

private fun summaryErrorSchema(error: ErrorObject, root: JsonSchema): JsonSchema? {
    val segments = summaryErrorSegments(error)
    val field = segments.lastOrNull()
    val schemaPointer = error.schemaPath?.substringBeforeLast('/')?.let { parent ->
        if (error.keyword == "required" && error.params["missingProperty"] != null && field != null)
            "$parent/properties/${JsonPointer.encode(field)}"
        else parent
    }
    return schemaPointer?.let { Resolve.schema(root, it, root) }
        ?: schemaForDataSegments(root, segments)
}

private fun schemaForDataSegments(root: JsonSchema, segments: List<String>): JsonSchema? {
    var pointer = "#"
    var schema = root
    for (segment in segments) {
        pointer += if (segment.toIntOrNull() != null) {
            val index = segment.toInt()
            when {
                schema.prefixItems?.getOrNull(index) != null -> "/prefixItems/$index"
                schema.items is JsonSchemaOrSchemas.Multiple -> "/items/$index"
                else -> "/items"
            }
        } else "/properties/${JsonPointer.encode(segment)}"
        schema = Resolve.schema(root, pointer, root) ?: return null
    }
    return schema
}

private fun summaryText(i18n: JsonFormsI18nState, key: String, en: String, bg: String,
    values: Map<String, Any?> = emptyMap()): String = translateRendererText(
    i18n.translate, i18n.locale, key, en,
    mapOf("bg" to JsonObject(mapOf(key to JsonPrimitive(bg)))), values,
)
