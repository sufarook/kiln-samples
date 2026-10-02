package io.github.sufarook.kiln.sample.compose

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.sufarook.kiln.runtime.JvmDatabaseDriverFactory

fun main() = application {
    val driver = JvmDatabaseDriverFactory().create("kiln-compose-sample.db")
    val store = TaskStore(driver)
    Window(onCloseRequest = ::exitApplication, title = "Kiln — Compose Multiplatform") {
        App(store)
    }
}
