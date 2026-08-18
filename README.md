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

| Module | What it shows |
|---|---|
| [`composeApp`](composeApp) | **Compose Multiplatform** — one `@Composable` and one set of generated repositories shared by Android and iOS. Includes a **many-to-many junction table** using a composite primary key. |
| [`sample-android`](sample-android) | Plain Android app with XML views and a `RecyclerView` — Kiln in a traditional, non-Compose codebase. |

## The whole setup

```kotlin
plugins {
    id("io.github.sufarook.kiln") version "1.0.0-alpha04"
}
```

That one line applies KSP, wires the processor, and adds the `annotations` +
`runtime` dependencies. You still choose a SQLite driver for your platform —
Kiln doesn't bundle one.

## What the Compose sample demonstrates

Three data classes are the entire schema ([`Entities.kt`](composeApp/src/commonMain/kotlin/io/github/sufarook/kiln/sample/compose/Entities.kt)):

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

// A junction table: both columns are part of the composite primary key
// *and* foreign keys — which is exactly what a join table is.
@DbEntity(tableName = "task_tags")
data class TaskTag(
    @PrimaryKey @Relation val taskId: Long,
    @PrimaryKey @Relation val tagId: Long
)
```

From `TaskTag` alone, Kiln generates:

```kotlin
TaskTagKey(taskId, tagId)          // composite key type, used by findById / delete
taskTags.findByTask(taskId)        // every tag on a task
taskTags.findByTag(tagId)          // every task with a tag
taskTags.observeByTask(taskId)     // ...and reactive variants
taskTags.deleteByTask(taskId)      // one-call cascade cleanup
```

and this SQL — note the correct table-level composite constraint:

```sql
CREATE TABLE IF NOT EXISTS "task_tags" (
    "task_id" INTEGER NOT NULL,
    "tag_id" INTEGER NOT NULL,
    PRIMARY KEY ("task_id", "tag_id")
)
```

Also shown: `observeAll()` driving Compose state reactively, `createTable()`
auto-migration on every launch, and CRUD through generated repositories.

## Running

**Android** (either module):

```sh
./gradlew :composeApp:installDebug
./gradlew :sample-android:installDebug
```

**iOS** — see [`iosApp/README.md`](iosApp/README.md). The Xcode project isn't
checked in; the Kotlin side compiles without Xcode:

```sh
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

> Compose Multiplatform 1.11+ no longer publishes an `iosX64` artifact, so the
> iOS targets here are Apple Silicon only (`iosArm64`, `iosSimulatorArm64`).

## Requirements

| | |
|---|---|
| Kiln | 1.0.0-alpha04 |
| Kotlin | 2.3.20 |
| Compose Multiplatform | 1.11.1 |
| AGP | 8.11.2 |
| JDK | 17 |
| `minSdk` / `compileSdk` | 24 / 36 |

## Trying an unreleased Kiln

`settings.gradle.kts` includes `mavenLocal()`, so you can point these samples at a
locally-built Kiln:

```sh
cd ../Kiln && ./gradlew publishToMavenLocal    # then set `kiln` in gradle/libs.versions.toml
```

## License

Apache-2.0, same as [Kiln](https://github.com/sufarook/Kiln).
