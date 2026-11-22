package com.chobantonov.jsonfroms.webcomponent

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.viewinterop.HtmlElementView
import androidx.compose.ui.window.ComposeViewport
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfControl
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.mapStateToControlProps
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.w3c.dom.HTMLElement
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalComposeUiApi::class)
class DomIslandInteropJsTest {
    @Test
    fun delegatedControlIslandReleasesWhenShadowHostDisconnects(): Promise<Unit> {
        val host = document.createElement("div") as HTMLElement
        val shadow: dynamic = host.asDynamic().attachShadow(js("({mode: 'open'})"))
        val container = document.createElement("div") as HTMLElement
        container.style.width = "300px"
        container.style.height = "200px"
        shadow.appendChild(container)
        document.body!!.appendChild(host)
        val connected = mutableStateOf(true)
        val scroll = ScrollState(0)
        var released = 0
        var writes = 0
        var initialTop = 0.0
        val island = JsonFormsRendererRegistryEntry(
            tester = { ui, _, _ -> if (ui is ControlElement) 1000 else -1 },
            renderer = ComposeJsonFormsRenderer { props ->
                val control = props.uischema as ControlElement
                val mapped = mapStateToControlProps(props.state, OwnPropsOfControl(
                    controlUiSchema = control,
                    own = OwnPropsOfRenderer(
                        uischema = control, schema = props.schema, enabled = props.enabled,
                        path = props.path, renderers = props.renderers, cells = props.cells,
                    ),
                ))
                Box(Modifier.size(50.dp).clipToBounds()) {
                    HtmlElementView(
                        factory = {
                            (document.createElement("button") as HTMLElement).apply {
                                setAttribute("data-jsonforms-renderer-island", "")
                                setAttribute("data-path", mapped.path)
                            }
                        },
                        modifier = Modifier.requiredSize(100.dp),
                        onRelease = { released++ },
                    )
                }
            },
        )
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"name":{"type":"string"}}}"""
        ).jsonObject)
        ComposeViewport(viewportContainer = container) {
            if (connected.value) Column(Modifier.size(150.dp, 70.dp).verticalScroll(scroll)) {
                Spacer(Modifier.height(10.dp))
                JsonForms(
                    data = Json.parseToJsonElement("""{"name":"Ada"}"""), schema = schema,
                    uischema = ControlElement("#/properties/name"),
                    renderers = materialRenderers + island,
                    onChange = { writes++ },
                )
                Spacer(Modifier.height(150.dp))
            }
        }
        return afterFrame().then {
            val button = assertNotNull(container.querySelector("[data-jsonforms-renderer-island]") as? HTMLElement)
            assertEquals("name", button.getAttribute("data-path"))
            val bounds = button.getBoundingClientRect()
            assertTrue(bounds.width >= 90.0 && bounds.height >= 90.0)
            initialTop = bounds.top
            val consumed = scroll.dispatchRawDelta(30f)
            assertTrue(consumed > 0f, "scroll consumed $consumed")
            afterFrame()
        }.then {
            val button = assertNotNull(container.querySelector("[data-jsonforms-renderer-island]") as? HTMLElement)
            val scrolledTop = button.getBoundingClientRect().top
            assertTrue(scrolledTop < initialTop - 20.0,
                "DOM island top stayed at $scrolledTop after starting at $initialTop")
            button.focus()
            assertEquals(button, shadow.activeElement)
            connected.value = false
            host.remove()
            afterFrame()
        }.then {
            assertNull(container.querySelector("[data-jsonforms-renderer-island]"))
            assertEquals(1, released)
            document.body!!.appendChild(host)
            connected.value = true
            afterFrame()
        }.then {
            assertNotNull(container.querySelector("[data-jsonforms-renderer-island]"))
            assertEquals(1, released)
            assertEquals(0, writes)
            connected.value = false
            host.remove()
            afterFrame()
        }.then {
            assertEquals(2, released)
            assertNull(container.querySelector("[data-jsonforms-renderer-island]"))
        }
    }

    @Test
    fun htmlElementLeavesDomAndReleasesOnCompositionRemoval(): Promise<Unit> {
        val container = document.createElement("div") as HTMLElement
        container.style.width = "300px"
        container.style.height = "200px"
        document.body!!.appendChild(container)
        val connected = mutableStateOf(true)
        var released = 0
        ComposeViewport(viewportContainer = container) {
            if (connected.value) {
                Box(Modifier.size(50.dp).clipToBounds()) {
                    HtmlElementView(
                        factory = {
                            (document.createElement("button") as HTMLElement).apply {
                                setAttribute("data-jsonforms-island-probe", "")
                            }
                        },
                        modifier = Modifier.requiredSize(100.dp),
                        onRelease = { released++ },
                    )
                }
            }
        }
        return afterFrame().then {
            val button = assertNotNull(container.querySelector("[data-jsonforms-island-probe]") as? HTMLElement)
            val bounds = button.getBoundingClientRect()
            assertTrue(bounds.width >= 90.0 && bounds.height >= 90.0)
            val wrapperStyle = button.parentElement?.getAttribute("style").orEmpty()
            assertTrue(Regex("""clip-path: inset\([1-9]\d*px""").containsMatchIn(wrapperStyle),
                wrapperStyle)
            button.focus()
            assertEquals(button, document.activeElement)
            connected.value = false
            afterFrame()
        }.then {
            assertNull(container.querySelector("[data-jsonforms-island-probe]"))
            assertEquals(1, released)
            connected.value = true
            afterFrame()
        }.then {
            assertNotNull(container.querySelector("[data-jsonforms-island-probe]"))
            assertEquals(1, released)
            container.remove()
        }
    }

    private fun afterFrame() = Promise<Unit> { resolve, _ ->
        window.setTimeout({ resolve(Unit) }, 200)
    }
}
