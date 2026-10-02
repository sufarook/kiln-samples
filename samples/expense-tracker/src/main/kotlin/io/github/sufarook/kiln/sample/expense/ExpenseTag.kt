package io.github.sufarook.kiln.sample.expense

import io.github.sufarook.kiln.annotations.DbEntity
import io.github.sufarook.kiln.annotations.PrimaryKey
import io.github.sufarook.kiln.annotations.Relation

@DbEntity(tableName = "expense_tags")
data class ExpenseTag(
    @PrimaryKey @Relation val expenseId: Long,
    @PrimaryKey @Relation val tagId: Long
)
