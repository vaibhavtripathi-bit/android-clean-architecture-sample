package com.vaibhav.restaurant.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    val imageUrl: String
)
