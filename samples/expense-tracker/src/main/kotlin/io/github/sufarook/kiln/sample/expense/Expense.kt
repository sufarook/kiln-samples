package io.github.sufarook.kiln.sample.expense

import io.github.sufarook.kiln.annotations.Column
import io.github.sufarook.kiln.annotations.DbEntity
import io.github.sufarook.kiln.annotations.PrimaryKey
import io.github.sufarook.kiln.annotations.Relation

@DbEntity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @Column(name = "category_id", index = true) @Relation val categoryId: Long,
    val amount: Double,
    val note: String = "",
    val date: String,
    @Column(name = "is_recurring") val isRecurring: Boolean = false
)
