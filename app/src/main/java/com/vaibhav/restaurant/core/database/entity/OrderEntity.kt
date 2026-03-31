package com.vaibhav.restaurant.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey
    val id: String,
    val items: String, // JSON serialized list of order items
    val totalAmount: Double,
    val status: String,
    val createdAt: Long,
    val estimatedDeliveryMinutes: Int
)
