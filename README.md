<p align="center">
  <img src="https://raw.githubusercontent.com/sufarook/Kiln/main/docs/src/assets/kiln-mark.svg" width="88" height="88" alt="Kiln logo">
</p>

<h1 align="center">Kiln Samples</h1>

<p align="center">
Runnable examples for <a href="https://github.com/sufarook/Kiln">Kiln</a> — compile-time
CRUD generation for Kotlin Multiplatform SQLite.
</p>

These samples consume Kiln exactly the way you would: the plugin and artifacts come
from Maven Central, and there are no `project(":")` references back into the library.
What you see here is what your own project looks like.

## Samples

Each sample is a self-contained folder under [`samples/`](samples) with its own README.

| Sample | What it shows |
|---|---|
| [**`compose-multiplatform`**](samples/compose-multiplatform) | One `@Composable` and one set of generated repositories shared by **Android, Desktop (JVM), and iOS**. Centres on a **many-to-many junction table** with a composite primary key. |
| [**`android-views`**](samples/android-views) | The same library in a traditional **XML / RecyclerView** Android app — for codebases not on Compose. |
| [**`expense-tracker`**](samples/expense-tracker) | A real-world **multi-table** expense tracker: one-to-many (`@Relation`), many-to-many (junction table with composite PK), transactions, type-safe DSL queries, and reactive filtering. |
| [**`pos`**](samples/pos) | A **Point of Sale** stress test exercising **every** Kiln operation: all 14 DSL operators, transactions, pagination, reactive observation, enums, nullable relations, composite keys, and raw `KilnDriver` for aggregate reports. |

```sh
./gradlew :compose-multiplatform:installDebug   # Compose, Android
./gradlew :compose-multiplatform:run            # Compose, Desktop (JVM)
./gradlew :android-views:installDebug           # Views, Android
./gradlew :expense-tracker:installDebug         # Multi-table, Android
./gradlew :pos:installDebug                     # POS stress test, Android
./gradlew :compose-multiplatform:linkDebugFrameworkIosSimulatorArm64   # iOS (macOS only)
```

## The whole setup

```kotlin
plugins {
    id("io.github.sufarook.kiln") version "1.0.0-alpha08"
}
```

That one line applies KSP, wires the processor, and adds the `annotations` +
`runtime` dependencies. Kiln bundles its own SQLite driver — no extra dependency needed.

## Highlight: a junction table from three data classes

```kotlin
@DbEntity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    @Column(name = "is_done") val isDone: Boolean = false
)

@DbEntity(tableName = "tags")
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

// Both columns are part of the composite primary key *and* foreign keys —
// which is exactly what a join table is.
@DbEntity(tableName = "task_tags")
data class TaskTag(
    @PrimaryKey @Relation val taskId: Long,
    @PrimaryKey @Relation val tagId: Long
)
```

From `TaskTag` alone, Kiln generates `TaskTagKey(taskId, tagId)`,
`findByTask` / `findByTag`, their `observeBy…` variants, and
`deleteByTask` / `deleteByTag` — plus the correct table-level
`PRIMARY KEY ("task_id", "tag_id")` constraint.

## Requirements

| | |
|---|---|
| Kiln | 1.0.0-alpha08 |
| Kotlin | 2.3.20 |
| Compose Multiplatform | 1.11.1 |
| AGP | 8.11.2 |
| JDK | 17 |
| `minSdk` / `compileSdk` | 24 / 36 |

## Trying an unreleased Kiln

`settings.gradle.kts` includes `mavenLocal()`, so you can point these samples at a
locally-built Kiln:

```sh
cd ../Kiln && ./gradlew publishToMavenLocal    # then bump `kiln` in gradle/libs.versions.toml
```

## License

Apache-2.0, same as [Kiln](https://github.com/sufarook/Kiln).
