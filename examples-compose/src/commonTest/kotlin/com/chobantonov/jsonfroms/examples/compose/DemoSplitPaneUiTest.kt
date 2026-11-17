package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DemoSplitPaneUiTest {
    @Test
    fun progressResizeKeepsMinimumWidthsAndStacksWhenNarrow() = runComposeUiTest {
        var fraction by mutableFloatStateOf(.75f)
        var width by mutableStateOf(1200.dp)
        setContent {
            Box(Modifier.requiredWidth(width).height(400.dp)) {
                DemoSplitPane(fraction, { fraction = it }, "Resize form and data panes",
                    form = { pane -> Box(pane) }, data = { pane -> Box(pane) })
            }
        }
        fun form() = onNodeWithTag("demo-form-pane").fetchSemanticsNode().boundsInRoot
        fun data() = onNodeWithTag("demo-data-pane").fetchSemanticsNode().boundsInRoot
        assertTrue(data().width >= 280f)
        val initialRatio = form().width / (form().width + data().width)
        assertTrue(kotlin.math.abs(initialRatio - .75f) < .02f,
            "initial ratio=$initialRatio form=${form().width} data=${data().width}")
        val initial = form().width
        onNodeWithTag("demo-splitter").performSemanticsAction(SemanticsActions.SetProgress) { it(.5f) }
        waitForIdle()
        assertTrue(form().width < initial - 80f)
        assertTrue(form().width >= 280f && data().width >= 280f)
        runOnIdle { width = 500.dp }
        onNodeWithTag("demo-splitter").assertDoesNotExist()
        assertTrue(data().top > form().top)
        assertTrue(form().width >= 499f && data().width >= 499f)
    }

    @Test
    fun keyboardAndTouchResizeRespectRtl() = runComposeUiTest {
        var fraction by mutableFloatStateOf(.5f)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(Modifier.width(900.dp).height(400.dp)) {
                    DemoSplitPane(fraction, { fraction = it }, "Преоразмеряване на формуляра и данните",
                        form = { pane -> Box(pane) }, data = { pane -> Box(pane) })
                }
            }
        }
        val splitter = onNodeWithTag("demo-splitter")
        splitter.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        splitter.performKeyInput { keyDown(Key.DirectionLeft); keyUp(Key.DirectionLeft) }
        assertTrue(fraction > .5f)
        val afterKey = fraction
        splitter.performTouchInput { swipeRight() }
        assertTrue(fraction < afterKey)
    }
}
