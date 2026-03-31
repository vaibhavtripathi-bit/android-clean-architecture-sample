package com.vaibhav.restaurant.feature.menu.domain.model

data class MenuItem(
    val id: String,
    val categoryId: String,
    val name: String,
    val description: String,
    val price: Double,
    val imageUrl: String,
    val isAvailable: Boolean
)
