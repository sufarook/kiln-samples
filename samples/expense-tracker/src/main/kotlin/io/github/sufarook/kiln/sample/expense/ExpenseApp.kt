package io.github.sufarook.kiln.sample.expense

import android.app.Application
import io.github.sufarook.kiln.runtime.AndroidDatabaseDriverFactory

class ExpenseApp : Application() {

    lateinit var store: ExpenseStore
        private set

    override fun onCreate() {
        super.onCreate()
        val driver = AndroidDatabaseDriverFactory(this).create("expenses.db")
        store = ExpenseStore(driver)
    }
}
