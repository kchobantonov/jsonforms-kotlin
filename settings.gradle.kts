pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "jsonforms-kotlin"

include(":core")
include(":compose")
include(":material-renderers")
include(":cupertino-renderers")
include(":examples")
include(":examples-compose")
include(":examples-compose-android")
