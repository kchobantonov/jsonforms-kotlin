package com.chobantonov.jsonfroms.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/** A quoted "true" is an authored string, not a Boolean option. */
fun strictControlBooleanOption(options: Map<String, Any?>, key: String): Boolean? = when (val value = options[key]) {
    is Boolean -> value
    is JsonPrimitive -> if (value.isString) null else value.booleanOrNull
    else -> null
}

fun initialControlFocusRequested(options: Map<String, Any?>): Boolean =
    strictControlBooleanOption(options, "focus") == true

@Composable
fun rememberInitialControlFocusRequester(
    path: String,
    options: Map<String, Any?>,
    focusableOnMount: Boolean,
): FocusRequester {
    val requester = remember(path) { FocusRequester() }
    val requestOnMount = remember(path) { focusableOnMount && initialControlFocusRequested(options) }
    LaunchedEffect(requester) {
        if (requestOnMount) requester.requestFocus()
    }
    return requester
}
