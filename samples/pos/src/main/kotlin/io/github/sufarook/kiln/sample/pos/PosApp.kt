package io.github.sufarook.kiln.sample.pos

import android.app.Application
import io.github.sufarook.kiln.runtime.AndroidDatabaseDriverFactory

class PosApp : Application() {

    lateinit var store: PosStore
        private set

    lateinit var reports: PosReports
        private set

    override fun onCreate() {
        super.onCreate()
        val driver = AndroidDatabaseDriverFactory(this).create("pos.db")
        store = PosStore(driver)
        reports = PosReports(driver)
    }
}
