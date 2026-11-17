package com.chobantonov.jsonfroms.cupertino.renderers.additional

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.chobantonov.jsonfroms.cupertino.renderers.util.LocalCupertinoColors
import com.slapps.cupertino.CupertinoButton
import com.slapps.cupertino.CupertinoButtonDefaults
import com.slapps.cupertino.CupertinoDropdownMenu
import com.slapps.cupertino.CupertinoText
import com.slapps.cupertino.ExperimentalCupertinoApi
import com.slapps.cupertino.MenuPickerAction

@OptIn(ExperimentalCupertinoApi::class)
@Composable
internal fun CupertinoCollectionPager(
    options: CollectionPaginationOptions,
    window: CollectionPageWindow,
    size: Int,
    total: Int,
    errorPages: Map<Int, Int>,
    translator: Translator?,
    onChange: (page: Int, size: Int) -> Unit,
) {
    val showSize = options.pageSizeOptions.distinct().size > 1 &&
        options.pageSizeOptions.any { it < total }
    if (!options.enabled || total == 0 || (window.pages <= 1 && !showSize)) return
    val locale = useJsonForms().state.jsonforms.i18n.locale
    fun translated(key: String, fallback: String, values: Map<String, Any?> = emptyMap()) =
        translateCollectionPagerText(translator, locale, key, fallback, values)
    var sizeMenuOpen by remember { mutableStateOf(false) }
    val colors = LocalCupertinoColors.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.End) {
        Spacer(Modifier.fillMaxWidth().height(1.dp).background(colors.separator))
        if (showSize) Box {
            CupertinoButton(
                onClick = { sizeMenuOpen = true },
                colors = CupertinoButtonDefaults.plainButtonColors(),
                modifier = Modifier.semantics {
                    contentDescription = translated("collection.pageSize", "Page size")
                },
            ) { CupertinoText(size.toString()) }
            CupertinoDropdownMenu(expanded = sizeMenuOpen,
                onDismissRequest = { sizeMenuOpen = false }) {
                options.pageSizeOptions.forEach { choice ->
                    MenuPickerAction(isSelected = choice == size, onClick = {
                        sizeMenuOpen = false
                        onChange(window.page, choice)
                    }, title = { CupertinoText(choice.toString()) })
                }
            }
        }
        if (window.pages > 1) Row(Modifier.padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            CupertinoButton(
                enabled = window.page > 1,
                onClick = { onChange(window.page - 1, size) },
                colors = CupertinoButtonDefaults.plainButtonColors(),
                modifier = Modifier.semantics {
                    contentDescription = translated("collection.previous", "Previous page")
                },
            ) { CupertinoText("‹") }
            CupertinoText(translated("collection.page", "${window.page} of ${window.pages}",
                mapOf("current" to window.page, "pages" to window.pages)))
            CupertinoButton(
                enabled = window.page < window.pages,
                onClick = { onChange(window.page + 1, size) },
                colors = CupertinoButtonDefaults.plainButtonColors(),
                modifier = Modifier.semantics {
                    contentDescription = translated("collection.next", "Next page")
                },
            ) { CupertinoText("›") }
        }
        errorPages.toList().sortedBy { it.first }.forEach { (page, count) ->
            CupertinoButton(onClick = { onChange(page, size) },
                colors = CupertinoButtonDefaults.plainButtonColors()) {
                CupertinoText(translateCollectionErrorPage(translator, locale, page, count))
            }
        }
    }
}
