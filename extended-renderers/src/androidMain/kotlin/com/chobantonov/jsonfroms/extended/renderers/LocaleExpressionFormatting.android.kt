package com.chobantonov.jsonfroms.extended.renderers

import java.text.DateFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Locale
import java.util.TimeZone
import java.util.Date

internal actual fun formatExpressionNumber(value: Double, locale: String, fractionDigits: Int?): String =
    NumberFormat.getNumberInstance(Locale.forLanguageTag(locale)).apply {
        if (fractionDigits != null) {
            minimumFractionDigits = fractionDigits
            maximumFractionDigits = fractionDigits
        }
    }.format(value)

internal actual fun formatExpressionCurrency(value: Double, currencyCode: String, locale: String): String =
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag(locale)).apply {
        currency = Currency.getInstance(currencyCode)
    }.format(value)

internal actual fun formatExpressionPercent(value: Double, locale: String): String =
    NumberFormat.getPercentInstance(Locale.forLanguageTag(locale)).format(value)

internal actual fun formatExpressionTemporal(epochMillis: Long, locale: String, kind: TemporalFormat, fixedUtc: Boolean): String {
    val language = Locale.forLanguageTag(locale)
    val formatter = when (kind) {
        TemporalFormat.DATE -> DateFormat.getDateInstance(DateFormat.MEDIUM, language)
        TemporalFormat.TIME -> DateFormat.getTimeInstance(DateFormat.SHORT, language)
        TemporalFormat.DATE_TIME -> DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, language)
    }
    if (fixedUtc) formatter.timeZone = TimeZone.getTimeZone("UTC")
    return formatter.format(Date(epochMillis))
}

internal actual fun formatExpressionLocalTemporal(localDateTime: String, locale: String, kind: TemporalFormat): String {
    val parsed = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.ROOT).apply {
        // Calendar fields were validated in commonMain; normalize a daylight-saving gap like JS Date.
        isLenient = true
        timeZone = TimeZone.getDefault()
    }.parse(localDateTime) ?: throw IllegalArgumentException("invalid local date-time")
    return formatExpressionTemporal(parsed.time, locale, kind, fixedUtc = false)
}

internal actual fun viewerOffsetMillis(epochMillis: Long): Int =
    TimeZone.getDefault().getOffset(epochMillis)
