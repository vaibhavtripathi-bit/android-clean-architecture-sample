package com.vaibhav.restaurant.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class MenuItemDto(
    val id: String,
    val categoryId: String,
    val name: String,
    val description: String,
    val price: Double,
    val imageUrl: String,
    val isAvailable: Boolean
)
