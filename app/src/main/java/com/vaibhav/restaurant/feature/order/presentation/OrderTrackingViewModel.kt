package com.vaibhav.restaurant.feature.order.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.vaibhav.restaurant.core.common.UiEffect
import com.vaibhav.restaurant.feature.order.data.repository.OrderRepository
import com.vaibhav.restaurant.feature.order.domain.model.Order
import com.vaibhav.restaurant.feature.order.domain.model.OrderStatus
import com.vaibhav.restaurant.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrderTrackingUiState(
    val order: Order? = null,
    val currentStatus: OrderStatus = OrderStatus.PLACED,
    val isLoading: Boolean = true,
    val statusHistory: List<OrderStatus> = listOf(OrderStatus.PLACED)
)

sealed interface OrderTrackingEvent {
    data object GoHome : OrderTrackingEvent
}

@HiltViewModel
class OrderTrackingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val orderId: String = savedStateHandle.toRoute<Route.OrderTracking>().orderId

    private val _uiState = MutableStateFlow(OrderTrackingUiState())
    val uiState: StateFlow<OrderTrackingUiState> = _uiState.asStateFlow()

    private val _effects = Channel<UiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadOrder()
        observeStatus()
    }

    fun onEvent(event: OrderTrackingEvent) {
        when (event) {
            is OrderTrackingEvent.GoHome -> {
                viewModelScope.launch { _effects.send(UiEffect.Navigate("home")) }
            }
        }
    }

    private fun loadOrder() {
        viewModelScope.launch {
            orderRepository.getOrderById(orderId)
                .catch { e ->
                    _effects.send(UiEffect.ShowSnackbar(e.message ?: "Failed to load order"))
                }
                .collect { order ->
                    _uiState.update {
                        it.copy(order = order, isLoading = false)
                    }
                }
        }
    }

    private fun observeStatus() {
        viewModelScope.launch {
            orderRepository.observeOrderStatus(orderId)
                .catch { e ->
                    _effects.send(UiEffect.ShowSnackbar("Status update failed"))
                }
                .collect { status ->
                    _uiState.update { state ->
                        val history = state.statusHistory.toMutableList()
                        if (!history.contains(status)) history.add(status)
                        state.copy(
                            currentStatus = status,
                            statusHistory = history
                        )
                    }
                }
        }
    }
}
