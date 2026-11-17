package com.chobantonov.jsonfroms.core.store

import com.chobantonov.jsonfroms.core.validation.ErrorObject
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class HostAdditionalErrorsTest {
    private fun data(text: String) = Json.parseToJsonElement(text)
    private fun error(path: String) = ErrorObject(instancePath = path, message = path)

    @Test
    fun editingOneFieldClearsOnlyItsHostError() {
        val before = data("""{"contact":{"email":"ada@example.com","phone":"555"}}""")
        val after = data("""{"contact":{"email":"grace@example.com","phone":"555"}}""")
        val errors = listOf(error("/contact/email"), error("/contact/phone"))
        assertEquals(listOf(error("/contact/phone")), retainHostErrorsForUnchangedValues(before, after, errors))
    }

    @Test
    fun rewritingWithSameValueKeepsErrorAndMissingDiffersFromNull() {
        val before = data("""{"value":"Ada"}""")
        assertEquals(listOf(error("/value")), retainHostErrorsForUnchangedValues(before, before, listOf(error("/value"))))
        val explicitNull = data("""{"value":null}""")
        assertEquals(emptyList(), retainHostErrorsForUnchangedValues(data("{}"), explicitNull, listOf(error("/value"))))
    }

    @Test
    fun pointerEscapingAndArrayIndicesUseExactValues() {
        val before = data("""{"a/b":[{"~key":1},{"~key":2}]}""")
        val after = data("""{"a/b":[{"~key":1},{"~key":3}]}""")
        val errors = listOf(error("/a~1b/0/~0key"), error("/a~1b/1/~0key"))
        assertEquals(listOf(error("/a~1b/0/~0key")), retainHostErrorsForUnchangedValues(before, after, errors))
    }

    @Test
    fun changingValueBeneathEmptyPropertyClearsOnlyItsHostError() {
        val before = data("""{"":{"name":"Ada"},"name":"root"}""")
        val after = data("""{"":{"name":"Grace"},"name":"root"}""")
        val errors = listOf(error("//name"), error("/name"))
        assertEquals(listOf(error("/name")),
            retainHostErrorsForUnchangedValues(before, after, errors))
    }
}
