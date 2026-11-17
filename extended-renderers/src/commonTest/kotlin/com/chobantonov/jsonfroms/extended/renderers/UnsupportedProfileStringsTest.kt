package com.chobantonov.jsonfroms.extended.renderers

import com.chobantonov.jsonfroms.core.i18n.translateRendererText
import com.chobantonov.jsonfroms.core.store.Translator
import kotlin.test.Test
import kotlin.test.assertEquals

class UnsupportedProfileStringsTest {
    @Test fun unavailableCodeEditorUsesRegionalBundleAndFormOverride() {
        val key = "editor.monacoUnavailable"
        val fallback = "Monaco editor is unavailable on this host"
        assertEquals("Monaco editor is unavailable on this host",
            translateRendererText(null, "en-US", key, fallback, codeProfileLocaleBundles))
        assertEquals("Редакторът Monaco не е наличен на този хост",
            translateRendererText(null, "bg_BG", key, fallback, codeProfileLocaleBundles))
        assertEquals("Own notice", translateRendererText(Translator { id, default, _ ->
            if (id == key) "Own notice" else default
        }, "en", key, fallback, codeProfileLocaleBundles))
    }

    @Test fun templateReasonsKeepLanguageAndUseStableKeys() {
        val bundles = templateLayoutLocaleBundles("jsx")
        assertEquals("Unsupported: jsx",
            translateRendererText(null, "en", "template.unsupportedLanguage", "Unsupported: jsx", bundles))
        assertEquals("Шаблонният двигател не е наличен на този хост: jsx",
            translateRendererText(null, "bg", "template.engineUnavailable", "Unavailable: jsx", bundles))
        assertEquals("Disabled",
            translateRendererText(null, "en-GB", "template.evaluationDisabled", "Disabled", bundles))
    }
}
