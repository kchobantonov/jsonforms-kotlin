package com.chobantonov.jsonfroms.core.reducers

import com.chobantonov.jsonfroms.core.actions.AddCellRendererAction
import com.chobantonov.jsonfroms.core.actions.AddRendererAction
import com.chobantonov.jsonfroms.core.actions.RemoveCellRendererAction
import com.chobantonov.jsonfroms.core.actions.RemoveRendererAction
import com.chobantonov.jsonfroms.core.actions.SetConfigAction
import com.chobantonov.jsonfroms.core.testers.NOT_APPLICABLE
import com.chobantonov.jsonfroms.core.testers.RankedTester
import kotlin.test.Test
import kotlin.test.assertEquals

class RegistryReducersTest {
    private val tester: RankedTester = { _, _, _ -> NOT_APPLICABLE }

    @Test
    fun registersAndUnregistersRenderers() {
        val afterRegister = rendererReducer(
            state = emptyList(),
            action = AddRendererAction(tester, "renderer"),
        )
        val afterUnregister = rendererReducer(
            state = afterRegister,
            action = RemoveRendererAction(tester),
        )

        assertEquals(1, afterRegister.size)
        assertEquals(emptyList(), afterUnregister)
    }

    @Test
    fun registersAndUnregistersCells() {
        val afterRegister = cellReducer(
            state = emptyList(),
            action = AddCellRendererAction(tester, "cell"),
        )
        val afterUnregister = cellReducer(
            state = afterRegister,
            action = RemoveCellRendererAction(tester),
        )

        assertEquals(1, afterRegister.size)
        assertEquals(emptyList(), afterUnregister)
    }

    @Test
    fun mergesConfigWithDefaults() {
        val config = configReducer(
            state = null,
            action = SetConfigAction(mapOf("trim" to true)),
        )

        assertEquals(true, config["trim"])
        assertEquals(false, config["restrict"])
    }
}
