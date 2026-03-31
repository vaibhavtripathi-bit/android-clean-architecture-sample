# Restaurant App - Android Kotlin Coroutines & Flow Showcase

A modern Android restaurant ordering app built with **Jetpack Compose**, **Kotlin Coroutines**, and **Flow** — designed as a comprehensive showcase of reactive programming patterns and Android best practices.

## Table of Contents

- [Screenshots & Features](#screenshots--features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Coroutines & Flow Patterns](#coroutines--flow-patterns)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Demo Credentials](#demo-credentials)

---

## Screenshots & Features

| Screen | Description |
|--------|-------------|
| **Login / Register** | Email + password authentication with form validation |
| **Home / Menu** | Browse menu categories, search items with live filtering |
| **Item Detail** | View item details, adjust quantity, add to cart |
| **Cart** | View/edit cart items, adjust quantities, place order |
| **Order Tracking** | Real-time order status updates with step-by-step progress |
| **Order History** | View past orders with status and details |
| **Profile** | User info, dark mode toggle, notification settings, logout |
| **Connectivity Banner** | Animated banner showing network status changes |

---

## Architecture

The app follows **MVVM + Clean Architecture** with clear separation into three layers:

```
┌─────────────────────────────────────────────────┐
│                 Presentation Layer               │
│  (Compose Screens + ViewModels + UI State)       │
│                                                  │
│  Screen ──observes──> ViewModel.uiState          │
│  Screen ──sends────> ViewModel.onEvent(Event)    │
│  Screen ──collects──> ViewModel.effects          │
└─────────────────────┬───────────────────────────┘
                      │
┌─────────────────────┴───────────────────────────┐
│                  Domain Layer                    │
│  (Models + Use Cases)                            │
│  Pure Kotlin data classes, no Android deps        │
└─────────────────────┬───────────────────────────┘
                      │
┌─────────────────────┴───────────────────────────┐
│                   Data Layer                     │
│  (Repositories + Data Sources)                   │
│  FakeAPI, Room DB, DataStore Preferences          │
└─────────────────────────────────────────────────┘
```

### Event System (UI → ViewModel)

Each screen defines a **sealed interface** for its events:

```kotlin
sealed interface MenuEvent {
    data class SearchQueryChanged(val query: String) : MenuEvent
    data class CategorySelected(val categoryId: String?) : MenuEvent
    data object Refresh : MenuEvent
}
```

The ViewModel exposes a single `onEvent(event)` function that the UI calls.

### UI State (ViewModel → UI)

Each ViewModel exposes an **immutable data class** as `StateFlow`:

```kotlin
data class HomeUiState(
    val categories: List<Category> = emptyList(),
    val menuItems: List<MenuItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

val uiState: StateFlow<HomeUiState> = ...
```

Compose screens observe this with `collectAsStateWithLifecycle()`.

### One-Shot Effects (ViewModel → UI)

For navigation, snackbars, and other one-time events, a **Channel** guarantees exactly-once delivery:

```kotlin
sealed interface UiEffect {
    data class ShowSnackbar(val message: String) : UiEffect
    data class Navigate(val route: String) : UiEffect
    data object NavigateBack : UiEffect
}

private val _effects = Channel<UiEffect>(Channel.BUFFERED)
val effects = _effects.receiveAsFlow()
```

---

## Tech Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Kotlin | 2.1.0 |
| UI | Jetpack Compose + Material 3 | BOM 2024.12.01 |
| Architecture | MVVM + Clean Architecture | - |
| DI | Hilt (Dagger) | 2.53.1 |
| Navigation | Compose Navigation (type-safe routes) | 2.8.5 |
| Local DB | Room | 2.6.1 |
| Preferences | DataStore | 1.1.1 |
| Networking | Retrofit + OkHttp + Kotlin Serialization | 2.11.0 / 4.12.0 |
| Image Loading | Coil 3 | 3.0.4 |
| Async | Kotlin Coroutines + Flow | 1.9.0 |
| Build | Gradle KTS + Version Catalogs | 8.11.1 |

---

## Coroutines & Flow Patterns

This is the core educational value of the project. Every pattern is used in a real, meaningful context.

### 1. `StateFlow` / `MutableStateFlow` — UI State Management

**Where:** Every ViewModel (`LoginViewModel`, `HomeViewModel`, `CartViewModel`, etc.)

```kotlin
private val _uiState = MutableStateFlow(LoginUiState())
val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
```

- Hot stream that always holds the latest value
- Survives configuration changes when scoped to ViewModel
- UI observes via `collectAsStateWithLifecycle()`

**File:** `feature/auth/presentation/LoginViewModel.kt`

---

### 2. `Channel` (BUFFERED) — One-Shot Events

**Where:** Every ViewModel for `UiEffect` delivery

```kotlin
private val _effects = Channel<UiEffect>(Channel.BUFFERED)
val effects = _effects.receiveAsFlow()
```

- Guarantees each event is consumed **exactly once** (unlike SharedFlow)
- BUFFERED capacity prevents suspension when UI isn't collecting momentarily
- Used for snackbar messages, navigation commands

**File:** `core/common/UiEffect.kt`, all ViewModels

---

### 3. `MutableSharedFlow` + `debounce` + `distinctUntilChanged` + `flatMapLatest` — Search

**Where:** `HomeViewModel` — live search with debouncing

```kotlin
private val _searchQuery = MutableSharedFlow<String>(replay = 1)

private val debouncedSearch = _searchQuery
    .debounce(300)                // Wait 300ms after last keystroke
    .distinctUntilChanged()       // Skip if same query
    .flatMapLatest { query ->     // Cancel previous search on new query
        flowOf(query)
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
```

- `debounce` prevents excessive processing on rapid typing
- `distinctUntilChanged` skips duplicate emissions
- `flatMapLatest` cancels the previous downstream when a new value arrives

**File:** `feature/menu/presentation/HomeViewModel.kt`

---

### 4. `combine()` — Merging Multiple State Sources

**Where:** `HomeViewModel` — combines 5 flows into one UI state

```kotlin
val uiState: StateFlow<HomeUiState> = combine(
    _menuData,
    debouncedSearch,
    _selectedCategory,
    _isLoading,
    cartRepository.getCartItemCount()
) { data, query, categoryId, loading, cartCount ->
    // Recompute UI state whenever ANY source changes
    HomeUiState(...)
}
```

- Whenever any input flow emits, the combine block re-executes
- Produces a single coherent UI state from multiple sources

**File:** `feature/menu/presentation/HomeViewModel.kt`

---

### 5. `stateIn()` with `SharingStarted.WhileSubscribed(5000)` — Cold to Hot Conversion

**Where:** `CartViewModel`, `OrderHistoryViewModel`, `ProfileViewModel`

```kotlin
val uiState: StateFlow<CartUiState> = cartRepository.getCartItems()
    .map { items -> CartUiState(items = items, ...) }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CartUiState()
    )
```

- Converts cold Room/DataStore Flow to hot StateFlow
- `WhileSubscribed(5000)` keeps upstream alive for 5 seconds after last subscriber (survives quick config changes)
- Avoids re-querying the database on every rotation

**Files:** `feature/cart/presentation/CartViewModel.kt`, `feature/order/presentation/OrderHistoryViewModel.kt`, `feature/profile/presentation/ProfileViewModel.kt`

---

### 6. `flow { emit() }` — Cold Flow Builder

**Where:** `AuthRepository`, `MenuRepository`

```kotlin
fun login(email: String, password: String): Flow<User> = flow {
    val userDto = api.login(email, password)
    emit(user)
}
```

- Creates a cold flow that executes on collection
- Each collector gets its own execution

**File:** `feature/auth/data/repository/AuthRepository.kt`

---

### 7. `flowOn(Dispatchers.IO)` — Context Switching

**Where:** All repositories

```kotlin
fun getMenuItems(): Flow<List<MenuItem>> = flow {
    val dtos = api.getMenuItems()
    emit(dtos.map { ... })
}.flowOn(Dispatchers.IO)
```

- Switches upstream execution to IO dispatcher
- Downstream (ViewModel) stays on Main

**File:** `feature/menu/data/repository/MenuRepository.kt`

---

### 8. `onStart` / `onCompletion` / `onEach` — Flow Lifecycle Operators

**Where:** `LoginViewModel`, `RegisterViewModel`

```kotlin
authRepository.login(state.email, state.password)
    .onStart { _uiState.update { it.copy(isLoading = true) } }
    .onEach { user ->
        _effects.send(UiEffect.ShowSnackbar("Welcome!"))
    }
    .catch { e ->
        _effects.send(UiEffect.ShowSnackbar(e.message ?: "Failed"))
    }
    .onCompletion { _uiState.update { it.copy(isLoading = false) } }
    .launchIn(viewModelScope)
```

- `onStart`: Side effect before first emission (show loading)
- `onEach`: Process each emission
- `catch`: Handle upstream errors
- `onCompletion`: Cleanup after flow completes (hide loading)
- `launchIn`: Terminal operator that launches collection in a scope

**File:** `feature/auth/presentation/LoginViewModel.kt`

---

### 9. `callbackFlow` + `awaitClose` — Bridging Callback APIs

**Where:** `NetworkConnectivityObserver` — wraps Android's `ConnectivityManager`

```kotlin
override val isConnected: Flow<Boolean> = callbackFlow {
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { trySend(true) }
        override fun onLost(network: Network) { trySend(false) }
    }
    connectivityManager.registerNetworkCallback(request, callback)
    awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
}.distinctUntilChanged()
```

- Bridges callback-based Android API to reactive Flow
- `awaitClose` ensures cleanup when flow is cancelled
- `trySend` is non-suspending (safe in callbacks)

**File:** `core/connectivity/NetworkConnectivityObserver.kt`

Also used in `OrderRepository.observeOrderStatusViaCallback()` to simulate a WebSocket-like callback.

---

### 10. `channelFlow` — Concurrent Emissions

**Where:** `OrderRepository.observeOrderStatus()` — simulates real-time order tracking

```kotlin
fun observeOrderStatus(orderId: String): Flow<OrderStatus> = channelFlow {
    launch {
        for (status in statuses) {
            delay(3000L)
            send(status)
            // Update DB concurrently
            withTimeout(5000) {
                withContext(Dispatchers.IO) { ... }
            }
        }
    }
    awaitClose()
}
```

- Unlike `flow {}`, `channelFlow` allows launching concurrent coroutines
- Multiple coroutines can `send()` into the same channel
- Demonstrates how you'd bridge WebSocket/Firebase real-time updates

**File:** `feature/order/data/repository/OrderRepository.kt`

---

### 11. `MutableStateFlow.update {}` — Atomic State Updates

**Where:** All ViewModels for state mutations

```kotlin
_uiState.update { it.copy(quantity = (it.quantity + 1).coerceAtMost(10)) }
```

- Thread-safe atomic read-modify-write operation
- Prevents lost updates from concurrent modifications

**File:** `feature/menu/presentation/MenuItemDetailViewModel.kt`

---

### 12. `Mutex` — Thread-Safe Mutations

**Where:** `CartRepository` — protecting cart operations

```kotlin
private val mutex = Mutex()

suspend fun addToCart(item: MenuItem, quantity: Int) {
    mutex.withLock {
        withContext(Dispatchers.IO) {
            cartDao.upsertCartItem(...)
        }
    }
}
```

- Ensures only one cart operation runs at a time
- Prevents race conditions (e.g., simultaneous add + remove)

**File:** `feature/cart/data/repository/CartRepository.kt`

---

### 13. `supervisorScope` + `async` / `awaitAll` — Parallel Loading

**Where:** `MenuRepository.getCategoriesAndItems()`

```kotlin
fun getCategoriesAndItems(): Flow<Pair<List<Category>, List<MenuItem>>> = flow {
    val result = supervisorScope {
        val categoriesDeferred = async { api.getCategories() }
        val itemsDeferred = async { api.getMenuItems() }
        categoriesDeferred.await() to itemsDeferred.await()
    }
    emit(result)
}
```

- `supervisorScope` prevents one failure from cancelling the other
- `async` launches concurrent decomposition
- Both API calls run in parallel, reducing total load time

**File:** `feature/menu/data/repository/MenuRepository.kt`

---

### 14. `withTimeout` — Timeout Handling

**Where:** `OrderRepository.observeOrderStatus()`

```kotlin
withTimeout(5000) {
    withContext(Dispatchers.IO) {
        val entity = orderDao.getOrderById(orderId).first()
        entity?.let { orderDao.updateOrder(...) }
    }
}
```

- Throws `TimeoutCancellationException` if operation exceeds 5 seconds
- Prevents hanging on slow DB operations

**File:** `feature/order/data/repository/OrderRepository.kt`

---

### 15. `collectAsStateWithLifecycle()` — Lifecycle-Aware Collection

**Where:** Every Compose screen

```kotlin
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
```

- Automatically stops collection when lifecycle drops below STARTED
- Prevents wasted work when app is in background
- Resumes collection when lifecycle returns to STARTED

**File:** All screen composables

---

### 16. `map` / `filter` — Standard Flow Operators

**Where:** Repositories and ViewModels for data transformation

```kotlin
cartDao.getAllCartItems()
    .map { entities -> entities.map { it.toDomain() } }
```

**File:** `feature/cart/data/repository/CartRepository.kt`

---

### 17. `catch` — Error Handling

**Where:** ViewModels for graceful error handling

```kotlin
menuRepository.getCategoriesAndItems()
    .catch { e -> _error.value = e.message }
    .collect { ... }
```

- Catches upstream exceptions without crashing the flow
- Allows emitting error states to the UI

**File:** `feature/menu/presentation/HomeViewModel.kt`

---

### 18. `viewModelScope.launch` — Structured Concurrency

**Where:** Every ViewModel

```kotlin
viewModelScope.launch {
    cartRepository.addToCart(item, quantity)
    _effects.send(UiEffect.ShowSnackbar("Added!"))
}
```

- Automatically cancelled when ViewModel is cleared
- No manual lifecycle management needed

**File:** All ViewModels

---

### 19. Room `Flow<List<T>>` — Reactive Database Queries

**Where:** `CartDao`, `OrderDao`

```kotlin
@Query("SELECT * FROM cart_items")
fun getAllCartItems(): Flow<List<CartItemEntity>>
```

- Room automatically re-emits when underlying table changes
- Combined with `stateIn()` in ViewModel for hot observation

**File:** `core/database/dao/CartDao.kt`, `core/database/dao/OrderDao.kt`

---

### 20. DataStore `Flow<Preferences>` — Reactive Preferences

**Where:** `UserPreferences`

```kotlin
val userPreferencesFlow: Flow<UserPreferencesData> = dataStore.data.map { prefs ->
    UserPreferencesData(
        isDarkMode = prefs[Keys.IS_DARK_MODE] ?: false,
        ...
    )
}
```

- DataStore exposes preferences as Flow
- Changes propagate reactively to all observers

**File:** `core/datastore/UserPreferences.kt`

---

### 21. `Flow<T>.asResource()` — Extension Function

**Where:** `core/common/Extensions.kt`

```kotlin
fun <T> Flow<T>.asResource(): Flow<Resource<T>> =
    map<T, Resource<T>> { Resource.Success(it) }
        .onStart { emit(Resource.Loading) }
        .catch { emit(Resource.Error(it.message ?: "Unknown error", it)) }
```

- Wraps any Flow into Loading/Success/Error states
- Reusable pattern across the entire app

**File:** `core/common/Extensions.kt`

---

## Project Structure

```
com.vaibhav.restaurant/
├── RestaurantApp.kt                          # @HiltAndroidApp Application
├── MainActivity.kt                           # Single Activity, Compose host, connectivity banner
│
├── core/
│   ├── common/
│   │   ├── Resource.kt                       # Success/Error/Loading sealed interface
│   │   ├── UiEffect.kt                       # Global one-shot event sealed interface
│   │   └── Extensions.kt                     # Flow.asResource() extension
│   ├── connectivity/
│   │   ├── ConnectivityObserver.kt           # Interface
│   │   └── NetworkConnectivityObserver.kt    # callbackFlow implementation
│   ├── database/
│   │   ├── RestaurantDatabase.kt             # Room database
│   │   ├── dao/
│   │   │   ├── CartDao.kt                    # Cart DAO with Flow queries
│   │   │   └── OrderDao.kt                   # Order DAO with Flow queries
│   │   ├── entity/
│   │   │   ├── CartItemEntity.kt
│   │   │   └── OrderEntity.kt
│   │   └── converter/
│   │       └── Converters.kt                 # Room type converters
│   ├── datastore/
│   │   └── UserPreferences.kt               # DataStore preferences with Flow
│   ├── di/
│   │   └── AppModule.kt                     # Hilt DI module
│   └── network/
│       ├── FakeRestaurantApi.kt              # Mock API with delay simulation
│       └── dto/
│           ├── CategoryDto.kt
│           ├── MenuItemDto.kt
│           └── UserDto.kt
│
├── feature/
│   ├── auth/
│   │   ├── data/repository/
│   │   │   └── AuthRepository.kt            # flow {} builder, flowOn
│   │   ├── domain/model/
│   │   │   └── User.kt
│   │   └── presentation/
│   │       ├── LoginViewModel.kt             # Channel, onStart/onEach/catch/onCompletion
│   │       ├── LoginScreen.kt
│   │       ├── RegisterViewModel.kt
│   │       └── RegisterScreen.kt
│   │
│   ├── menu/
│   │   ├── data/repository/
│   │   │   └── MenuRepository.kt            # supervisorScope, async, flowOn
│   │   ├── domain/model/
│   │   │   ├── Category.kt
│   │   │   └── MenuItem.kt
│   │   └── presentation/
│   │       ├── HomeViewModel.kt              # combine, debounce, flatMapLatest, SharedFlow
│   │       ├── HomeScreen.kt
│   │       ├── MenuItemDetailViewModel.kt    # SavedStateHandle, update{}
│   │       └── MenuItemDetailScreen.kt
│   │
│   ├── cart/
│   │   ├── data/repository/
│   │   │   └── CartRepository.kt            # Mutex, Room Flow, map, flowOn
│   │   ├── domain/model/
│   │   │   └── CartItem.kt
│   │   └── presentation/
│   │       ├── CartViewModel.kt              # stateIn, WhileSubscribed
│   │       └── CartScreen.kt
│   │
│   ├── order/
│   │   ├── data/repository/
│   │   │   └── OrderRepository.kt           # channelFlow, callbackFlow, withTimeout
│   │   ├── domain/model/
│   │   │   └── Order.kt                     # OrderStatus enum
│   │   └── presentation/
│   │       ├── OrderTrackingViewModel.kt
│   │       ├── OrderTrackingScreen.kt
│   │       ├── OrderHistoryViewModel.kt      # Room Flow → stateIn
│   │       └── OrderHistoryScreen.kt
│   │
│   └── profile/
│       └── presentation/
│           ├── ProfileViewModel.kt           # DataStore Flow → stateIn
│           └── ProfileScreen.kt
│
├── navigation/
│   ├── Routes.kt                             # Type-safe @Serializable routes
│   └── AppNavGraph.kt                        # Compose Navigation graph
│
└── ui/theme/
    ├── Color.kt
    ├── Type.kt
    └── Theme.kt                              # Material 3 theme
```

---

## Getting Started

### Prerequisites

- Android Studio Ladybug (2024.2) or newer
- JDK 17
- Android SDK 35

### Build & Run

1. Clone the repository:
   ```bash
   git clone https://github.com/vaibhavtripathi-bit/android_restaurant.git
   ```

2. Open in Android Studio

3. Sync Gradle and run on an emulator or device (API 26+)

### Key Build Configuration

- **Gradle**: Kotlin DSL with Version Catalogs (`gradle/libs.versions.toml`)
- **Compose**: Compiler via Kotlin plugin (`kotlin-compose`)
- **DI**: Hilt with KSP (not kapt)
- **Serialization**: Kotlin Serialization for type-safe navigation routes and JSON

---

## Demo Credentials

| Field | Value |
|-------|-------|
| Email | `test@test.com` |
| Password | `password` |

Or register a new account with any email/password (data is stored locally).

---

## Summary of Coroutine/Flow Concepts

| # | Concept | File(s) |
|---|---------|---------|
| 1 | `StateFlow` / `MutableStateFlow` | All ViewModels |
| 2 | `Channel` (BUFFERED) + `receiveAsFlow` | All ViewModels (`UiEffect`) |
| 3 | `MutableSharedFlow` + `debounce` | `HomeViewModel` |
| 4 | `distinctUntilChanged` | `HomeViewModel`, `ConnectivityObserver` |
| 5 | `flatMapLatest` | `HomeViewModel` |
| 6 | `combine()` | `HomeViewModel` |
| 7 | `stateIn()` + `WhileSubscribed` | `CartViewModel`, `OrderHistoryViewModel`, `ProfileViewModel` |
| 8 | `flow { emit() }` | `AuthRepository`, `MenuRepository` |
| 9 | `flowOn(Dispatchers.IO)` | All Repositories |
| 10 | `onStart` / `onEach` / `onCompletion` | `LoginViewModel`, `RegisterViewModel` |
| 11 | `catch` | All ViewModels |
| 12 | `launchIn` | `LoginViewModel`, `RegisterViewModel` |
| 13 | `callbackFlow` + `awaitClose` | `NetworkConnectivityObserver`, `OrderRepository` |
| 14 | `channelFlow` | `OrderRepository` |
| 15 | `supervisorScope` + `async` | `MenuRepository` |
| 16 | `withTimeout` | `OrderRepository` |
| 17 | `Mutex` | `CartRepository` |
| 18 | `MutableStateFlow.update {}` | All ViewModels |
| 19 | `collectAsStateWithLifecycle()` | All Compose Screens |
| 20 | Room `Flow<List<T>>` | `CartDao`, `OrderDao` |
| 21 | DataStore `Flow<Preferences>` | `UserPreferences` |
| 22 | `Flow<T>.asResource()` extension | `Extensions.kt` |
| 23 | `viewModelScope.launch` | All ViewModels |
| 24 | `map` / `filter` | Repositories, ViewModels |
| 25 | `trySend` (non-suspending) | `NetworkConnectivityObserver` |
| 26 | `first()` terminal operator | `OrderRepository` |

---

## License

This project is for educational purposes. Feel free to use it as a reference for learning Kotlin Coroutines and Flow in Android development.
