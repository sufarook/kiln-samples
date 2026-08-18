plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose.compiler)
    // Kiln runs the processor once over commonMain, so one generated repository
    // is shared by every target — Android and iOS both use the same code.
    alias(libs.plugins.kiln)
}

kotlin {
    jvmToolchain(17)

    androidTarget()

    // Compose Multiplatform 1.11+ no longer publishes an iosX64 (Intel simulator)
    // artifact, so only the Apple Silicon targets are declared here.
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.kotlinx.coroutines.core)
            // Kiln's annotations + runtime are added by the plugin — no need to
            // declare them here.
        }
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.sqldelight.android.driver)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
    }
}

android {
    namespace = "io.github.sufarook.kiln.sample.compose"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        applicationId = "io.github.sufarook.kiln.sample.compose"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
