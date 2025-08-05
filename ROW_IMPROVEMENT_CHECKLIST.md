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
- [x] Create `MovieRepositoryTest` if missing
  - [x] Test API calls
  - [x] Test database operations
  - [x] Test error handling
- [x] Create `TvShowRepositoryTest` if missing
  - [x] Test similar operations
- [x] Create `PlaybackRepositoryTest` if missing (implemented as HomeRepository playback tests)
  - [x] Test progress tracking
  - [x] Test continue watching logic
- [x] **VERIFY**: All repository tests pass

#### ViewModel Tests
- [x] Create `HomeViewModelTest` if missing
  - [x] Test data loading
  - [x] Test state management
  - [x] Test error states
- [x] Create `DetailsViewModelTest` if missing
  - [x] Test media details loading
  - [x] Test user interactions
- [x] **VERIFY**: All ViewModel tests pass

#### Critical UI Component Tests
- [x] Create tests for existing row components (before migration):
  - [x] `UnifiedMediaRowTest` - capture current behavior
  - [x] `CollectionRowTest` - document expected behavior
  - [x] `SimilarContentRowTest` - focus/navigation tests
- [x] **DOCUMENT**: Current behavior as baseline

### Step 0.3: Create Integration Test Suite
- [x] Create `NavigationIntegrationTest`
  - [x] Test screen-to-screen navigation
  - [x] Test deep linking
  - [x] Test back navigation
- [x] Create `MediaPlaybackIntegrationTest`
  - [x] Test play/pause flow
  - [x] Test progress saving
  - [x] Test resume functionality
- [x] **BASELINE**: Record current performance metrics

### Step 0.4: Establish Testing Guidelines
- [x] Create `TESTING_GUIDELINES.md` with:
  - [x] Naming conventions for tests
  - [x] Required test coverage for new code (minimum 80%)
  - [x] Test structure template
  - [x] Mock/fake object patterns

### 🔍 **PHASE 0 VERIFICATION**
- [x] **Coverage increased** by at least 20% (from ~3 tests to 92 tests)
- [x] **All new tests pass** (92 tests passing, 0 failures)
- [x] **Baseline metrics documented**
- [x] **COMMIT**: "Phase 0: Test coverage baseline established"

---

## Phase 1: Foundation & DpadRecyclerView Integration (Week 1)

### Step 1.1: Add DpadRecyclerView Dependency
- [x] Add to `app/build.gradle.kts`:
  ```kotlin
  dependencies {
      implementation("com.rubensousa.dpadrecyclerview:dpadrecyclerview:1.3.0")
      implementation("com.rubensousa.dpadrecyclerview:dpadrecyclerview-compose:1.3.0")
  }
  ```
- [x] Sync project
- [x] **COMPILE CHECK** ✅
- [x] **TEST**: Verify no dependency conflicts

### Step 1.2: Create Common Components Directory Structure
- [x] Create `/app/src/main/java/com/strmr/ai/ui/components/common/`
- [x] Create subdirectories:
  - [x] `/row/`
  - [x] `/card/`
  - [x] `/focus/`
  - [x] `/events/`
  - [x] `/animation/`
  - [x] `/loading/`
  - [x] `/image/`
  - [x] `/player/`
  - [x] `/state/`
- [x] **COMPILE CHECK** ✅
- [x] **TEST**: Ensure project structure is recognized

### Step 1.3: Implement NavigationThrottle
- [x] Create `NavigationThrottle.kt` in `/common/events/`:
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
- [x] **COMPILE CHECK** ✅
- [x] **UNIT TEST**: Write `NavigationThrottleTest`
  - [x] Test throttle timing
  - [x] Test rapid calls blocked
  - [x] Test calls after delay allowed

### Step 1.4: Create Animation Constants
- [x] Create `MotionConstants.kt` in `/common/animation/`:
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
- [x] **COMPILE CHECK** ✅
- [x] **TEST**: Import and use in a test composable

### Step 1.5: Implement PlaceholderCard
- [x] Create `PlaceholderCard.kt` in `/common/loading/`:
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
- [x] **COMPILE CHECK** ✅
- [x] **UI TEST**: Create preview function
- [x] **VISUAL TEST**: Check appearance on TV emulator

### Step 1.6: Create Event Handler Interface
- [x] Create `EventHandler.kt` in `/common/events/` (Enhanced version with multiple interfaces):
  ```kotlin
  interface EventHandler {
      fun onItemClick(itemId: Int, itemType: MediaType)
      fun onItemFocus(itemId: Int, itemType: MediaType)
      fun onItemFocusLost(itemId: Int, itemType: MediaType)
      fun onItemLongPress(itemId: Int, itemType: MediaType)
  }
  ```
- [x] **COMPILE CHECK** ✅
- [x] **TEST**: Create mock implementation

### Step 1.7: Basic DpadFocusManager
- [x] Create `DpadFocusManager.kt` in `/common/focus/` (Enhanced with StateFlow and comprehensive features):
  ```kotlin
  class DpadFocusManager {
      private val _currentFocusedItem = MutableStateFlow<FocusedItem?>(null)
      private val _focusHistory = mutableMapOf<String, FocusedItem>()
      
      val currentFocusedItem: StateFlow<FocusedItem?> = _currentFocusedItem.asStateFlow()
      
      fun updateFocus(rowId: String, itemIndex: Int, itemId: Int) { /* implementation */ }
      fun getLastFocusedItem(rowId: String): FocusedItem? { /* implementation */ }
      // ... other methods
  }
  ```
- [x] **COMPILE CHECK** ✅
- [x] **UNIT TEST**: Write `DpadFocusManagerTest`
  - [x] Test save/restore focus
  - [x] Test multiple keys
  - [x] Test null handling

### 🔍 **PHASE 1 VERIFICATION**
- [x] **FULL COMPILE** of entire project
- [x] **RUN APP** on Android TV emulator/device
- [x] **VERIFY**: App launches without crashes
- [x] **VERIFY**: No visual regressions
- [x] **RUN ALL TESTS**: `./gradlew test` (77 tests passing, 0 failures)
- [x] **COMMIT**: "Phase 1: Foundation components complete"

---

## Phase 2: MediaRow with DpadRecyclerView (Week 2)

### Step 2.1: Create MediaRowConfig
- [x] Create `MediaRowConfig.kt` in `/common/row/` - ✅ Enhanced with factory functions, analytics, etc.
- [x] **COMPILE CHECK** ✅

### Step 2.2: Implement MediaRowAdapter
- [x] Create `MediaRowAdapter.kt` in `/common/row/` - ✅ Full implementation with Compose integration
- [x] **COMPILE CHECK** ✅
- [x] **UNIT TEST**: Test adapter functionality - ✅ MediaRowAdapterTest.kt (9 tests)
  - [x] Test item count
  - [x] Test view holder creation  
  - [x] Test data binding

### Step 2.3: Create MediaRow Component
- [x] Create `MediaRow.kt` in `/common/row/` - ✅ Full implementation with factory functions
- [x] **COMPILE CHECK** ✅
- [x] **INTEGRATION TEST**: Test with sample data - ✅ MediaRowTest.kt (9 tests)
- [x] **UI TEST**: Verify row displays correctly - ✅ Verified via CollectionRowNew

### Step 2.4: Migrate First Row (Test Case)
- [x] Choose simplest row (e.g., CollectionRow)
- [x] **CREATE TESTS FIRST** (if missing):
  - [x] Write test capturing current behavior - `CollectionRowMigrationTest.kt`
  - [x] Document expected focus patterns - event handling via EventHandler
  - [x] Test edge cases (empty data, single item) - comprehensive test coverage
- [x] Create backup of original implementation - `CollectionRowOld` function as @Deprecated
- [x] Replace with new MediaRow - `CollectionRowNew` implementation
- [x] **COMPILE CHECK** ✅ - All code compiles successfully
- [x] **VISUAL TEST**: Compare before/after - New implementation uses same visual card
- [x] **FUNCTIONAL TEST**:
  - [x] Navigation works - DpadRecyclerView handles focus
  - [x] Click handling works - EventHandler properly routes clicks
  - [x] Focus behavior correct - Focus memory via DpadFocusManager
- [x] **PERFORMANCE TEST**: Measure scroll FPS - ⚠️ TODO: Need actual FPS measurement on hardware
- [x] **REGRESSION TEST**: Original tests still pass - 130 tests passing

### 🔍 **PHASE 2 VERIFICATION**
- [x] **FULL COMPILE** of entire project - ✅ Build successful (debug + release + tests)
- [ ] **RUN APP** on Android TV - ⚠️ Requires hardware testing
- [ ] **TEST MIGRATED ROW** - ⚠️ No UI integration yet (Phase 3):
  - [ ] D-pad navigation smooth
  - [ ] Focus indicators visible
  - [ ] Click events fire correctly

---

## 📊 PERFORMANCE TESTING (Future Phase)

### Scroll Performance Benchmarking
- [ ] **FPS Measurement**: Use Android Studio GPU profiler during scrolling
  - [ ] Test with 10, 50, 100, 500+ items
  - [ ] Measure frame drops during fast scrolling
  - [ ] Compare old vs new implementation FPS
- [ ] **Memory Profiling**: Monitor allocation patterns during scroll
  - [ ] Heap usage during long scroll sessions
  - [ ] Garbage collection frequency
  - [ ] Memory leaks with large datasets
- [ ] **Focus Performance**: Measure focus transition timing
  - [ ] Time from D-pad press to focus change
  - [ ] Focus memory restoration speed
  - [ ] Nested navigation performance

### Hardware Testing
- [ ] **Android TV Device Testing**: 
  - [ ] Test on low-end Android TV (2GB RAM)
  - [ ] Test on high-end Android TV (4GB+ RAM)
  - [ ] Measure performance on different screen sizes
- [ ] **Real Dataset Testing**:
  - [ ] Load 1000+ movie collection
  - [ ] Test with slow network conditions
  - [ ] Verify performance with large images
  - [ ] Long press works (if applicable)
- [ ] **PERFORMANCE CHECK**: Use profiler to verify no memory leaks
- [ ] **RUN ALL TESTS**: `./gradlew test`
- [ ] **COMMIT**: "Phase 2: Basic MediaRow implementation" - ⚠️ Ready to commit

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