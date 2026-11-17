package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class AllOfPreTouchUiTest {
    @Test fun materialOwnErrorAppearsAfterChildBlur() = scenario(false)
    @Test fun cupertinoOwnErrorAppearsAfterChildBlur() = scenario(true)

    private fun scenario(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"contact":{"type":"object","allOf":[{"type":"object","minProperties":3,"properties":{"phone":{"type":"string"}}},{"type":"object","properties":{"city":{"type":"string"}}}]},"next":{"type":"string"}}}"""
        ).jsonObject)
        var changes = 0
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"contact":{"phone":"5550100","city":"Boston"},"next":"after"}"""),
                schema = schema,
                uischema = VerticalLayout(elements = listOf(
                    ControlElement("#/properties/contact"),
                    ControlElement("#/properties/next"),
                )),
                config = mapOf(
                    "enableFilterErrorsBeforeTouch" to true,
                    "filterErrorKeywordsBeforeTouch" to listOf("minProperties"),
                ),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(translateError = { error, _, _ ->
                    "ERR:${error.keyword}:${error.instancePath}"
                }),
                onChange = { changes++ },
            )
        }
        onAllNodes(hasText("ERR:minProperties:/contact", substring = true)).assertCountEquals(0)
        onAllNodes(hasSetTextAction() and hasText("5550100", substring = true)).onFirst()
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onAllNodes(hasSetTextAction() and hasText("Boston", substring = true)).onFirst()
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onAllNodes(hasText("ERR:minProperties:/contact", substring = true)).assertCountEquals(0)
        onAllNodes(hasSetTextAction() and hasText("after", substring = true)).onFirst()
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onAllNodes(hasText("ERR:minProperties:/contact", substring = true)).assertCountEquals(1)
        assertEquals(0, changes)
    }
}
