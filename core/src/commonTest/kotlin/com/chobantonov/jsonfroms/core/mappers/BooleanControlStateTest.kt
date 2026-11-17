package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.util.JsonPath
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class BooleanControlStateTest {
    @Test fun objectPropertiesDistinguishMissingFalseAndInvalidData() {
        val root = Json.parseToJsonElement("""{"yes":true,"no":false,"bad":"false","null":null}""")
        assertEquals(BooleanControlState(true, true, null), booleanControlState(root, "yes"))
        assertEquals(BooleanControlState(false, true, null), booleanControlState(root, "no"))
        assertEquals(BooleanControlState(null, true, null, JsonPrimitive("false")), booleanControlState(root, "bad"))
        assertEquals(BooleanControlState(null, true, null, Json.parseToJsonElement("null")), booleanControlState(root, "null"))
        assertEquals(BooleanControlState(null, false, null), booleanControlState(root, "missing"))
    }

    @Test fun arrayPositionClearsToFalseWithoutRemovingTheItem() {
        val root = Json.parseToJsonElement("""{"flags":[true,false,"false"]}""")
        assertEquals(BooleanControlState(true, true, JsonPrimitive(false)),
            booleanControlState(root, "flags.0"))
        assertEquals(BooleanControlState(false, true, JsonPrimitive(false)),
            booleanControlState(root, "flags.1"))
        assertEquals(BooleanControlState(null, true, JsonPrimitive(false), JsonPrimitive("false")),
            booleanControlState(root, "flags.2"))
        assertEquals(Json.parseToJsonElement("""{"flags":[false,false,"false"]}"""),
            JsonPath.set(root, "flags.0", booleanControlState(root, "flags.0").clearValue))
    }
}
