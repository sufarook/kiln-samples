package io.github.sufarook.kiln.sample.compose

import io.github.sufarook.kiln.annotations.Column
import io.github.sufarook.kiln.annotations.DbEntity
import io.github.sufarook.kiln.annotations.PrimaryKey
import io.github.sufarook.kiln.annotations.Relation

/**
 * These three data classes are the *entire* schema. Kiln generates
 * TaskRepository, TagRepository, and TaskTagRepository at compile time —
 * no SQL, no mappers, no migration files.
 */

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

/**
 * A junction table: many tasks <-> many tags.
 *
 * Both columns are part of the composite primary key *and* foreign keys, which
 * is exactly what a join table is. Kiln generates:
 *
 *  - `TaskTagKey(taskId, tagId)` — the composite key type used by
 *    `findById(key)` / `delete(key)`
 *  - `findByTask(taskId)` / `observeByTask(taskId)` / `deleteByTask(taskId)`
 *  - `findByTag(tagId)`   / `observeByTag(tagId)`   / `deleteByTag(tagId)`
 *
 * The pair is UNIQUE by virtue of being the primary key, so the same tag can't
 * be attached to the same task twice.
 */
@DbEntity(tableName = "task_tags")
data class TaskTag(
    @PrimaryKey @Relation val taskId: Long,
    @PrimaryKey @Relation val tagId: Long
)
