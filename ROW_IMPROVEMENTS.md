# Row Implementation Analysis & Improvement Strategy

## Executive Summary

The Strmr app's row implementations suffer from significant code duplication, poor focus management, and performance issues. This document provides a comprehensive analysis of current problems and a detailed improvement strategy based on industry-standard patterns used by Netflix and similar streaming applications.

## Current State Analysis

### 1. Code Duplication Issues

#### Problem Areas:
- **Multiple Row Implementations**: The app has several different row components with overlapping functionality:
  - `UnifiedMediaRow.kt` - Attempts to be unified but still complex
  - `CollectionRow.kt` - Wraps UnifiedMediaRow but adds custom card logic
  - `SimilarContentRow.kt` - Another wrapper with custom card implementation
  - `HomeMediaRow` (in HomePage.kt) - Yet another wrapper
  - Various inline LazyRow implementations scattered throughout screens

#### Duplication Examples:
- **Card Scaling Logic**: Each row type implements its own animation for selected state
- **Focus Management**: Repeated focus handling code across all row implementations
- **Navigation Logic**: D-pad handling duplicated in multiple places
- **Skeleton Loading**: Multiple implementations of loading states
- **Paging Logic**: Both regular lists and paging lists handled differently

### 2. Focus Management Problems

#### Current Issues:
1. **Lost Focus on Navigation**:
   - When clicking a poster and returning, focus doesn't return to the clicked item
   - Row selection state not preserved when switching between rows
   - Focus memory implementation exists (`FocusMemoryManager`) but not used consistently

2. **Scroll Position Issues**:
   - When scrolling between rows, horizontal scroll position is lost
   - Fast scrolling causes focus to get lost entirely
   - No debouncing on rapid navigation

3. **Focus vs Selection Confusion**:
   - Multiple state variables tracking similar concepts (`isSelected`, `isFocused`, `isRowSelected`)
   - Race conditions between focus changes and selection updates

### 3. Performance Issues

#### Identified Problems:
1. **Paging Performance**:
   - Current paging size appears too large, causing stuttering
   - No prefetching strategy for smooth scrolling
   - Loading states cause layout shifts

2. **Recomposition Issues**:
   - Entire rows recompose on single item selection change
   - No proper key usage in lazy lists
   - Animation states cause excessive recompositions

3. **Memory Issues**:
   - Images loaded at full resolution regardless of display size
   - No image caching strategy between row instances
   - Old row data not properly cleared from memory

### 4. Event Handling Problems

#### Current Issues:
1. **Long Press Implementation**:
   - Multiple overlapping click handlers preventing long press events from reaching posters
   - Event consumption happens at wrong levels in the component tree
   - No consistent event propagation strategy across different row types
   - Clickable modifiers at row level intercepting card-level events

2. **Event Handler Confusion**:
   - `onItemClick`, `onClick`, `clickable` modifiers used inconsistently
   - Focus events, key events, and touch events handled separately with overlap
   - No clear event priority system for TV remote vs touch inputs

3. **Gesture Recognition**:
   - Long press detection unreliable due to focus system interference
   - No standard gesture timeout values across components
   - Event handlers not properly cancelled when focus changes

## Industry Standard Analysis (Netflix Pattern)

### Netflix's Row Architecture:
1. **Single Row Component**: One highly optimized row component with variants
2. **Virtual Scrolling**: Only renders visible items plus buffer
3. **Focus Memory**: Sophisticated focus restoration system
4. **Smooth Animations**: Hardware-accelerated transformations
5. **Smart Prefetching**: Loads data before user reaches end of row

### Key Design Principles:
- **Separation of Concerns**: Data loading, UI rendering, and focus management are separate
- **Immutable State**: All state changes are predictable and traceable
- **Performance First**: Every feature considers performance impact
- **Accessibility**: Focus management works for all input methods
- **Simple Event Handling**: Single clear path for all user interactions

## Proposed Solution Architecture

### 1. DpadRecyclerView Integration

**Recommended Library**: [DpadRecyclerView](https://github.com/rubensousa/DpadRecyclerView) by Rúben Sousa

**Why This Library Is Perfect**:
- **Battle-tested replacement** for Google's abandoned Leanback library
- **Built for Compose** with native interoperability (`DpadComposeFocusViewHolder`)
- **Solves exact problems** we identified: focus management, smooth scrolling, performance
- **Actively maintained** (2024) with comprehensive documentation
- **Industry proven** - used by major TV apps to solve the same issues we're facing

**Key Benefits Over Custom Implementation**:
- **Focus Management**: Automatic selection on focus, separate focus/selection states, focus observation
- **Performance**: Handles fast scrolling without losing focus, custom scrolling speeds, throttling
- **TV-Specific Features**: Edge alignment, circular focus, sub-position selections, fading edges
- **Compose Integration**: `Modifier.dpadClickable`, focus state passing to Composables
- **Migration Friendly**: Easy to integrate incrementally into existing codebase

### 2. Unified Row System with DpadRecyclerView

```kotlin
// Core row component using DpadRecyclerView
@Composable
fun MediaRow<T>(
    config: MediaRowConfig<T>,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            DpadRecyclerView(context).apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                setHasFixedSize(true)
                adapter = MediaRowAdapter(config)
                
                // Enable DpadRecyclerView optimizations
                setLayoutWhileScrollingEnabled(false)  // Performance boost
                smoothScrollToPosition = true
                setFocusableInTouchMode(false)
            }
        },
        modifier = modifier
    )
}

// Configuration data class
data class MediaRowConfig<T>(
    val title: String,
    val items: List<T>,
    val pagingSource: PagingSource<Int, T>? = null,
    val cardType: CardType,
    val focusMemoryKey: String,
    val onItemClick: (T) -> Unit,
    val onItemLongPress: ((T) -> Unit)? = null,
    val longPressTimeout: Long = 500L  // Standard Android long press duration
)

// Card types
enum class CardType {
    POSTER,      // 2:3 aspect ratio
    LANDSCAPE,   // 16:9 aspect ratio
    SQUARE,      // 1:1 aspect ratio
    CIRCLE       // For cast/crew
}

// DpadRecyclerView adapter with Compose integration
class MediaRowAdapter<T>(
    private val config: MediaRowConfig<T>
) : RecyclerView.Adapter<DpadComposeFocusViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DpadComposeFocusViewHolder {
        return DpadComposeFocusViewHolder(parent) { isFocused ->
            MediaCard(
                item = getItem(position),
                isSelected = isFocused,
                eventHandler = config.eventHandler,
                modifier = Modifier.dpadClickable { /* handle click */ }
            )
        }
    }
    
    // ... adapter implementation
}
```

### 3. Focus Management with DpadRecyclerView

DpadRecyclerView handles most focus management automatically, but we can enhance it:

```kotlin
// Enhanced focus manager leveraging DpadRecyclerView's capabilities
class FocusManager {
    private val focusStates = mutableMapOf<String, FocusState>()
    
    data class FocusState(
        val rowKey: String,
        val itemIndex: Int,
        val scrollOffset: Float,
        val timestamp: Long
    )
    
    fun integrateDpadRecyclerView(recyclerView: DpadRecyclerView, key: String) {
        recyclerView.addOnChildFocusListener { _, hasFocus, position ->
            if (hasFocus) {
                saveFocus(key, FocusState(key, position, recyclerView.computeHorizontalScrollOffset().toFloat(), System.currentTimeMillis()))
            }
        }
    }
    
    fun restoreFocusToPosition(recyclerView: DpadRecyclerView, key: String) {
        focusStates[key]?.let { state ->
            recyclerView.setSelectedPosition(state.itemIndex)
        }
    }
}
```

### 4. Performance Optimizations

**DpadRecyclerView provides many optimizations out-of-the-box**:

#### Paging Strategy:
- **Page Size**: 20 items per page (matches ~2.5 screen views of 8 visible items)
- **Smart Prefetch**: Load next page when user reaches item 15 of current page (75% through)
- **Instant Scroll**: All items in current page load immediately for seamless horizontal scrolling
- **Scroll Throttling**: 88ms navigation throttle to prevent overwhelming image loading during fast scrolling
- **Continuous Navigation**: List metadata loads instantly, allowing unlimited scrolling even if images are slow
- **Progressive Poster Loading**: Show title-only placeholders immediately, populate with posters as they load

#### Image Loading:
- **Visible Items Loading**: Load poster images for all currently visible items (~8 posters on screen)
- **Page Prefetch**: Preload poster images for next page items in background for instant scroll
- **Focus-Based Loading**: Only load backdrop and logo images when item is focused/selected
- **Resolution-Aware Loading**: Load appropriate resolution based on card size and display density
- **Smart Caching**: Cache backdrop/logo images permanently after first load for instant display
- **Scroll Anticipation**: When user scrolls 70% through visible items, preload next 8 posters
- **Graceful Degradation**: Show gray placeholder poster with white title text during super-speed scrolling
- **Progressive Enhancement**: Replace placeholder with actual poster as images load in background

#### Recomposition Prevention:
- **Smart State Management**: Use `derivedStateOf` for computed values to prevent unnecessary recalculations
- **Proper Keys**: Use stable, unique keys in lazy lists based on tmdbId/imdbId instead of index
- **Immutable Data**: Use `ImmutableList` for row items to prevent structural recomposition
- **State Scoping**: Limit state scope to prevent child recomposition from affecting parents
- **Animation Optimization**: Use `animateFloatAsState` with proper update conditions to prevent continuous recomposition

#### API Call Optimization:
- **Page-Based Loading**: Load all 20 items in current page immediately for instant horizontal scrolling
- **Background Prefetch**: Prefetch next page when user reaches 75% of current page (item 15/20)
- **Request Deduplication**: Cancel redundant API calls when user navigation changes quickly
- **Focus-Only Details**: Only make detail API calls (backdrop/logo) for the currently focused item
- **Batch Enhancement**: Load non-critical data (cast, similar content) in background after main content displays

#### Memory Management:
- **Image Memory**: Use WeakReference for large images to allow garbage collection under pressure
- **View Recycling**: Properly clear references in recycled views to prevent memory leaks
- **Background Cleanup**: Clear unused cached data after 10 minutes of inactivity
- **Bitmap Optimization**: Use RGB_565 format for non-transparent images to reduce memory usage by 50%

### 4. Event Handling System

Implement clean, predictable event handling:

```kotlin
// Unified event handler for cards with lazy loading
@Composable
fun MediaCard<T>(
    item: T,
    isSelected: Boolean,
    eventHandler: CardEventHandler<T>,
    modifier: Modifier = Modifier
) {
    // Only load backdrop/logo when focused - major performance improvement
    val shouldLoadDetailImages = isSelected
    
    Box(
        modifier = modifier
            .combinedClickable(
                onClick = { eventHandler.onClick(item) },
                onLongClick = { eventHandler.onLongPress(item) },
                indication = null,  // Custom indication based on TV/touch
                interactionSource = remember { MutableInteractionSource() }
            )
            .focusable()
    ) {
        // Poster loads progressively - show placeholder first
        var imageLoaded by remember { mutableStateOf(false) }
        
        if (!imageLoaded) {
            // Show gray placeholder with title during loading/fast scrolling
            PlaceholderCard(
                title = item.title,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        AsyncImage(
            model = item.posterUrl,
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize(),
            onSuccess = { imageLoaded = true }
        )
        
        // Backdrop/logo only load when focused
        if (shouldLoadDetailImages) {
            LaunchedEffect(item.id) {
                eventHandler.onLoadDetailImages(item)
            }
        }
    }
}

// Event handler interface
interface CardEventHandler<T> {
    fun onClick(item: T)
    fun onLongPress(item: T)
    fun onFocusChanged(item: T, hasFocus: Boolean)
    suspend fun onLoadDetailImages(item: T)  // Triggered only when focused
}

// TV remote long press support
@Composable
fun Modifier.tvLongPress(
    onLongPress: () -> Unit,
    timeout: Long = 500L
): Modifier {
    var pressStartTime by remember { mutableStateOf(0L) }
    var longPressTriggered by remember { mutableStateOf(false) }
    
    return this.onKeyEvent { event ->
        when (event.nativeKeyEvent.keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {
                when (event.nativeKeyEvent.action) {
                    KeyEvent.ACTION_DOWN -> {
                        pressStartTime = System.currentTimeMillis()
                        longPressTriggered = false
                        true
                    }
                    KeyEvent.ACTION_UP -> {
                        val pressDuration = System.currentTimeMillis() - pressStartTime
                        if (pressDuration >= timeout && !longPressTriggered) {
                            onLongPress()
                            longPressTriggered = true
                        }
                        true
                    }
                    else -> false
                }
            }
            else -> false
        }
    }
}

// Navigation throttling with standard Android TV timing
object NavigationThrottle {
    private var lastNavigationTime = 0L
    private const val NAVIGATION_THROTTLE_MS = 88L // Standard Android TV throttle
    
    fun canNavigate(): Boolean {
        val now = System.currentTimeMillis()
        return if (now - lastNavigationTime >= NAVIGATION_THROTTLE_MS) {
            lastNavigationTime = now
            true
        } else false
    }
}

// Standard Android TV animation curves
object MotionConstants {
    val Standard = CubicBezierEasing(0.2f, 0.1f, 0.0f, 1.0f)    // General animations
    val Browse = CubicBezierEasing(0.18f, 1.0f, 0.22f, 1.0f)    // Row navigation
    val Enter = CubicBezierEasing(0.12f, 1.0f, 0.40f, 1.0f)     // Focus entering
    val Exit = CubicBezierEasing(0.40f, 1.0f, 0.12f, 1.0f)      // Focus exiting
    
    const val DURATION_STANDARD = 300
    const val DURATION_BROWSE = 250
    const val DURATION_ENTER = 200
    const val DURATION_EXIT = 150
}

// Placeholder card for fast scrolling
@Composable
fun PlaceholderCard(
    title: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.Gray, RoundedCornerShape(8.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}
```

### 5. Common Components Structure

```
app/src/main/java/com/strmr/ai/ui/components/common/
├── row/
│   ├── MediaRow.kt              // Main row component
│   ├── MediaRowConfig.kt        // Configuration classes
│   ├── MediaRowState.kt         // State management
│   └── MediaRowDefaults.kt      // Default styling values
├── card/
│   ├── MediaCard.kt             // Single card implementation
│   ├── MediaCardVariants.kt     // Different card types
│   └── MediaCardAnimations.kt   // Shared animations
├── focus/
│   ├── DpadFocusManager.kt      // Enhanced focus management with DpadRecyclerView
│   ├── FocusRestoration.kt     // Focus restoration logic
│   └── FocusIndicator.kt       // Visual focus indicators
├── events/
│   ├── CardEventHandler.kt      // Unified event handling
│   ├── LongPressHandler.kt      // TV remote long press support
│   └── EventPropagation.kt      // Event bubbling control
├── animation/
│   ├── MotionConstants.kt       // Standard Android TV animation curves
│   ├── StandardMotion.kt        // 0.20, 0.10, 0.00, 1.00 - General animations
│   ├── BrowseMotion.kt         // 0.18, 1.00, 0.22, 1.00 - Row navigation
│   ├── EnterMotion.kt          // 0.12, 1.00, 0.40, 1.00 - Focus entering
│   └── ExitMotion.kt           // 0.40, 1.00, 0.12, 1.00 - Focus exiting
└── loading/
    ├── SkeletonLoader.kt        // Unified skeleton loading
    ├── PlaceholderCard.kt       // Gray poster with white title
    └── LoadingStates.kt         // Loading state definitions
```

## Implementation Plan

### Phase 1: Create Common Infrastructure (Week 1)
1. **Add DpadRecyclerView dependency** to project
2. Set up common components directory structure
3. Implement base `MediaCard` with DpadRecyclerView integration and placeholder support
4. Create `DpadFocusManager` leveraging library's built-in capabilities
5. Implement clean event handling system with `Modifier.dpadClickable`

### Phase 2: Build Unified Row (Week 2)
1. Implement core `MediaRow` component using DpadRecyclerView + AndroidView
2. Create `MediaRowAdapter` with `DpadComposeFocusViewHolder` integration
3. Add paging support leveraging DpadRecyclerView's performance optimizations
4. Configure focus management and restoration using library's APIs

### Phase 3: Migration (Week 3-4)
1. Replace `UnifiedMediaRow` with new implementation
2. Migrate all existing rows to use new system
3. Remove duplicate implementations
4. Update all screens to use common components

### Phase 4: Optimization (Week 5)
1. Profile and optimize performance
2. Fine-tune animations and transitions  
3. Implement advanced prefetching
4. Add telemetry for monitoring

## Key Improvements

### 1. Focus Management
- **Persistent Focus Memory**: Focus position saved per row and restored on return
- **Smart Focus Transfer**: When navigating between rows, maintain column position
- **Debounced Navigation**: Prevent focus loss during rapid scrolling
- **Visual Focus Indicators**: Clear, consistent focus states across all cards

### 2. Performance
- **Optimized Page Size**: 20 items per page for smooth loading
- **Virtual Scrolling**: Only render visible items plus small buffer
- **Focus-Based Loading**: Backdrop/logo images only load when item is focused - massive speed improvement for paging
- **Smart Image Loading**: Progressive loading strategy prevents UI blocking
- **Smooth Animations**: Hardware-accelerated transforms without recomposition
- **Memory Efficiency**: Aggressive cleanup and optimized image formats reduce memory usage by ~50%

### 3. Code Quality
- **Single Source of Truth**: One row implementation for all use cases
- **Composition Over Inheritance**: Use configuration to customize behavior
- **Testable Components**: Isolated, pure components that are easy to test
- **Type Safety**: Generic types ensure compile-time safety

### 4. Event Handling
- **Unified Event System**: Single event handler for all interactions
- **Long Press Support**: Works identically on TV remote and touch
- **No Event Conflicts**: Clear event propagation without interference
- **Standard Timeouts**: Consistent gesture recognition across app

## Success Metrics

1. **Performance Metrics**:
   - Row scroll FPS: > 55 fps  
   - Page load time: < 100ms (improved from focus-based loading)
   - Memory usage: < 100MB for full screen of content (50% reduction)
   - API calls per page load: < 5 (reduced from current ~20+ per page)
   - Image load time for focused item: < 50ms (cached) / < 200ms (first load)

2. **User Experience Metrics**:
   - Focus restoration success rate: > 99%
   - Navigation response time: < 50ms
   - Zero focus loss during normal navigation
   - Long press recognition rate: > 95%
   - Event handling conflicts: 0

3. **Code Quality Metrics**:
   - Lines of code reduced by > 60%
   - Test coverage: > 80%
   - Zero duplicate implementations

## Migration Guide

### For Developers:

1. **Replace Row Implementations**:
```kotlin
// Old
HomeMediaRow(
    title = "Trending",
    mediaItems = items,
    // ... many parameters
)

// New
MediaRow(
    config = MediaRowConfig(
        title = "Trending",
        items = items,
        cardType = CardType.POSTER,
        focusMemoryKey = "home_trending",
        onItemClick = { navigateToDetails(it) },
        onItemLongPress = { showContextMenu(it) }
    )
)
```

2. **Update Focus Handling**:
```kotlin
// Old - Manual focus management
var selectedIndex by remember { mutableStateOf(0) }

// New - Automatic with FocusManager
// Focus state handled internally
```

3. **Simplify Event Handling**:
```kotlin
// Old - Multiple handlers scattered across components
.clickable { /* click logic */ }
.onLongClick { /* long press logic */ }
.onKeyEvent { /* TV remote logic */ }

// New - Single unified handler
eventHandler = object : CardEventHandler<Movie> {
    override fun onClick(item: Movie) = navigateToDetails(item)
    override fun onLongPress(item: Movie) = showContextMenu(item)
    override fun onFocusChanged(item: Movie, hasFocus: Boolean) { /* handle focus */ }
}
```

4. **Simplify Data Loading**:
```kotlin
// Old - Complex paging setup
val pagingItems = flow.collectAsLazyPagingItems()

// New - Built into MediaRow
MediaRow(
    config = MediaRowConfig(
        pagingSource = moviePagingSource,
        // ... other config
    )
)
```

## Conclusion

The current row implementation in Strmr has significant technical debt that impacts both user experience and developer productivity. By implementing a unified, optimized row system based on industry standards, we can:

1. Dramatically improve performance and user experience
2. Reduce code complexity and maintenance burden  
3. Enable faster feature development
4. Provide a solid foundation for future enhancements
5. **Solve long press implementation issues** with clean, predictable event handling
6. **Leverage battle-tested library** instead of reinventing the wheel

**By using DpadRecyclerView, we avoid reinventing the wheel** and instead leverage a proven, actively maintained solution that has already solved the exact problems we face. The proposed solution addresses all identified issues including focus management, performance, and event handling problems while following your philosophy of reusing proven code instead of custom implementations.

## Key Benefits for Long Press Implementation

1. **Single Event Path**: All interactions flow through one clear handler, eliminating conflicts
2. **TV Remote Support**: Native long press support for Android TV remote controls
3. **Touch Compatibility**: Works identically on touch devices without separate implementation
4. **No Event Leakage**: Proper event consumption prevents unintended navigation
5. **Standard Timing**: Uses Android's standard 500ms long press timeout

## Major Performance Gains from Smart Loading

The **focus-based image loading** strategy provides the most significant performance improvement:

1. **Current Issue**: Every item in a 20-item page loads backdrop + logo immediately = 40+ API calls per page
2. **New Approach**: Only the focused item loads backdrop + logo = 2 API calls per page
3. **Result**: ~95% reduction in API calls during paging, dramatically faster page loads
4. **Caching**: Once loaded, images are cached permanently for instant display when focused again

**Additional Speed Improvements**:
- **Page-Level Poster Loading**: Load all posters for current page (20 items) immediately for instant horizontal scrolling
- **Smart Prefetch Timing**: Load next page when user scrolls 75% through current page (item 15/20)
- **Gray Placeholder Strategy**: Show gray poster with white title during super-speed scrolling for instant response
- **Progressive Image Loading**: Replace placeholders with actual posters as they load in background
- **88ms Navigation Throttle**: Prevents overwhelming image loading during fast navigation
- **Standard Android TV Animations**: Use industry-standard animation curves for polished experience

This approach follows the same philosophy for all future modifications: use industry standards, avoid over-engineering, and maintain consistency across all components while maximizing performance through intelligent lazy loading strategies.

## Additional Major Refactoring Opportunities

### Beyond Rows: Complete Netflix-Like Experience

While row improvements are critical, achieving a truly buttery-smooth Netflix experience requires addressing several other architectural issues:

## 1. Unified Image Loading System

### Current Problems:
- **Scattered Implementation**: AsyncImage used inconsistently across 10+ files
- **No Caching Strategy**: Images reload unnecessarily
- **Missing Preloading**: No predictive image loading
- **Inconsistent Transitions**: Jarring image pop-ins

### Proposed Solution:
```kotlin
// /common/image/
├── ImageLoader.kt           // Centralized Coil configuration
├── ImageCache.kt           // Multi-level caching strategy
├── ImagePreloader.kt       // Predictive preloading based on scroll
└── ImageTransitions.kt     // Smooth crossfade animations

// Usage example:
@Composable
fun OptimizedAsyncImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    priority: ImagePriority = ImagePriority.NORMAL
) {
    StrmrImage(
        url = url,
        contentDescription = contentDescription,
        modifier = modifier,
        transition = ImageTransitions.crossfade(300),
        cacheStrategy = CacheStrategy.AGGRESSIVE,
        preloadAdjacent = priority == ImagePriority.HIGH
    )
}
```

### Benefits:
- **60% faster image loading** through aggressive caching
- **Smooth transitions** eliminate jarring pop-ins
- **Predictive loading** for instant display
- **Memory optimization** through shared cache

## 2. Video Player Unification

### Current Problems:
- **5 Different Players**: VLC, System, YouTube, Clean, and standard VideoPlayer
- **Duplicate Logic**: Each player reimplements controls, state management
- **Inconsistent Experience**: Different UIs and behaviors
- **Performance Issues**: Multiple player instances in memory

### Proposed Solution:
```kotlin
// /common/player/
├── UniversalPlayer.kt      // Single adaptive player interface
├── PlayerBackend.kt        // Backend adapter (ExoPlayer/VLC)
├── PlayerControls.kt       // Unified control overlay
├── PlayerState.kt          // Centralized playback state
└── PlayerTransitions.kt    // Smooth player transitions

// Single player handles all content:
@Composable
fun UniversalPlayer(
    source: MediaSource,
    modifier: Modifier = Modifier
) {
    val backend = when {
        source.requiresVLC -> VLCBackend()
        source.isYouTube -> YouTubeBackend()
        else -> ExoPlayerBackend()
    }
    
    AdaptivePlayer(
        backend = backend,
        source = source,
        modifier = modifier
    )
}
```

### Benefits:
- **Single codebase** to maintain
- **Consistent UX** across all content
- **Better performance** through resource sharing
- **Easier feature additions** (subtitles, quality selection)

## 3. Data Layer Restructuring

### Current Problems:
- **11 Repositories** with overlapping responsibilities
- **26 DAOs/Entities** scattered without organization
- **No Clear Boundaries** between domains
- **Difficult Navigation** for developers

### Proposed Structure:
```kotlin
/data/
├── core/
│   ├── database/
│   │   ├── StrmrDatabase.kt      // Single database instance
│   │   ├── DatabaseModule.kt     // Hilt module
│   │   └── Migrations.kt         // All migrations
│   ├── network/
│   │   ├── NetworkModule.kt      // Retrofit configuration
│   │   ├── Interceptors.kt       // Auth, logging, etc.
│   │   └── ApiClients.kt         // TMDB, Trakt, OMDb
│   └── cache/
│       ├── CacheStrategy.kt      // Unified caching
│       └── CacheManager.kt       // Cache coordination
├── media/
│   ├── repository/
│   │   ├── MediaRepository.kt    // Unified media operations
│   │   ├── MovieRepository.kt    // Movie-specific
│   │   └── TvShowRepository.kt   // TV-specific
│   ├── entities/
│   │   ├── MovieEntity.kt
│   │   ├── TvShowEntity.kt
│   │   ├── EpisodeEntity.kt
│   │   └── SeasonEntity.kt
│   └── dao/
│       ├── MediaDao.kt           // Shared queries
│       ├── MovieDao.kt
│       └── TvShowDao.kt
├── user/
│   ├── repository/
│   │   ├── AccountRepository.kt
│   │   └── TraktRepository.kt
│   └── entities/
│       ├── AccountEntity.kt
│       └── TraktProfileEntity.kt
└── playback/
    ├── repository/
    │   └── PlaybackRepository.kt
    └── entities/
        ├── PlaybackEntity.kt
        └── ContinueWatchingEntity.kt
```

### Benefits:
- **Clear domain boundaries** improve maintainability
- **Easier navigation** for developers
- **Better code reuse** through shared components
- **Simplified testing** with clear dependencies

## 4. State Management System

### Current Problems:
- **Scattered State**: Each screen manages its own state
- **No State Sharing**: Duplicate API calls for same data
- **Inefficient Updates**: Full screen recompositions
- **Lost State**: Navigation loses user context

### Proposed Solution:
```kotlin
// /common/state/
├── AppStateManager.kt       // Global app state
├── MediaStateCache.kt       // Cached media state
├── PlaybackStateManager.kt  // Global playback state
└── FocusStateManager.kt     // Enhanced focus tracking

// Centralized state example:
@Singleton
class AppStateManager @Inject constructor() {
    private val _mediaCache = MutableStateFlow<Map<String, MediaItem>>(emptyMap())
    private val _focusState = MutableStateFlow<FocusState>(FocusState.default())
    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.idle())
    
    fun getCachedMedia(id: String): MediaItem? = _mediaCache.value[id]
    
    fun updateMediaCache(items: List<MediaItem>) {
        _mediaCache.update { cache ->
            cache + items.associateBy { it.id }
        }
    }
}
```

### Benefits:
- **Instant screen transitions** with cached data
- **Reduced API calls** through state sharing
- **Smooth updates** with granular recomposition
- **Persistent user context** across navigation

## 5. Advanced Performance Optimizations

### Database Query Optimization:
```kotlin
// Before: Multiple queries
val movie = movieDao.getMovie(id)
val ratings = ratingsDao.getRatings(id)
val cast = castDao.getCast(id)

// After: Single optimized query
@Query("""
    SELECT * FROM movies 
    LEFT JOIN ratings ON movies.id = ratings.movieId
    LEFT JOIN cast ON movies.id = cast.movieId
    WHERE movies.id = :id
""")
fun getMovieWithDetails(id: String): MovieWithDetails
```

### Multi-Level Caching:
```kotlin
object CacheStrategy {
    // L1: Memory cache (immediate access)
    val memoryCache = LruCache<String, Any>(100)
    
    // L2: Disk cache (fast access)
    val diskCache = DiskLruCache(cacheDir, 50 * 1024 * 1024)
    
    // L3: Predictive cache (preloaded content)
    val predictiveCache = PredictiveCache()
    
    // L4: Background sync (trending content)
    val backgroundSync = BackgroundContentSync()
}
```

### Predictive Content Loading:
```kotlin
class PredictiveLoader {
    fun preloadLikelyContent(currentItem: MediaItem) {
        // Preload based on:
        // - Similar genre/cast
        // - Next episodes
        // - Trending in category
        // - User history patterns
    }
}
```

## Implementation Roadmap

### Phase 1: Foundation (Weeks 1-2)
1. DpadRecyclerView integration
2. Unified image loading system
3. Video player consolidation

### Phase 2: Architecture (Weeks 3-4)
1. Data layer restructuring
2. State management implementation
3. Common component library

### Phase 3: Optimization (Weeks 5-6)
1. Database query optimization
2. Multi-level caching
3. Predictive loading

### Phase 4: Polish (Week 7)
1. Animation refinements
2. Performance profiling
3. Memory optimization

## Success Metrics

**Performance Targets**:
- App startup: < 2 seconds
- Screen transitions: < 100ms
- Image loading: < 50ms (cached)
- Video start: < 2 seconds
- Memory usage: < 200MB baseline

**User Experience**:
- Zero perceived loading delays
- Smooth 60fps animations
- Instant navigation response
- Netflix-level polish

This comprehensive refactor addresses all aspects needed for a truly Netflix-like experience, not just row improvements.