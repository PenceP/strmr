# MelodiqTV Android App Refactor Plan

## Executive Summary

This document provides a comprehensive analysis of the MelodiqTV Android application and outlines a strategic refactor plan focused on implementing industry-standard patterns for media row components and paging logic. The analysis reveals that the application currently lacks a unified media row component and proper paging implementation, presenting an opportunity for significant architectural improvements.

## Current State Analysis

### Architecture Overview

The MelodiqTV application follows a clean architecture pattern with:
- **Presentation Layer**: Jetpack Compose for Android TV using the Fudge TV library
- **Domain Layer**: Use cases with business logic
- **Data Layer**: Firebase Firestore for backend, with repository pattern implementation
- **Dependency Injection**: Hilt for DI
- **State Management**: Custom FudgeTvViewModel base class with UiState pattern

### Key Findings

#### 1. Missing UnifiedMediaRow Component
- **Finding**: There is no UnifiedMediaRow component in the codebase
- **Current Implementation**: Each screen implements its own row logic using standard `LazyRow`
- **Examples**:
  - Categories.kt uses `LazyRow` directly (line 48)
  - Home screen components implement separate row patterns
  - No reusable row component for consistent media display

#### 2. Absence of Paging Implementation
- **Finding**: No paging library or pagination logic found
- **Current Implementation**: 
  - ViewModels fetch complete data sets in single API calls
  - No use of Android Paging 3 library
  - Simple list-based state management (e.g., `favoriteSongs: List<SongBO>`)
- **Performance Risk**: Loading entire datasets could cause memory issues with large content libraries

#### 3. Fudge TV Library Limitations
- **FudgeTvCard**: Fixed size (196.dp x 158.25.dp), hardcoded styling
- **FudgeTvButton**: Complex conditional logic, limited customization
- **FudgeTvLazyVerticalGrid**: Uses deprecated Guava's Iterables API

### Current Data Flow Pattern

```kotlin
// Typical ViewModel pattern found (example from FavoritesViewModel)
executeUseCase(
    useCase = getFavoritesSongsByUserUseCase,
    onSuccess = ::onGetFavoritesSongsSuccessfully,
    onMapExceptionToState = ::onMapExceptionToState
)

// State update
private fun onGetFavoritesSongsSuccessfully(songList: List<SongBO>) {
    updateState { it.copy(favoriteSongs = songList) }
}
```

## Recommended Architecture

### 1. UnifiedMediaRow Component

#### Design Principles
- **Composable and Reusable**: Single source of truth for all media rows
- **Performance Optimized**: Lazy loading with proper key management
- **TV-First Design**: D-pad navigation, focus states, and smooth scrolling
- **Flexible Content**: Support for different media types (songs, artists, categories)

#### Proposed Implementation

```kotlin
@Composable
fun UnifiedMediaRow<T : MediaItem>(
    title: String,
    items: LazyPagingItems<T>,
    onItemClick: (T) -> Unit,
    onItemFocus: (T) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(horizontal = 32.dp),
    itemContent: @Composable (T) -> Unit
) {
    Column {
        // Title
        Text(
            text = title,
            modifier = Modifier.padding(start = contentPadding.calculateStartPadding(LayoutDirection.Ltr))
        )
        
        // Content Row
        TvLazyRow(
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            pivotOffsets = PivotOffsets(0.1f, 0.5f)
        ) {
            items(
                count = items.itemCount,
                key = items.itemKey { it.id },
                contentType = items.itemContentType()
            ) { index ->
                items[index]?.let { item ->
                    MediaItemWrapper(
                        item = item,
                        onClick = { onItemClick(item) },
                        onFocus = { onItemFocus(item) },
                        content = { itemContent(item) }
                    )
                }
            }
        }
    }
}
```

### 2. Paging Implementation Strategy

#### Technology Choice: Paging 3 Library

**Rationale**:
- Industry standard for Android pagination
- Built-in support for Compose
- Handles loading states, error handling, and retry logic
- Memory efficient with automatic page dropping

#### Implementation Approach

##### Step 1: Add Dependencies
```toml
[versions]
paging = "3.3.2"
paging-compose = "3.3.2"

[libraries]
androidx-paging = { module = "androidx.paging:paging-runtime", version.ref = "paging" }
androidx-paging-compose = { module = "androidx.paging:paging-compose", version.ref = "paging-compose" }
```

##### Step 2: Create PagingSource
```kotlin
class SongsPagingSource(
    private val songRepository: ISongRepository,
    private val filterParams: SongFilterParams
) : PagingSource<Int, SongBO>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SongBO> {
        return try {
            val page = params.key ?: 0
            val pageSize = params.loadSize
            
            val songs = songRepository.getSongsPaged(
                page = page,
                pageSize = pageSize,
                filters = filterParams
            )
            
            LoadResult.Page(
                data = songs,
                prevKey = if (page > 0) page - 1 else null,
                nextKey = if (songs.size == pageSize) page + 1 else null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
```

##### Step 3: Update Repository Pattern
```kotlin
interface ISongRepository {
    suspend fun getSongsPaged(
        page: Int,
        pageSize: Int,
        filters: SongFilterParams
    ): List<SongBO>
}
```

##### Step 4: ViewModel Integration
```kotlin
@HiltViewModel
class SongHubViewModel @Inject constructor(
    private val songRepository: ISongRepository
) : ViewModel() {
    
    private val filterState = MutableStateFlow(SongFilterParams())
    
    val songsPager = filterState.flatMapLatest { filters ->
        Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                initialLoadSize = 20
            ),
            pagingSourceFactory = { SongsPagingSource(songRepository, filters) }
        ).flow.cachedIn(viewModelScope)
    }
}
```

### 3. FudgeCard Optimization

#### Issues Identified
- Fixed dimensions limiting flexibility
- Hardcoded aspect ratios
- No loading states or placeholders

#### Proposed Improvements
```kotlin
@Composable
fun EnhancedFudgeCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    imageUrl: String,
    title: String,
    subtitle: String,
    cardSize: DpSize = DpSize(196.dp, 158.dp),
    cardAspectRatio: Float? = null,
    showLoadingState: Boolean = false
) {
    Card(
        modifier = modifier
            .then(
                if (cardAspectRatio != null) {
                    Modifier.aspectRatio(cardAspectRatio)
                } else {
                    Modifier.size(cardSize)
                }
            ),
        onClick = onClick
    ) {
        if (showLoadingState) {
            // Loading placeholder
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        } else {
            // Content implementation
        }
    }
}
```

### 4. FudgeButton Optimization

#### Issues Identified
- Complex conditional logic reducing readability
- Limited style customization
- Performance overhead from nested conditionals

#### Proposed Improvements
```kotlin
// Create style presets
object FudgeButtonStyles {
    val Primary = ButtonStyle(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        borderColor = MaterialTheme.colorScheme.onPrimaryContainer
    )
    
    val Secondary = ButtonStyle(
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        borderColor = MaterialTheme.colorScheme.onSecondaryContainer
    )
}

// Simplified button implementation
@Composable
fun EnhancedFudgeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = FudgeButtonStyles.Primary,
    size: ButtonSize = ButtonSize.Medium,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.size(size.dimensions),
        enabled = enabled,
        colors = ButtonDefaults.colors(
            containerColor = style.containerColor,
            contentColor = style.contentColor
        )
    ) {
        Text(text = text)
    }
}
```

## Migration Strategy

### Phase 1: Foundation (Week 1)
1. Add Paging 3 dependencies
2. Create base interfaces for paging support
3. Implement UnifiedMediaRow component
4. Create comprehensive unit tests

### Phase 2: Repository Layer (Week 2)
1. Update repository interfaces with paging support
2. Implement PagingSource for each content type
3. Add caching layer for performance
4. Update Firebase queries for pagination

### Phase 3: ViewModel Migration (Week 3)
1. Migrate one ViewModel at a time (start with SongHubViewModel)
2. Replace list-based state with PagingData flows
3. Update error handling for paging scenarios
4. Add retry mechanisms

### Phase 4: UI Integration (Week 4)
1. Replace LazyRow implementations with UnifiedMediaRow
2. Update screens to use LazyPagingItems
3. Implement loading and error states
4. Add pull-to-refresh functionality

### Phase 5: Component Optimization (Week 5)
1. Create enhanced versions of FudgeCard and FudgeButton
2. Gradually replace existing usages
3. Performance testing and optimization
4. Update theme and styling

## Testing Strategy

### Unit Tests
- PagingSource implementations
- Repository paging methods
- ViewModel paging logic
- Component behavior

### Integration Tests
- End-to-end paging flow
- Error handling scenarios
- Network failure recovery
- Memory leak detection

### UI Tests
- Focus navigation in UnifiedMediaRow
- Scroll performance
- Loading state transitions
- Error state handling

## Performance Metrics

### Target Improvements
- **Memory Usage**: 40% reduction through proper paging
- **Initial Load Time**: 50% faster with smaller page sizes
- **Scroll Performance**: Maintain 60fps during fast scrolling
- **Network Efficiency**: 60% reduction in data transfer

### Monitoring
- Firebase Performance Monitoring for network calls
- Android Studio Profiler for memory analysis
- Custom metrics for page load times
- User engagement analytics

## Risk Mitigation

### Potential Risks
1. **Firebase Query Limitations**: May need backend changes for efficient pagination
2. **Focus Management**: Complex TV navigation with dynamic content
3. **State Persistence**: Maintaining scroll position across configuration changes
4. **Backwards Compatibility**: Ensuring existing features continue to work

### Mitigation Strategies
1. **Incremental Migration**: One screen at a time
2. **Feature Flags**: Toggle between old and new implementations
3. **Comprehensive Testing**: Automated test suite
4. **Rollback Plan**: Git branching strategy for quick reversion

## Success Criteria

1. **Technical Success**
   - All content screens use UnifiedMediaRow
   - Paging implemented for all list-based content
   - Memory usage reduced by target metrics
   - No regression in existing functionality

2. **User Experience Success**
   - Smoother scrolling performance
   - Faster content loading
   - Consistent UI across all screens
   - Improved focus navigation

3. **Code Quality Success**
   - Reduced code duplication by 70%
   - Improved testability with 80% coverage
   - Clear separation of concerns
   - Documentation for all new components

## Next Steps

1. **Review and Approval**: Team review of this plan
2. **Proof of Concept**: Implement UnifiedMediaRow with one screen
3. **Performance Baseline**: Measure current metrics
4. **Development Environment**: Set up feature branch
5. **Begin Phase 1**: Start foundation implementation

## Questions for Clarification

1. **Backend Pagination Support**: Does the Firebase backend support offset/limit queries efficiently? 
   - I do not know, but we need to use STANDARD solutions to this problem.
2. **Design Specifications**: Are there specific design requirements for loading states and error handling?
   - No, just whatever STANDARD solutions fit best
3. **Performance Targets**: Are the proposed metrics aligned with business goals?
   - yes, overall goal is faster execution, better, more standardized code that Google & Netflix would be proud of.
4. **Release Strategy**: Should this be released incrementally or as one major update?
   - Since we are not even in Beta, it doesn't matter, but i will do a commit after every major task completion.
5. **A/B Testing**: Should we implement analytics to compare old vs new implementation?
   - yes!

---

This refactor plan provides a clear path forward for modernizing the MelodiqTV application with industry-standard patterns while maintaining the existing functionality and improving performance.