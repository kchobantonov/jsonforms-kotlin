import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose.compiler)
}

android {
    namespace = "com.chobantonov.jsonfroms.examples.compose.android"
    compileSdk = libs.versions.android.compile.sdk.get().toInt()

    defaultConfig {
        applicationId = "com.chobantonov.jsonfroms.examples.compose.android"
        minSdk = libs.versions.android.min.sdk.get().toInt()
        targetSdk = libs.versions.android.target.sdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":examples-compose"))
    implementation(libs.androidx.activity.compose)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

val adbPath = providers.provider {
    val sdkDir = localProperties.getProperty("sdk.dir")
        ?: System.getenv("ANDROID_HOME")
        ?: System.getenv("ANDROID_SDK_ROOT")
        ?: error(
            "Android SDK location is missing. Set sdk.dir in local.properties or define ANDROID_HOME/ANDROID_SDK_ROOT.",
        )
    "${sdkDir}/platform-tools/adb"
}

tasks.register<Exec>("runDebug") {
    dependsOn("installDebug")
    commandLine(
        adbPath.get(),
        "shell",
        "am",
        "start",
        "-n",
        "com.chobantonov.jsonfroms.examples.compose.android/.MainActivity",
    )
}
