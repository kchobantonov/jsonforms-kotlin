package com.chobantonov.jsonfroms.material.renderers

import com.chobantonov.jsonfroms.core.store.Translator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class MaterialTooltipTranslationsTest {
    @Test
    fun translatesTooltipAndAccessibilityLabelWithControlPrefix() {
        val values = mapOf<String, Any?>("label" to "Comments")
        val receivedValues = mutableListOf<Map<String, Any?>>()
        val translator = Translator { id, defaultMessage, passedValues ->
            receivedValues += passedValues
            when (id) {
                "comments.addTooltip" -> "Ajouter"
                "comments.addAriaLabel" -> "Ajouter un élément"
                else -> defaultMessage
            }
        }

        val result = translatedActionText(
            translator = translator,
            i18nKeyPrefix = "comments",
            tooltipKey = "addTooltip",
            ariaLabelKey = "addAriaLabel",
            tooltipFallback = "Add item",
            values = values,
        )

        assertEquals("Ajouter", result.tooltip)
        assertEquals("Ajouter un élément", result.ariaLabel)
        receivedValues.forEach { assertSame(values, it) }
    }

    @Test
    fun fallsBackWhenTranslationIsMissing() {
        val translator = Translator { _, _, _ -> null }

        val result = translatedActionText(
            translator = translator,
            i18nKeyPrefix = "comments",
            tooltipKey = "removeTooltip",
            ariaLabelKey = "removeAriaLabel",
            tooltipFallback = "Delete",
            ariaLabelFallback = "Delete item",
        )

        assertEquals("Delete", result.tooltip)
        assertEquals("Delete item", result.ariaLabel)
    }

    @Test
    fun usesGlobalKeyWhenThereIsNoControlPrefix() {
        var receivedId: String? = null
        val translator = Translator { id, defaultMessage, _ ->
            receivedId = id
            defaultMessage
        }

        assertEquals("Next", translatedText(translator, null, "Next", "Next"))
        assertEquals("Next", receivedId)
    }
}
