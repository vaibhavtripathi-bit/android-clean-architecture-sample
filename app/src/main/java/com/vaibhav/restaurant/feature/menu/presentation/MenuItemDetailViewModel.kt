package com.vaibhav.restaurant.feature.menu.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.vaibhav.restaurant.core.common.UiEffect
import com.vaibhav.restaurant.feature.cart.data.repository.CartRepository
import com.vaibhav.restaurant.feature.menu.data.repository.MenuRepository
import com.vaibhav.restaurant.feature.menu.domain.model.MenuItem
import com.vaibhav.restaurant.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MenuItemDetailUiState(
    val menuItem: MenuItem? = null,
    val quantity: Int = 1,
    val isLoading: Boolean = true,
    val error: String? = null
)

sealed interface MenuItemDetailEvent {
    data object IncreaseQuantity : MenuItemDetailEvent
    data object DecreaseQuantity : MenuItemDetailEvent
    data object AddToCartClicked : MenuItemDetailEvent
    data object GoToCartClicked : MenuItemDetailEvent
}

@HiltViewModel
class MenuItemDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val menuRepository: MenuRepository,
    private val cartRepository: CartRepository
) : ViewModel() {

    private val itemId: String = savedStateHandle.toRoute<Route.MenuItemDetail>().itemId

    private val _uiState = MutableStateFlow(MenuItemDetailUiState())
    val uiState: StateFlow<MenuItemDetailUiState> = _uiState.asStateFlow()

    private val _effects = Channel<UiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadItem()
    }

    fun onEvent(event: MenuItemDetailEvent) {
        when (event) {
            is MenuItemDetailEvent.IncreaseQuantity -> {
                _uiState.update { it.copy(quantity = (it.quantity + 1).coerceAtMost(10)) }
            }
            is MenuItemDetailEvent.DecreaseQuantity -> {
                _uiState.update { it.copy(quantity = (it.quantity - 1).coerceAtLeast(1)) }
            }
            is MenuItemDetailEvent.AddToCartClicked -> addToCart()
            is MenuItemDetailEvent.GoToCartClicked -> {
                viewModelScope.launch { _effects.send(UiEffect.Navigate("cart")) }
            }
        }
    }

    private fun loadItem() {
        viewModelScope.launch {
            menuRepository.getMenuItem(itemId)
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { item ->
                    _uiState.update { it.copy(menuItem = item, isLoading = false) }
                }
        }
    }

    private fun addToCart() {
        val state = _uiState.value
        val item = state.menuItem ?: return

        viewModelScope.launch {
            cartRepository.addToCart(item, state.quantity)
            _effects.send(UiEffect.ShowSnackbar("${item.name} added to cart"))
        }
    }
}
