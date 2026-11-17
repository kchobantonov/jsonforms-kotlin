package com.chobantonov.jsonfroms.extended.renderers

internal actual fun formatExpressionNumber(value: Double, locale: String, fractionDigits: Int?): String =
    js("(new Intl.NumberFormat(locale, fractionDigits == null ? {} : { minimumFractionDigits: fractionDigits, maximumFractionDigits: fractionDigits })).format(value)") as String

internal actual fun formatExpressionCurrency(value: Double, currencyCode: String, locale: String): String =
    js("(new Intl.NumberFormat(locale, { style: 'currency', currency: currencyCode })).format(value)") as String

internal actual fun formatExpressionPercent(value: Double, locale: String): String =
    js("(new Intl.NumberFormat(locale, { style: 'percent' })).format(value)") as String

internal actual fun formatExpressionTemporal(epochMillis: Long, locale: String, kind: TemporalFormat, fixedUtc: Boolean): String {
    val milliseconds = epochMillis.toDouble()
    val datePart = kind != TemporalFormat.TIME
    val timePart = kind != TemporalFormat.DATE
    return js("(new Intl.DateTimeFormat(locale, Object.assign({}, datePart ? { dateStyle: 'medium' } : {}, timePart ? { timeStyle: 'short' } : {}, fixedUtc ? { timeZone: 'UTC' } : {}))).format(new Date(milliseconds))") as String
}

internal actual fun formatExpressionLocalTemporal(localDateTime: String, locale: String, kind: TemporalFormat): String {
    val datePart = kind != TemporalFormat.TIME
    val timePart = kind != TemporalFormat.DATE
    return js("(new Intl.DateTimeFormat(locale, Object.assign({}, datePart ? { dateStyle: 'medium' } : {}, timePart ? { timeStyle: 'short' } : {}))).format(new Date(localDateTime))") as String
}

internal actual fun viewerOffsetMillis(epochMillis: Long): Int {
    val milliseconds = epochMillis.toDouble()
    return -(js("new Date(milliseconds).getTimezoneOffset()") as Int) * 60_000
}
