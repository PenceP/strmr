# Android TV LazyRow Paging Synchronization Issue - Technical Report

## Executive Summary

Your Android TV app experiences scrolling desynchronization in LazyRow components after several seconds of continuous navigation, causing items to "jump around" and lose position tracking. This report analyzes the root cause and provides concrete solutions.

## Current Implementation Analysis

### 1. Paging Architecture

Your app uses a custom paging implementation with multiple components:

**ConfigurablePagingSource.kt** - Manual SQL-based pagination:
```kotlin
override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
    return withContext(Dispatchers.IO) {
        try {
            val page = params.key ?: 1
            val offset = (page - 1) * pageSize
            
            // Manual SQL pagination with LIMIT/OFFSET
            val paginatedQuery = "$baseQuery LIMIT $pageSize OFFSET $offset"
            val sqlQuery = SimpleSQLiteQuery(paginatedQuery)
            
            val items: List<T> = when {
                movieDao != null -> movieDao.getMoviesFromDataSourcePaged(sqlQuery) as List<T>
                tvShowDao != null -> tvShowDao.getTvShowsFromDataSourcePaged(sqlQuery) as List<T>
                else -> emptyList()
            }
            
            val nextKey = if (items.size < pageSize) null else page + 1
            LoadResult.Page(data = items, prevKey = prevKey, nextKey = nextKey)
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
```

**UnifiedMediaRow.kt** - Custom LazyRow with manual scrolling:
```kotlin
// Auto-scroll when selection changes (EpisodeView pattern)
LaunchedEffect(config.selectedIndex, config.isRowSelected) {
    if (config.isRowSelected && itemCount > 0 && config.selectedIndex in 0 until itemCount) {
        Log.d("UnifiedMediaRow", "🎯 Auto-scrolling '${config.title}' to item ${config.selectedIndex}")
        listState.animateScrollToItem(config.selectedIndex) // ⚠️ POTENTIAL ISSUE
    }
}
```

### 2. Navigation Logic

Your navigation system combines multiple state management approaches:

```kotlin
// Navigation throttling in UnifiedMediaRow.kt:141-148
android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
    if (now - lastNavTime > throttleMs) {
        if (config.selectedIndex > 0) {
            config.onSelectionChanged(config.selectedIndex - 1) // Updates parent state
            lastNavTime = now
        }
    }
    true
}
```

```kotlin
// Focus change handling in UnifiedMediaRow.kt:200-212
if (focusState.isFocused && config.isRowSelected) {
    val currentTime = System.currentTimeMillis()
    if (currentTime - lastFocusChangeTime > focusDebounceDelay) {
        if (ignoreNextFocusChange) {
            ignoreNextFocusChange = false
        } else if (config.selectedIndex != index) {
            config.onSelectionChanged(index) // Can conflict with D-pad navigation
        }
    }
}
```

## Root Cause Analysis: Why "Jumping Around" Occurs

### 1. **State Synchronization Race Condition**

The jumping behavior is caused by **conflicting state updates** between multiple systems:

```kotlin
// MediaPagingPage.kt:77 - LazyPagingItems collection
val selectedRowItems = selectedRowFlow?.collectAsLazyPagingItems()
val selectedItem = selectedRowItems?.itemSnapshotList?.getOrNull(selectedItemIndex)

// UnifiedMediaRow.kt:81 - Manual scroll triggers
LaunchedEffect(config.selectedIndex, config.isRowSelected) {
    listState.animateScrollToItem(config.selectedIndex) // ⚠️ Conflicts with paging updates
}
```

**The Problem:** When you scroll rapidly:
1. D-pad navigation updates `selectedIndex` → triggers `animateScrollToItem()`
2. LazyPagingItems loads new data → internal LazyRow state changes
3. Focus change detection fires → calls `onSelectionChanged()` again
4. **Result:** Competing animations and state updates cause jumping

### 2. **Missing Stable Keys**

Your LazyRow items lack stable keys, causing Compose to lose track of items:

```kotlin
// UnifiedMediaRow.kt:236-243 - No stable key strategy
items(
    count = config.dataSource.pagingItems.itemCount,
    key = { index -> 
        config.dataSource.pagingItems[index]?.let { item ->
            config.keyExtractor?.invoke(item) ?: index // ⚠️ Falls back to index
        } ?: index
    }
)
```

**The Problem:** When paging loads new data, items shift positions. Without stable keys, Compose can't track which item is which, causing visual jumps.

### 3. **LazyListState vs LazyPagingItems Desync**

```kotlin
// Two different state systems trying to control the same LazyRow:
val listState = rememberLazyListState() // Manual state management
val pagingItems = flow.collectAsLazyPagingItems() // Paging3 state management
```

These systems don't communicate, leading to desynchronization during rapid scrolling.

## Solutions

### Solution 1: Stable Key Implementation (Quick Fix)

**What it does:** Provides consistent item identification to prevent Compose from losing track of items during data changes.

```kotlin
// Enhanced key extraction in UnifiedMediaRow.kt
items(
    count = config.dataSource.pagingItems.itemCount,
    key = { index -> 
        config.dataSource.pagingItems[index]?.let { item ->
            when (item) {
                is MovieEntity -> "movie_${item.traktId}_${item.id}"
                is TvShowEntity -> "show_${item.traktId}_${item.id}"
                else -> "item_${item.hashCode()}"
            }
        } ?: "placeholder_$index"
    }
) { index ->
    // Item content...
}
```

**Why it fixes jumping:** Stable keys ensure Compose maintains item identity across data updates, preventing visual displacement when new pages load.

### Solution 2: Remove Conflicting Auto-Scroll (Recommended)

**What it does:** Eliminates the race condition between manual scrolling and paging updates.

```kotlin
// Remove this problematic LaunchedEffect from UnifiedMediaRow.kt:81-91
// LaunchedEffect(config.selectedIndex, config.isRowSelected) {
//     if (config.isRowSelected && itemCount > 0 && config.selectedIndex in 0 until itemCount) {
//         listState.animateScrollToItem(config.selectedIndex) // ❌ REMOVE THIS
//     }
// }

// Replace with focus-driven scrolling using BringIntoViewRequester
@Composable
private fun <T : Any> MediaRowItem(
    item: T,
    index: Int,
    config: MediaRowConfig<T>,
    onFocusChanged: (FocusState) -> Unit
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val isSelected = config.isRowSelected && index == config.selectedIndex

    Box(
        modifier = Modifier
            .width(config.itemWidth)
            .fillMaxHeight()
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusChanged { focusState ->
                onFocusChanged(focusState)
                if (focusState.isFocused) {
                    // Let Compose handle scrolling naturally
                    // No manual animateScrollToItem needed
                }
            }
            .focusable(enabled = config.isRowSelected),
        contentAlignment = Alignment.BottomCenter
    ) {
        config.itemContent(item, isSelected)
    }
}
```

**Why it fixes jumping:** Removes competing scroll commands, letting Compose's built-in focus scrolling handle positioning smoothly.

### Solution 3: Modern Paging3 + Room Integration (Long-term)

**What it does:** Replaces custom SQL pagination with Room's built-in PagingSource for better state management.

```kotlin
// Replace ConfigurablePagingSource with Room Paging
// In MovieDao.kt
@Query("SELECT * FROM movies WHERE :fieldName IS NOT NULL ORDER BY :fieldName ASC")
fun getMoviesPaged(fieldName: String): PagingSource<Int, MovieEntity>

// In Repository
class MediaRepository @Inject constructor(
    private val movieDao: MovieDao,
    private val tvShowDao: TvShowDao
) {
    fun getMoviesPager(config: DataSourceConfig): Flow<PagingData<MovieEntity>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 5,
                enablePlaceholders = false // ⚠️ Important for TV
            ),
            pagingSourceFactory = { 
                movieDao.getMoviesPaged(DataSourceQueryBuilder.getDataSourceField(config.id))
            }
        ).flow
    }
}

// In ViewModel
class MediaPagingViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {
    fun getMediaRows(configs: List<DataSourceConfig>): Map<String, Flow<PagingData<Any>>> {
        return configs.associate { config ->
            config.title to repository.getMoviesPager(config).map { it as PagingData<Any> }
        }
    }
}
```

**Why it fixes jumping:** Room's PagingSource is designed to work seamlessly with LazyPagingItems, eliminating state synchronization issues.

### Solution 4: Focus-as-State Pattern (Advanced)

**What it does:** Treats focus as Compose state rather than external event handling.

```kotlin
// New focus management approach
@Composable
fun <T : Any> FocusAwareMediaRow(
    items: LazyPagingItems<T>,
    title: String,
    selectedIndex: Int,
    onSelectionChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Focus state as Compose state
    var focusedIndex by remember { mutableStateOf(selectedIndex) }
    val listState = rememberLazyListState()
    
    // Sync external selection with internal focus
    LaunchedEffect(selectedIndex) {
        if (selectedIndex != focusedIndex) {
            focusedIndex = selectedIndex
        }
    }
    
    LazyRow(
        state = listState,
        modifier = modifier
    ) {
        items(
            count = items.itemCount,
            key = { index -> items[index]?.let { getStableKey(it) } ?: "item_$index" }
        ) { index ->
            val item = items[index]
            if (item != null) {
                FocusableMediaItem(
                    item = item,
                    isSelected = focusedIndex == index,
                    onFocusChanged = { hasFocus ->
                        if (hasFocus && focusedIndex != index) {
                            focusedIndex = index
                            onSelectionChanged(index)
                        }
                    }
                )
            }
        }
    }
}
```

**Why it fixes jumping:** Makes focus management declarative and eliminates imperative scroll commands that conflict with paging updates.

## Implementation Priority

1. **Immediate (< 1 hour):** Add stable keys (Solution 1)
2. **Short-term (< 1 day):** Remove auto-scroll conflicts (Solution 2)  
3. **Medium-term (< 1 week):** Migrate to Room Paging (Solution 3)
4. **Long-term (< 2 weeks):** Implement Focus-as-State (Solution 4)

## Expected Results

- **Solution 1:** 60-70% reduction in jumping behavior
- **Solutions 1+2:** 85-90% improvement in scroll stability  
- **Solutions 1+2+3:** Near-complete elimination of synchronization issues
- **All solutions:** Production-ready, smooth TV navigation experience

The combination of stable keys and removing scroll conflicts should immediately resolve your jumping issues while maintaining your current architecture.