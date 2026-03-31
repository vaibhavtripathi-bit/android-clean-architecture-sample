package com.vaibhav.restaurant.feature.cart.domain.model

data class CartItem(
    val menuItemId: String,
    val name: String,
    val price: Double,
    val imageUrl: String,
    val quantity: Int
) {
    val totalPrice: Double get() = price * quantity
}
