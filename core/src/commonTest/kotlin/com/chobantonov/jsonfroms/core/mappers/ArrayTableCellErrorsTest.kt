package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArrayTableCellErrorsTest {
    private val first = ErrorObject(instancePath = "/rows/1/firstName", message = "first")
    private val last = ErrorObject(instancePath = "/rows/1/lastName", message = "last")
    private val notes = ErrorObject(instancePath = "/rows/1/notes", message = "notes")
    private val otherRow = ErrorObject(instancePath = "/rows/10/firstName", message = "other")
    private val required = ErrorObject(instancePath = "/rows/1", keyword = "required",
        message = "missing", params = mapOf("missingProperty" to "firstName"))

    @Test fun virtualDetailCountsOnlyItsExposedControls() {
        val detail = VerticalLayout(elements = listOf(
            ControlElement("#/properties/firstName"), ControlElement("#/properties/lastName"),
        ))
        assertEquals(listOf(first, last, required), tableCellErrors(
            listOf(first, last, notes, otherRow, required), "rows.1", detail, labelSummary = true,
        ))
    }

    @Test fun labelWithoutDetailHasNoMarkerAndWholeValueDetailIncludesDescendants() {
        assertTrue(tableCellErrors(listOf(first), "rows.1", null, labelSummary = true).isEmpty())
        assertEquals(listOf(first, notes, required), tableCellErrors(
            listOf(first, notes, required), "rows.1", ControlElement("#"), labelSummary = true,
        ))
    }

    @Test fun propertyDetailStaysBelowItsCellPath() {
        val city = ErrorObject(instancePath = "/rows/1/contact/city", message = "city")
        val phone = ErrorObject(instancePath = "/rows/1/contact/phone", message = "phone")
        val detail = ControlElement("#/properties/city")
        assertEquals(listOf(city), tableCellErrors(listOf(city, phone, notes),
            "rows.1.contact", detail, labelSummary = false))
    }
}
