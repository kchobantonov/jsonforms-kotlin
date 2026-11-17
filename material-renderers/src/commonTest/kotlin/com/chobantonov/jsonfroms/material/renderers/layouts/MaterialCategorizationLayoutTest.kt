package com.chobantonov.jsonfroms.material.renderers.layouts

import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.testers.NOT_APPLICABLE
import com.chobantonov.jsonfroms.core.testers.TesterContext
import kotlinx.serialization.json.JsonPrimitive
import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.Rule
import com.chobantonov.jsonfroms.core.models.RuleEffect
import com.chobantonov.jsonfroms.core.models.SchemaBasedCondition
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import com.chobantonov.jsonfroms.core.validation.OptimumCodeJsonSchemaValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class MaterialCategorizationLayoutTest {
    private val validator = OptimumCodeJsonSchemaValidator()
    private val categorization = CategorizationElement(
        elements = listOf(
            CategoryElement(label = "Basic"),
            CategoryElement(
                label = "Additional",
                rule = Rule(
                    effect = RuleEffect.SHOW,
                    condition = SchemaBasedCondition(
                        scope = "#/properties/vegetarian",
                        schema = buildJsonSchema { put("const", true) },
                    ),
                ),
            ),
        ),
    )

    @Test
    fun accordionRequiresDirectCategoriesAndOutranksTabs() {
        val context = TesterContext()
        val accordion = CategorizationElement(
            options = mapOf("variant" to JsonPrimitive("accordion")),
            elements = listOf(CategoryElement(label = "Unbound")),
        )
        assertEquals(3, materialCategorizationAccordionTester(accordion, null, context))
        assertEquals(1, materialCategorizationTester(accordion, null, context))
        assertEquals(NOT_APPLICABLE, materialCategorizationAccordionTester(
            accordion.copy(elements = listOf(ControlElement(scope = "#/properties/a"))), null, context,
        ))
        assertEquals(NOT_APPLICABLE, materialCategorizationAccordionTester(
            accordion.copy(options = emptyMap()), null, context,
        ))
    }

    @Test
    fun addsAndRemovesCategoriesWhenRuleDataChanges() {
        assertEquals(
            listOf("Basic"),
            visibleCategories(
                categorization,
                buildJsonObject { put("vegetarian", false) },
                validator,
            ).map { it.label },
        )
        assertEquals(
            listOf("Basic", "Additional"),
            visibleCategories(
                categorization,
                buildJsonObject { put("vegetarian", true) },
                validator,
            ).map { it.label },
        )
    }
}
