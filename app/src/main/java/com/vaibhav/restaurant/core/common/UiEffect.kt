package com.vaibhav.restaurant.core.common

sealed interface UiEffect {
    data class ShowSnackbar(val message: String) : UiEffect
    data class Navigate(val route: String) : UiEffect
    data object NavigateBack : UiEffect
}
