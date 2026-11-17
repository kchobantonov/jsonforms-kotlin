package com.chobantonov.jsonfroms.core.i18n

import kotlin.test.Test
import kotlin.test.assertEquals

class ShippedRendererLocalesTest {
    @Test
    fun builtInRendererBundlesOnlyAddBulgarianToEnglishDefaults() {
        val bundles = listOf(
            controlRendererLocaleBundles,
            arrayChoiceLocaleBundles,
            mixedRendererLocaleBundles,
            tupleLocaleBundles,
            groupRendererLocaleBundles,
            confirmationLocaleBundles,
            temporalControlLocaleBundles("date"),
        )
        bundles.forEach { assertEquals(setOf("bg"), it.keys) }
    }
}
