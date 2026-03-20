@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose.compiler)
}

kotlin {
    androidLibrary {
        namespace = "com.chobantonov.jsonfroms.examples.compose"
        compileSdk = libs.versions.android.compile.sdk.get().toInt()
        minSdk = libs.versions.android.min.sdk.get().toInt()
    }

    jvm()

    js(IR) {
        browser {
            commonWebpackConfig {
                outputFileName = "examples-compose.js"
            }
        }
        binaries.executable()
    }

    wasmJs {
        browser()
        binaries.executable()
    }

    iosX64 {
        binaries.framework {
            baseName = "ExamplesComposeApp"
        }
    }
    iosArm64 {
        binaries.framework {
            baseName = "ExamplesComposeApp"
        }
    }
    iosSimulatorArm64 {
        binaries.framework {
            baseName = "ExamplesComposeApp"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core"))
            implementation(project(":compose"))
            implementation(project(":material-renderers"))
            implementation(project(":examples"))
            implementation(libs.kotlinx.serialization.json)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.chobantonov.jsonfroms.examples.compose.desktop.MainKt"
    }
}
