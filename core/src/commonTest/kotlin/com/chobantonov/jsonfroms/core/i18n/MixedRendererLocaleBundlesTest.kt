package com.chobantonov.jsonfroms.core.i18n

import com.chobantonov.jsonfroms.core.models.JsonSchemaType
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import kotlin.test.Test
import kotlin.test.assertEquals

class MixedRendererLocaleBundlesTest {
    @Test fun everyMixedChromeAndTypeNameHasRegionalDefaults() {
        val keys = listOf("selectType", "openTooltip", "openAriaLabel", "searchTree", "clearSearch",
            "expand", "collapse", "showPrimitives", "hidePrimitives", "root", "type",
            "rename", "remove", "confirm", "cancel")
        for (locale in listOf("bg", "bg-BG")) {
            val i18n = JsonFormsI18nState(locale = locale)
            for (key in keys) assertEquals(false,
                translateMixedRendererText(i18n, null, key, "English default") == "English default",
                "$locale: $key")
            for (type in JsonSchemaType.entries) assertEquals(
                lookupRendererCatalogString(mixedRendererLocaleBundles.getValue(locale.substringBefore('-')),
                    "mixed.type.${type.name.lowercase()}"),
                translateMixedTypeName(i18n, type), "$locale: $type")
        }
        assertEquals("Search tree…", translateMixedRendererText(
            JsonFormsI18nState(locale = "fr-FR"), null, "searchTree", "Search tree…"))
    }

    @Test fun formTranslatorOverridesLocaleDefaultWithExistingScopedKey() {
        var receivedDefault: String? = null
        var receivedControl: Any? = null
        val i18n = JsonFormsI18nState(locale = "bg-BG", translate = { key, fallback, values ->
            assertEquals("field.selectType", key)
            receivedDefault = fallback
            receivedControl = values["control"]
            "Host type prompt"
        })
        assertEquals("Host type prompt", translateMixedRendererText(i18n, "field", "selectType",
            "Select a type", mapOf("control" to "Value")))
        assertEquals("Избери тип", receivedDefault)
        assertEquals("Value", receivedControl)
    }
}
