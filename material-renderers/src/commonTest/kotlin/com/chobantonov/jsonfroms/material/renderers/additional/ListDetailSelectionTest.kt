package com.chobantonov.jsonfroms.material.renderers.additional

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray

class ListDetailSelectionTest {
    private fun rows() = Json.parseToJsonElement("""[{"text":"same"},{"text":"same"}]""") as JsonArray

    @Test fun duplicateObjectsFollowTheSurvivingReference() {
        val before = rows()
        val selected = ListDetailSelection(1, before[1])
        assertEquals(
            ListDetailSelection(0, before[1]),
            reconcileListDetailSelection(selected, before, JsonArray(listOf(before[1]))),
        )
        assertNull(reconcileListDetailSelection(selected, before, JsonArray(listOf(before[0]))))
    }

    @Test fun equivalentExternalReplacementDoesNotReuseAnOccurrence() {
        val before = rows()
        assertNull(reconcileListDetailSelection(ListDetailSelection(1, before[1]), before, rows()))
    }

    @Test fun editingOnlyTheSelectedObjectKeepsItsOccurrence() {
        val before = rows()
        val changed = Json.parseToJsonElement("""{"text":"edited"}""")
        assertEquals(
            ListDetailSelection(1, changed),
            reconcileListDetailSelection(
                ListDetailSelection(1, before[1]), before, JsonArray(listOf(before[0], changed)),
            ),
        )
    }

    @Test fun duplicatePrimitiveValuesAreAmbiguousAfterExternalChange() {
        val before = Json.parseToJsonElement("""["same","same"]""") as JsonArray
        assertNull(reconcileListDetailSelection(
            ListDetailSelection(1, before[1]), before, JsonArray(listOf(before[1])),
        ))
    }
}
