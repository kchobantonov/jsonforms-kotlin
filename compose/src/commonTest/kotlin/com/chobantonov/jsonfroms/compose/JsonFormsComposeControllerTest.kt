package com.chobantonov.jsonfroms.compose

import com.chobantonov.jsonfroms.core.actions.Actions
import com.chobantonov.jsonfroms.core.actions.SetUISchemaAction
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JsonFormsComposeControllerTest {
    @Test
    fun `dispatch publishes the completed data change synchronously`() {
        val controller = JsonFormsComposeController()

        val change = controller.dispatchAndGetChange(
            Actions.update("", updater = { JsonPrimitive("Ada") }),
        )

        assertEquals(JsonPrimitive("Ada"), change?.data)
        assertEquals(JsonPrimitive("Ada"), controller.state.jsonforms.core.data)
    }

    @Test
    fun `dispatch does not publish changes without new data or errors`() {
        val controller = JsonFormsComposeController()

        val change = controller.dispatchAndGetChange(SetUISchemaAction(VerticalLayout()))

        assertNull(change)
    }

    @Test
    fun `setting the same data does not publish another change`() {
        val controller = JsonFormsComposeController()
        controller.dispatch(Actions.update("", updater = { JsonPrimitive("Ada") }))

        val duplicate = controller.dispatchAndGetChange(
            Actions.update("", updater = { JsonPrimitive("Ada") }),
        )

        assertNull(duplicate)
    }
}
