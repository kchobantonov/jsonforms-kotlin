package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.examples.StateProps
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class AdditionalErrorsDemoUiTest {
    @Test
    fun materialServerErrorsClearByEditedValue() = verify(DemoUiStyle.Material)

    @Test
    fun cupertinoServerErrorsClearByEditedValue() = verify(DemoUiStyle.Cupertino)

    @Test
    fun bulgarianMaterialActionsAndServerErrorsAreTranslated() = verify(
        DemoUiStyle.Material, DemoLocale.Bulgarian,
        submit = "Изпрати (отхвърлено)", clear = "Изчисти грешките",
        emailError = "Този адрес вече е регистриран.", phoneError = "Не успяхме да се свържем с този номер.",
    )

    @Test
    fun bulgarianCupertinoActionsAndServerErrorsAreTranslated() = verify(
        DemoUiStyle.Cupertino, DemoLocale.Bulgarian,
        submit = "Изпрати (отхвърлено)", clear = "Изчисти грешките",
        emailError = "Този адрес вече е регистриран.", phoneError = "Не успяхме да се свържем с този номер.",
    )

    private fun verify(
        style: DemoUiStyle,
        locale: DemoLocale = DemoLocale.English,
        submit: String = "Submit (rejected)",
        clear: String = "Clear them",
        emailError: String = "That address is already registered.",
        phoneError: String = "We could not reach this number.",
    ) = runComposeUiTest {
        val fixture = getBuiltInExamples().first { it.name == "spec-additional-errors" }
        var state by mutableStateOf(StateProps(
            data = fixture.data,
            schema = fixture.schema,
            uischema = fixture.uischema,
            config = fixture.config,
            localizedTranslations = fixture.localizedTranslations,
            translations = fixture.translations,
        ))
        val settings = DemoSettings(uiStyle = style, locale = locale)
        setContent {
            MaterialTheme {
                FormCard(state, compact = true, settings, { state = it }, {}, Modifier.fillMaxSize(), scrollContent = true)
            }
        }
        onNodeWithText(submit).performClick()
        waitForIdle()
        onNodeWithText(emailError).assertExists()
        onNodeWithText(phoneError).assertExists()
        onAllNodesWithText("ada@example.com").onFirst().performTextReplacement("grace@example.com")
        waitForIdle()
        onNodeWithText(emailError).assertDoesNotExist()
        onNodeWithText(phoneError).assertExists()
        onNodeWithText(clear).performClick()
        waitForIdle()
        onNodeWithText(phoneError).assertDoesNotExist()
    }
}
