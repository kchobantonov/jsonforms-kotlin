package com.chobantonov.jsonfroms.examples.compose

import com.chobantonov.jsonfroms.extended.renderers.ExtendedFilePicker
import com.chobantonov.jsonfroms.extended.renderers.browserFilePicker

internal actual fun createDemoFilePicker(): ExtendedFilePicker? = browserFilePicker()
