package com.chobantonov.jsonfroms.examples.compose

internal fun webComponentRegistryTesterNotice(locale: DemoLocale, count: Int): String? =
    if (count <= 0) null else demoChromeText(DemoTextKey.WebComponentRegistryTesterNotice, locale)
