# Compose Multiplatform sample

One `@Composable` and one set of generated repositories, shared by **Android and
iOS**. This is the sample that shows Kiln's headline claim: entities defined once
in `commonMain`, repositories generated once, used on every target.

## Run

**Android**

```sh
./gradlew :compose-multiplatform:installDebug
```

**iOS** — see [`iosApp/README.md`](iosApp/README.md). The Kotlin side compiles
without Xcode:

```sh
./gradlew :compose-multiplatform:linkDebugFrameworkIosSimulatorArm64
```

> Compose Multiplatform 1.11+ no longer publishes an `iosX64` artifact, so the
> iOS targets here are Apple Silicon only (`iosArm64`, `iosSimulatorArm64`).

## Layout

```
src/commonMain/   Entities, TaskStore, and the shared Compose UI (App.kt)
src/androidMain/  MainActivity — creates the Android driver, calls setContent { App() }
src/iosMain/      MainViewController — creates the iOS driver, returns a UIViewController
iosApp/           Swift entry point that hosts the Compose UI
```

## The junction table

The centrepiece is a **many-to-many** relationship
([`Entities.kt`](src/commonMain/kotlin/io/github/sufarook/kiln/sample/compose/Entities.kt)):

```kotlin
@DbEntity(tableName = "task_tags")
data class TaskTag(
    @PrimaryKey @Relation val taskId: Long,
    @PrimaryKey @Relation val tagId: Long
)
```

Both columns are part of the composite primary key *and* foreign keys — which is
exactly what a join table is. From that alone Kiln generates:

```kotlin
TaskTagKey(taskId, tagId)       // composite key type, used by findById / delete
taskTags.findByTask(taskId)     // every tag on a task
taskTags.findByTag(tagId)       // every task with a tag
taskTags.observeByTask(taskId)  // ...and reactive variants
taskTags.deleteByTask(taskId)   // one-call cascade cleanup
```

and the correct table-level constraint:

```sql
CREATE TABLE IF NOT EXISTS "task_tags" (
    "task_id" INTEGER NOT NULL,
    "tag_id" INTEGER NOT NULL,
    PRIMARY KEY ("task_id", "tag_id")
)
```

Also demonstrated: `observeAll()` driving Compose state via `collectAsState`,
auto-migration on launch, and CRUD through generated repositories.

## One-call schema setup

With three entities in this module, [`TaskStore`](src/commonMain/kotlin/io/github/sufarook/kiln/sample/compose/TaskStore.kt)
skips per-repository `createTable()` calls in favor of the generated `KilnSchema`
object, which covers every `@DbEntity` Kiln sees in `commonMain`:

```kotlin
init {
    KilnSchema.createAll(driver) // creates + auto-migrates Task, Tag, and TaskTag
}
```

Adding a fourth entity later needs no change to this line.
