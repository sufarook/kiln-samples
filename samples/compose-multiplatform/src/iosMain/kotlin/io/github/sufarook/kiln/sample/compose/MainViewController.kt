package io.github.sufarook.kiln.sample.compose

import androidx.compose.ui.window.ComposeUIViewController
import io.github.sufarook.kiln.runtime.IosDatabaseDriverFactory

/**
 * Entry point used by iosApp/iOSApp.swift. The same `App` composable and the
 * same generated repositories as Android — only the driver differs.
 */
fun MainViewController() = ComposeUIViewController {
    val driver = IosDatabaseDriverFactory().create("kiln-compose-sample.db")
    App(TaskStore(driver))
}
