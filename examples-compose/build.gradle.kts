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
            implementation(project(":cupertino-renderers"))
            implementation(project(":extended-renderers"))
            implementation(project(":examples"))
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.multiplatform.settings.no.arg)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.cupertino)
            implementation(compose.ui)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.multiplatform.settings.test)
            implementation(libs.compose.ui.test)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }

        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.chobantonov.jsonfroms.examples.compose.desktop.MainKt"
    }
}

tasks.named<Copy>("jsProcessResources") {
    // Mounted UI tests never load the demo webcomponent. Skipping its production
    // bundle keeps the browser test build independent of the demo distribution.
    if (providers.gradleProperty("jsonforms.skipWebcomponentResources").orNull != "true") {
        dependsOn(":webcomponent:jsBrowserDistribution")
        from(project(":webcomponent").layout.buildDirectory.dir("dist/js/productionExecutable")) {
            into("webcomponent")
            exclude("index.html")
        }
    }
}
