package io.github.sufarook.kiln.sample.pos

import io.github.sufarook.kiln.annotations.Column
import io.github.sufarook.kiln.annotations.DbEntity
import io.github.sufarook.kiln.annotations.PrimaryKey
import io.github.sufarook.kiln.annotations.Relation

@DbEntity(tableName = "order_items")
data class OrderItem(
    @PrimaryKey @Column(name = "order_id") @Relation(cascade = true) val orderId: Long,
    @PrimaryKey @Column(name = "product_id") @Relation val productId: Long,
    val quantity: Int,
    @Column(name = "unit_price") val unitPrice: Double,
    val discount: Double = 0.0
)
