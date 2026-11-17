package com.chobantonov.jsonfroms.compose

import com.chobantonov.jsonfroms.core.actions.Actions
import com.chobantonov.jsonfroms.core.actions.InitAction
import com.chobantonov.jsonfroms.core.actions.MoveArrayContext
import com.chobantonov.jsonfroms.core.actions.MoveOperation
import com.chobantonov.jsonfroms.core.actions.UpdateAction
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import com.chobantonov.jsonfroms.core.actions.SetUISchemaAction
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import com.chobantonov.jsonfroms.core.store.JsonFormsCoreState
import com.chobantonov.jsonfroms.core.store.JsonFormsSubStates
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JsonFormsComposeControllerTest {
    @Test
    fun `pending owner remains observable while an error makes validity invalid`() {
        val controller = JsonFormsComposeController()
        controller.publishAdditionalErrors("host", "/value", listOf(ErrorObject(message = "error")))
        controller.setAdditionalErrorsPending("draft", true)
        assertEquals(CombinedValidityStatus.Invalid, controller.combinedValidity.status)
        assertEquals(true, controller.hasPendingOwnedWork)
        controller.setAdditionalErrorsPending("draft", false)
        assertEquals(false, controller.hasPendingOwnedWork)
    }

    @Test
    fun `moving structurally equal rows still publishes an occurrence change`() {
        val before = Json.parseToJsonElement("""[{"name":"same"},{"name":"same"}]""") as JsonArray
        val controller = JsonFormsComposeController(ComposeJsonFormsState(JsonFormsSubStates(
            core = JsonFormsCoreState().copy(data = before),
        )))
        val change = controller.dispatchAndGetChange(UpdateAction(
            path = "",
            context = MoveArrayContext(listOf(MoveOperation(1, 0))),
            updater = { JsonArray(listOf(before[1], before[0])) },
        ))
        val after = controller.state.jsonforms.core.data as JsonArray
        assertEquals(before, change?.data)
        assertEquals(true, after !== before)
        assertEquals(true, after[0] === before[1])

        val invalid = controller.dispatchAndGetChange(UpdateAction(
            path = "",
            context = MoveArrayContext(listOf(MoveOperation(8, 0))),
            updater = { JsonArray(after) },
        ))
        assertNull(invalid)
    }

    @Test
    fun `owner errors update validity and preserve external data`() {
        val controller = JsonFormsComposeController()
        val owner = ErrorObject(instancePath = "/value", message = "draft")
        controller.setAdditionalErrorsPending("editor", true)
        assertEquals(CombinedValidityStatus.Pending, controller.combinedValidity.status)
        controller.publishAdditionalErrors("editor", "/value", listOf(owner))
        assertEquals(JsonNull, controller.state.jsonforms.core.data)
        assertEquals(listOf(owner), controller.state.jsonforms.core.additionalErrors)
        assertEquals(CombinedValidityStatus.Invalid, controller.combinedValidity.status)
        controller.clearAdditionalErrors("editor")
        assertEquals(emptyList(), controller.state.jsonforms.core.additionalErrors)
        assertEquals(CombinedValidityStatus.Valid, controller.combinedValidity.status)
    }

    @Test
    fun `external host errors merge with owners and data replacement clears stale owners`() {
        val controller = JsonFormsComposeController()
        val owner = ErrorObject(instancePath = "/value", message = "draft")
        val host = ErrorObject(instancePath = "/value", message = "server")
        controller.publishAdditionalErrors("editor", "/value", listOf(owner))
        val sameData = ComposeJsonFormsState(JsonFormsSubStates(
            core = JsonFormsCoreState().copy(additionalErrors = listOf(host)),
        ))
        controller.replaceState(sameData)
        assertEquals(listOf(host, owner), controller.state.jsonforms.core.additionalErrors)
        assertEquals(0L, controller.hostSubjectVersion)
        val replaced = sameData.copy(jsonforms = sameData.jsonforms.copy(
            core = JsonFormsCoreState().copy(data = JsonPrimitive("new"), additionalErrors = listOf(host)),
        ))
        controller.replaceState(replaced)
        assertEquals(listOf(host), controller.state.jsonforms.core.additionalErrors)
        assertEquals(1L, controller.hostSubjectVersion)
    }

    @Test
    fun `controlled edit echo does not retire row occurrences`() {
        val controller = JsonFormsComposeController()
        controller.dispatch(Actions.update("", updater = { JsonPrimitive("edited") }))
        assertEquals(0L, controller.hostSubjectVersion)
        controller.replaceState(controller.state)
        assertEquals(0L, controller.hostSubjectVersion)
        controller.replaceState(controller.state.copy(jsonforms = controller.state.jsonforms.copy(
            core = (controller.state.jsonforms.core as JsonFormsCoreState).copy(
                data = JsonPrimitive("external")),
        )))
        assertEquals(1L, controller.hostSubjectVersion)
    }

    @Test
    fun `late async owner results cannot overwrite newer requests or external data`() {
        val controller = JsonFormsComposeController()
        val old = controller.beginAdditionalErrors("editor")
        val current = controller.beginAdditionalErrors("editor")
        controller.publishAdditionalErrors("editor", "/value", listOf(ErrorObject(message = "old")), old)
        assertEquals(CombinedValidityStatus.Pending, controller.combinedValidity.status)
        assertEquals(emptyList(), controller.state.jsonforms.core.additionalErrors)
        controller.publishAdditionalErrors("editor", "/value", listOf(ErrorObject(message = "current")), current)
        assertEquals(CombinedValidityStatus.Invalid, controller.combinedValidity.status)
        assertEquals("current", controller.state.jsonforms.core.additionalErrors.single().message)

        val obsolete = controller.beginAdditionalErrors("editor")
        controller.replaceState(ComposeJsonFormsState(JsonFormsSubStates(
            core = JsonFormsCoreState().copy(data = JsonPrimitive("replacement")),
        )))
        controller.publishAdditionalErrors("editor", "/value", listOf(ErrorObject(message = "obsolete")), obsolete)
        assertEquals(emptyList(), controller.state.jsonforms.core.additionalErrors)
        assertEquals(CombinedValidityStatus.Valid, controller.combinedValidity.status)
    }

    @Test
    fun `editing a target invalidates its owner and async result but preserves unrelated errors`() {
        val controller = JsonFormsComposeController()
        val first = controller.beginAdditionalErrors("first", "/first")
        val second = controller.beginAdditionalErrors("second", "/second")
        controller.publishAdditionalErrors("second", "/second", listOf(ErrorObject(instancePath = "/second", message = "other")), second)

        controller.dispatch(Actions.update("first", updater = { JsonPrimitive("new") }))
        controller.publishAdditionalErrors("first", "/first", listOf(ErrorObject(message = "stale")), first)

        assertEquals(listOf(ErrorObject(instancePath = "/second", message = "other")),
            controller.state.jsonforms.core.additionalErrors)
        assertEquals(CombinedValidityStatus.Invalid, controller.combinedValidity.status)
    }

    @Test
    fun `moving a pending owner updates its invalidation target`() {
        val controller = JsonFormsComposeController()
        val request = controller.beginAdditionalErrors("row", "/items/1")
        controller.moveAdditionalErrors("row", "/items/0")
        controller.dispatch(Actions.update("items.1", updater = { JsonPrimitive("old row") }))
        assertEquals(CombinedValidityStatus.Pending, controller.combinedValidity.status)
        controller.dispatch(Actions.update("items.0", updater = { JsonPrimitive("moved row") }))
        controller.publishAdditionalErrors("row", "/items/0", listOf(ErrorObject(message = "stale")), request)
        assertEquals(emptyList(), controller.state.jsonforms.core.additionalErrors)
    }

    @Test
    fun `moved pending owner rejects old path result and accepts current target`() {
        val controller = JsonFormsComposeController()
        val request = controller.beginAdditionalErrors("row", "/items/1")
        controller.moveAdditionalErrors("row", "/items/0")

        controller.publishAdditionalErrors("row", "/items/1",
            listOf(ErrorObject(instancePath = "/items/1", message = "stale")), request)
        assertEquals(emptyList(), controller.state.jsonforms.core.additionalErrors)
        assertEquals(CombinedValidityStatus.Pending, controller.combinedValidity.status)

        val current = ErrorObject(instancePath = "/items/0", message = "current")
        controller.publishAdditionalErrors("row", "/items/0", listOf(current), request)
        assertEquals(listOf(current), controller.state.jsonforms.core.additionalErrors)
        assertEquals(CombinedValidityStatus.Invalid, controller.combinedValidity.status)
    }

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

    @Test
    fun `selecting null publishes absent to explicit null transition`() {
        val controller = JsonFormsComposeController()
        controller.dispatch(
            InitAction(
                data = JsonNull,
                dataProvided = false,
                schema = buildJsonSchema {},
            ),
        )

        val change = controller.dispatchAndGetChange(
            Actions.update("", updater = { JsonNull }),
        )

        assertEquals(JsonNull, change?.data)
        assertEquals(true, change?.dataProvided)
    }
}
