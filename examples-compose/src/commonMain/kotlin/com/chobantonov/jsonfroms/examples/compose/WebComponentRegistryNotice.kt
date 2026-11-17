package com.chobantonov.jsonfroms.examples.compose

internal fun webComponentRegistryTesterNotice(locale: DemoLocale, count: Int): String? =
    if (count <= 0) null else if (locale == DemoLocale.Bulgarian) {
        "Функциите за избор на регистрирани UI схеми не са налични тук"
    } else {
        "Registered UI Schema tester functions are unavailable here"
    }
