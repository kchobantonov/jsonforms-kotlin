package com.chobantonov.jsonfroms.core.mappers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class TableLabelSummaryTest {
    private val root = Json.parseToJsonElement("""{"title":"Applicants","rows":[{"name":"Ada","contact":{"city":"Boston"}}]}""")
    private val item = Json.parseToJsonElement("""{"name":"Ada","contact":{"city":"Boston"}}""")
    private val params = Json.parseToJsonElement(
        """{"name":"{item.name}","city":"{item.contact.city}","title":"{data.title}"}"""
    ).jsonObject
    private val enabled = mapOf("jsonformsExtended" to mapOf("dynamicValues" to mapOf("enabled" to true)))

    @Test fun resolvesDeclaredRowAndRootParameters() {
        assertEquals("Ada · Boston — Applicants",
            resolveTableLabelSummary("{name} · {city} — {title}", params, root, item, enabled))
    }

    @Test fun staticParametersInterpolateWithoutDynamicValueGate() {
        val staticParams = Json.parseToJsonElement("""{"prefix":"Person","count":"2"}""").jsonObject
        assertEquals("Person 2", resolveTableLabelSummary("{prefix} {count}",
            staticParams, root, item, emptyMap()))
        assertEquals("Person 2", resolveTableLabelSummary("{prefix} {count}",
            staticParams, root, item, enabled))
    }

    @Test fun gateAndUnknownExpressionsNeverExposeTemplate() {
        assertNull(resolveTableLabelSummary("{name}", params, root, item, emptyMap()))
        assertNull(resolveTableLabelSummary("{name}", null, root, item, enabled))
        assertNull(resolveTableLabelSummary("{name}", Json.parseToJsonElement(
            """{"name":"{context.secret}"}"""
        ).jsonObject, root, item, enabled))
        assertNull(resolveTableLabelSummary("{missing}", params, root, item, enabled))
        assertNull(resolveTableLabelSummary("{item.contact.city}", params, root, item, enabled))
    }

    @Test fun dynamicValuesGateReadsOnlyLiteralExtendedNamespace() {
        val misplaced = mapOf(
            "dynamicValues" to mapOf("enabled" to true),
            "jsonFormsExtended" to mapOf("dynamicValues" to mapOf("enabled" to true)),
        )
        assertNull(resolveTableLabelSummary("{name}", params, root, item, misplaced))
        assertEquals("Ada", resolveTableLabelSummary("{name}", params, root, item,
            misplaced + ("jsonformsExtended" to mapOf("dynamicValues" to mapOf("enabled" to true)))))
    }

    @Test fun quotedGateDoesNotEnableDynamicSummary() {
        val quotedHost = mapOf("jsonformsExtended" to mapOf(
            "dynamicValues" to mapOf("enabled" to "true"),
        ))
        val quotedJson = mapOf("jsonformsExtended" to Json.parseToJsonElement(
            """{"dynamicValues":{"enabled":"true"}}"""
        ).jsonObject)
        assertNull(resolveTableLabelSummary("{name}", params, root, item, quotedHost))
        assertNull(resolveTableLabelSummary("{name}", params, root, item, quotedJson))
    }
}
