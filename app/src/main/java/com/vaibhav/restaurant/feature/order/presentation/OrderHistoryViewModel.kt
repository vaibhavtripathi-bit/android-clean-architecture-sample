package com.vaibhav.restaurant.feature.order.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaibhav.restaurant.core.common.UiEffect
import com.vaibhav.restaurant.feature.order.data.repository.OrderRepository
import com.vaibhav.restaurant.feature.order.domain.model.Order
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrderHistoryUiState(
    val orders: List<Order> = emptyList(),
    val isEmpty: Boolean = true
)

sealed interface OrderHistoryEvent {
    data class OrderClicked(val orderId: String) : OrderHistoryEvent
}

@HiltViewModel
class OrderHistoryViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _effects = Channel<UiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    val uiState: StateFlow<OrderHistoryUiState> = orderRepository.getAllOrders()
        .map { orders ->
            OrderHistoryUiState(
                orders = orders,
                isEmpty = orders.isEmpty()
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = OrderHistoryUiState()
        )

    fun onEvent(event: OrderHistoryEvent) {
        when (event) {
            is OrderHistoryEvent.OrderClicked -> {
                viewModelScope.launch {
                    _effects.send(UiEffect.Navigate(event.orderId))
                }
            }
        }
    }
}
