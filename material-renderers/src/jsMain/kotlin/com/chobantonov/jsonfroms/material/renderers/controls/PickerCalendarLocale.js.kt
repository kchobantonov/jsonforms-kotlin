package com.chobantonov.jsonfroms.material.renderers.controls

import androidx.compose.material3.CalendarLocale
import androidx.compose.ui.text.intl.Locale

internal actual fun pickerCalendarLocale(language: String): CalendarLocale = Locale(language)
