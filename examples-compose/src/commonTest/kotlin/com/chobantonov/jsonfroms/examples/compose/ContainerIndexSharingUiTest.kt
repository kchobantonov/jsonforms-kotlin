package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.compose.useJsonForms
import com.chobantonov.jsonfroms.core.mappers.ContainerErrorIndex
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class ContainerIndexSharingUiTest {
    @Test fun materialGroupsShareProviderIndexThroughHostErrorChange() = scenario(false)
    @Test fun cupertinoGroupsShareProviderIndexThroughHostErrorChange() = scenario(true)

    private fun scenario(cupertino: Boolean) = runComposeUiTest {
        val seen = mutableMapOf<String, ContainerErrorIndex>()
        val groupProbe = JsonFormsRendererRegistryEntry(
            tester = { ui, _, _ -> if (ui is GroupElement) 1000 else -1 },
            renderer = ComposeJsonFormsRenderer { props ->
                val label = (props.uischema as GroupElement).label.orEmpty()
                val index = useJsonForms().containerErrorIndex
                SideEffect { seen[label] = index }
                Text(label)
            },
        )
        val ui = VerticalLayout(elements = listOf(
            GroupElement(label = "First"), GroupElement(label = "Second"),
        ))
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"second":{"type":"string"}}}"""
        ).jsonObject)
        var host by mutableStateOf(listOf(ErrorObject(instancePath = "/first", message = "first")))
        setContent {
            JsonForms(data = Json.parseToJsonElement("{}"), schema = schema, uischema = ui,
                renderers = (if (cupertino) cupertinoRenderers else materialRenderers) + groupProbe,
                additionalErrors = host)
        }
        onNodeWithText("First").assertExists()
        onNodeWithText("Second").assertExists()
        waitForIdle()
        val before = seen.getValue("First")
        assertSame(before, seen.getValue("Second"))
        assertTrue(before.containsAny(listOf(listOf("first"))))

        runOnIdle { host = listOf(ErrorObject(instancePath = "/second", message = "second")) }
        waitForIdle()
        val after = seen.getValue("First")
        assertNotSame(before, after)
        assertSame(after, seen.getValue("Second"))
        assertTrue(after.containsAny(listOf(listOf("second"))))
    }
}
