package com.vaibhav.restaurant.feature.cart.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaibhav.restaurant.core.common.UiEffect
import com.vaibhav.restaurant.feature.cart.data.repository.CartRepository
import com.vaibhav.restaurant.feature.cart.domain.model.CartItem
import com.vaibhav.restaurant.feature.order.data.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CartUiState(
    val items: List<CartItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val isEmpty: Boolean = true
)

sealed interface CartEvent {
    data class IncreaseQuantity(val menuItemId: String, val currentQty: Int) : CartEvent
    data class DecreaseQuantity(val menuItemId: String, val currentQty: Int) : CartEvent
    data class RemoveItem(val menuItemId: String) : CartEvent
    data object ClearCart : CartEvent
    data object PlaceOrder : CartEvent
}

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _effects = Channel<UiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    /**
     * stateIn converts the cold Room Flow into a hot StateFlow.
     * WhileSubscribed(5000) keeps the upstream alive for 5s after the last subscriber
     * disappears (survives quick config changes without requerying).
     */
    val uiState: StateFlow<CartUiState> = cartRepository.getCartItems()
        .map { items ->
            CartUiState(
                items = items,
                totalAmount = items.sumOf { it.totalPrice },
                isEmpty = items.isEmpty()
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CartUiState()
        )

    fun onEvent(event: CartEvent) {
        when (event) {
            is CartEvent.IncreaseQuantity -> {
                viewModelScope.launch {
                    cartRepository.updateQuantity(event.menuItemId, event.currentQty + 1)
                }
            }
            is CartEvent.DecreaseQuantity -> {
                viewModelScope.launch {
                    cartRepository.updateQuantity(event.menuItemId, event.currentQty - 1)
                }
            }
            is CartEvent.RemoveItem -> {
                viewModelScope.launch {
                    cartRepository.removeItem(event.menuItemId)
                    _effects.send(UiEffect.ShowSnackbar("Item removed from cart"))
                }
            }
            is CartEvent.ClearCart -> {
                viewModelScope.launch {
                    cartRepository.clearCart()
                    _effects.send(UiEffect.ShowSnackbar("Cart cleared"))
                }
            }
            is CartEvent.PlaceOrder -> placeOrder()
        }
    }

    private fun placeOrder() {
        viewModelScope.launch {
            val items = uiState.value.items
            if (items.isEmpty()) {
                _effects.send(UiEffect.ShowSnackbar("Cart is empty"))
                return@launch
            }

            val orderId = orderRepository.placeOrder(items, uiState.value.totalAmount)
            cartRepository.clearCart()
            _effects.send(UiEffect.Navigate(orderId))
        }
    }
}
