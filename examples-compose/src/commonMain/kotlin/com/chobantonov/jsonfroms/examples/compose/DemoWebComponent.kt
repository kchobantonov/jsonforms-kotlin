package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.Composable
import com.chobantonov.jsonfroms.core.store.ValidationMode

internal data class WebComponentInput(
    val data: String,
    val dataProvided: Boolean,
    val schema: String?,
    val uischema: String?,
    val uischemas: String?,
    val config: String?,
    val translations: String?,
    val additionalErrors: String,
    val readonly: Boolean,
    val validationMode: ValidationMode,
    val direction: DemoDirection,
    val formOnly: Boolean,
    val locale: DemoLocale,
    val formLocale: DemoLocale,
    val darkTheme: Boolean,
    val unavailableRegistryTesters: Int,
)

internal expect val supportsWebComponentView: Boolean

@Composable
internal expect fun WebComponentOverlay(
    input: WebComponentInput,
    onClose: () -> Unit,
    onExitFormOnly: () -> Unit,
    onDataChange: (String, Boolean) -> Unit,
    onFormAction: (String, String, Boolean) -> Unit,
)
