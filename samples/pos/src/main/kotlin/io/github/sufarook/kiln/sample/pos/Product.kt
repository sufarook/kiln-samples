package io.github.sufarook.kiln.sample.pos

import io.github.sufarook.kiln.annotations.Column
import io.github.sufarook.kiln.annotations.DbEntity
import io.github.sufarook.kiln.annotations.PrimaryKey
import io.github.sufarook.kiln.annotations.Relation

@DbEntity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @Column(unique = true, index = true) val sku: String,
    val price: Double,
    @Column(name = "category_id", index = true) @Relation val categoryId: Long,
    @Column(name = "stock_qty") val stockQty: Int,
    @Column(name = "is_active") val isActive: Boolean = true
)
