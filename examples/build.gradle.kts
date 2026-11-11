@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

val generateSpecExamples = tasks.register("generateSpecExamples") {
    val specDirectory = layout.projectDirectory.dir("spec")
    val generatedDirectory = layout.buildDirectory.dir("generated/spec-examples")
    inputs.dir(specDirectory)
    outputs.dir(generatedDirectory)
    doLast {
        val parser = JsonSlurper()
        val catalog = parser.parseText(specDirectory.file("catalog.json").asFile.readText()) as Map<*, *>
        val fixtures = (catalog["examples"] as List<*>).map { entry ->
            val metadata = entry as Map<*, *>
            val id = metadata["id"] as String
            val title = metadata["title"] as String
            linkedMapOf<String, Any?>("name" to "spec-$id", "label" to "Spec: $title").apply {
                for (key in listOf("data", "schema", "uischema", "config", "translations")) {
                    val file = specDirectory.file("$id/$key.json").asFile
                    if (file.exists()) this[key] = parser.parseText(file.readText())
                }
            }
        }
        fun literal(value: String): String = "\"" + value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("$", "\\$")
            .replace("\n", "\\n")
            .replace("\r", "\\r") + "\""
        val chunks = JsonOutput.toJson(fixtures).chunked(8000)
        val names = fixtures.joinToString("\n") { "    ${literal(it.getValue("name") as String)}," }
        val body = chunks.joinToString("\n") { "        ${literal(it)}," }
        val output = generatedDirectory.get().file("com/chobantonov/jsonfroms/examples/SpecExamplesGenerated.kt").asFile
        output.parentFile.mkdirs()
        output.writeText("""// Generated from examples/spec JSON by Gradle. Edit the JSON source files instead.
package com.chobantonov.jsonfroms.examples

const val specExamplesCommit = ${literal(catalog["commit"] as String)}

internal val specSnapshotExampleNames: Set<String> = setOf(
$names
)

internal val specSnapshotExamples: List<ExampleDescription> by lazy {
    parseSpecExamples(listOf(
$body
    ).joinToString(""))
}
""")
    }
}

kotlin {
    androidLibrary {
        namespace = "com.chobantonov.jsonfroms.examples"
        compileSdk = libs.versions.android.compile.sdk.get().toInt()
        minSdk = libs.versions.android.min.sdk.get().toInt()
    }

    jvm()

    js(IR) {
        browser()
    }

    wasmJs {
        browser()
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core"))
            implementation(libs.kotlinx.serialization.json)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

kotlin.sourceSets.getByName("commonMain").kotlin.srcDir(generateSpecExamples)
