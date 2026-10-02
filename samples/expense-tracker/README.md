# Expense Tracker sample

A real-world expense tracking app that demonstrates Kiln's multi-table capabilities:
relations, junction tables, composite primary keys, transactions, and type-safe queries.

## Run

```sh
./gradlew :expense-tracker:installDebug
```

## Data model

Four tables with one-to-many and many-to-many relationships:

```
┌──────────────┐       ┌──────────────┐
│  categories  │──1:N──│   expenses   │
└──────────────┘       └──────┬───────┘
                              │
                         M:N (junction)
                              │
┌──────────────┐       ┌──────┴───────┐
│     tags     │──1:N──│ expense_tags │
└──────────────┘       └──────────────┘
```

### Entities

```kotlin
@DbEntity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @Column(unique = true) val name: String,
    val icon: String,
    val color: Long
)

@DbEntity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @Column(name = "category_id", index = true) @Relation val categoryId: Long,
    val amount: Double,
    val note: String = "",
    val date: String,
    @Column(name = "is_recurring") val isRecurring: Boolean = false
)

@DbEntity(tableName = "tags")
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @Column(unique = true) val name: String
)

// Junction table — composite PK + dual @Relation
@DbEntity(tableName = "expense_tags")
data class ExpenseTag(
    @PrimaryKey @Relation val expenseId: Long,
    @PrimaryKey @Relation val tagId: Long
)
```

## What it shows

| Feature | Where |
|---------|-------|
| **One-to-many** (`@Relation`) | `Expense.categoryId` → `findByCategory`, `observeByCategory` |
| **Many-to-many** (junction table) | `ExpenseTag` with composite PK + dual `@Relation` → `findByExpense`, `findByTag` |
| **Transactions** | `addExpense` inserts an expense + tag links atomically; `deleteExpense` cleans up both tables |
| **Type-safe DSL** | `findWhere { amount gte 100.0 }`, `findWhere { isRecurring eq true }` |
| **Reactive queries** | `observeExpenses()`, `observeExpensesByCategory(id)` — filter by category in real time |
| **One-call schema** | `KilnSchema.createAll(driver)` creates and migrates all four tables |
| **Seed data** | `seedIfEmpty()` populates categories, tags, and sample expenses inside a single transaction |

## Setup

```kotlin
plugins {
    id("io.github.sufarook.kiln") version "1.0.0-alpha08"
}
```

Kiln bundles its own SQLite driver — no extra dependency needed.
