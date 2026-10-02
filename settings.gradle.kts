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

// Each sample lives in its own self-contained folder under samples/.
// projectDir is set explicitly so the task paths stay short — you run
// `:compose-multiplatform:assembleDebug`, not `:samples:compose-multiplatform:...`.
include(":android-views")
project(":android-views").projectDir = file("samples/android-views")

include(":compose-multiplatform")
project(":compose-multiplatform").projectDir = file("samples/compose-multiplatform")

include(":expense-tracker")
project(":expense-tracker").projectDir = file("samples/expense-tracker")
