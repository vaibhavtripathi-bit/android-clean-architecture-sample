package com.vaibhav.restaurant.feature.order.domain.model

data class Order(
    val id: String,
    val items: List<OrderItem>,
    val totalAmount: Double,
    val status: OrderStatus,
    val createdAt: Long,
    val estimatedDeliveryMinutes: Int
)

data class OrderItem(
    val menuItemId: String,
    val name: String,
    val price: Double,
    val quantity: Int
)

enum class OrderStatus(val displayName: String) {
    PLACED("Order Placed"),
    CONFIRMED("Confirmed"),
    PREPARING("Preparing"),
    READY("Ready for Pickup"),
    OUT_FOR_DELIVERY("Out for Delivery"),
    DELIVERED("Delivered")
}
