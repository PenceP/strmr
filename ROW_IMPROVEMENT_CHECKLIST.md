# ROW_IMPROVEMENT_CHECKLIST.md

## Implementation Checklist for Netflix-Like Row & App Improvements

This checklist provides a step-by-step implementation guide based on ROW_IMPROVEMENTS.md. Each phase includes compilation checks, testing requirements, and verification steps.

---

## 🚨 CRITICAL RULES
- ✅ **COMPILE** after EVERY step
- 🧪 **TEST** all changes before proceeding
- 📱 **RUN ON DEVICE** after each phase
- 🔄 **COMMIT** working code frequently
- ⚠️ **ROLLBACK** if tests fail

---

## Phase 0: Test Coverage Assessment & Creation

### Step 0.1: Analyze Current Test Coverage
- [x] Run coverage report: `./gradlew testDebugUnitTestCoverage` (Note: task not available, used `./gradlew test` instead)
- [x] Document current coverage percentage: **VERY LOW** - Only 3 test files found
- [x] Identify critical untested components:
  - [x] ViewModels without tests (13 found, 0 tested):
    - HomeViewModel ⚠️ CRITICAL
    - DetailsViewModel ⚠️ CRITICAL
    - SearchViewModel ⚠️ CRITICAL
    - GenericMoviesViewModel
    - GenericTvShowsViewModel
    - IntermediateViewViewModel
    - OnboardingViewModel
    - PremiumizeSettingsViewModel
    - SettingsViewModel
    - StreamSelectionViewModel
    - UpdateViewModel
    - BaseConfigurableViewModel (abstract)
    - BaseMediaViewModel (abstract)
  - [x] Repositories without tests (10 found, 1 tested):
    - MovieRepository ⚠️ CRITICAL
    - TvShowRepository ⚠️ CRITICAL
    - HomeRepository ⚠️ CRITICAL
    - AccountRepository
    - GenericTraktRepository
    - IntermediateViewRepository
    - OmdbRepository
    - ScraperRepository
    - SearchRepository
    - UpdateRepository
  - [x] Complex UI components without tests:
    - UnifiedMediaRow (451 lines) ⚠️ CRITICAL
    - CollectionRow (160 lines)
    - SimilarContentRow (161 lines)
  - [x] Navigation logic without tests (No test files found)

### Step 0.2: Create Tests for Critical Existing Components
**Before refactoring, ensure safety net exists:**

#### Repository Tests
- [ ] Create `MovieRepositoryTest` if missing
  - [ ] Test API calls
  - [ ] Test database operations
  - [ ] Test error handling
- [ ] Create `TvShowRepositoryTest` if missing
  - [ ] Test similar operations
- [ ] Create `PlaybackRepositoryTest` if missing
  - [ ] Test progress tracking
  - [ ] Test continue watching logic
- [ ] **VERIFY**: All repository tests pass

#### ViewModel Tests
- [ ] Create `HomeViewModelTest` if missing
  - [ ] Test data loading
  - [ ] Test state management
  - [ ] Test error states
- [ ] Create `DetailsViewModelTest` if missing
  - [ ] Test media details loading
  - [ ] Test user interactions
- [ ] **VERIFY**: All ViewModel tests pass

#### Critical UI Component Tests
- [ ] Create tests for existing row components (before migration):
  - [ ] `UnifiedMediaRowTest` - capture current behavior
  - [ ] `CollectionRowTest` - document expected behavior
  - [ ] `SimilarContentRowTest` - focus/navigation tests
- [ ] **DOCUMENT**: Current behavior as baseline

### Step 0.3: Create Integration Test Suite
- [ ] Create `NavigationIntegrationTest`
  - [ ] Test screen-to-screen navigation
  - [ ] Test deep linking
  - [ ] Test back navigation
- [ ] Create `MediaPlaybackIntegrationTest`
  - [ ] Test play/pause flow
  - [ ] Test progress saving
  - [ ] Test resume functionality
- [ ] **BASELINE**: Record current performance metrics

### Step 0.4: Establish Testing Guidelines
- [ ] Create `TESTING_GUIDELINES.md` with:
  - [ ] Naming conventions for tests
  - [ ] Required test coverage for new code (minimum 80%)
  - [ ] Test structure template
  - [ ] Mock/fake object patterns

### 🔍 **PHASE 0 VERIFICATION**
- [ ] **Coverage increased** by at least 20%
- [ ] **All new tests pass**
- [ ] **Baseline metrics documented**
- [ ] **COMMIT**: "Phase 0: Test coverage baseline established"

---

## Phase 1: Foundation & DpadRecyclerView Integration (Week 1)

### Step 1.1: Add DpadRecyclerView Dependency
- [ ] Add to `app/build.gradle.kts`:
  ```kotlin
  dependencies {
      implementation("com.rubensousa.dpadrecyclerview:dpadrecyclerview:1.3.0")
      implementation("com.rubensousa.dpadrecyclerview:dpadrecyclerview-compose:1.3.0")
  }
  ```
- [ ] Sync project
- [ ] **COMPILE CHECK** ✅
- [ ] **TEST**: Verify no dependency conflicts

### Step 1.2: Create Common Components Directory Structure
- [ ] Create `/app/src/main/java/com/strmr/ai/ui/components/common/`
- [ ] Create subdirectories:
  - [ ] `/row/`
  - [ ] `/card/`
  - [ ] `/focus/`
  - [ ] `/events/`
  - [ ] `/animation/`
  - [ ] `/loading/`
  - [ ] `/image/`
  - [ ] `/player/`
  - [ ] `/state/`
- [ ] **COMPILE CHECK** ✅
- [ ] **TEST**: Ensure project structure is recognized

### Step 1.3: Implement NavigationThrottle
- [ ] Create `NavigationThrottle.kt` in `/common/events/`:
  ```kotlin
  object NavigationThrottle {
      private var lastNavigationTime = 0L
      private const val NAVIGATION_THROTTLE_MS = 88L
      
      fun canNavigate(): Boolean {
          val now = System.currentTimeMillis()
          return if (now - lastNavigationTime >= NAVIGATION_THROTTLE_MS) {
              lastNavigationTime = now
              true
          } else false
      }
  }
  ```
- [ ] **COMPILE CHECK** ✅
- [ ] **UNIT TEST**: Write `NavigationThrottleTest`
  - [ ] Test throttle timing
  - [ ] Test rapid calls blocked
  - [ ] Test calls after delay allowed

### Step 1.4: Create Animation Constants
- [ ] Create `MotionConstants.kt` in `/common/animation/`:
  ```kotlin
  object MotionConstants {
      val Standard = CubicBezierEasing(0.2f, 0.1f, 0.0f, 1.0f)
      val Browse = CubicBezierEasing(0.18f, 1.0f, 0.22f, 1.0f)
      val Enter = CubicBezierEasing(0.12f, 1.0f, 0.40f, 1.0f)
      val Exit = CubicBezierEasing(0.40f, 1.0f, 0.12f, 1.0f)
      
      const val DURATION_STANDARD = 300
      const val DURATION_BROWSE = 250
      const val DURATION_ENTER = 200
      const val DURATION_EXIT = 150
  }
  ```
- [ ] **COMPILE CHECK** ✅
- [ ] **TEST**: Import and use in a test composable

### Step 1.5: Implement PlaceholderCard
- [ ] Create `PlaceholderCard.kt` in `/common/loading/`:
  ```kotlin
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
- [ ] **COMPILE CHECK** ✅
- [ ] **UI TEST**: Create preview function
- [ ] **VISUAL TEST**: Check appearance on TV emulator

### Step 1.6: Create Event Handler Interface
- [ ] Create `CardEventHandler.kt` in `/common/events/`:
  ```kotlin
  interface CardEventHandler<T> {
      fun onClick(item: T)
      fun onLongPress(item: T)
      fun onFocusChanged(item: T, hasFocus: Boolean)
      suspend fun onLoadDetailImages(item: T)
  }
  ```
- [ ] **COMPILE CHECK** ✅
- [ ] **TEST**: Create mock implementation

### Step 1.7: Basic DpadFocusManager
- [ ] Create `DpadFocusManager.kt` in `/common/focus/`:
  ```kotlin
  class DpadFocusManager {
      private val focusStates = mutableMapOf<String, FocusState>()
      
      data class FocusState(
          val rowKey: String,
          val itemIndex: Int,
          val scrollOffset: Float,
          val timestamp: Long
      )
      
      fun saveFocus(key: String, state: FocusState) {
          focusStates[key] = state
      }
      
      fun restoreFocus(key: String): FocusState? = focusStates[key]
  }
  ```
- [ ] **COMPILE CHECK** ✅
- [ ] **UNIT TEST**: Write `DpadFocusManagerTest`
  - [ ] Test save/restore focus
  - [ ] Test multiple keys
  - [ ] Test null handling

### 🔍 **PHASE 1 VERIFICATION**
- [ ] **FULL COMPILE** of entire project
- [ ] **RUN APP** on Android TV emulator/device
- [ ] **VERIFY**: App launches without crashes
- [ ] **VERIFY**: No visual regressions
- [ ] **RUN ALL TESTS**: `./gradlew test`
- [ ] **COMMIT**: "Phase 1: Foundation components complete"

---

## Phase 2: MediaRow with DpadRecyclerView (Week 2)

### Step 2.1: Create MediaRowConfig
- [ ] Create `MediaRowConfig.kt` in `/common/row/`:
  ```kotlin
  data class MediaRowConfig<T>(
      val title: String,
      val items: List<T>,
      val pagingSource: PagingSource<Int, T>? = null,
      val cardType: CardType,
      val focusMemoryKey: String,
      val onItemClick: (T) -> Unit,
      val onItemLongPress: ((T) -> Unit)? = null,
      val longPressTimeout: Long = 500L
  )
  
  enum class CardType {
      POSTER, LANDSCAPE, SQUARE, CIRCLE
  }
  ```
- [ ] **COMPILE CHECK** ✅

### Step 2.2: Implement MediaRowAdapter
- [ ] Create `MediaRowAdapter.kt` in `/common/row/`:
  ```kotlin
  class MediaRowAdapter<T>(
      private val config: MediaRowConfig<T>
  ) : RecyclerView.Adapter<DpadComposeFocusViewHolder>() {
      // Implementation
  }
  ```
- [ ] **COMPILE CHECK** ✅
- [ ] **UNIT TEST**: Test adapter functionality
  - [ ] Test item count
  - [ ] Test view holder creation
  - [ ] Test data binding

### Step 2.3: Create MediaRow Component
- [ ] Create `MediaRow.kt` in `/common/row/`:
  ```kotlin
  @Composable
  fun <T> MediaRow(
      config: MediaRowConfig<T>,
      modifier: Modifier = Modifier
  ) {
      AndroidView(
          factory = { context ->
              DpadRecyclerView(context).apply {
                  layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                  setHasFixedSize(true)
                  adapter = MediaRowAdapter(config)
                  setLayoutWhileScrollingEnabled(false)
              }
          },
          modifier = modifier
      )
  }
  ```
- [ ] **COMPILE CHECK** ✅
- [ ] **INTEGRATION TEST**: Test with sample data
- [ ] **UI TEST**: Verify row displays correctly

### Step 2.4: Migrate First Row (Test Case)
- [ ] Choose simplest row (e.g., CollectionRow)
- [ ] **CREATE TESTS FIRST** (if missing):
  - [ ] Write test capturing current behavior
  - [ ] Document expected focus patterns
  - [ ] Test edge cases (empty data, single item)
- [ ] Create backup of original implementation
- [ ] Replace with new MediaRow
- [ ] **COMPILE CHECK** ✅
- [ ] **VISUAL TEST**: Compare before/after
- [ ] **FUNCTIONAL TEST**:
  - [ ] Navigation works
  - [ ] Click handling works
  - [ ] Focus behavior correct
- [ ] **PERFORMANCE TEST**: Measure scroll FPS
- [ ] **REGRESSION TEST**: Original tests still pass

### 🔍 **PHASE 2 VERIFICATION**
- [ ] **FULL COMPILE** of entire project
- [ ] **RUN APP** on Android TV
- [ ] **TEST MIGRATED ROW**:
  - [ ] D-pad navigation smooth
  - [ ] Focus indicators visible
  - [ ] Click events fire correctly
  - [ ] Long press works (if applicable)
- [ ] **PERFORMANCE CHECK**: Use profiler to verify no memory leaks
- [ ] **RUN ALL TESTS**: `./gradlew test`
- [ ] **COMMIT**: "Phase 2: Basic MediaRow implementation"

---

## Phase 3: Image Loading System (Week 3)

### Step 3.1: Configure Coil
- [ ] Create `ImageLoader.kt` in `/common/image/`:
  ```kotlin
  @Singleton
  class StrmrImageLoader @Inject constructor(
      @ApplicationContext context: Context
  ) {
      val imageLoader = ImageLoader.Builder(context)
          .memoryCache {
              MemoryCache.Builder(context)
                  .maxSizePercent(0.25)
                  .build()
          }
          .diskCache {
              DiskCache.Builder()
                  .directory(context.cacheDir.resolve("image_cache"))
                  .maxSizeBytes(100 * 1024 * 1024) // 100MB
                  .build()
          }
          .crossfade(true)
          .build()
  }
  ```
- [ ] **COMPILE CHECK** ✅
- [ ] **UNIT TEST**: Verify cache configuration

### Step 3.2: Create StrmrImage Composable
- [ ] Create `StrmrImage.kt` in `/common/image/`:
  ```kotlin
  @Composable
  fun StrmrImage(
      url: String?,
      contentDescription: String?,
      modifier: Modifier = Modifier,
      placeholder: @Composable (() -> Unit)? = null
  ) {
      // Implementation with placeholder support
  }
  ```
- [ ] **COMPILE CHECK** ✅
- [ ] **UI TEST**: Test with various URLs
- [ ] **TEST**: Placeholder shows during loading
- [ ] **TEST**: Error state handled gracefully

### Step 3.3: Replace AsyncImage Usage
- [ ] Find all AsyncImage usages: `grep -r "AsyncImage" app/src/`
- [ ] Replace ONE instance with StrmrImage
- [ ] **COMPILE CHECK** ✅
- [ ] **VISUAL TEST**: Verify image loads correctly
- [ ] **PERFORMANCE TEST**: Check load time
- [ ] Repeat for remaining instances (compile after each)

### Step 3.4: Implement Image Preloader
- [ ] Create `ImagePreloader.kt` in `/common/image/`:
  ```kotlin
  class ImagePreloader @Inject constructor(
      private val imageLoader: StrmrImageLoader
  ) {
      fun preloadImages(urls: List<String>) {
          urls.forEach { url ->
              imageLoader.enqueue(
                  ImageRequest.Builder(context)
                      .data(url)
                      .build()
              )
          }
      }
  }
  ```
- [ ] **COMPILE CHECK** ✅
- [ ] **INTEGRATION TEST**: Verify preloading works

### 🔍 **PHASE 3 VERIFICATION**
- [ ] **FULL COMPILE** of entire project
- [ ] **VISUAL REGRESSION TEST**:
  - [ ] All images still load
  - [ ] Placeholders show correctly
  - [ ] No broken images
- [ ] **PERFORMANCE TEST**:
  - [ ] Measure image load times
  - [ ] Check memory usage
  - [ ] Verify cache hit rate
- [ ] **RUN ALL TESTS**: `./gradlew test`
- [ ] **COMMIT**: "Phase 3: Unified image loading system"

---

## Phase 4: Complete Row Migration (Week 4)

### Step 4.1: Migrate HomePage Rows
- [ ] Backup original HomePage.kt
- [ ] Replace Continue Watching row
  - [ ] **COMPILE CHECK** ✅
  - [ ] **TEST**: Navigation, focus, clicks
- [ ] Replace Trending row
  - [ ] **COMPILE CHECK** ✅
  - [ ] **TEST**: Paging works correctly
- [ ] Replace remaining rows one by one
- [ ] **FULL PAGE TEST**: All rows work together

### Step 4.2: Migrate MoviesPage
- [ ] Backup original MoviesPage.kt
- [ ] Replace all movie rows with MediaRow
- [ ] **COMPILE CHECK** ✅
- [ ] **TEST**: Page loads, navigation smooth
- [ ] **PAGING TEST**: Verify infinite scroll

### Step 4.3: Migrate TvShowsPage
- [ ] Similar process as MoviesPage
- [ ] **COMPILE CHECK** ✅
- [ ] **INTEGRATION TEST**: Shows load correctly

### Step 4.4: Remove Old Row Implementations
- [ ] Delete UnifiedMediaRow.kt
- [ ] Delete CollectionRow.kt
- [ ] Delete SimilarContentRow.kt
- [ ] **COMPILE CHECK** ✅
- [ ] **VERIFY**: No broken imports

### 🔍 **PHASE 4 VERIFICATION**
- [ ] **FULL APP TEST**:
  - [ ] Navigate through all screens
  - [ ] Test all row types
  - [ ] Verify focus memory works
  - [ ] Check performance metrics
- [ ] **REGRESSION TEST SUITE**: Run all existing tests
- [ ] **MEMORY PROFILER**: Check for leaks
- [ ] **COMMIT**: "Phase 4: Complete row migration"

---

## Phase 5: Video Player Unification (Week 5)

### Step 5.1: Create Player Interfaces
- [ ] Create `PlayerBackend.kt` in `/common/player/`:
  ```kotlin
  interface PlayerBackend {
      fun prepare(source: MediaSource)
      fun play()
      fun pause()
      fun release()
      fun seekTo(position: Long)
  }
  ```
- [ ] **COMPILE CHECK** ✅

### Step 5.2: Implement ExoPlayerBackend
- [ ] Create `ExoPlayerBackend.kt`
- [ ] Implement PlayerBackend interface
- [ ] **COMPILE CHECK** ✅
- [ ] **UNIT TEST**: Test all player functions

### Step 5.3: Implement VLCBackend
- [ ] Create `VLCBackend.kt`
- [ ] Implement PlayerBackend interface
- [ ] **COMPILE CHECK** ✅
- [ ] **UNIT TEST**: Test VLC-specific features

### Step 5.4: Create UniversalPlayer
- [ ] Create `UniversalPlayer.kt` in `/common/player/`
- [ ] Implement adaptive backend selection
- [ ] **COMPILE CHECK** ✅
- [ ] **INTEGRATION TEST**: Test backend switching
- [ ] **UI TEST**: Verify controls work

### Step 5.5: Migrate VideoPlayerScreen
- [ ] Replace existing player with UniversalPlayer
- [ ] **COMPILE CHECK** ✅
- [ ] **FUNCTIONAL TEST**:
  - [ ] Play various formats
  - [ ] Test controls
  - [ ] Verify seek functionality
  - [ ] Check subtitle support

### 🔍 **PHASE 5 VERIFICATION**
- [ ] **VIDEO PLAYBACK TEST**:
  - [ ] Test HLS streams
  - [ ] Test MP4 files
  - [ ] Test YouTube content
  - [ ] Verify smooth backend switching
- [ ] **PERFORMANCE**: No stuttering or delays
- [ ] **COMMIT**: "Phase 5: Unified video player"

---

## Phase 6: Data Layer Restructure (Week 6)

### Step 6.1: Create New Directory Structure
- [ ] Create `/data/core/`, `/data/media/`, `/data/user/`, `/data/playback/`
- [ ] **COMPILE CHECK** ✅ (should still compile)

### Step 6.2: Move Database Classes
- [ ] Move StrmrDatabase to `/data/core/database/`
- [ ] **COMPILE CHECK** ✅
- [ ] Update imports across project
- [ ] **TEST**: Database still initializes

### Step 6.3: Consolidate Media Entities
- [ ] Move MovieEntity, TvShowEntity to `/data/media/entities/`
- [ ] **COMPILE CHECK** ✅
- [ ] Update all imports
- [ ] **DATABASE TEST**: Queries still work

### Step 6.4: Consolidate Repositories
- [ ] Group related repositories by domain
- [ ] **COMPILE CHECK** ✅
- [ ] **INTEGRATION TEST**: API calls work
- [ ] **UNIT TEST**: Repository functions

### 🔍 **PHASE 6 VERIFICATION**
- [ ] **FULL REGRESSION TEST**: All features work
- [ ] **DATABASE MIGRATION TEST**: No data loss
- [ ] **API TEST**: All endpoints reachable
- [ ] **COMMIT**: "Phase 6: Data layer restructured"

---

## Phase 7: State Management (Week 7)

### Step 7.1: Create AppStateManager
- [ ] Create `AppStateManager.kt` in `/common/state/`
- [ ] Implement as Singleton with Hilt
- [ ] **COMPILE CHECK** ✅
- [ ] **UNIT TEST**: State management functions

### Step 7.2: Integrate with ViewModels
- [ ] Update one ViewModel to use AppStateManager
- [ ] **COMPILE CHECK** ✅
- [ ] **TEST**: State persists correctly
- [ ] **TEST**: No duplicate API calls

### Step 7.3: Migrate All ViewModels
- [ ] Update remaining ViewModels one by one
- [ ] **COMPILE CHECK** after each
- [ ] **INTEGRATION TEST**: Cross-screen state

### 🔍 **PHASE 7 VERIFICATION**
- [ ] **STATE PERSISTENCE TEST**:
  - [ ] Navigate between screens
  - [ ] Verify data cached
  - [ ] Check focus restored
- [ ] **PERFORMANCE**: Faster screen loads
- [ ] **COMMIT**: "Phase 7: Centralized state management"

---

## Final Integration Testing

### System-Wide Tests
- [ ] **FULL APP NAVIGATION**: Test every screen
- [ ] **PERFORMANCE BENCHMARK**:
  - [ ] App startup time < 2s
  - [ ] Screen transitions < 100ms
  - [ ] Row scroll @ 60fps
  - [ ] Image load < 50ms (cached)
- [ ] **MEMORY PROFILING**: No leaks after 30min use
- [ ] **NETWORK PROFILING**: Reduced API calls
- [ ] **CRASH TESTING**: Monkey test for stability

### User Acceptance Tests
- [ ] **VISUAL POLISH**: Animations smooth
- [ ] **FOCUS BEHAVIOR**: Predictable and smooth
- [ ] **LOADING STATES**: No jarring transitions
- [ ] **ERROR HANDLING**: Graceful failures

### Documentation
- [ ] Update README with new architecture
- [ ] Document common components API
- [ ] Create migration guide for future devs

---

## 🎉 **FINAL VERIFICATION**
- [ ] **ALL TESTS PASS**: `./gradlew test`
- [ ] **LINT CLEAN**: `./gradlew lint`
- [ ] **NO WARNINGS**: Build output clean
- [ ] **PERFORMANCE TARGETS MET**: Per success metrics
- [ ] **FINAL COMMIT**: "Netflix-like experience complete"

---

## Rollback Procedures

If any phase fails:
1. **IMMEDIATE**: `git stash` current changes
2. **CHECKOUT**: Last working commit
3. **ANALYZE**: What broke and why
4. **FIX**: Address root cause
5. **RETRY**: Smaller incremental change

## Testing Philosophy & Requirements

### When to Create Tests

**ALWAYS create tests when:**
- 🆕 Adding new functionality
- 🔧 Fixing bugs (test to prevent regression)
- 🔄 Refactoring existing code
- 🚨 Finding untested critical paths
- 📊 Coverage drops below 80% for a module

### Test Creation Checklist

For **EVERY** new component:
- [ ] **Unit Test**: Test in isolation
- [ ] **Integration Test**: Test with dependencies
- [ ] **Edge Cases**: Empty data, nulls, errors
- [ ] **Performance Test**: If performance critical

For **EVERY** bug fix:
- [ ] **Failing Test**: Write test that reproduces bug
- [ ] **Fix Implementation**: Make test pass
- [ ] **Regression Test**: Ensure it stays fixed

For **EVERY** refactor:
- [ ] **Baseline Test**: Capture current behavior
- [ ] **Refactor Code**: With confidence
- [ ] **Verify Tests**: All still pass

### Testing Standards

**Minimum Coverage Requirements:**
- New code: 80% coverage
- Critical paths: 95% coverage
- UI components: Snapshot tests + interaction tests
- Business logic: 100% coverage

**Test Naming Convention:**
```kotlin
@Test
fun `methodName - given condition - should expected behavior`() {
    // Example:
    // `loadMovies - given network error - should show error state`
}
```

## Testing Commands Reference

```bash
# Run all unit tests
./gradlew test

# Run specific test class
./gradlew test --tests "com.strmr.ai.NavigationThrottleTest"

# Run with coverage
./gradlew testDebugUnitTestCoverage

# Run instrumented tests
./gradlew connectedAndroidTest

# Check for memory leaks
./gradlew leakCanary

# Performance profiling
# Use Android Studio Profiler during manual testing

# Coverage report location
# app/build/reports/coverage/testDebugUnitTest/html/index.html
```

## Success Criteria

Each phase is complete when:
- ✅ All code compiles without errors
- ✅ All tests pass
- ✅ No visual regressions
- ✅ Performance targets met
- ✅ No memory leaks detected
- ✅ Code committed to version control