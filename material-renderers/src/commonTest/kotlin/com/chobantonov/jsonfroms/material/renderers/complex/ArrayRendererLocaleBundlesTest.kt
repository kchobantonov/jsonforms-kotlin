package com.chobantonov.jsonfroms.material.renderers.complex

import com.chobantonov.jsonfroms.core.i18n.translateRendererText
import com.chobantonov.jsonfroms.core.store.Translator
import kotlin.test.Test
import kotlin.test.assertEquals

class ArrayRendererLocaleBundlesTest {
    @Test fun regionalNoticeUsesBundleAndFormOverride() {
        val key = "array.agGridFallback"
        val default = "AG Grid is unavailable here; showing an array table."
        assertEquals("AG Grid is unavailable here; showing an array table.",
            translateRendererText(null, "en-US", key, default, arrayRendererLocaleBundles))
        assertEquals("AG Grid не е наличен тук; показва се таблица за масив.",
            translateRendererText(null, "bg_BG", key, default, arrayRendererLocaleBundles))
        assertEquals("Form notice", translateRendererText(Translator { id, fallback, _ ->
            if (id == key) "Form notice" else fallback
        }, "en", key, default, arrayRendererLocaleBundles))
    }

    @Test fun invalidValueLabelUsesOnlyEnglishAndBulgarian() {
        val key = "array.invalidCurrentValue"
        val fallback = "Invalid current value"
        assertEquals(fallback, translateRendererText(null, "en-US", key, fallback, arrayRendererLocaleBundles))
        assertEquals("Невалидна текуща стойност",
            translateRendererText(null, "bg-BG", key, fallback, arrayRendererLocaleBundles))
        assertEquals(fallback, translateRendererText(null, "de-DE", key, fallback, arrayRendererLocaleBundles))
    }

    @Test fun compositeDialogActionsHaveBulgarianDefaultsAndFormOverrides() {
        assertEquals("Приложи", translateRendererText(null, "bg-BG", "composite.apply",
            "Apply", arrayRendererLocaleBundles))
        assertEquals("Изчисти", translateRendererText(null, "bg", "composite.empty",
            "Clear", arrayRendererLocaleBundles))
        assertEquals("Премахни", translateRendererText(null, "bg", "composite.remove",
            "Remove", arrayRendererLocaleBundles))
        assertEquals("Apply", translateRendererText(null, "en-US", "composite.apply",
            "Apply", arrayRendererLocaleBundles))
        assertEquals("Form action", translateRendererText(Translator { id, fallback, _ ->
            if (id == "composite.apply") "Form action" else fallback
        }, "bg", "composite.apply", "Apply", arrayRendererLocaleBundles))
    }

    @Test fun compositeDialogTooltipsUseEnglishAndBulgarianOnly() {
        assertEquals("Приложи промените и затвори.", translateRendererText(null, "bg-BG",
            "composite.applyTooltip", "Apply changes and close.", arrayRendererLocaleBundles))
        assertEquals("Отхвърли промените и затвори.", translateRendererText(null, "bg",
            "composite.cancelTooltip", "Discard changes and close.", arrayRendererLocaleBundles))
        assertEquals("Apply changes and close.", translateRendererText(null, "en-US",
            "composite.applyTooltip", "Apply changes and close.", arrayRendererLocaleBundles))
    }
}
