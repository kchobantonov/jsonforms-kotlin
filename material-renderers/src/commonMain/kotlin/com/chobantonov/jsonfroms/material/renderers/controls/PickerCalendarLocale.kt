package com.chobantonov.jsonfroms.material.renderers.controls

import androidx.compose.material3.CalendarLocale

/** Match the calendar grid and its platform formatter to the form locale. */
internal expect fun pickerCalendarLocale(language: String): CalendarLocale
