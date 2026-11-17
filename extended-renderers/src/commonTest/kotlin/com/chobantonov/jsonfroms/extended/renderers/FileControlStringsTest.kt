package com.chobantonov.jsonfroms.extended.renderers

import com.chobantonov.jsonfroms.core.i18n.translateRendererText
import kotlin.test.Test
import kotlin.test.assertEquals

class FileControlStringsTest {
    @Test fun keyedBundlesCoverActionsWarningsAndPluralSizeErrors() {
        assertEquals("The selected file could not be read",
            translateRendererText(null, "en-US", "file.readFailed", "The selected file could not be read",
                fileControlLocaleBundles))
        assertEquals("Този файл вече е прикачен",
            translateRendererText(null, "bg_BG", "file.duplicate", "This file is already attached",
                fileControlLocaleBundles))
        assertEquals("2 selected files violate size limits",
            translateRendererText(null, "en", "file.sizeInvalid", "2 selected files violate size limits",
                fileCountLocaleBundles(2)))
        assertEquals("3 избрани файла нарушават ограниченията за размер",
            translateRendererText(null, "bg", "file.sizeInvalid", "3 selected files violate size limits",
                fileCountLocaleBundles(3)))
        assertEquals("2 errors", translateRendererText(null, "en", "file.errorCount", "2 errors",
            fileCountLocaleBundles(2)))
        assertEquals("Избраният файл нарушава ограничението за размер",
            translateRendererText(null, "bg", "file.sizeInvalid", "Selected file violates the size limit",
                fileControlLocaleBundles))
        assertEquals("Добави прикачен файл", translateRendererText(null,
            "bg", "file.add", "Add attachment", fileControlLocaleBundles,
            bundleKey = "file.addAttachment"))
        assertEquals("Host override", translateRendererText(
            com.chobantonov.jsonfroms.core.store.Translator { key, _, _ ->
                if (key == "field.file.add") "Host override" else null
            }, "bg", "file.add", "Add attachment", fileControlLocaleBundles,
            i18nKeyPrefix = "field", bundleKey = "file.addAttachment"))
        assertEquals("Clear all", translateRendererText(null, "fr", "file.clearAll", "Clear all",
            fileControlLocaleBundles))
        assertEquals("Unknown host message", translateRendererText(null, "en", "file.unknown",
            "Unknown host message", fileControlLocaleBundles))
    }
}
