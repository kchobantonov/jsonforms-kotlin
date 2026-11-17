package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.store.ValidationMode
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import kotlin.test.Test
import kotlin.test.assertEquals

/** Repeatable comparison of the former per-container scan and indexed count lookup. */
class ContainerErrorIndexBenchmarkTest {
    @Test fun manyContainersAndErrors() {
        val contexts = (0 until 242).map { "section.$it" }
        val scope = listOf(emptyList<String>())
        for (errorCount in listOf(200, 1_000)) {
            val errors = List(errorCount) { index ->
                ErrorObject(instancePath = "/section/${index % contexts.size}/field", message = "bad")
            }
            val paths = List(errorCount) { index ->
                listOf("section", (index % contexts.size).toString(), "field")
            }
            val scopes = contexts.indices.map { listOf("section", it.toString()) }
            repeat(5) { ContainerErrorIndex.build(errors, emptyList(), ValidationMode.ValidateAndShow) }
            val builtAt = System.nanoTime()
            val indexed = ContainerErrorIndex.build(errors, emptyList(), ValidationMode.ValidateAndShow)
            val buildNanos = System.nanoTime() - builtAt

            fun scanCounts(): Int = scopes.sumOf { prefix ->
                paths.count { path -> path.size >= prefix.size &&
                    path.subList(0, prefix.size) == prefix }
            }
            fun indexedCounts(): Int = contexts.sumOf { context -> indexed.countAny(scope, context) }

            repeat(5) { assertEquals(errorCount, scanCounts()); assertEquals(errorCount, indexedCounts()) }
            val passes = 30
            val scanStart = System.nanoTime()
            var scanTotal = 0
            repeat(passes) { scanTotal += scanCounts() }
            val scanNanos = System.nanoTime() - scanStart
            val indexedStart = System.nanoTime()
            var indexedTotal = 0
            repeat(passes) { indexedTotal += indexedCounts() }
            val indexedNanos = System.nanoTime() - indexedStart
            assertEquals(errorCount * passes, scanTotal)
            assertEquals(scanTotal, indexedTotal)
            println("container-index benchmark errors=$errorCount containers=${contexts.size} " +
                "build-ns=$buildNanos scan-ns=$scanNanos indexed-ns=$indexedNanos passes=$passes")
        }
    }
}
