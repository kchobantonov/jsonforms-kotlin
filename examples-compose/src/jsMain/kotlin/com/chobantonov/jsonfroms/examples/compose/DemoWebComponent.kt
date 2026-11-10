package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlin.js.JSON

internal actual val supportsWebComponentView: Boolean = true

@Composable
internal actual fun WebComponentOverlay(
    input: WebComponentInput,
    onClose: () -> Unit,
    onDataChange: (String) -> Unit,
) {
    val closeHandler = rememberUpdatedState(onClose)
    val changeHandler = rememberUpdatedState(onDataChange)
    val overlay = remember {
        val root: dynamic = js("document.createElement('div')")
        root.style.position = "fixed"
        root.style.inset = "0"
        root.style.zIndex = "1000"
        root.style.background = "#faf5fd"
        root.style.display = "flex"
        root.style.flexDirection = "column"

        val bar: dynamic = js("document.createElement('div')")
        bar.style.cssText = "height:48px;display:flex;align-items:center;gap:16px;padding:0 16px;background:#f1e9f7;font:14px sans-serif"
        val back: dynamic = js("document.createElement('button')")
        back.textContent = "← Compose form"
        back.style.cssText = "border:0;border-radius:6px;padding:8px 12px;background:#6750a4;color:white;cursor:pointer"
        bar.appendChild(back)
        val title: dynamic = js("document.createElement('span')")
        title.textContent = "Web component"
        bar.appendChild(title)
        root.appendChild(bar)

        val form: dynamic = js("document.createElement('jsonforms-kotlin-compose')")
        form.style.cssText = "display:block;flex:1;min-height:0;overflow:auto;padding:16px"
        root.appendChild(form)
        arrayOf(root, back, form)
    }
    val root: dynamic = overlay[0]
    val back: dynamic = overlay[1]
    val form: dynamic = overlay[2]

    DisposableEffect(Unit) {
        val onClick: (dynamic) -> Unit = { closeHandler.value() }
        val onChange: (dynamic) -> Unit = { event ->
            val value = JSON.stringify(event.detail.data)
            changeHandler.value(value)
        }
        back.addEventListener("click", onClick)
        form.addEventListener("jsonforms-change", onChange)
        val body: dynamic = js("document.body")
        body.appendChild(root)

        val registry: dynamic = js("customElements")
        if (registry.get("jsonforms-kotlin-compose") == null) {
            val script: dynamic = js("document.createElement('script')")
            script.src = "webcomponent/jsonforms-kotlin-webcomponent.js"
            body.appendChild(script)
        }
        onDispose {
            back.removeEventListener("click", onClick)
            form.removeEventListener("jsonforms-change", onChange)
            root.remove()
            // The registration remains available for later toggles.
        }
    }
    SideEffect {
        setAttribute(form, "data", input.data)
        setAttribute(form, "schema", input.schema)
        setAttribute(form, "uischema", input.uischema)
        if (input.readonly) form.setAttribute("readonly", "") else form.removeAttribute("readonly")
    }
}

private fun setAttribute(element: dynamic, name: String, value: String?) {
    if (value == null) {
        element.removeAttribute(name)
    } else if (element.getAttribute(name) != value) {
        element.setAttribute(name, value)
    }
}
