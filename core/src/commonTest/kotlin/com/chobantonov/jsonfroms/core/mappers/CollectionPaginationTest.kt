package com.chobantonov.jsonfroms.core.mappers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

class CollectionPaginationTest {
    private val config = mapOf("jsonformsExtended" to mapOf("array" to mapOf(
        "pagination" to mapOf("pageSize" to 8, "pageSizeOptions" to listOf(8, 16)),
    )))

    @Test fun localValueReplacesWholeScopedValueAndFalseWins() {
        assertEquals(CollectionPaginationOptions(true, 8, listOf(8, 16)),
            resolveCollectionPagination(emptyMap(), config))
        assertEquals(CollectionPaginationOptions(true, 5, listOf(5, 10, 25, 50)),
            resolveCollectionPagination(mapOf("pagination" to JsonPrimitive(true)), config))
        assertEquals(false,
            resolveCollectionPagination(mapOf("pagination" to JsonPrimitive(false)), config).enabled)
        assertEquals(false, resolveCollectionPagination(emptyMap(),
            mapOf("pagination" to true), defaultEnabled = false).enabled)
    }

    @Test fun objectOptionsSanitizeChoicesAndIncludeInitialSize() {
        val local = Json.parseToJsonElement(
            """{"pagination":{"pageSize":6,"pageSizeOptions":[10,6,10,0,-1,2.5,"4"]}}"""
        ).jsonObject
        assertEquals(CollectionPaginationOptions(true, 6, listOf(6, 10)),
            resolveCollectionPagination(local, config))
        val invalid = Json.parseToJsonElement(
            """{"pagination":{"pageSize":0,"pageSizeOptions":[]}}"""
        ).jsonObject
        assertEquals(CollectionPaginationOptions(true, 5, listOf(5, 10, 25, 50)),
            resolveCollectionPagination(invalid, emptyMap()))
        val unusable = Json.parseToJsonElement(
            """{"pagination":{"pageSize":6,"pageSizeOptions":[0,-1,"10"]}}"""
        ).jsonObject
        assertEquals(CollectionPaginationOptions(true, 6, listOf(5, 6, 10, 25, 50)),
            resolveCollectionPagination(unusable, emptyMap()))
    }

    @Test fun nonBooleanPrimitivesDoNotCoercePaginationActivation() {
        val quotedFalse = Json.parseToJsonElement("""{"pagination":"false"}""").jsonObject
        assertEquals(CollectionPaginationOptions(true, 8, listOf(8, 16)),
            resolveCollectionPagination(quotedFalse, config))
        val numeric = Json.parseToJsonElement("""{"pagination":1}""").jsonObject
        assertEquals(CollectionPaginationOptions(false, 5, listOf(5, 10, 25, 50)),
            resolveCollectionPagination(numeric, emptyMap(), defaultEnabled = false))
        assertEquals(CollectionPaginationOptions(false, 5, listOf(5, 10, 25, 50)),
            resolveCollectionPagination(emptyMap(), mapOf("jsonformsExtended" to mapOf(
                "array" to mapOf("pagination" to "true"))), defaultEnabled = false))
    }

    @Test fun windowAndSizeChangeKeepAbsoluteItemIndices() {
        assertEquals(CollectionPageWindow(3, 3, 10, 12), collectionPageWindow(12, 3, 5, true))
        assertEquals(CollectionPageWindow(3, 3, 10, 11), collectionPageWindow(11, 4, 5, true))
        assertEquals(CollectionPageWindow(1, 3, 0, 12), collectionPageWindow(12, 3, 5, false))
        assertEquals(2, pageForFirstVisibleItem(5, 4))
    }

    @Test fun agGridNativeOverridesOnlyExplicitPaginationFields() {
        val portable = CollectionPaginationOptions(true, 6, listOf(3, 6, 12))
        assertEquals(portable, applyAgGridPaginationOverrides(portable, null))
        val sizeOnly = Json.parseToJsonElement("""{"paginationPageSize":2}""").jsonObject
        assertEquals(CollectionPaginationOptions(true, 2, listOf(2, 3, 6, 12)),
            applyAgGridPaginationOverrides(portable, sizeOnly))
        val disabled = Json.parseToJsonElement("""{
            "pagination":false,"paginationPageSize":2,"paginationPageSizeSelector":false
        }""").jsonObject
        assertEquals(CollectionPaginationOptions(false, 2, listOf(2)),
            applyAgGridPaginationOverrides(portable, disabled))
        val explicit = Json.parseToJsonElement("""{
            "pagination":true,"paginationPageSize":3,"paginationPageSizeSelector":[2,3,5,5]
        }""").jsonObject
        assertEquals(CollectionPaginationOptions(true, 3, listOf(2, 3, 5)),
            applyAgGridPaginationOverrides(portable.copy(enabled = false), explicit))
        val quoted = Json.parseToJsonElement("""{
            "pagination":"false","paginationPageSizeSelector":"false"
        }""").jsonObject
        assertEquals(portable, applyAgGridPaginationOverrides(portable, quoted))
    }
}
