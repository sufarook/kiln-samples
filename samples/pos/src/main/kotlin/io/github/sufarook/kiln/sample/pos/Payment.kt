package io.github.sufarook.kiln.sample.pos

import io.github.sufarook.kiln.annotations.Column
import io.github.sufarook.kiln.annotations.DbEntity
import io.github.sufarook.kiln.annotations.PrimaryKey
import io.github.sufarook.kiln.annotations.Relation

enum class PaymentMethod { CASH, CARD, MOBILE }

@DbEntity(tableName = "payments")
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @Column(name = "order_id", index = true) @Relation val orderId: Long,
    val method: PaymentMethod,
    val amount: Double,
    @Column(name = "paid_at") val paidAt: String
)
