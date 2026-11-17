@file:OptIn(kotlinx.cinterop.BetaInteropApi::class)

package com.chobantonov.jsonfroms.extended.renderers

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterCurrencyStyle
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.NSNumberFormatterPercentStyle
import platform.Foundation.NSTimeZone
import platform.Foundation.create

internal actual fun formatExpressionNumber(value: Double, locale: String, fractionDigits: Int?): String =
    NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterDecimalStyle
        this.locale = NSLocale(localeIdentifier = locale)
        if (fractionDigits != null) {
            minimumFractionDigits = fractionDigits.toULong()
            maximumFractionDigits = fractionDigits.toULong()
        }
    }.stringFromNumber(NSNumber(value)) ?: throw IllegalArgumentException("number formatting failed")

internal actual fun formatExpressionCurrency(value: Double, currencyCode: String, locale: String): String =
    NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterCurrencyStyle
        this.locale = NSLocale(localeIdentifier = locale)
        this.currencyCode = currencyCode
    }.stringFromNumber(NSNumber(value)) ?: throw IllegalArgumentException("currency formatting failed")

internal actual fun formatExpressionPercent(value: Double, locale: String): String =
    NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterPercentStyle
        this.locale = NSLocale(localeIdentifier = locale)
    }.stringFromNumber(NSNumber(value)) ?: throw IllegalArgumentException("percent formatting failed")

internal actual fun formatExpressionTemporal(epochMillis: Long, locale: String, kind: TemporalFormat, fixedUtc: Boolean): String =
    NSDateFormatter().apply {
        this.locale = NSLocale(localeIdentifier = locale)
        if (kind != TemporalFormat.TIME) dateStyle = NSDateFormatterMediumStyle
        if (kind != TemporalFormat.DATE) timeStyle = NSDateFormatterShortStyle
        if (fixedUtc) timeZone = NSTimeZone.create("UTC") ?: throw IllegalArgumentException("UTC is unavailable")
    }.stringFromDate(NSDate(timeIntervalSinceReferenceDate = epochMillis / 1000.0 - 978307200.0))

internal actual fun formatExpressionLocalTemporal(localDateTime: String, locale: String, kind: TemporalFormat): String {
    val parsed = NSDateFormatter().apply {
        dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS"
        this.locale = NSLocale(localeIdentifier = "en_US_POSIX")
        // Common parsing validates the fields; local-zone DST gaps follow host normalization.
        lenient = true
    }.dateFromString(localDateTime) ?: throw IllegalArgumentException("invalid local date-time")
    return NSDateFormatter().apply {
        this.locale = NSLocale(localeIdentifier = locale)
        if (kind != TemporalFormat.TIME) dateStyle = NSDateFormatterMediumStyle
        if (kind != TemporalFormat.DATE) timeStyle = NSDateFormatterShortStyle
    }.stringFromDate(parsed)
}

internal actual fun viewerOffsetMillis(epochMillis: Long): Int =
    NSDateFormatter().timeZone.secondsFromGMTForDate(
        NSDate(timeIntervalSinceReferenceDate = epochMillis / 1000.0 - 978307200.0)
    ).toInt() * 1000
