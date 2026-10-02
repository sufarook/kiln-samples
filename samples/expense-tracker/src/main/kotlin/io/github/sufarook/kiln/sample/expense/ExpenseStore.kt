package io.github.sufarook.kiln.sample.expense

import io.github.sufarook.kiln.runtime.KilnDriver
import io.github.sufarook.kiln.runtime.eq
import io.github.sufarook.kiln.runtime.gte
import io.github.sufarook.kiln.runtime.withTransaction
import kotlinx.coroutines.flow.Flow

class ExpenseStore(private val driver: KilnDriver) {

    private val categories = CategoryRepository(driver)
    private val expenses = ExpenseRepository(driver)
    private val tags = TagRepository(driver)
    private val expenseTags = ExpenseTagRepository(driver)

    init {
        KilnSchema.createAll(driver)
    }

    // ── Categories ───────────────────────────────────────────────────────────

    fun observeCategories(): Flow<List<Category>> = categories.observeAll()

    suspend fun addCategory(name: String, icon: String, color: Long) {
        categories.insert(Category(name = name, icon = icon, color = color))
    }

    private suspend fun categoryIdByName(name: String): Long =
        categories.findWhere { CategoryColumns.name eq name }.first().id

    // ── Expenses ─────────────────────────────────────────────────────────────

    fun observeExpenses(): Flow<List<Expense>> = expenses.observeAll()

    fun observeExpensesByCategory(categoryId: Long): Flow<List<Expense>> =
        expenses.observeByCategory(categoryId)

    suspend fun addExpense(
        categoryId: Long,
        amount: Double,
        note: String,
        date: String,
        isRecurring: Boolean = false,
        tagIds: List<Long> = emptyList()
    ) {
        driver.withTransaction {
            expenses.insert(
                Expense(
                    categoryId = categoryId,
                    amount = amount,
                    note = note,
                    date = date,
                    isRecurring = isRecurring
                )
            )
            if (tagIds.isNotEmpty()) {
                val expenseId = expenses.findAll().last().id
                tagIds.forEach { tagId ->
                    expenseTags.insert(ExpenseTag(expenseId = expenseId, tagId = tagId))
                }
            }
        }
    }

    suspend fun deleteExpense(expense: Expense) {
        driver.withTransaction {
            expenseTags.deleteByExpense(expense.id)
            expenses.delete(expense.id)
        }
    }

    suspend fun highValueExpenses(threshold: Double): List<Expense> =
        expenses.findWhere { ExpenseColumns.amount gte threshold }

    suspend fun recurringExpenses(): List<Expense> =
        expenses.findWhere { ExpenseColumns.isRecurring eq true }

    // ── Tags ─────────────────────────────────────────────────────────────────

    fun observeTags(): Flow<List<Tag>> = tags.observeAll()

    suspend fun addTag(name: String) {
        tags.insert(Tag(name = name))
    }

    suspend fun tagIdsForExpense(expenseId: Long): List<Long> =
        expenseTags.findByExpense(expenseId).map { it.tagId }

    suspend fun expenseIdsForTag(tagId: Long): List<Long> =
        expenseTags.findByTag(tagId).map { it.expenseId }

    // ── Dashboard ────────────────────────────────────────────────────────────

    suspend fun totalSpent(): Double =
        expenses.findAll().sumOf { it.amount }

    suspend fun totalByCategory(categoryId: Long): Double =
        expenses.findByCategory(categoryId).sumOf { it.amount }

    suspend fun expenseCount(): Long = expenses.count()

    // ── Seed data ────────────────────────────────────────────────────────────

    suspend fun seedIfEmpty() {
        if (categories.count() > 0L) return
        driver.withTransaction {
            addCategory("Food", "restaurant", 0xFF4CAF50)
            addCategory("Transport", "directions_car", 0xFF2196F3)
            addCategory("Entertainment", "movie", 0xFFFF9800)
            addCategory("Shopping", "shopping_cart", 0xFFE91E63)
            addCategory("Bills", "receipt_long", 0xFF9C27B0)

            addTag("business")
            addTag("personal")
            addTag("tax-deductible")
            addTag("recurring")
        }

        val food = categoryIdByName("Food")
        val transport = categoryIdByName("Transport")
        val entertainment = categoryIdByName("Entertainment")
        val shopping = categoryIdByName("Shopping")
        val bills = categoryIdByName("Bills")

        val allTags = tags.findAll()
        val personalId = allTags.first { it.name == "personal" }.id
        val businessId = allTags.first { it.name == "business" }.id
        val taxId = allTags.first { it.name == "tax-deductible" }.id
        val recurringId = allTags.first { it.name == "recurring" }.id

        addExpense(food, 45.50, "Dinner with friends", "2026-10-01", tagIds = listOf(personalId))
        addExpense(transport, 30.00, "Uber to office", "2026-10-01", tagIds = listOf(businessId, taxId))
        addExpense(entertainment, 15.99, "Netflix subscription", "2026-10-01", isRecurring = true, tagIds = listOf(personalId, recurringId))
        addExpense(shopping, 129.99, "New headphones", "2026-10-02", tagIds = listOf(businessId))
        addExpense(bills, 85.00, "Electric bill", "2026-10-02", isRecurring = true, tagIds = listOf(taxId, recurringId))
        addExpense(food, 12.50, "Coffee and pastry", "2026-10-02", tagIds = listOf(personalId))
    }
}
