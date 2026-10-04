package io.github.sufarook.kiln.sample.pos

import io.github.sufarook.kiln.annotations.Column
import io.github.sufarook.kiln.annotations.DbEntity
import io.github.sufarook.kiln.annotations.PrimaryKey

@DbEntity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @Column(unique = true) val name: String
)
