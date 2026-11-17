package com.chobantonov.jsonfroms.material.renderers

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import com.chobantonov.jsonfroms.core.i18n.translateScopedText
import com.chobantonov.jsonfroms.core.store.Translator

data class ActionText(
    val tooltip: String,
    val ariaLabel: String,
)

fun translatedText(
    translator: Translator?,
    i18nKeyPrefix: String?,
    key: String,
    fallback: String,
    values: Map<String, Any?> = emptyMap(),
): String = translateScopedText(translator, i18nKeyPrefix, key, fallback, values)

fun translatedActionText(
    translator: Translator?,
    i18nKeyPrefix: String?,
    tooltipKey: String,
    ariaLabelKey: String,
    tooltipFallback: String,
    ariaLabelFallback: String = tooltipFallback,
    values: Map<String, Any?> = emptyMap(),
): ActionText = ActionText(
    tooltip = translatedText(translator, i18nKeyPrefix, tooltipKey, tooltipFallback, values),
    ariaLabel = translatedText(translator, i18nKeyPrefix, ariaLabelKey, ariaLabelFallback, values),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ActionTooltip(
    label: String,
    content: @Composable () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
        content = content,
    )
}
