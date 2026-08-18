package io.github.sufarook.kiln.sample.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.sufarook.kiln.runtime.AndroidDatabaseDriverFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Kiln ships the Android driver factory; you pick the database file name.
        val driver = AndroidDatabaseDriverFactory(this).create("kiln-compose-sample.db")
        val store = TaskStore(driver)
        setContent { App(store) }
    }
}
