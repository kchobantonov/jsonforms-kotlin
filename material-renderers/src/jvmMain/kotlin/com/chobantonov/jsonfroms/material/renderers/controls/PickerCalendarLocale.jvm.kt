package com.chobantonov.jsonfroms.material.renderers.controls

import androidx.compose.material3.CalendarLocale
import java.util.Locale

internal actual fun pickerCalendarLocale(language: String): CalendarLocale = Locale.forLanguageTag(language)
