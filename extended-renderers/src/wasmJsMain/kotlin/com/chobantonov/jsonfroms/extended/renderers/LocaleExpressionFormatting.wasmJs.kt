package com.chobantonov.jsonfroms.extended.renderers

import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun formatExpressionNumber(value: Double, locale: String, fractionDigits: Int?): String =
    browserNumber(value, locale, fractionDigits ?: -1)

@OptIn(ExperimentalWasmJsInterop::class)
private fun browserNumber(value: Double, locale: String, fractionDigits: Int): String = js(
    "(new Intl.NumberFormat(locale, fractionDigits < 0 ? {} : { minimumFractionDigits: fractionDigits, maximumFractionDigits: fractionDigits })).format(value)"
)

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun formatExpressionCurrency(value: Double, currencyCode: String, locale: String): String = js(
    "(new Intl.NumberFormat(locale, { style: 'currency', currency: currencyCode })).format(value)"
)

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun formatExpressionPercent(value: Double, locale: String): String = js(
    "(new Intl.NumberFormat(locale, { style: 'percent' })).format(value)"
)

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun formatExpressionTemporal(epochMillis: Long, locale: String, kind: TemporalFormat, fixedUtc: Boolean): String =
    browserTemporal(epochMillis.toDouble(), locale, kind != TemporalFormat.TIME, kind != TemporalFormat.DATE, fixedUtc)

@OptIn(ExperimentalWasmJsInterop::class)
private fun browserTemporal(epochMillis: Double, locale: String, datePart: Boolean, timePart: Boolean, fixedUtc: Boolean): String = js(
    "(new Intl.DateTimeFormat(locale, Object.assign({}, datePart ? { dateStyle: 'medium' } : {}, timePart ? { timeStyle: 'short' } : {}, fixedUtc ? { timeZone: 'UTC' } : {}))).format(new Date(epochMillis))"
)

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun formatExpressionLocalTemporal(localDateTime: String, locale: String, kind: TemporalFormat): String =
    browserLocalTemporal(localDateTime, locale, kind != TemporalFormat.TIME, kind != TemporalFormat.DATE)

@OptIn(ExperimentalWasmJsInterop::class)
private fun browserLocalTemporal(localDateTime: String, locale: String, datePart: Boolean, timePart: Boolean): String = js(
    "(new Intl.DateTimeFormat(locale, Object.assign({}, datePart ? { dateStyle: 'medium' } : {}, timePart ? { timeStyle: 'short' } : {}))).format(new Date(localDateTime))"
)

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun viewerOffsetMillis(epochMillis: Long): Int =
    -browserOffsetMinutes(epochMillis.toDouble()) * 60_000

@OptIn(ExperimentalWasmJsInterop::class)
private fun browserOffsetMinutes(epochMillis: Double): Int = js("new Date(epochMillis).getTimezoneOffset()")
