package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.Rule
import com.chobantonov.jsonfroms.core.models.RuleEffect
import com.chobantonov.jsonfroms.core.models.ValidateFunctionCondition
import com.chobantonov.jsonfroms.core.validation.OptimumCodeJsonSchemaValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class CategorySelectionTest {
    private val first = CategoryElement(label = "First", name = "first")
    private val second = CategoryElement(label = "Second", name = "second")
    private val third = CategoryElement(label = "Third", name = "third")

    @Test fun categoryFunctionRuleReceivesPathAndConfig() {
        val conditional = first.copy(rule = Rule(
            RuleEffect.SHOW,
            ValidateFunctionCondition(scope = "#", validate = { context ->
                context.path == "profile" && (context.config as? Map<*, *>)?.get("show") == true
            }),
        ))
        val root = CategorizationElement(elements = listOf(conditional, second))
        val data = buildJsonObject { }
        val validator = OptimumCodeJsonSchemaValidator()
        assertEquals(listOf(conditional, second), visibleCategories(root, data, validator, mapOf("show" to true), "profile"))
        assertEquals(listOf(second), visibleCategories(root, data, validator, mapOf("show" to false), "profile"))
        assertEquals(listOf(second), visibleCategories(root, data, validator, mapOf("show" to true), "other"))
    }

    @Test fun verticalRequiresBooleanTrue() {
        val root = CategorizationElement(elements = listOf(first, second))
        assertEquals(false, isVerticalCategorization(root))
        assertEquals(true, isVerticalCategorization(
            root.copy(options = mapOf("vertical" to JsonPrimitive(true))),
        ))
        assertEquals(false, isVerticalCategorization(
            root.copy(options = mapOf("vertical" to JsonPrimitive("true"))),
        ))
        assertEquals(false, isVerticalCategorization(
            root.copy(options = mapOf("orientation" to JsonPrimitive("vertical"))),
        ))
    }

    @Test fun namedSelectionSurvivesReorderWhileUnnamedFollowsPosition() {
        val named = CategorizationElement(elements = listOf(first, second))
        val reordered = named.copy(elements = listOf(second, first))
        assertEquals(
            categorySelectionKey(named, second),
            categorySelectionKey(reordered, second),
        )
        val unnamedFirst = CategoryElement(label = "One")
        val unnamedSecond = CategoryElement(label = "Two")
        val unnamed = CategorizationElement(elements = listOf(unnamedFirst, unnamedSecond))
        val unnamedReordered = unnamed.copy(elements = listOf(unnamedSecond, unnamedFirst))
        assertEquals(
            categorySelectionKey(unnamed, unnamedSecond),
            categorySelectionKey(unnamedReordered, unnamedFirst),
        )
    }

    @Test fun duplicateNamesStayDistinguishable() {
        val left = CategoryElement(label = "Left", name = "duplicate")
        val right = CategoryElement(label = "Right", name = "duplicate")
        val root = CategorizationElement(elements = listOf(left, right))
        kotlin.test.assertNotEquals(
            categorySelectionKey(root, left),
            categorySelectionKey(root, right),
        )
        assertEquals(setOf("duplicate"), duplicateCategoryNames(root))
        val referenced = root.copy(options = mapOf("initial" to JsonPrimitive("duplicate")))
        val result = resolveInitialCategorySelection(referenced, listOf(left, right))
        assertEquals(0, result.originalIndex)
        assertEquals("categorization.duplicateName", result.diagnostic?.code)
        assertEquals("duplicate", result.diagnostic?.details?.get("name"))
    }

    @Test fun namedInitialSelectsDirectVisibleCategory() {
        val root = CategorizationElement(
            options = mapOf("initial" to JsonPrimitive("second")),
            elements = listOf(first, second, third),
        )
        val result = resolveInitialCategorySelection(root, listOf(first, second, third))
        assertEquals(1, result.originalIndex)
        assertNull(result.diagnostic)
    }

    @Test fun missingOrRuleHiddenInitialFallsBackAndDiagnoses() {
        for (name in listOf("missing", "second")) {
            val root = CategorizationElement(
                options = mapOf("initial" to JsonPrimitive(name)),
                elements = listOf(first, second),
            )
            val result = resolveInitialCategorySelection(root, listOf(first))
            assertEquals(0, result.originalIndex)
            assertEquals("categorization.initialNotFound", result.diagnostic?.code)
            assertEquals(name, result.diagnostic?.details?.get("initial"))
        }
    }

    @Test fun authoredIdentitySurvivesVisibilityFiltering() {
        val root = CategorizationElement(elements = listOf(first, second, third))
        assertEquals(1, resolveVisibleCategoryIndex(root, listOf(first, third), 2))
        assertEquals(0, resolveVisibleCategoryIndex(root, listOf(third), 2))
        assertEquals(0, resolveVisibleCategoryIndex(root, listOf(first, third), 1))
        assertEquals(-1, resolveVisibleCategoryIndex(root, emptyList(), 2))
    }
}
