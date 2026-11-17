package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.models.ListWithDetailElement
import com.chobantonov.jsonfroms.core.store.ValidationMode
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonPrimitive

class ContainerErrorIndexTest {
    private val group = GroupElement(elements = listOf(HorizontalLayout(elements = listOf(
        ControlElement(scope = "#/properties/employees"),
        ControlElement(scope = "#/properties/employees"),
        ListWithDetailElement(scope = "#/properties/contacts"),
    ))))

    @Test fun detailErrorsExcludeBoundFieldsAndObjectLevelFeedback() {
        val own = ErrorObject(instancePath = "/person", message = "own")
        val bound = ErrorObject(instancePath = "/person/name", message = "bound")
        val omitted = ErrorObject(instancePath = "/person/secret", message = "omitted")
        val sibling = ErrorObject(instancePath = "/people/secret", message = "sibling")
        val index = ContainerErrorIndex.build(listOf(own, bound, omitted, sibling),
            emptyList(), ValidationMode.ValidateAndShow)

        assertEquals(listOf(omitted), index.unrepresentedDetailErrors(
            listOf(listOf("name")), "person"))
        assertEquals(listOf(bound, omitted), index.unrepresentedDetailErrors(emptyList(), "person"))
        assertEquals(emptyList(), index.unrepresentedDetailErrors(listOf(emptyList()), "person"))
    }

    @Test fun indexedPresenceUsesCompleteSegmentsAndIncludesBoundCollection() {
        val scopes = groupBoundScopes(group)
        assertEquals(2, scopes.controlScopes.size)
        assertEquals(2, scopes.validationScopes.size)
        val index = ContainerErrorIndex.build(
            listOf(ErrorObject(instancePath = "/employees/10/name", message = "bad")),
            listOf(ErrorObject(instancePath = "/contacts/0/email", message = "bad")),
            ValidationMode.ValidateAndShow,
        )
        assertTrue(index.containsAny(scopes.validationScopes))
        assertEquals(2, index.countAny(scopes.validationScopes))
        assertFalse(index.containsAny(listOf(emptyList()), "employees.1"))
        assertTrue(index.containsAny(listOf(emptyList()), "employees.10"))
    }

    @Test fun nestedItemContextSeparatesRowsAndRootScopeUsesCurrentItem() {
        val index = ContainerErrorIndex.build(
            listOf(
                ErrorObject(instancePath = "/rows/1/name", message = "one"),
                ErrorObject(instancePath = "/rows/10/name", message = "ten"),
                ErrorObject(instancePath = "/rows/1/other", message = "other"),
            ),
            emptyList(), ValidationMode.ValidateAndShow,
        )
        val nameScopes = groupBoundScopes(GroupElement(elements = listOf(
            ControlElement(scope = "#/properties/name"),
        ))).validationScopes
        assertEquals(1, index.countAny(nameScopes, "rows.1"))
        assertEquals(1, index.countAny(nameScopes, "rows.10"))
        assertFalse(index.containsAny(nameScopes, "rows.2"))
        val currentItemScopes = groupBoundScopes(GroupElement(elements = listOf(
            ListWithDetailElement(scope = "#"),
        ))).validationScopes
        assertEquals(listOf(emptyList()), currentItemScopes)
        assertEquals(2, index.countAny(currentItemScopes, "rows.1"))
        assertEquals(1, index.countAny(currentItemScopes, "rows.10"))
    }

    @Test fun overlappingScopesCountEachErrorOnceFromIndexedPrefixes() {
        val index = ContainerErrorIndex.build(
            listOf(
                ErrorObject(instancePath = "/section", message = "own"),
                ErrorObject(instancePath = "/section/deep/name", message = "nested"),
                ErrorObject(instancePath = "/section/deep/other", message = "other"),
                ErrorObject(instancePath = "/sections/deep/name", message = "sibling"),
            ),
            emptyList(), ValidationMode.ValidateAndShow,
        )
        assertEquals(3, index.countAny(listOf(
            listOf("section", "deep"), listOf("section"), listOf("section"))))
        assertEquals(2, index.countAny(listOf(listOf("deep"), listOf("deep", "name")), "section"))
        assertEquals(mapOf("deep" to 2), index.countsByImmediateChild("section"))
        assertEquals(mapOf("name" to 1, "other" to 1), index.countsByImmediateChild("section.deep"))
        assertEquals(1, index.countAny(listOf(listOf("sections"))))
    }

    @Test fun requiredRelocationAndLiteralPointerSegments() {
        val index = ContainerErrorIndex.build(
            listOf(ErrorObject(instancePath = "/contact", keyword = "required",
                message = "missing", params = mapOf("missingProperty" to "a.b"))),
            emptyList(), ValidationMode.ValidateAndShow,
        )
        assertTrue(index.containsAny(listOf(listOf("contact", "a.b"))))
        assertFalse(index.containsAny(listOf(listOf("contact", "a", "b"))))
    }

    @Test fun missingDependencyCountsAtTriggerOnlyWhenTargetIsUnrepresented() {
        val dependency = ErrorObject(instancePath = "/rows/1", schemaPath = "/dependencies/purchaseOrder",
            keyword = "dependencies", message = "missing billingAddress",
            params = mapOf("missingProperty" to "billingAddress"))
        val schemaDependency = ErrorObject(instancePath = "/rows/1",
            schemaPath = "/dependentSchemas/purchaseOrder/required",
            keyword = "required", message = "missing shippingAddress",
            params = mapOf("missingProperty" to "shippingAddress"))
        val ordinary = ErrorObject(instancePath = "/rows/1", schemaPath = "/required",
            keyword = "required", message = "missing other",
            params = mapOf("missingProperty" to "other"))
        val index = ContainerErrorIndex.build(listOf(dependency, schemaDependency, ordinary),
            emptyList(), ValidationMode.ValidateAndShow)
        val trigger = listOf(listOf("purchaseOrder"))
        assertEquals(2, index.unrepresentedDependencyCount(trigger, trigger, "rows.1"))
        assertEquals(0, index.unrepresentedDependencyCount(trigger, trigger, "rows.10"))
        assertEquals(0, index.unrepresentedDependencyCount(listOf(listOf("other")),
            listOf(listOf("other")), "rows.1"))
        assertEquals(1, index.unrepresentedDependencyCount(trigger,
            trigger + listOf(listOf("billingAddress")), "rows.1"))
        assertEquals(0, index.unrepresentedDependencyCount(trigger,
            trigger + listOf(listOf("billingAddress"), listOf("shippingAddress")), "rows.1"))
        val hidden = ContainerErrorIndex.build(listOf(dependency), emptyList(),
            ValidationMode.ValidateAndHide)
        assertEquals(0, hidden.unrepresentedDependencyCount(trigger, trigger, "rows.1"))
    }

    @Test fun indexedErrorLookupPreservesOrderAndCompleteSegments() {
        val required = ErrorObject(instancePath = "/rows/1", keyword = "required",
            message = "missing", params = mapOf("missingProperty" to "a.b"))
        val host = ErrorObject(instancePath = "/rows/1/a~1b", message = "host")
        val tenth = ErrorObject(instancePath = "/rows/10/name", message = "tenth")
        val index = ContainerErrorIndex.build(
            listOf(required, tenth), listOf(host), ValidationMode.ValidateAndShow,
        )
        assertEquals(listOf(required, host), index.errorsAtOrBelow("rows.1"))
        assertEquals(listOf(required), index.errorsAtOrBelow("/rows/1/a.b"))
        assertEquals(listOf(host), index.errorsAtOrBelow("/rows/1/a~1b"))
        assertEquals(listOf(tenth), index.errorsAtOrBelow("rows.10"))
        assertEquals(emptyList(), index.errorsAtOrBelow("rows.2"))
        val hidden = ContainerErrorIndex.build(
            listOf(required, tenth), listOf(host), ValidationMode.ValidateAndHide,
        )
        assertEquals(listOf(host), hidden.errorsAtOrBelow("rows.1"))
    }

    @Test fun cacheSharesOneIndexUntilAnErrorListReferenceOrModeChanges() {
        val error = ErrorObject(instancePath = "/first/name", message = "bad")
        val schema = mutableListOf(error)
        val additional = mutableListOf<ErrorObject>()
        val cache = ContainerErrorIndexCache()
        val first = cache.get(schema, additional, ValidationMode.ValidateAndShow)
        assertTrue(first.containsAny(listOf(listOf("first"))))
        assertSame(first, cache.get(schema, additional, ValidationMode.ValidateAndShow))

        val replacedSchema = mutableListOf(error)
        val replacement = cache.get(replacedSchema, additional, ValidationMode.ValidateAndShow)
        assertNotSame(first, replacement)
        assertEquals(1, replacement.countAny(listOf(listOf("first"))))
        val hostReplacement = cache.get(replacedSchema, mutableListOf(), ValidationMode.ValidateAndShow)
        assertNotSame(replacement, hostReplacement)
        val hidden = cache.get(replacedSchema, additional, ValidationMode.ValidateAndHide)
        assertNotSame(hostReplacement, hidden)
        assertFalse(hidden.containsAny(listOf(listOf("first"))))
    }

    @Test fun displayPolicyHidesSchemaErrorsButKeepsHostErrors() {
        val schema = listOf(ErrorObject(instancePath = "/schemaOnly", message = "schema"))
        val host = listOf(ErrorObject(instancePath = "/hostOnly", message = "host"))
        val hidden = ContainerErrorIndex.build(schema, host, ValidationMode.ValidateAndHide)
        assertFalse(hidden.containsAny(listOf(listOf("schemaOnly"))))
        assertTrue(hidden.containsAny(listOf(listOf("hostOnly"))))
        assertEquals(1, hidden.countAny(listOf(emptyList())))
        val none = ContainerErrorIndex.build(schema, host, ValidationMode.NoValidation)
        assertEquals(1, none.countAny(listOf(emptyList())))
    }

    @Test fun immediateChildCountsDecodeNamesAndExcludeDeclaredOrOtherScopes() {
        val index = ContainerErrorIndex.build(
            listOf(
                ErrorObject(instancePath = "/profile/a~1b/nested", message = "one"),
                ErrorObject(instancePath = "/profile/a~1b", message = "two"),
                ErrorObject(instancePath = "/profile/declared", message = "declared"),
                ErrorObject(instancePath = "/profiles/a~1b", message = "other"),
            ),
            listOf(ErrorObject(instancePath = "/profile/a.b", message = "host")),
            ValidationMode.ValidateAndShow,
        )
        assertEquals(mapOf("a/b" to 2, "declared" to 1, "a.b" to 1),
            index.countsByImmediateChild("profile"))
        val hidden = ContainerErrorIndex.build(
            listOf(ErrorObject(instancePath = "/profile/schema", message = "schema")),
            listOf(ErrorObject(instancePath = "/profile/host", message = "host")),
            ValidationMode.ValidateAndHide,
        )
        assertEquals(mapOf("host" to 1), hidden.countsByImmediateChild("profile"))
    }

    @Test fun arrayItemErrorsGroupOnceByCompleteSegmentsAndKeepOwnErrorSeparate() {
        val own = ErrorObject(instancePath = "/rows", keyword = "minItems", message = "own")
        val first = ErrorObject(instancePath = "/rows/1/name", message = "one")
        val tenth = ErrorObject(instancePath = "/rows/10/name", message = "ten")
        val escaped = ErrorObject(instancePath = "/rows/1/a~1b", message = "slash")
        val required = ErrorObject(
            instancePath = "/rows/1", keyword = "required", message = "missing",
            params = mapOf("missingProperty" to "a.b"),
        )
        val unrelated = ErrorObject(instancePath = "/other/1/name", message = "other")

        val grouped = groupArrayErrors(
            listOf(own, first, tenth, escaped, required, unrelated), "rows",
        )

        assertEquals(listOf(first, tenth, escaped, required), grouped.descendants)
        assertEquals(listOf(first, escaped, required), grouped.byIndex[1])
        assertEquals(listOf(tenth), grouped.byIndex[10])
        assertEquals(null, grouped.byIndex[0])
    }

    @Test fun groupOptionsRequireExplicitCountAndRespectLocalFalse() {
        val global = mapOf("jsonformsExtended" to mapOf(
            "showValidationIndicator" to true, "showValidationIndicatorCount" to true,
        ))
        val enabled = resolveGroupExpansionOptions(emptyMap(), global)
        assertTrue(enabled.showValidationIndicator)
        assertTrue(enabled.showValidationIndicatorCount)
        val local = resolveGroupExpansionOptions(mapOf(
            "showValidationIndicator" to JsonPrimitive(false),
            "showValidationIndicatorCount" to JsonPrimitive(false),
        ), global)
        assertFalse(local.showValidationIndicator)
        assertFalse(local.showValidationIndicatorCount)
        val default = resolveGroupExpansionOptions(emptyMap(), emptyMap())
        assertFalse(default.showValidationIndicator)
        assertFalse(default.showValidationIndicatorCount)
    }
}
