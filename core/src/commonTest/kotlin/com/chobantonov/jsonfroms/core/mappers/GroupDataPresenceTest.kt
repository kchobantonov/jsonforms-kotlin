package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.models.Rule
import com.chobantonov.jsonfroms.core.models.RuleEffect
import com.chobantonov.jsonfroms.core.models.SchemaBasedCondition
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.put

class GroupDataPresenceTest {
    private fun data(json: String) = Json.parseToJsonElement(json)

    @Test fun falseAndZeroCountInHiddenNestedControls() {
        val hidden = ControlElement(
            scope = "#/properties/flag",
            rule = Rule(
                effect = RuleEffect.SHOW,
                condition = SchemaBasedCondition(
                    scope = "#/properties/show",
                    schema = buildJsonSchema { put("const", true) },
                ),
            ),
        )
        val group = GroupElement(elements = listOf(
            HorizontalLayout(elements = listOf(hidden, ControlElement(scope = "#/properties/count"))),
        ))
        assertTrue(groupContainsData(group, data("""{"flag":false,"count":null,"show":false}""")))
        assertTrue(groupContainsData(group, data("""{"flag":null,"count":0,"show":false}""")))
    }

    @Test fun emptyValuesAndUnrelatedDataDoNotCount() {
        val group = GroupElement(elements = listOf(
            ControlElement(scope = "#/properties/text"),
            ControlElement(scope = "#/properties/emptyArray"),
            ControlElement(scope = "#/properties/emptyObject"),
        ))
        assertFalse(groupContainsData(group, data(
            """{"text":"  \n ","emptyArray":[null,"",{"x":[]}],"emptyObject":{"x":null},"unrelated":"present"}"""
        )))
        assertFalse(groupContainsData(group, data("""{"unrelated":"present"}""")))
        assertTrue(groupContainsData(group, data(
            """{"text":"","emptyArray":[null,{"x":[1]}],"emptyObject":{}}"""
        )))
    }

    @Test fun currentArrayItemContextAndLiteralScopeSegments() {
        val group = GroupElement(elements = listOf(ControlElement(scope = "#/properties/a.b")))
        val root = data("""{"rows":[{"a.b":null},{"a.b":"value"}],"a.b":"root"}""")
        assertFalse(groupContainsData(group, root, "rows.0"))
        assertTrue(groupContainsData(group, root, "rows.1"))
        assertTrue(groupContainsData(group, root))
    }

    @Test fun rootControlUsesOnlyCurrentContext() {
        val group = GroupElement(elements = listOf(ControlElement(scope = "#")))
        val root = data("""{"rows":[{},{"nested":{"flag":false}}],"unrelated":"root"}""")
        assertFalse(groupContainsData(group, root, "rows.0"))
        assertTrue(groupContainsData(group, root, "rows.1"))
    }

    @Test fun directLayoutScopesLeaveNestedContainersToTheirOwnFeedback() {
        val scopes = directLayoutBoundScopes(listOf(
            ControlElement(scope = "#/properties/purchaseOrder"),
            GroupElement(elements = listOf(ControlElement(scope = "#/properties/nested"))),
            HorizontalLayout(elements = listOf(ControlElement(scope = "#/properties/other"))),
        ))
        assertEquals(listOf(listOf("purchaseOrder")), scopes.controlScopes)
        assertEquals(listOf(listOf("purchaseOrder"), listOf("nested"), listOf("other")),
            scopes.validationScopes)
    }
}
