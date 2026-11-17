package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsState
import com.chobantonov.jsonfroms.compose.JsonFormsComposeController
import com.chobantonov.jsonfroms.compose.JsonFormsStateProvider
import com.chobantonov.jsonfroms.compose.useJsonForms
import com.chobantonov.jsonfroms.core.store.CombinedValidity
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import com.chobantonov.jsonfroms.core.store.JsonFormsCoreState
import com.chobantonov.jsonfroms.core.store.JsonFormsSubStates
import com.chobantonov.jsonfroms.core.store.ValidationMode
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class CombinedValidityCallbackUiTest {
    @Test
    fun modeAndSameCountErrorChangesNotifyHostWithoutDataEdits() = runComposeUiTest {
        var mode by mutableStateOf(ValidationMode.ValidateAndShow)
        var hostErrors by mutableStateOf(listOf(ErrorObject(instancePath = "/name", message = "first")))
        val snapshots = mutableListOf<CombinedValidity>()
        var dataChanges = 0
        setContent {
            JsonFormsStateProvider(
                initState = ComposeJsonFormsState(JsonFormsSubStates(core = JsonFormsCoreState().copy(
                    validationMode = mode,
                    additionalErrors = hostErrors,
                ))),
                onChange = { dataChanges++ },
                onValidityChange = snapshots::add,
            ) {}
        }
        waitForIdle()
        assertEquals(CombinedValidityStatus.Invalid, snapshots.last().status)
        val initialCount = snapshots.size

        runOnIdle { mode = ValidationMode.ValidateAndHide }
        waitForIdle()
        assertEquals(initialCount + 1, snapshots.size)
        assertEquals(ValidationMode.ValidateAndHide, snapshots.last().validationMode)
        assertEquals(CombinedValidityStatus.Invalid, snapshots.last().status)

        val beforeReplacement = snapshots.size
        runOnIdle { hostErrors = listOf(ErrorObject(instancePath = "/name", message = "second")) }
        waitForIdle()
        assertEquals(beforeReplacement + 1, snapshots.size)
        assertEquals(1, snapshots.last().additionalErrorCount)

        runOnIdle { mode = ValidationMode.NoValidation }
        waitForIdle()
        assertEquals(ValidationMode.NoValidation, snapshots.last().validationMode)
        assertEquals(CombinedValidityStatus.Invalid, snapshots.last().status)
        runOnIdle { hostErrors = emptyList() }
        waitForIdle()
        assertEquals(CombinedValidityStatus.Unvalidated, snapshots.last().status)
        assertEquals(0, dataChanges)
    }

    @Test
    fun pendingChangesNotifyHostEvenWhenAnErrorAlreadyMakesFormInvalid() = runComposeUiTest {
        var controller: JsonFormsComposeController? = null
        val snapshots = mutableListOf<CombinedValidity>()
        var dataChanges = 0
        setContent {
            JsonFormsStateProvider(
                initState = ComposeJsonFormsState(),
                onChange = { dataChanges++ },
                onValidityChange = snapshots::add,
            ) {
                val current = useJsonForms().controller
                SideEffect { controller = current }
            }
        }
        waitForIdle()
        assertEquals(CombinedValidityStatus.Valid, snapshots.last().status)

        runOnIdle { controller!!.publishAdditionalErrors("server", "/name",
            listOf(ErrorObject(instancePath = "/name", message = "taken"))) }
        waitForIdle()
        assertEquals(CombinedValidityStatus.Invalid, snapshots.last().status)
        assertEquals(false, snapshots.last().pending)
        val countBeforePending = snapshots.size

        runOnIdle { controller!!.setAdditionalErrorsPending("editor", true) }
        waitForIdle()
        assertEquals(countBeforePending + 1, snapshots.size)
        assertEquals(CombinedValidityStatus.Invalid, snapshots.last().status)
        assertEquals(true, snapshots.last().pending)

        runOnIdle { controller!!.setAdditionalErrorsPending("editor", false) }
        waitForIdle()
        assertEquals(false, snapshots.last().pending)
        assertEquals(0, dataChanges)
    }
}
