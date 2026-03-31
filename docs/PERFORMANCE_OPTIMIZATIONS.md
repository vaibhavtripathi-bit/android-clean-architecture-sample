# Performance Optimizations — Kitchen Dashboard (500 Live Tickets)

This document details every performance optimization applied to the Kitchen Dashboard feature, which renders **500 live order tickets** with per-second countdown timers, animated progress bars, real-time status changes, and priority indicators — all at 60fps.

---

## Table of Contents

- [The Challenge](#the-challenge)
- [Architecture: Computation Boundary](#architecture-computation-boundary)
- [Optimization 1: All Computation in ViewModel on Dispatchers.Default](#optimization-1-all-computation-in-viewmodel-on-dispatchersdefault)
- [Optimization 2: @Immutable Data Classes](#optimization-2-immutable-data-classes)
- [Optimization 3: ImmutableList from kotlinx-collections-immutable](#optimization-3-immutablelist-from-kotlinx-collections-immutable)
- [Optimization 4: LazyColumn key for O(1) Diffing](#optimization-4-lazycolumn-key-for-o1-diffing)
- [Optimization 5: contentType for Node Reuse](#optimization-5-contenttype-for-node-reuse)
- [Optimization 6: Ticker Flow Instead of Timer/Handler](#optimization-6-ticker-flow-instead-of-timerhandler)
- [Optimization 7: Pre-Computed Strings](#optimization-7-pre-computed-strings)
- [Optimization 8: Extracted Composables for Recomposition Scoping](#optimization-8-extracted-composables-for-recomposition-scoping)
- [Optimization 9: remember for Lambda Stability](#optimization-9-remember-for-lambda-stability)
- [Optimization 10: animateFloatAsState for Progress Bars](#optimization-10-animatefloatasstate-for-progress-bars)
- [Optimization 11: stateIn with WhileSubscribed(5000)](#optimization-11-statein-with-whilesubscribed5000)
- [Optimization 12: combine() for Atomic State Updates](#optimization-12-combine-for-atomic-state-updates)
- [Optimization 13: flowOn(Dispatchers.Default) for Off-Main Computation](#optimization-13-flowondispatchersdefault-for-off-main-computation)
- [Common Bottlenecks Avoided](#common-bottlenecks-avoided)
- [How to Measure Performance](#how-to-measure-performance)
- [Summary Table](#summary-table)

---

## The Challenge

The Kitchen Dashboard must:

- Render **500 order tickets** in a scrollable list
- Update **every ticket's countdown timer** every second (500 timer recalculations/sec)
- Show **animated progress bars** that smoothly advance
- Display **real-time status changes** (Queued → In Progress → Almost Done → Ready)
- Support **filtering** by status and **sorting** by priority
- Allow **interactive actions** (advance status, bump priority) on each ticket
- Maintain **60fps** with zero frame drops during scrolling

Without optimization, this would cause:
- 500 recompositions per second (one per timer tick per visible item)
- String allocations in every composable frame
- Lambda allocations on every recomposition
- Full-list recomposition on any state change

---

## Architecture: Computation Boundary

The single most important design decision:

```
┌─────────────────────────────────────────────────────────┐
│                    ViewModel (Default Dispatcher)         │
│                                                          │
│  Raw Data + Current Time                                 │
│       ↓                                                  │
│  computeUiState() — ALL math, formatting, sorting        │
│       ↓                                                  │
│  ImmutableList<KitchenListItem> — fully pre-computed     │
└──────────────────────┬──────────────────────────────────┘
                       │ StateFlow (Main)
┌──────────────────────┴──────────────────────────────────┐
│                    Compose UI (Main Thread)               │
│                                                          │
│  ZERO computation — just reads pre-formatted strings     │
│  and displays them                                       │
└─────────────────────────────────────────────────────────┘
```

**File:** `KitchenViewModel.kt`

---

## Optimization 1: All Computation in ViewModel on Dispatchers.Default

### Problem
Computing elapsed time, formatting "3:45" strings, calculating progress fractions, and sorting 500 items on the main thread causes frame drops.

### Solution
Every computation happens in `computeUiState()` which runs on `Dispatchers.Default`:

```kotlin
val uiState: StateFlow<KitchenUiState> = combine(
    repository.tickets,
    ticker,
    _filterStatus,
    _sortByPriority
) { rawTickets, now, filterStatus, sortByPriority ->
    computeUiState(rawTickets, now, filterStatus, sortByPriority)
}
    .flowOn(Dispatchers.Default)  // ← All computation off main thread
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), KitchenUiState())
```

### What runs on Default
- Timer calculation for 500 tickets (`elapsedSeconds`, `remainingSeconds`)
- String formatting (`"3:45"`, `"OVERDUE 1:20"`)
- Progress fraction computation (`elapsed / estimated`)
- Overdue flag computation
- Item count display string (`"3 dishes, 7 items"`)
- Filtering by status
- Sorting by priority
- Grouping into sections with headers
- Building the final `ImmutableList`

### Impact
Main thread does ZERO math. It receives a fully pre-computed `KitchenUiState` and just displays it.

**File:** `KitchenViewModel.kt` lines 60-72

---

## Optimization 2: @Immutable Data Classes

### Problem
Compose cannot skip recomposition for a composable parameter unless it can prove the parameter hasn't changed. Regular `data class` instances are considered "unstable" by the Compose compiler if they contain mutable types.

### Solution
All UI model classes are annotated with `@Immutable`:

```kotlin
@Immutable
data class KitchenTicket(
    val id: String,
    val orderNumber: Int,
    val tableName: String,
    val items: ImmutableList<TicketItem>,
    val status: TicketStatus,
    val priority: TicketPriority,
    // ... all val, all primitive or @Immutable types
)
```

### How it helps
When the Compose compiler sees `@Immutable`, it generates code that compares by **reference equality** (`===`). If the same `KitchenTicket` instance is passed to `TicketCard`, the entire composable is **skipped** — no recomposition at all.

### Rule
Every field must be:
- A primitive (`Int`, `Float`, `Boolean`, `String`)
- An enum
- Another `@Immutable` type
- `ImmutableList` (not `List`)

**File:** `KitchenModels.kt`

---

## Optimization 3: ImmutableList from kotlinx-collections-immutable

### Problem
`List<T>` in Kotlin is an interface that could be backed by a `MutableList`. The Compose compiler treats `List` parameters as **unstable** — it cannot prove they haven't changed, so it always recomposes.

### Solution
Use `ImmutableList<T>` from `kotlinx-collections-immutable`:

```kotlin
@Immutable
data class KitchenUiState(
    val listItems: ImmutableList<KitchenListItem> = persistentListOf(),
    // ...
)
```

And in the ViewModel:
```kotlin
return result.toImmutableList()
```

### Impact
The Compose compiler treats `ImmutableList` as stable. When the list reference is the same, Compose skips recomposition of the entire `LazyColumn` items block.

**Dependency:** `org.jetbrains.kotlinx:kotlinx-collections-immutable`

**File:** `KitchenModels.kt`, `KitchenViewModel.kt`

---

## Optimization 4: LazyColumn key for O(1) Diffing

### Problem
Without keys, `LazyColumn` uses positional identity. If an item is inserted at position 0, every item shifts and every composable is recreated.

### Solution
Every item has a stable, unique key:

```kotlin
LazyColumn {
    items(
        items = uiState.listItems,
        key = { it.stableId },  // ← "TKT-0001", "header_QUEUED", etc.
        // ...
    ) { item -> ... }
}
```

### How it helps
- LazyColumn uses keys to **diff** the old and new lists
- Items that didn't change are **skipped entirely**
- Items that moved are **repositioned** without recreation
- Only items with changed data are **recomposed**

### Key design
- Ticket items: `stableId = ticket.id` (e.g., `"TKT-0042"`)
- Section headers: `stableId = "header_${status.name}"` (e.g., `"header_QUEUED"`)

**File:** `KitchenDashboardScreen.kt` LazyColumn block

---

## Optimization 5: contentType for Node Reuse

### Problem
`LazyColumn` creates compose nodes for visible items. When scrolling, it recycles off-screen nodes for new items. But if a header node is reused for a ticket, the entire compose tree must be rebuilt.

### Solution
`contentType` tells LazyColumn to only reuse nodes of the same type:

```kotlin
items(
    items = uiState.listItems,
    key = { it.stableId },
    contentType = { item ->
        when (item) {
            is KitchenListItem.Header -> ListContentType.HEADER
            is KitchenListItem.Ticket -> ListContentType.TICKET
        }
    }
) { item -> ... }
```

### How it helps
- Header compose nodes are only reused for other headers
- Ticket compose nodes are only reused for other tickets
- This is the Compose equivalent of `RecyclerView.getItemViewType()`
- Reduces compose tree rebuilds during fast scrolling

**File:** `KitchenDashboardScreen.kt` LazyColumn block

---

## Optimization 6: Ticker Flow Instead of Timer/Handler

### Problem
Using `java.util.Timer`, `Handler.postDelayed`, or `CoroutineScope.launch { while(true) }` outside of a flow can cause:
- Memory leaks if not properly cancelled
- Race conditions with state updates
- Lifecycle issues

### Solution
A cold `flow` builder with `delay`:

```kotlin
private val ticker = flow {
    while (true) {
        emit(System.currentTimeMillis())
        delay(1000)
    }
}
```

### Why this is better
- **Lifecycle-safe**: The flow is collected via `combine` → `stateIn` → `viewModelScope`. When the ViewModel is cleared, the scope is cancelled, and the ticker stops automatically.
- **No leaks**: No `Timer` to cancel, no `Handler` to remove callbacks.
- **Backpressure**: If computation takes >1s, the next tick is naturally delayed — no pile-up.
- **Testable**: In tests, you can use `TestCoroutineScheduler` to advance time.

**File:** `KitchenViewModel.kt` ticker property

---

## Optimization 7: Pre-Computed Strings

### Problem
Calling `String.format()`, string interpolation, or `.joinToString()` inside a `@Composable` function causes **allocations on every recomposition**. With 500 items recomposing every second, that's thousands of string allocations per second.

### Solution
All strings are computed in the ViewModel and stored as `val` properties:

```kotlin
// In ViewModel's computeTicket():
val remainingTimeFormatted = if (isOverdue) {
    "OVERDUE ${formatTime(overdueSeconds)}"
} else {
    formatTime(remainingSeconds)
}

val itemCountDisplay = "${raw.items.size} dish${if (...) "es" else ""}, ..."
```

The composable just reads them:
```kotlin
// In TicketCard composable — ZERO computation:
Text(text = ticket.remainingTimeFormatted)
Text(text = ticket.itemCountDisplay)
```

### Impact
Zero string allocations in composable functions. The UI thread only reads pre-computed `String` values.

**File:** `KitchenViewModel.kt` `computeTicket()`, `KitchenDashboardScreen.kt` `TicketCard()`

---

## Optimization 8: Extracted Composables for Recomposition Scoping

### Problem
If the entire screen is one big composable, changing any state recomposes everything.

### Solution
Each visual section is a separate composable with its own recomposition scope:

```
KitchenDashboardScreen
├── StatsBar(stats)              ← only recomposes when stats change
├── FilterRow(selectedStatus)    ← only recomposes when filter changes
└── LazyColumn
    ├── SectionHeader(title, count)  ← only recomposes when section changes
    └── TicketCard(ticket, ...)      ← only recomposes when THIS ticket changes
```

### How it helps
- `StatsBar` doesn't recompose when a ticket changes
- `FilterRow` doesn't recompose when timers tick
- Each `TicketCard` only recomposes when its specific `KitchenTicket` reference changes
- `SectionHeader` only recomposes when the count changes

**File:** `KitchenDashboardScreen.kt` — all private composables

---

## Optimization 9: remember for Lambda Stability

### Problem
Lambdas created inside `items {}` are new instances on every recomposition. This makes the lambda parameter "unstable" and forces the child composable to recompose even if nothing else changed.

```kotlin
// BAD: new lambda every recomposition
TicketCard(
    onAdvanceStatus = { viewModel.onEvent(KitchenEvent.AdvanceTicket(item.ticket.id)) }
)
```

### Solution
Use `remember` keyed on the stable ticket ID:

```kotlin
val onAdvance = remember(item.ticket.id) {
    { viewModel.onEvent(KitchenEvent.AdvanceTicket(item.ticket.id)) }
}
TicketCard(
    onAdvanceStatus = onAdvance  // ← same instance across recompositions
)
```

### How it helps
- The lambda instance is **stable** — same reference as long as the key doesn't change
- Compose can skip recomposition of `TicketCard` if the ticket data AND lambdas are the same
- Eliminates ~500 lambda allocations per second (one per visible ticket per tick)

**File:** `KitchenDashboardScreen.kt` LazyColumn items block

---

## Optimization 10: animateFloatAsState for Progress Bars

### Problem
Directly setting `progress` on `LinearProgressIndicator` causes a hard jump every second. Using a custom animation loop would add complexity and potential frame drops.

### Solution
`animateFloatAsState` with a tween:

```kotlin
val animatedProgress by animateFloatAsState(
    targetValue = ticket.progressFraction.coerceIn(0f, 1f),
    animationSpec = tween(durationMillis = 800),
    label = "progress"
)

LinearProgressIndicator(progress = { animatedProgress })
```

### How it helps
- Smooth animation between tick values (no jarring jumps)
- Animation runs on the render thread, not the main thread
- The `label` parameter helps identify this animation in Layout Inspector
- Using `progress = { animatedProgress }` (lambda form) avoids recomposing `LinearProgressIndicator` — only the drawing changes

**File:** `KitchenDashboardScreen.kt` `TicketCard()`

---

## Optimization 11: stateIn with WhileSubscribed(5000)

### Problem
Using `stateIn(SharingStarted.Eagerly)` keeps the upstream flow (including the ticker) running even when the screen is in the background, wasting CPU and battery.

### Solution
```kotlin
.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = KitchenUiState()
)
```

### How it helps
- **WhileSubscribed(5000)**: Keeps the upstream alive for 5 seconds after the last subscriber disappears
- During configuration changes (rotation), the 5s window prevents restarting the ticker
- When the user navigates away, the ticker stops after 5 seconds — no background CPU usage
- Combined with `collectAsStateWithLifecycle()` in the UI, collection stops when the lifecycle drops below STARTED

**File:** `KitchenViewModel.kt`

---

## Optimization 12: combine() for Atomic State Updates

### Problem
If filter and sort state are updated separately and each triggers a recomputation, you get two rapid state emissions — the UI renders an intermediate state.

### Solution
`combine()` merges all 4 input flows atomically:

```kotlin
combine(
    repository.tickets,    // raw data
    ticker,                // current time (every 1s)
    _filterStatus,         // user's filter selection
    _sortByPriority        // user's sort toggle
) { rawTickets, now, filterStatus, sortByPriority ->
    computeUiState(rawTickets, now, filterStatus, sortByPriority)
}
```

### How it helps
- All inputs are combined into a single emission
- No intermediate states reach the UI
- The computation runs once per tick, not once per changed input

**File:** `KitchenViewModel.kt`

---

## Optimization 13: flowOn(Dispatchers.Default) for Off-Main Computation

### Problem
`combine()` by default runs on the collector's dispatcher (Main). Computing 500 tickets' derived state on Main causes frame drops.

### Solution
```kotlin
.flowOn(Dispatchers.Default)
```

### How it helps
- The `computeUiState()` function (sorting, filtering, formatting 500 items) runs on the Default dispatcher (thread pool)
- Only the final `StateFlow` emission happens on Main
- Main thread workload: receive one `KitchenUiState` object and trigger recomposition

**File:** `KitchenViewModel.kt`

---

## Common Bottlenecks Avoided

| Bottleneck | How We Avoid It |
|---|---|
| **String allocation in composables** | All strings pre-computed in ViewModel |
| **Lambda allocation in LazyColumn items** | `remember(key)` for callbacks |
| **Full-list recomposition on tick** | `@Immutable` + `ImmutableList` + `key` |
| **Main thread computation** | `flowOn(Dispatchers.Default)` |
| **Timer memory leaks** | `flow { delay() }` auto-cancelled with scope |
| **Unstable List parameters** | `ImmutableList` from kotlinx-collections |
| **ViewHolder type mismatch** | `contentType` for headers vs tickets |
| **Background CPU waste** | `WhileSubscribed(5000)` stops ticker when away |
| **Intermediate state flicker** | `combine()` for atomic multi-source updates |
| **Progress bar jank** | `animateFloatAsState` with lambda-based progress |

---

## How to Measure Performance

### 1. Compose Recomposition Counter
In Android Studio: Layout Inspector → Show Recomposition Counts

Expected: Only visible `TicketCard` composables recompose each second. `StatsBar` recomposes once per second. `FilterRow` only on filter change.

### 2. GPU Profiling
Developer Options → Profile GPU rendering → On screen as bars

Expected: All bars below the green 16ms line during scrolling.

### 3. Systrace / Perfetto
```bash
adb shell perfetto -o /data/misc/perfetto-traces/trace.perfetto-trace -t 10s \
  sched freq idle am wm gfx view binder_driver hal dalvik camera input res memory
```

Look for: Main thread frame time < 16ms, no GC pauses during scroll.

### 4. Memory Profiler
Android Studio → Profiler → Memory

Expected: No growing allocation trend. Flat memory during scrolling. No leaked `KitchenTicket` instances.

### 5. Compose Metrics (Build-time)
Add to `build.gradle.kts`:
```kotlin
kotlinOptions {
    freeCompilerArgs += listOf(
        "-P", "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=${buildDir}/compose_metrics",
        "-P", "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=${buildDir}/compose_metrics"
    )
}
```

Check the generated report for:
- `restartable` vs `skippable` composables (all should be skippable)
- Unstable parameters (should be none in kitchen feature)

---

## Summary Table

| # | Optimization | Where | What It Prevents |
|---|---|---|---|
| 1 | Dispatchers.Default computation | ViewModel | Main thread jank |
| 2 | @Immutable data classes | KitchenModels.kt | Unnecessary recomposition |
| 3 | ImmutableList | KitchenModels.kt, ViewModel | List instability |
| 4 | LazyColumn key | Screen | O(n) diffing, node recreation |
| 5 | contentType | Screen | Cross-type node reuse |
| 6 | Ticker flow | ViewModel | Memory leaks, lifecycle issues |
| 7 | Pre-computed strings | ViewModel | Per-frame allocations |
| 8 | Extracted composables | Screen | Blast-radius recomposition |
| 9 | remember lambdas | Screen | Lambda instability |
| 10 | animateFloatAsState | Screen | Progress bar jank |
| 11 | WhileSubscribed(5000) | ViewModel | Background CPU waste |
| 12 | combine() | ViewModel | Intermediate state flicker |
| 13 | flowOn(Default) | ViewModel | Main thread blocking |

---

## Files

| File | Role |
|---|---|
| `feature/kitchen/domain/model/KitchenModels.kt` | @Immutable models, ImmutableList, sealed interface |
| `feature/kitchen/data/repository/KitchenRepository.kt` | 500 ticket generation, state mutations |
| `feature/kitchen/presentation/KitchenViewModel.kt` | Ticker flow, combine, computeUiState, Dispatchers.Default |
| `feature/kitchen/presentation/KitchenDashboardScreen.kt` | LazyColumn with key/contentType, extracted composables, remember lambdas |
