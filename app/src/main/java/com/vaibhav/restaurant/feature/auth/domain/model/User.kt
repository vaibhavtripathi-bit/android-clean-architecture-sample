package com.vaibhav.restaurant.feature.auth.domain.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val phone: String
)
