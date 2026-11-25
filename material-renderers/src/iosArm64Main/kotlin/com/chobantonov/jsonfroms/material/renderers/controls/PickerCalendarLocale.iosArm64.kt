package com.chobantonov.jsonfroms.material.renderers.controls

import androidx.compose.material3.CalendarLocale
import platform.Foundation.NSLocale

internal actual fun pickerCalendarLocale(language: String): CalendarLocale = NSLocale(language)
