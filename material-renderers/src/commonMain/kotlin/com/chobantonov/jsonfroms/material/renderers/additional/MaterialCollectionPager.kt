package com.chobantonov.jsonfroms.material.renderers.additional

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.core.mappers.CollectionPageWindow
import com.chobantonov.jsonfroms.core.mappers.CollectionPaginationOptions
import com.chobantonov.jsonfroms.compose.useJsonForms
import com.chobantonov.jsonfroms.core.i18n.translateCollectionErrorPage
import com.chobantonov.jsonfroms.core.i18n.translateCollectionPagerText
import com.chobantonov.jsonfroms.core.store.Translator

@Composable
fun MaterialCollectionPager(
    options: CollectionPaginationOptions,
    window: CollectionPageWindow,
    size: Int,
    total: Int,
    errorPages: Map<Int, Int>,
    translator: Translator?,
    canNavigate: Boolean = true,
    onGoToFirstError: (() -> Unit)? = null,
    onChange: (page: Int, size: Int) -> Unit,
) {
    val showSize = options.pageSizeOptions.distinct().size > 1 &&
        options.pageSizeOptions.any { it < total }
    if (!options.enabled || total == 0 || (window.pages <= 1 && !showSize && onGoToFirstError == null)) return
    val locale = useJsonForms().state.jsonforms.i18n.locale
    fun translated(key: String, fallback: String, values: Map<String, Any?> = emptyMap()) =
        translateCollectionPagerText(translator, locale, key, fallback, values)
    var sizeMenuOpen by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.End,
    ) {
        HorizontalDivider()
        if (showSize) {
            androidx.compose.foundation.layout.Box {
                OutlinedButton(
                    enabled = canNavigate,
                    modifier = Modifier.semantics {
                        contentDescription = translated("collection.pageSize", "Page size")
                    },
                    onClick = { sizeMenuOpen = true },
                ) { Text(size.toString()) }
                DropdownMenu(expanded = sizeMenuOpen, onDismissRequest = { sizeMenuOpen = false }) {
                    options.pageSizeOptions.forEach { choice ->
                        DropdownMenuItem(text = { Text(choice.toString()) }, onClick = {
                            sizeMenuOpen = false
                            onChange(window.page, choice)
                        })
                    }
                }
            }
        }
        if (window.pages > 1) {
            Row(
                Modifier.padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    enabled = canNavigate && window.page > 1,
                    modifier = Modifier.semantics {
                        contentDescription = translated("collection.previous", "Previous page")
                    },
                    onClick = { onChange(window.page - 1, size) },
                ) { Text("‹") }
                Text(translated("collection.page", "${window.page} of ${window.pages}",
                    mapOf("current" to window.page, "pages" to window.pages)))
                OutlinedButton(
                    enabled = canNavigate && window.page < window.pages,
                    modifier = Modifier.semantics {
                        contentDescription = translated("collection.next", "Next page")
                    },
                    onClick = { onChange(window.page + 1, size) },
                ) { Text("›") }
            }
        }
        if (onGoToFirstError != null) OutlinedButton(
            enabled = canNavigate,
            onClick = onGoToFirstError,
        ) { Text(translated("collection.firstError", "Go to first error")) }
        errorPages.toList().sortedBy { it.first }.forEach { (page, count) ->
            OutlinedButton(enabled = canNavigate, onClick = { onChange(page, size) }) {
                Text(translateCollectionErrorPage(translator, locale, page, count))
            }
        }
    }
}
