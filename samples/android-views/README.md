# Android (Views) sample

Kiln in a traditional, non-Compose Android app — XML layouts and a `RecyclerView`.
Useful if you're adding Kiln to an existing codebase that isn't on Compose.

## Run

```sh
./gradlew :android-views:installDebug
```

## What it shows

One annotated data class ([`Todo.kt`](src/main/kotlin/io/github/sufarook/kiln/sample/Todo.kt))
is the entire schema:

```kotlin
@DbEntity(tableName = "todos")
data class Todo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    @Column(name = "is_completed") val isCompleted: Boolean = false,
    @Column(index = true) val priority: Int = 0
)
```

Kiln generates `TodoRepository` and `TodoColumns` at compile time. The app uses:

- `createTable()` — creates and auto-migrates on every launch
- `observeAll()` — a `Flow` that re-emits after every write, wired to the adapter
- `insert` / `update` / `delete` — suspend CRUD from `lifecycleScope`

## Setup

The only Kiln-specific line in [`build.gradle.kts`](build.gradle.kts):

```kotlin
plugins {
    id("io.github.sufarook.kiln") version "1.0.0-alpha05"
}
```

Plus a SQLite driver of your choosing — here `app.cash.sqldelight:android-driver`.
