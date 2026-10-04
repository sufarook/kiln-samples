package io.github.sufarook.kiln.sample.pos

import io.github.sufarook.kiln.annotations.Column
import io.github.sufarook.kiln.annotations.DbEntity
import io.github.sufarook.kiln.annotations.PrimaryKey
import io.github.sufarook.kiln.annotations.Relation

enum class OrderStatus { PENDING, COMPLETED, CANCELLED, REFUNDED }

@DbEntity(tableName = "orders")
data class Order(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @Column(name = "customer_id", index = true) @Relation val customerId: Long? = null,
    val status: OrderStatus = OrderStatus.PENDING,
    @Column(name = "total_amount") val totalAmount: Double = 0.0,
    @Column(name = "created_at") val createdAt: String,
    val note: String = ""
)
