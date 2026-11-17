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
    onExitFormOnly: () -> Unit,
    onDataChange: (String, Boolean) -> Unit,
    onFormAction: (String, String, Boolean) -> Unit,
) {
    val closeHandler = rememberUpdatedState(onClose)
    val exitHandler = rememberUpdatedState(onExitFormOnly)
    val changeHandler = rememberUpdatedState(onDataChange)
    val formActionHandler = rememberUpdatedState(onFormAction)
    val overlay = remember {
        val root: dynamic = js("document.createElement('div')")
        root.style.position = "fixed"
        root.style.inset = "0"
        root.style.zIndex = "1000"
        root.style.background = "#faf5fd"
        root.style.display = "flex"
        root.style.flexDirection = "column"
        root.setAttribute("data-jsonforms-demo-overlay", "")

        val bar: dynamic = js("document.createElement('div')")
        bar.style.cssText = "height:48px;display:flex;align-items:center;gap:16px;padding:0 16px;background:#f1e9f7;font:14px sans-serif"
        bar.setAttribute("data-jsonforms-demo-bar", "")
        val back: dynamic = js("document.createElement('button')")
        back.textContent = "← Compose form"
        back.style.cssText = "border:0;border-radius:6px;padding:8px 12px;background:#6750a4;color:white;cursor:pointer"
        bar.appendChild(back)
        val title: dynamic = js("document.createElement('span')")
        title.textContent = "Web component"
        bar.appendChild(title)
        val registryNotice: dynamic = js("document.createElement('span')")
        registryNotice.style.cssText = "font:12px sans-serif;color:#5f4b73;white-space:nowrap;overflow:hidden;text-overflow:ellipsis"
        registryNotice.setAttribute("role", "status")
        bar.appendChild(registryNotice)
        root.appendChild(bar)

        val exit: dynamic = js("document.createElement('button')")
        exit.style.cssText = "position:absolute;top:8px;inset-inline-end:8px;z-index:1;border:0;border-radius:6px;padding:8px 12px;background:#6750a4;color:white;cursor:pointer"
        exit.setAttribute("data-jsonforms-demo-exit", "")
        root.appendChild(exit)

        val frame: dynamic = js("document.createElement('iframe')")
        frame.style.cssText = "display:block;flex:1;min-height:0;width:100%;border:0"
        frame.setAttribute("title", "Web component form")
        frame.src = "webcomponent/embed.html"
        root.appendChild(frame)
        arrayOf(root, bar, back, exit, title, registryNotice, frame)
    }
    val root: dynamic = overlay[0]
    val bar: dynamic = overlay[1]
    val back: dynamic = overlay[2]
    val exit: dynamic = overlay[3]
    val title: dynamic = overlay[4]
    val registryNotice: dynamic = overlay[5]
    val frame: dynamic = overlay[6]
    val latestInput = rememberUpdatedState(input)

    DisposableEffect(Unit) {
        val onClick: (dynamic) -> Unit = { closeHandler.value() }
        val onExit: (dynamic) -> Unit = { exitHandler.value() }
        val onChange: (dynamic) -> Unit = { event ->
            val value = JSON.stringify(event.detail.data)
            changeHandler.value(value, event.detail.dataProvided != false)
        }
        val onAction: (dynamic) -> Unit = { event ->
            formActionHandler.value(event.detail.action as String,
                JSON.stringify(event.detail.params),
                event.currentTarget.getAttribute("theme") == "dark")
        }
        var mountedForm: dynamic = null
        val onFrameLoad: (dynamic) -> Unit = {
            val form: dynamic = frame.contentDocument?.querySelector("jsonforms-kotlin-compose")
            if (form != null) {
                if (mountedForm != null) {
                    mountedForm.removeEventListener("jsonforms-change", onChange)
                    mountedForm.removeEventListener("jsonforms-action", onAction)
                }
                mountedForm = form
                mountedForm.addEventListener("jsonforms-change", onChange)
                mountedForm.addEventListener("jsonforms-action", onAction)
                applyWebComponentInput(form, latestInput.value)
            }
        }
        back.addEventListener("click", onClick)
        exit.addEventListener("click", onExit)
        frame.addEventListener("load", onFrameLoad)
        val body: dynamic = js("document.body")
        body.appendChild(root)
        if (frame.contentDocument?.readyState == "complete") onFrameLoad(null)
        onDispose {
            back.removeEventListener("click", onClick)
            exit.removeEventListener("click", onExit)
            frame.removeEventListener("load", onFrameLoad)
            if (mountedForm != null) {
                mountedForm.removeEventListener("jsonforms-change", onChange)
                mountedForm.removeEventListener("jsonforms-action", onAction)
            }
            root.remove()
        }
    }
    SideEffect {
        root.dataset.formOnly = input.formOnly.toString()
        val bulgarian = input.locale == DemoLocale.Bulgarian
        back.textContent = if (bulgarian) "← Compose формуляр" else "← Compose form"
        title.textContent = if (bulgarian) "Уеб компонент" else "Web component"
        val notice = webComponentRegistryTesterNotice(input.locale, input.unavailableRegistryTesters)
        registryNotice.textContent = notice ?: ""
        registryNotice.style.display = if (notice != null) "inline" else "none"
        exit.textContent = if (bulgarian) "Покажи целия интерфейс" else "Show full UI"
        bar.style.display = if (input.formOnly) "none" else "flex"
        exit.style.display = if (input.formOnly) "block" else "none"
        val direction = if (input.direction == DemoDirection.Rtl) "rtl" else "ltr"
        setAttribute(root, "dir", direction)
        val form: dynamic = frame.contentDocument?.querySelector("jsonforms-kotlin-compose")
        if (form != null) applyWebComponentInput(form, input)
    }
}

private fun applyWebComponentInput(form: dynamic, input: WebComponentInput) {
    setAttribute(form, "dir", if (input.direction == DemoDirection.Rtl) "rtl" else "ltr")
    setAttribute(form, "data", input.data)
    setAttribute(form, "data-provided", if (input.dataProvided) null else "false")
    setAttribute(form, "schema", input.schema)
    setAttribute(form, "uischema", input.uischema)
    setAttribute(form, "uischemas", input.uischemas)
    setAttribute(form, "config", input.config)
    setAttribute(form, "validation-mode", when (input.validationMode) {
        com.chobantonov.jsonfroms.core.store.ValidationMode.ValidateAndShow -> "validate-and-show"
        com.chobantonov.jsonfroms.core.store.ValidationMode.ValidateAndHide -> "validate-and-hide"
        com.chobantonov.jsonfroms.core.store.ValidationMode.NoValidation -> "no-validation"
    })
    setAttribute(form, "translations", input.translations)
    setAttribute(form, "additional-errors", input.additionalErrors)
    setAttribute(form, "lang", input.formLocale.languageTag)
    setAttribute(form, "theme", if (input.darkTheme) "dark" else "light")
    if (input.readonly) form.setAttribute("readonly", "") else form.removeAttribute("readonly")
}

private fun setAttribute(element: dynamic, name: String, value: String?) {
    if (value == null) {
        element.removeAttribute(name)
    } else if (element.getAttribute(name) != value) {
        element.setAttribute(name, value)
    }
}
