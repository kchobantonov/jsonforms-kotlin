package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.models.Rule
import com.chobantonov.jsonfroms.core.models.RuleEffect
import com.chobantonov.jsonfroms.core.models.SpacerElement
import com.chobantonov.jsonfroms.core.models.ValidateFunctionCondition
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.validation.OptimumCodeJsonSchemaValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.buildJsonObject

class EffectiveLayoutChildrenTest {
    @Test fun emptyStructuralDescendantsDropButVisibleGroupAndSpacerRemain() {
        val field = ControlElement("#/properties/name")
        val conditional = field.copy(rule = Rule(RuleEffect.SHOW,
            ValidateFunctionCondition(scope = "#", validate = { context ->
                context.path == "person" && (context.config as? Map<*, *>)?.get("show") == true
            })))
        val empty = VerticalLayout(elements = listOf(HorizontalLayout()))
        val conditionallyEmpty = HorizontalLayout(elements = listOf(VerticalLayout(elements = listOf(conditional))))
        val group = GroupElement(label = "Heading")
        val spacer = SpacerElement()
        val items = listOf(empty, conditionallyEmpty, group, spacer, field)
        val data = buildJsonObject { }
        val validator = OptimumCodeJsonSchemaValidator()

        assertEquals(listOf(group, spacer, field), effectiveLayoutChildren(
            items, data, validator, "person", mapOf("show" to false)))
        assertEquals(listOf(conditionallyEmpty, group, spacer, field), effectiveLayoutChildren(
            items, data, validator, "person", mapOf("show" to true)))
        assertEquals(listOf(group, spacer, field), effectiveLayoutChildren(
            items, data, validator, "other", mapOf("show" to true)))
    }
}
