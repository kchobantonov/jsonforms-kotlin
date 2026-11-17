package com.chobantonov.jsonfroms.core.mappers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class ArrayTableColumnDefsTest {
    private fun options(json: String): JsonObject = Json.parseToJsonElement(json).jsonObject

    @Test fun missingAndEmptyDefinitionsAreDistinct() {
        assertNull(resolveArrayTableColumnDefs(emptyMap(), false))
        assertEquals(emptyList(), resolveArrayTableColumnDefs(options("""{"columnDefs":[]}"""), false))
    }

    @Test fun parsesPortableOrderWholeRowWidthsAndFirstDuplicate() {
        val result = resolveArrayTableColumnDefs(options("""{
          "columnDefs":[
            {"field":"email","width":180.5,"minWidth":120,"maxWidth":300},
            {"field":"summary","scope":"#"},
            {"field":"email","width":10},
            {"field":"","width":20},
            {"field":"name","width":-2,"minWidth":"50"}
          ]
        }"""), false)
        assertEquals(listOf("email", "summary", "name"), result?.map { it.field })
        assertEquals(180.5f, result?.first()?.width)
        assertEquals(120f, result?.first()?.minWidth)
        assertEquals(300f, result?.first()?.maxWidth)
        assertEquals(true, result?.get(1)?.wholeRow)
        assertNull(result?.last()?.width)
        assertNull(result?.last()?.minWidth)
    }

    @Test fun nativeGridListReplacesPortableListOnlyForGrid() {
        val values = options("""{
          "columnDefs":[{"field":"name"}],
          "agGridOptions":{"columnDefs":[{"field":"email","headerName":"Mail"}]}
        }""")
        assertEquals(listOf("name"), resolveArrayTableColumnDefs(values, false)?.map { it.field })
        assertEquals(listOf("email"), resolveArrayTableColumnDefs(values, true)?.map { it.field })
        assertEquals("Mail", resolveArrayTableColumnDefs(values, true)?.first()?.title)
        assertEquals(emptyList(), resolveArrayTableColumnDefs(options("""{
          "columnDefs":[{"field":"name"}],"agGridOptions":{"columnDefs":[]}
        }"""), true))
    }
}
