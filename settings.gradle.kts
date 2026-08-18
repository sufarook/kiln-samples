pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        // Dev convenience: lets you try the samples against a locally-built Kiln
        // (`./gradlew publishToMavenLocal` in the Kiln repo) before a version is on
        // Maven Central. A real consumer needs only the three repositories above.
        mavenLocal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
}

rootProject.name = "kiln-samples"

include(":sample-android")
include(":composeApp")
