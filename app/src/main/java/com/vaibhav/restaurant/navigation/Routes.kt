package com.vaibhav.restaurant.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable data object Login : Route
    @Serializable data object Register : Route
    @Serializable data object Home : Route
    @Serializable data class MenuItemDetail(val itemId: String) : Route
    @Serializable data object Cart : Route
    @Serializable data class OrderTracking(val orderId: String) : Route
    @Serializable data object OrderHistory : Route
    @Serializable data object Profile : Route
}
