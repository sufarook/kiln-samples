package io.github.sufarook.kiln.sample.pos

import io.github.sufarook.kiln.annotations.Column
import io.github.sufarook.kiln.annotations.DbEntity
import io.github.sufarook.kiln.annotations.PrimaryKey

@DbEntity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @Column(unique = true) val phone: String? = null,
    val email: String? = null,
    @Column(name = "loyalty_points") val loyaltyPoints: Int = 0
)
