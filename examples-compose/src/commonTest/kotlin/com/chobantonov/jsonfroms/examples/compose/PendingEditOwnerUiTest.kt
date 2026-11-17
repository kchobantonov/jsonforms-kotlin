package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsState
import com.chobantonov.jsonfroms.compose.JsonFormsStateProvider
import com.chobantonov.jsonfroms.compose.rememberPendingEditOwner
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class PendingEditOwnerUiTest {
    @Test
    fun disappearingEditorDisposesItsPendingOwner() = runComposeUiTest {
        var editorVisible by mutableStateOf(true)
        var validity: CombinedValidityStatus? by mutableStateOf(null)
        val initialState = ComposeJsonFormsState()
        setContent {
            JsonFormsStateProvider(
                initState = initialState,
                onValidityChange = { validity = it.status },
            ) {
                if (editorVisible) PendingEditorProbe()
            }
        }
        waitForIdle()
        assertEquals(CombinedValidityStatus.Pending, validity)

        runOnIdle { editorVisible = false }
        waitForIdle()
        assertEquals(CombinedValidityStatus.Valid, validity)
    }
}

@Composable
private fun PendingEditorProbe() {
    val setPending = rememberPendingEditOwner("test:pending-editor")
    LaunchedEffect(setPending) { setPending(true) }
}
