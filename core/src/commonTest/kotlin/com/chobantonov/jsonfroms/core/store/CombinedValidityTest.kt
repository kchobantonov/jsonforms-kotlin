package com.chobantonov.jsonfroms.core.store

import com.chobantonov.jsonfroms.core.validation.ErrorObject
import kotlin.test.Test
import kotlin.test.assertEquals

class CombinedValidityTest {
    private fun state(
        mode: ValidationMode,
        schemaErrors: List<ErrorObject> = emptyList(),
        additionalErrors: List<ErrorObject> = emptyList(),
    ): JsonFormsState<Any, Any> = JsonFormsState(
        JsonFormsSubStates(
            core = JsonFormsCoreState().copy(
                validationMode = mode,
                errors = schemaErrors,
                additionalErrors = additionalErrors,
            ),
        ),
    )

    @Test
    fun noValidationIsUnvalidatedUnlessAdditionalErrorsExist() {
        assertEquals(
            CombinedValidityStatus.Unvalidated,
            getCombinedValidity(state(ValidationMode.NoValidation)).status,
        )
        val result = getCombinedValidity(
            state(ValidationMode.NoValidation, additionalErrors = listOf(ErrorObject(message = "server"))),
        )
        assertEquals(CombinedValidityStatus.Invalid, result.status)
        assertEquals(1, result.additionalErrorCount)
    }

    @Test
    fun hiddenSchemaErrorsStillInvalidate() {
        val result = getCombinedValidity(
            state(ValidationMode.ValidateAndHide, schemaErrors = listOf(ErrorObject(message = "schema"))),
        )
        assertEquals(CombinedValidityStatus.Invalid, result.status)
        assertEquals(1, result.schemaErrorCount)
    }

    @Test
    fun ownerStoreParticipatesWithoutChangingSchemaErrorVisibility() {
        val clean = state(ValidationMode.ValidateAndHide)
        val pending = OwnedAdditionalErrorStore().setPending("editor", true)
        assertEquals(CombinedValidityStatus.Pending, getCombinedValidity(clean, pending).status)
        val invalid = pending.publish("editor", "/value", listOf(ErrorObject(instancePath = "/value", message = "draft")))
        val result = getCombinedValidity(clean, invalid)
        assertEquals(CombinedValidityStatus.Invalid, result.status)
        assertEquals(1, result.additionalErrorCount)
    }

    @Test
    fun pendingAndStaleDoNotReportValidity() {
        val clean = state(ValidationMode.ValidateAndShow)
        assertEquals(CombinedValidityStatus.Valid, getCombinedValidity(clean).status)
        assertEquals(CombinedValidityStatus.Pending, getCombinedValidity(clean, pending = true).status)
        assertEquals(CombinedValidityStatus.Stale, getCombinedValidity(clean, stale = true).status)
    }

    @Test
    fun pendingAndStaleRemainObservableWhenErrorsAlreadyInvalidate() {
        val invalid = state(ValidationMode.ValidateAndShow,
            additionalErrors = listOf(ErrorObject(message = "server")))
        val initial = getCombinedValidity(invalid)
        val pending = getCombinedValidity(invalid, pending = true)
        val stale = getCombinedValidity(invalid, stale = true)
        assertEquals(CombinedValidityStatus.Invalid, pending.status)
        assertEquals(CombinedValidityStatus.Invalid, stale.status)
        assertEquals(true, pending.pending)
        assertEquals(true, stale.stale)
        assertEquals(false, initial.pending)
        kotlin.test.assertNotEquals(initial, pending)
        kotlin.test.assertNotEquals(initial, stale)
    }
}
