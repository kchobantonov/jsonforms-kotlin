package com.chobantonov.jsonfroms.core.util

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.GroupElement
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GroupRegionsTraversalTest {
    @Test fun readonlyTraversalIncludesAuthoredRegions() {
        val group = GroupElement(
            cover = ControlElement("#/properties/cover"),
            header = listOf(ControlElement("#/properties/heading")),
            elements = listOf(ControlElement("#/properties/body")),
            footer = listOf(ControlElement("#/properties/footer")),
        )
        val visited = mutableListOf<String>()
        iterateSchema(group) { visited += assertIs<ControlElement>(it).scope }
        assertEquals(listOf("#/properties/cover", "#/properties/heading", "#/properties/body",
            "#/properties/footer"), visited)

        val readonly = assertIs<GroupElement>(setReadonly(group))
        val children = listOfNotNull(readonly.cover) + readonly.header + readonly.elements + readonly.footer
        children.forEach { child ->
            assertEquals(JsonPrimitive(true), assertIs<ControlElement>(child).options["readonly"])
        }
    }
}
