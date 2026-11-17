package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.Composable

import com.chobantonov.jsonfroms.extended.renderers.ExtendedFilePickerEvents
import com.chobantonov.jsonfroms.extended.renderers.browserFilePickerEvents

@Composable
internal actual fun createDemoFilePicker(): ExtendedFilePickerEvents? = browserFilePickerEvents()
