package com.vaibhav.restaurant.feature.menu.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaibhav.restaurant.core.common.UiEffect
import com.vaibhav.restaurant.feature.menu.domain.model.Category
import com.vaibhav.restaurant.feature.menu.domain.model.MenuItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.vaibhav.restaurant.feature.menu.data.repository.MenuRepository
import javax.inject.Inject

data class HomeUiState(
    val categories: List<Category> = emptyList(),
    val menuItems: List<MenuItem> = emptyList(),
    val filteredItems: List<MenuItem> = emptyList(),
    val searchQuery: String = "",
    val selectedCategoryId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val cartItemCount: Int = 0
)

sealed interface HomeEvent {
    data class SearchQueryChanged(val query: String) : HomeEvent
    data class CategorySelected(val categoryId: String?) : HomeEvent
    data class ItemClicked(val itemId: String) : HomeEvent
    data object Refresh : HomeEvent
    data object CartClicked : HomeEvent
    data object OrdersClicked : HomeEvent
    data object ProfileClicked : HomeEvent
    data object KitchenClicked : HomeEvent
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val menuRepository: MenuRepository,
    private val cartRepository: com.vaibhav.restaurant.feature.cart.data.repository.CartRepository
) : ViewModel() {

    private val _effects = Channel<UiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val _searchQuery = MutableSharedFlow<String>(replay = 1)
    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _menuData = MutableStateFlow<Pair<List<Category>, List<MenuItem>>>(emptyList<Category>() to emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    /**
     * Debounced search: MutableSharedFlow -> debounce -> distinctUntilChanged -> flatMapLatest
     * This cancels previous filtering when a new query arrives.
     */
    private val debouncedSearch = _searchQuery
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { query -> flowOf(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    /**
     * combine() merges 5 flows into a single UI state.
     * Whenever ANY source changes, the UI state is recomputed.
     */
    val uiState: StateFlow<HomeUiState> = combine(
        _menuData,
        debouncedSearch,
        _selectedCategory,
        _isLoading,
        cartRepository.getCartItemCount()
    ) { data, query, categoryId, loading, cartCount ->
        val (categories, items) = data
        val filtered = items.filter { item ->
            val matchesCategory = categoryId == null || item.categoryId == categoryId
            val matchesSearch = query.isBlank() ||
                item.name.contains(query, ignoreCase = true) ||
                item.description.contains(query, ignoreCase = true)
            matchesCategory && matchesSearch
        }
        HomeUiState(
            categories = categories,
            menuItems = items,
            filteredItems = filtered,
            searchQuery = query,
            selectedCategoryId = categoryId,
            isLoading = loading,
            error = _error.value,
            cartItemCount = cartCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    init {
        loadData()
        viewModelScope.launch { _searchQuery.emit("") }
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.SearchQueryChanged -> {
                viewModelScope.launch { _searchQuery.emit(event.query) }
            }
            is HomeEvent.CategorySelected -> {
                _selectedCategory.update { if (it == event.categoryId) null else event.categoryId }
            }
            is HomeEvent.ItemClicked -> {
                viewModelScope.launch {
                    _effects.send(UiEffect.Navigate(event.itemId))
                }
            }
            is HomeEvent.Refresh -> loadData()
            is HomeEvent.CartClicked -> {
                viewModelScope.launch { _effects.send(UiEffect.Navigate("cart")) }
            }
            is HomeEvent.OrdersClicked -> {
                viewModelScope.launch { _effects.send(UiEffect.Navigate("orders")) }
            }
            is HomeEvent.ProfileClicked -> {
                viewModelScope.launch { _effects.send(UiEffect.Navigate("profile")) }
            }
            is HomeEvent.KitchenClicked -> {
                viewModelScope.launch { _effects.send(UiEffect.Navigate("kitchen")) }
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            menuRepository.getCategoriesAndItems()
                .onStart { _isLoading.value = true }
                .catch { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
                .collect { (categories, items) ->
                    _menuData.value = categories to items
                    _isLoading.value = false
                    _error.value = null
                }
        }
    }
}
