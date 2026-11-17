package com.chobantonov.jsonfroms.examples.compose

import com.chobantonov.jsonfroms.examples.StateProps
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal data class DemoFormActionEffect(
    val state: StateProps? = null,
    val settings: DemoSettings? = null,
)

/** Apply the same demo Button action in the Compose and Web Component hosts. */
internal fun demoFormActionEffect(
    state: StateProps,
    settings: DemoSettings,
    action: String,
    params: JsonObject,
    currentFormDarkTheme: Boolean,
): DemoFormActionEffect = when (action) {
    "setLocale", "changeLang" -> DemoFormActionEffect(settings =
        requestedDemoLocale(action, params)?.let { settings.copy(formLocaleOverride = it) })
    "toggleMode" -> DemoFormActionEffect(settings = settings.copy(
        formDarkThemeOverride = !currentFormDarkTheme))
    "reportServer" -> DemoFormActionEffect(state = state.copy(
        additionalErrors = replaceServerErrors(state.additionalErrors, params)))
    "clearServer" -> DemoFormActionEffect(state = state.copy(
        additionalErrors = clearServerErrors(state.additionalErrors)))
    else -> {
        val data = (state.data as? JsonObject).orEmpty()
        val status = when (action) {
            "applyStatus", "simulateHttpSubmit" ->
                demoActionStatus(params, settings.formLocaleOverride ?: settings.locale)
            "runScript" -> "Updated from host action"
            else -> "Action: $action"
        }
        DemoFormActionEffect(state = state.copy(
            data = JsonObject(data + mapOf(
                "status" to JsonPrimitive(status),
                "lastTriggeredBy" to JsonPrimitive(action),
            )),
            dataProvided = true,
        ))
    }
}
