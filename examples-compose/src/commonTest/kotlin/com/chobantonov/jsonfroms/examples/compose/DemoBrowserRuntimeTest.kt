package com.chobantonov.jsonfroms.examples.compose

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DemoBrowserRuntimeTest {
    @Test fun destinationKeepsProjectPrefixExampleAndViewOptions() {
        val transfer = DemoRuntimeHandoff(DemoBrowserRuntime.Wasm, "space & / бг",
            Json.parseToJsonElement("""{"name":"Грейс"}"""), true)
        val destination = demoRuntimeDestination(
            "/jsonforms-kotlin/index.html", "?source=demo", "#old?active-tab=data&form-only=true",
            DemoBrowserRuntime.Js, DemoBrowserRuntime.Wasm, transfer, "123abc",
        )
        assertEquals("/jsonforms-kotlin/wasm/?source=demo&runtime-handoff=123abc" +
            "#space%20%26%20%2F%20%D0%B1%D0%B3?form-only=true&active-tab=data", destination)
        assertEquals("/jsonforms-kotlin/", demoRuntimePath(
            "/jsonforms-kotlin/wasm/index.html", DemoBrowserRuntime.Wasm, DemoBrowserRuntime.Js))
        assertEquals("/wasm/", demoRuntimePath("/", DemoBrowserRuntime.Js, DemoBrowserRuntime.Wasm))
    }

    @Test fun transferKeepsJsonNullDistinctFromMissingData() {
        val transfer = DemoRuntimeHandoff(DemoBrowserRuntime.Js, "nullable", JsonNull, false)
        assertEquals(transfer, decodeDemoRuntimeHandoff(transfer.encode()))
        assertNull(decodeDemoRuntimeHandoff("""{"version":1,"target":"Js","example":"nullable","data":null}"""))
        assertNull(decodeDemoRuntimeHandoff("""{"version":1,"target":"unknown","example":"nullable","data":null,"dataProvided":true}"""))
    }

    @Test fun oneTimeTokenIsRemovedWithoutChangingOtherQueryParameters() {
        assertEquals("123abc", demoRuntimeHandoffToken("?a=1&runtime-handoff=123abc&b=2"))
        assertEquals("?a=1&b=2", withoutDemoRuntimeHandoffToken("?a=1&runtime-handoff=123abc&b=2"))
        assertEquals("?a=1&b=2&runtime-handoff=456", withDemoRuntimeHandoffToken(
            "?a=1&runtime-handoff=123abc&b=2", "456"))
        assertNull(demoRuntimeHandoffToken("?runtime-handoff=../unsafe"))
    }
}
